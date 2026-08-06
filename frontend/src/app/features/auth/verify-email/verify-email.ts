import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ApiError } from '../../../core/interceptors/error.interceptor';
import { Button } from '../../../shared/ui/button/button';
import { Card } from '../../../shared/ui/card/card';
import { Input } from '../../../shared/ui/input/input';

type ResendVerificationField = 'email';

@Component({
  selector: 'app-verify-email',
  imports: [ReactiveFormsModule, Button, Card, Input, RouterLink],
  templateUrl: './verify-email.html',
  styleUrl: './verify-email.css',
})
export class VerifyEmail implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  protected readonly token: string | null = this.route.snapshot.queryParamMap.get('token');

  protected readonly status = signal<'verifying' | 'success' | 'failed'>(
    this.token ? 'verifying' : 'failed',
  );

  protected readonly resendForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  protected readonly resendConfirmed = signal(false);
  protected readonly resendSubmitting = signal(false);
  protected readonly resendError = signal<string | null>(null);

  ngOnInit(): void {
    if (this.token) {
      this.authService.verifyEmail({ rawToken: this.token }).subscribe({
        next: () => this.status.set('success'),
        error: () => this.status.set('failed'),
      });
    }
  }

  protected submitResend(): void {
    if (this.resendForm.invalid) {
      this.resendForm.markAllAsTouched();
      return;
    }

    this.resendError.set(null);
    this.resendSubmitting.set(true);

    this.authService.resendVerification(this.resendForm.getRawValue()).subscribe({
      next: () => {
        this.resendConfirmed.set(true);
        this.resendSubmitting.set(false);
      },
      error: (error: ApiError) => {
        this.resendSubmitting.set(false);
        this.resendError.set(error.detail);
      },
    });
  }

  protected errorFor(field: ResendVerificationField): string {
    const control = this.resendForm.get(field);
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
