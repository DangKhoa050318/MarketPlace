import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
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
      'getAnalyticsOverview',
      'getPromotionPerformance',
      'getPromotionTrend',
      'getProductPerformance',
      'createExport',
      'getExport',
      'downloadExport'
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
    dashboardService.getPromotionPerformance.and.returnValue(of({
      success: true, message: '', timestamp: '', data: [{
        campaign: 'Summer Launch', placement: 'HOME_TRENDING', strategy: 'TRENDING',
        impressions: 100, clicks: 32, clickThroughRate: 0.32,
        addToCarts: 15, attributedOrders: 6, lastUpdatedAt: '2026-08-01T12:00:00Z'
      }]
    }));
    dashboardService.getPromotionTrend.and.returnValue(of({
      success: true, message: '', timestamp: '', data: [
        { date: '2026-07-31', impressions: 20, clicks: 6, addToCarts: 3, attributedOrders: 1 },
        { date: '2026-08-01', impressions: 30, clicks: 10, addToCarts: 5, attributedOrders: 2 }
      ]
    }));
    dashboardService.getProductPerformance.and.returnValue(of({
      success: true, message: '', timestamp: '', data: {
        content: [{
          productId: 1, productName: 'Demo Camera', categoryId: 1,
          productViews: 30, wishlists: 12, addToCarts: 18, orders: 6,
          averageRating: 4.5, questionCount: 3, lastUpdatedAt: '2026-08-01T12:00:00Z'
        }],
        page: 0, size: 8, totalElements: 1, totalPages: 1, last: true
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

  it('renders an empty state when the selected period has no journey events', () => {
    dashboardService.getAnalyticsOverview.and.returnValue(of({
      success: true,
      message: '',
      timestamp: '',
      data: {
        productViews: 0,
        addToCarts: 0,
        beginCheckouts: 0,
        orders: 0,
        addToCartRate: 0,
        checkoutRate: 0,
        orderConversionRate: 0,
        returningCustomerRate: 0,
        lastUpdatedAt: ''
      }
    }));

    fixture.componentInstance.loadAnalytics();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No journey activity in this period');
  });

  it('renders an error state when analytics requests fail', () => {
    dashboardService.getAnalyticsOverview.and.returnValue(throwError(() => ({
      error: { message: 'Analytics service unavailable' }
    })));

    fixture.componentInstance.loadAnalytics();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Analytics data is unavailable');
    expect(fixture.nativeElement.textContent).toContain('Analytics service unavailable');
  });

  it('marks analytics as stale when lastUpdatedAt is older than ten minutes', () => {
    fixture.componentInstance.analyticsOverview = {
      ...fixture.componentInstance.analyticsOverview!,
      lastUpdatedAt: new Date(Date.now() - 11 * 60 * 1000).toISOString()
    };

    expect(fixture.componentInstance.analyticsStale).toBeTrue();
  });

  it('exposes keyboard links and accessible funnel progress values', () => {
    const clickableKpis = fixture.nativeElement.querySelectorAll('a.clickable-card');
    const funnel = fixture.nativeElement.querySelector('.funnel-panel');
    const progressBars = fixture.nativeElement.querySelectorAll('.funnel-step [role="progressbar"]');

    expect(clickableKpis.length).toBe(4);
    expect(funnel.getAttribute('aria-describedby')).toBe('funnel-description');
    expect(progressBars.length).toBe(4);
    expect(progressBars[1].getAttribute('aria-valuenow')).toBe('40');
  });

  it('renders product performance and campaign comparison data', () => {
    const text = fixture.nativeElement.textContent;

    expect(text).toContain('Product Performance');
    expect(text).toContain('Demo Camera');
    expect(text).toContain('Campaign & Placement Performance');
    expect(text).toContain('Summer Launch');
    expect(text).toContain('32%');
  });

  it('requests server-side sorting when a metric header changes', () => {
    dashboardService.getProductPerformance.calls.reset();

    fixture.componentInstance.sortProducts('orders');

    expect(dashboardService.getProductPerformance).toHaveBeenCalled();
    const args = dashboardService.getProductPerformance.calls.mostRecent().args;
    expect(args[5]).toBe('orders');
    expect(args[6]).toBe('desc');
  });
});
