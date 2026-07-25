import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StockDetails } from './stock-details';

describe('StockDetails', () => {
  let component: StockDetails;
  let fixture: ComponentFixture<StockDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StockDetails],
    }).compileComponents();

    fixture = TestBed.createComponent(StockDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
