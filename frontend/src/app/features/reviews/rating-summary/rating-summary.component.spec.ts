import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RatingSummaryComponent } from './rating-summary.component';

describe('RatingSummaryComponent', () => {
  let fixture: ComponentFixture<RatingSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatingSummaryComponent, NoopAnimationsModule]
    }).compileComponents();
    fixture = TestBed.createComponent(RatingSummaryComponent);
    fixture.componentRef.setInput('summary', {
      productId: 1,
      averageRating: 4.2,
      totalReviews: 10,
      starCounts: { 1: 0, 2: 1, 3: 1, 4: 3, 5: 5 }
    });
    fixture.detectChanges();
  });

  it('renders average, total, and all five distribution rows', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('4.2');
    expect(text).toContain('10 reviews');
    expect(fixture.nativeElement.querySelectorAll('.distribution .row').length).toBe(5);
  });

  it('calculates distribution percentages', () => {
    expect(fixture.componentInstance.percentage(5)).toBe(50);
    expect(fixture.componentInstance.percentage(1)).toBe(0);
  });
});
