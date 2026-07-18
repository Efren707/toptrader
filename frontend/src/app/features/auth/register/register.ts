import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError } from '../../../core/interceptors/error.interceptor';
import { AuthService, UserSummary } from '../../../core/services/auth.service';
import { Button } from '../../../shared/ui/button/button';
import { Card } from '../../../shared/ui/card/card';
import { Input } from '../../../shared/ui/input/input';

type RegisterField = 'email' | 'username' | 'password';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, Button, Card, Input],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    username: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly createdUser = signal<UserSummary | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.formError.set(null);
    this.submitting.set(true);

    this.authService.register(this.form.getRawValue()).subscribe({
      next: (user) => {
        this.submitting.set(false);
        this.createdUser.set(user);
      },
      error: (error: ApiError) => {
        this.submitting.set(false);
        if (Object.keys(error.fieldErrors).length > 0) {
          this.applyFieldErrors(error.fieldErrors);
        } else {
          this.formError.set(error.detail);
        }
      },
    });
  }

  protected errorFor(field: RegisterField): string {
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
    if (control.errors['minlength']) {
      return 'Must be at least 8 characters';
    }
    return '';
  }

  private applyFieldErrors(fieldErrors: Record<string, string>): void {
    for (const [field, message] of Object.entries(fieldErrors)) {
      this.form.get(field)?.setErrors({ server: message });
    }
  }
}
