import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { Layout } from './layout';
import { NotificationService } from '../../core/services/notification.service';

@Component({ selector: 'app-stub', template: '<p>routed content</p>' })
class StubPage {}

describe('Layout', () => {
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'dashboard', component: Layout, children: [{ path: '', component: StubPage }] }]),
      ],
    });

    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/dashboard');
  });

  it('renders the navbar on every authenticated page', () => {
    expect(harness.routeNativeElement?.querySelector('app-navbar')).toBeTruthy();
  });

  it('renders the routed page content via the router outlet', () => {
    expect(harness.routeNativeElement?.querySelector('main.content')?.textContent).toContain(
      'routed content',
    );
  });

  it('does not render the notice toast when there is no notification', () => {
    expect(harness.routeNativeElement?.querySelector('.notice-toast')).toBeNull();
  });

  it('renders the notice toast when a notification is shown', () => {
    const notificationService = TestBed.inject(NotificationService);
    notificationService.show('Account created — check your email to verify your address.');
    harness.detectChanges();

    expect(harness.routeNativeElement?.querySelector('.notice-toast')?.textContent).toContain(
      'Account created',
    );
  });

  it('clears the notification when the dismiss button is clicked', () => {
    const notificationService = TestBed.inject(NotificationService);
    notificationService.show('Account created — check your email to verify your address.');
    harness.detectChanges();

    harness.routeNativeElement?.querySelector<HTMLButtonElement>('.notice-dismiss')?.click();
    harness.detectChanges();

    expect(notificationService.message()).toBeNull();
    expect(harness.routeNativeElement?.querySelector('.notice-toast')).toBeNull();
  });
});
