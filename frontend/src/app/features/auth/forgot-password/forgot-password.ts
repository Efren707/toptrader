import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from '../../../shared/ui/button/button';
import { Card } from '../../../shared/ui/card/card';
import { Input } from '../../../shared/ui/input/input';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApiError } from '../../../core/interceptors/error.interceptor';

type ForgotPasswordField = 'email';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, Button, Card, Input, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
})
export class ForgotPassword {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

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

    this.authService.forgotPassword(this.form.getRawValue()).subscribe({
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

  protected errorFor(field: ForgotPasswordField): string {
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
    if (control.errors['email']) {
      return 'Must be a valid email address';
    }
    return '';
  }

}
