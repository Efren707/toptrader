import { Component, effect, inject, signal, input, output, computed } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { TradeRequest, TradeResult, TradeService, TradeSide } from '../../core/services/trade.service';
import { ApiError } from '../../core/interceptors/error.interceptor';
import { Button } from '../ui/button/button';
import { Card } from '../ui/card/card';

type TradeField = 'quantity'

@Component({
  selector: 'app-trade-form',
  imports: [ReactiveFormsModule, CurrencyPipe, Button, Card],
  templateUrl: './trade-form.html',
  styleUrl: './trade-form.css',
})
export class TradeForm {
  private readonly tradeService = inject(TradeService);

  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    quantity: ['', [Validators.required, Validators.min(1), Validators.pattern(/^[1-9]\d*$/)]]
  });

  readonly ticker = input.required<string>();
  readonly price = input.required<number>();
  readonly cashBalance = input.required<number>();
  readonly hasHolding = input<boolean>(false); 
  readonly readOnly = input<boolean>(false); 
  
  protected readonly TradeSide = TradeSide; 
  protected readonly side = signal<TradeSide>(TradeSide.BUY); 
  protected readonly submitting = signal(false);
  protected readonly confirming = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly pendingQuantity = signal<number | null>(null);
  protected readonly result = signal<TradeResult | null>(null);
  protected readonly submitted = signal(false);

  protected readonly quantity = computed(() => Number(this.quantityInput()) || 0);
  protected readonly pendingCost = computed(() => this.quantity() * this.price());

  traded = output<TradeResult>()

  private readonly quantityInput = toSignal(this.form.controls.quantity.valueChanges, {
    initialValue: this.form.getRawValue().quantity,
  });

  constructor() {
    effect(() => {
      if (this.readOnly()) {
        this.form.controls.quantity.disable();
      } else {
        this.form.controls.quantity.enable();
      }
    });
  }

  protected submit(): void {
    if (this.readOnly()) {
      return;
    }

    this.submitted.set(true);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.formError.set(null);
    this.pendingQuantity.set(Number(this.form.getRawValue().quantity));
    this.confirming.set(true);
  }

  protected confirmTrade(): void {
    const quantity = this.pendingQuantity();

    if (quantity === null) {
      return;
    }

    this.formError.set(null);
    this.submitting.set(true);

    const request: TradeRequest = {
      ticker: this.ticker(),
      quantity,
    };

    if(this.side() == TradeSide.BUY){
      this.tradeService.buyStock(request).subscribe({
        next: (response: TradeResult) => {
          this.submitting.set(false);
          this.confirming.set(false);
          this.result.set(response);
          this.traded.emit(response);
        },
        error: (error: ApiError) => {
          this.submitting.set(false);
          this.formError.set(error.detail);
        },
      });
    }

    if(this.side() == TradeSide.SELL){
      this.tradeService.sellStock(request).subscribe({
        next: (response: TradeResult) => {
          this.submitting.set(false);
          this.confirming.set(false);
          this.result.set(response);
          this.traded.emit(response);
        },
        error: (error: ApiError) => {
          this.submitting.set(false);
          this.formError.set(error.detail);
        },
      });
    }
    
  }

  protected selectSide(side: TradeSide): void {
    this.side.set(side)
    this.confirming.set(false);
    this.formError.set(null);
    this.submitted.set(false);
  }

  protected filterDigits(event: KeyboardEvent): void {
    if (event.ctrlKey || event.metaKey || event.altKey) {
      return;
    }
    if (event.key.length === 1 && !/[0-9]/.test(event.key)) {
      event.preventDefault();
    }
  }

  protected cancel(): void {
    this.confirming.set(false);
    this.pendingQuantity.set(null);
    this.formError.set(null);
  }

  protected tradeAgain(): void {
    this.result.set(null);
    this.pendingQuantity.set(null);
    this.formError.set(null);
    this.submitted.set(false);
    this.form.reset();
  }

  protected errorFor(field: TradeField): string {
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

}
