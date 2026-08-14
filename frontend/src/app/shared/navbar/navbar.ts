import { Component, ElementRef, HostListener, inject, signal, viewChild } from '@angular/core';
import { Quote, QuoteService } from '../../core/services/quote.service';
import { AuthService } from '../../core/services/auth.service';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError } from '../../core/interceptors/error.interceptor';

type SearchField = 'ticker';
type NavDestination = '/profile' | '/transactions' | '/performance';

@Component({
  selector: 'app-navbar',
  imports: [ ReactiveFormsModule, RouterLink ],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  private readonly fb = inject(FormBuilder);
  private readonly quoteService = inject(QuoteService);
  private readonly authService = inject(AuthService);
  protected readonly username = this.authService.currentUser()?.username;
  private readonly router = inject(Router);
  protected readonly searchForm = viewChild<ElementRef<HTMLElement>>('searchForm');
  protected readonly accountMenu = viewChild<ElementRef<HTMLElement>>('accountMenu');

  protected readonly form = this.fb.nonNullable.group({
    ticker: ['', [Validators.required]],
  });

  protected readonly quote = signal<Quote | null>(null);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly notFound = signal(false);
  protected readonly submitted = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly accountDropdownActive = signal(false);

  protected submit(): void {
    if (this.form.invalid) {
      this.submitted.set(true);
      return;
    }

    this.submitted.set(false);
    this.formError.set(null);
    this.notFound.set(false);
    this.quote.set(null);
    this.submitting.set(true);
    const tickerValue = this.form.controls.ticker.value;
    this.form.controls.ticker.reset('');

    this.quoteService.getQuote(tickerValue).subscribe({
      next: (quote) => {
        this.submitting.set(false);
        this.quote.set(quote);
      },
      error: (error: ApiError) => {
        this.submitting.set(false);
        if (error.status === 404) {
          this.notFound.set(true);
        } else {
          this.formError.set(error.detail);
        }
      },
    });
  }

  protected errorFor(field: SearchField): string {
    const control = this.form.get(field);
    if (!control || !this.submitted() || !control.errors) {
      return '';
    }
    if (control.errors['server']) {
      return control.errors['server'];
    }
    if (control.errors['required']) {
      return 'Required';
    }
    return '';
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    const formEl = this.searchForm()?.nativeElement;
    if (formEl && !formEl.contains(event.target as Node)) {
      this.quote.set(null);
      this.notFound.set(false);
      this.submitted.set(false);
    }

    const accountMenuEl = this.accountMenu()?.nativeElement;
    if (accountMenuEl && !accountMenuEl.contains(event.target as Node)) {
      this.accountDropdownActive.set(false);
    }
  }

  protected clearTicker(): void {
    this.form.controls.ticker.reset('');
    this.submitted.set(false);
  }

  protected onQuoteClick(): void {
    this.router.navigate([`/stocks/${this.quote()?.ticker}`]);
    this.quote.set(null); 
    this.notFound.set(false);
    this.submitted.set(false);
  }

  protected onAccountClick(): void {
    this.accountDropdownActive.update((active) => !active);
  }

  protected onNavigateClick(nextPage: NavDestination): void {
    this.accountDropdownActive.set(false);
    this.router.navigate([nextPage]);
  }

  protected onLogoutClick(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.accountDropdownActive.set(false);
        this.router.navigate(['/login']);
      },
      error: (error: ApiError) => {
        this.accountDropdownActive.set(false);
        this.formError.set(error.detail);
      },
    });
  }
}
