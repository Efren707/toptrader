import { TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { Layout } from './layout';

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
});
