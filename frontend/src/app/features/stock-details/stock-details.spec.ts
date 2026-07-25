import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { StockDetails } from './stock-details';

describe('StockDetails', () => {
  let component: StockDetails;
  let fixture: ComponentFixture<StockDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StockDetails],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ ticker: 'AAPL' }) } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StockDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
