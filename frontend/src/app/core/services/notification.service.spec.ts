import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationService);
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('has no message initially', () => {
    expect(service.message()).toBeNull();
  });

  it('sets the message on show()', () => {
    service.show('Hello');
    expect(service.message()).toBe('Hello');
  });

  it('clears the message on clear()', () => {
    service.show('Hello');
    service.clear();
    expect(service.message()).toBeNull();
  });

  it('auto-clears the message after 15 seconds', () => {
    service.show('Hello');
    vi.advanceTimersByTime(15000);
    expect(service.message()).toBeNull();
  });

  it('does not clear the message before 15 seconds have elapsed', () => {
    service.show('Hello');
    vi.advanceTimersByTime(14999);
    expect(service.message()).toBe('Hello');
  });

  it('resets the auto-clear timer on a fresh show()', () => {
    service.show('First');
    vi.advanceTimersByTime(10000);
    service.show('Second');
    vi.advanceTimersByTime(10000);

    expect(service.message()).toBe('Second');

    vi.advanceTimersByTime(5000);
    expect(service.message()).toBeNull();
  });

  it('does not let a cancelled timer clear a later message', () => {
    service.show('Hello');
    vi.advanceTimersByTime(5000);
    service.clear();
    service.show('New message');
    // Total elapsed since the first show() is now 15000ms - if clear() failed to
    // cancel that first timer, it would incorrectly wipe out "New message" here,
    // 5 seconds before its own fresh 15s timer is due.
    vi.advanceTimersByTime(10000);

    expect(service.message()).toBe('New message');
  });
});
