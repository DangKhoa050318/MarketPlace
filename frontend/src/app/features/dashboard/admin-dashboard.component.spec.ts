import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardComponent } from './admin-dashboard.component';

describe('DashboardComponent funnel visualization', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let dashboardService: jasmine.SpyObj<DashboardService>;

  beforeEach(async () => {
    dashboardService = jasmine.createSpyObj('DashboardService', [
      'getDashboardStats',
      'getFunnelSummary',
      'getAnalyticsOverview'
    ]);
    dashboardService.getDashboardStats.and.returnValue(of({
      success: true,
      message: '',
      timestamp: '',
      data: {
        totalOrders: 10,
        totalRevenue: 1000,
        pendingOrders: 2,
        completedOrders: 8,
        totalProducts: 5,
        totalCustomers: 4
      }
    }));
    dashboardService.getFunnelSummary.and.returnValue(of({
      success: true,
      message: '',
      timestamp: '',
      data: {
        from: '2026-07-01T00:00:00Z',
        to: '2026-08-01T00:00:00Z',
        steps: [
          { step: 'PRODUCT_VIEW', count: 100, conversionRate: 1, dropOffRate: 0 },
          { step: 'ADD_TO_CART', count: 40, conversionRate: 0.4, dropOffRate: 0.6 },
          { step: 'BEGIN_CHECKOUT', count: 20, conversionRate: 0.5, dropOffRate: 0.5 },
          { step: 'ORDER_CREATED', count: 5, conversionRate: 0.25, dropOffRate: 0.75 }
        ]
      }
    }));
    dashboardService.getAnalyticsOverview.and.returnValue(of({
      success: true,
      message: '',
      timestamp: '',
      data: {
        productViews: 100,
        addToCarts: 40,
        beginCheckouts: 20,
        orders: 5,
        addToCartRate: 0.4,
        checkoutRate: 0.5,
        orderConversionRate: 0.05,
        returningCustomerRate: 0.25,
        lastUpdatedAt: '2026-08-01T12:00:00Z'
      }
    }));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, RouterTestingModule],
      providers: [
        { provide: DashboardService, useValue: dashboardService },
        { provide: AuthService, useValue: { getRole: () => 'ADMIN', isAuthenticated: () => true } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
  });

  it('loads and renders funnel counts and drop-off rates', () => {
    const text = fixture.nativeElement.textContent;

    expect(dashboardService.getFunnelSummary).toHaveBeenCalled();
    expect(text).toContain('Storefront Funnel');
    expect(text).toContain('ORDER_CREATED');
    expect(fixture.componentInstance.funnel?.steps[3].dropOffRate).toBe(0.75);
  });

  it('renders analytics KPIs for the selected date range', () => {
    const text = fixture.nativeElement.textContent;

    expect(dashboardService.getAnalyticsOverview).toHaveBeenCalled();
    expect(text).toContain('Conversion Overview');
    expect(text).toContain('Product views');
    expect(text).toContain('Returning customers');
    expect(text).toContain('40%');
  });

  it('reloads overview and funnel when a preset changes', () => {
    dashboardService.getAnalyticsOverview.calls.reset();
    dashboardService.getFunnelSummary.calls.reset();

    fixture.componentInstance.setRange(7);

    expect(fixture.componentInstance.selectedRangeDays).toBe(7);
    expect(dashboardService.getAnalyticsOverview).toHaveBeenCalledTimes(1);
    expect(dashboardService.getFunnelSummary).toHaveBeenCalledTimes(1);
  });
});
