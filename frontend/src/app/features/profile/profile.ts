import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService, UpdateProfileRequest } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { ApiError } from '../../core/interceptors/error.interceptor';
import { Button } from '../../shared/ui/button/button';
import { Card } from '../../shared/ui/card/card';
import { Input } from '../../shared/ui/input/input';
import { Router } from '@angular/router';

type UpdateProfileField = 'email' | 'username' | 'avatarKey' | 'password';

const AVAILABLE_AVATARS = [
  'nova',
  'ember',
  'pixel',
  'quill',
  'sage',
  'orbit',
  'comet',
  'willow',
  'juniper',
  'echo',
  'flint',
  'marble',
  'clover',
  'breeze',
  'cosmo',
  'hazel',
];

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, Button, Card, Input],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  private currentUser = this.authService.currentUser();

  protected readonly form = this.fb.nonNullable.group({
    email: [this.currentUser?.email ?? '', [Validators.email]],
    username: [this.currentUser?.username ?? '', []],
    avatarKey: [this.currentUser?.avatarKey ?? '', []],
    password: ['', [Validators.minLength(8)]],
  });

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly avatarOptions = AVAILABLE_AVATARS;
  protected readonly pickerOpen = signal(false);
  protected readonly pendingAvatarKey = signal<string | null>(null);

  protected readonly confirming = signal(false);
  protected readonly submittingDeleteAccount = signal(false);
  protected readonly deleteAccountError = signal<string | null>(null);

  protected avatarSrc(): string {
    return this.avatarSrcFor(this.form.controls.avatarKey.value || 'nova');
  }

  protected avatarSrcFor(key: string): string {
    return `/avatars/${key}.svg`;
  }

  protected openAvatarPicker(): void {
    this.pendingAvatarKey.set(this.form.controls.avatarKey.value || 'nova');
    this.pickerOpen.set(true);
  }

  protected selectAvatarOption(key: string): void {
    this.pendingAvatarKey.set(key);
  }

  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cancelAvatarPick();
    }
  }

  protected confirmAvatarPick(): void {
    const key = this.pendingAvatarKey();
    if (key) {
      this.form.controls.avatarKey.setValue(key);
    }
    this.pickerOpen.set(false);
  }

  protected cancelAvatarPick(): void {
    this.pickerOpen.set(false);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const payload: UpdateProfileRequest = {};

    if (raw.username && raw.username !== this.currentUser?.username) {
      payload.username = raw.username;
    }
    if (raw.email && raw.email !== this.currentUser?.email) {
      payload.email = raw.email;
    }
    if (raw.avatarKey && raw.avatarKey !== this.currentUser?.avatarKey) {
      payload.avatarKey = raw.avatarKey;
    }
    if (raw.password) {
      payload.password = raw.password;
    }

    if (Object.keys(payload).length === 0) {
      return;
    }

    this.formError.set(null);
    this.submitting.set(true);

    this.authService.updateProfile(payload).subscribe({
      next: (user) => {
        this.currentUser = user;
        this.form.patchValue({ password: '' });
        this.submitting.set(false);
        this.notificationService.show('Account updated successfully');
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

  protected deleteAccount(): void {
    this.deleteAccountError.set(null);
    this.confirming.set(true);
  }

  protected onDeleteBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cancel();
    }
  }

  protected cancel(): void {
    this.confirming.set(false);
    this.deleteAccountError.set(null);
  }

  protected confirmedDeleteAccount(): void {
    this.deleteAccountError.set(null);
    this.submittingDeleteAccount.set(true);

    this.authService.deleteAccount().subscribe({
      next: () => {
        this.notificationService.show('Account deleted successfully');
        this.router.navigate(['/login']);
      },
      error: (error: ApiError) => {
        this.submittingDeleteAccount.set(false);
        this.deleteAccountError.set(error.detail);
      },
    });
  }

  protected errorFor(field: UpdateProfileField): string {
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
