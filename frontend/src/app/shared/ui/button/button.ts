import { Component, input, output } from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary';
export type ButtonType = 'button' | 'submit';

@Component({
  selector: 'app-button',
  imports: [],
  templateUrl: './button.html',
  styleUrl: './button.css',
})
export class Button {
  variant = input<ButtonVariant>('primary');
  type = input<ButtonType>('button');
  disabled = input(false);
  fullWidth = input(false);
  pressed = output<void>();

  protected onClick(): void {
    if (!this.disabled()) {
      this.pressed.emit();
    }
  }
}
