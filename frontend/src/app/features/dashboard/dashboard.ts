import { Component, ElementRef, HostListener, inject, signal, viewChild } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Quote, QuoteService } from '../../core/services/quote.service';
import { ApiError } from '../../core/interceptors/error.interceptor';

type SearchField = 'ticker';

@Component({
  selector: 'app-dashboard',
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly fb = inject(FormBuilder);
  private readonly quoteService = inject(QuoteService);
  private readonly authService = inject(AuthService);
  protected readonly username = this.authService.currentUser()?.username;
  protected readonly cashBalance = this.authService.currentUser()?.cashBalance;

  protected readonly searchForm = viewChild<ElementRef<HTMLElement>>('searchForm');

  protected readonly form = this.fb.nonNullable.group({
    ticker: ['', [Validators.required]],
  });

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly notFound = signal(false);
  protected readonly quote = signal<Quote | null>(null);
  protected readonly submitted = signal(false);

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
  }

  protected clearTicker(): void {
    this.form.controls.ticker.reset('');
    this.submitted.set(false);
  }
}
