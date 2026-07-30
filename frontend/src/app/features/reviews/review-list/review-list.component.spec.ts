import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { VoteService } from '../../../core/services/vote.service';
import { ReviewListComponent } from './review-list.component';

describe('ReviewListComponent', () => {
  let fixture: ComponentFixture<ReviewListComponent>;
  let component: ReviewListComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReviewListComponent, NoopAnimationsModule],
      providers: [
        {
          provide: VoteService,
          useValue: jasmine.createSpyObj<VoteService>('VoteService', ['toggleVote'])
        },
        {
          provide: AuthService,
          useValue: jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated'])
        },
        {
          provide: NotificationService,
          useValue: jasmine.createSpyObj<NotificationService>(
            'NotificationService',
            ['info', 'error']
          )
        }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ReviewListComponent);
    component = fixture.componentInstance;
  });

  it('emits rating and sort filters', () => {
    spyOn(component.filterChange, 'emit');
    component.rating = 5;
    component.sort = 'helpful';
    component.filtersChanged();
    expect(component.filterChange.emit).toHaveBeenCalledWith({ rating: 5, sort: 'helpful' });
  });

  it('renders loading, error with retry, and empty states independently', () => {
    component.loading = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Loading reviews');

    component.loading = false;
    component.error = 'Unable to load reviews.';
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Retry');

    component.error = '';
    component.reviews = [];
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No reviews match');
  });

  it('shows verified purchase, date, edited state, and owner edit action', () => {
    component.reviews = [{
      id: 7, userId: 2, username: 'buyer', productId: 1, rating: 5,
      title: 'Excellent', content: 'Works exactly as expected.', status: 'APPROVED',
      isVerifiedPurchase: true, isEdited: true, helpfulCount: 3,
      createdAt: '2026-07-28T10:00:00', updatedAt: '2026-07-28T11:00:00'
    }];
    component.editableReviewId = 7;
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Verified Purchase');
    expect(text).toContain('Edited');
    expect(text).toContain('Edit review');
  });
});
