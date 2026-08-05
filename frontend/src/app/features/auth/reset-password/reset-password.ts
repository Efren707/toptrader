import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from '../../../shared/ui/button/button';
import { Card } from '../../../shared/ui/card/card';
import { Input } from '../../../shared/ui/input/input';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApiError } from '../../../core/interceptors/error.interceptor';

type ResetPasswordField = 'password'; 

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, Button, Card, Input, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css',
})
export class ResetPassword {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  protected readonly token: string | null = this.route.snapshot.queryParamMap.get('token');

  protected readonly form = this.fb.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  protected readonly hasToken: boolean = this.token !== null;  
  protected readonly confirmed = signal(false);
  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.formError.set(null);
    this.submitting.set(true);

    this.authService.resetPassword({rawToken: this.token ?? "", password: this.form.getRawValue().password}).subscribe({
      next: () => {
        this.confirmed.set(true);
        this.submitting.set(false);
      },
      error: (error: ApiError) => {
        this.submitting.set(false);
        this.formError.set(error.detail);
      },
    });
    
  }

  protected errorFor(field: ResetPasswordField): string {
    const control = this.form.get(field);
    if (!control || !control.touched || !control.errors) {
      return '';
    }
    if (control.errors['server']) {
      return control.errors['server'];
    }
    if (control.errors['required']) {
      return 'Required';
    }
    if (control.errors['minlength']) {
      return 'Must be at least 8 characters';
    }
    return '';
  }
}
