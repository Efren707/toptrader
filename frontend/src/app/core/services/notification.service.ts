import { Injectable, signal } from '@angular/core';

const AUTO_CLEAR_MS = 15000;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly message = signal<string | null>(null);

  private timeoutId: ReturnType<typeof setTimeout> | null = null;

  show(message: string): void {
    this.message.set(message);
    this.resetTimer();
  }

  clear(): void {
    this.message.set(null);
    this.cancelTimer();
  }

  private resetTimer(): void {
    this.cancelTimer();
    this.timeoutId = setTimeout(() => this.clear(), AUTO_CLEAR_MS);
  }

  private cancelTimer(): void {
    if (this.timeoutId !== null) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }
  }
}
