import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ReviewFormComponent } from './review-form.component';

describe('ReviewFormComponent', () => {
  let fixture: ComponentFixture<ReviewFormComponent>;
  let component: ReviewFormComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReviewFormComponent, NoopAnimationsModule]
    }).compileComponents();
    fixture = TestBed.createComponent(ReviewFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('rejects ratings outside 1–5 and short content', () => {
    component.form.setValue({ rating: 0, title: 'Good', content: 'short' });
    expect(component.form.invalid).toBeTrue();
    component.form.controls.rating.setValue(6);
    expect(component.form.controls.rating.invalid).toBeTrue();
    expect(component.form.controls.content.invalid).toBeTrue();
  });

  it('accepts boundary ratings 1 and 5', () => {
    component.form.setValue({
      rating: 1,
      title: 'Useful review',
      content: 'This content is long enough.'
    });
    expect(component.form.valid).toBeTrue();
    component.form.controls.rating.setValue(5);
    expect(component.form.valid).toBeTrue();
  });

  it('emits a trimmed valid payload', () => {
    spyOn(component.save, 'emit');
    component.form.setValue({
      rating: 5,
      title: '  Excellent  ',
      content: '  A genuinely excellent product.  '
    });
    component.submit();
    expect(component.save.emit).toHaveBeenCalledWith({
      rating: 5,
      title: 'Excellent',
      content: 'A genuinely excellent product.'
    });
  });
});
