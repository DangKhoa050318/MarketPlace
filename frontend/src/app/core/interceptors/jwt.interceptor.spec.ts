import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

describe('jwtInterceptor', () => {
  let authService: AuthService;
  let http: HttpClient;
  let httpTestingController: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router }
      ]
    });
    authService = TestBed.inject(AuthService);
    http = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpTestingController.verify();
    localStorage.clear();
  });

  function login(): void {
    authService.login({ username: 'alice', password: 'secret' }).subscribe();
    httpTestingController.expectOne(`${environment.apiUrl}/auth/login`).flush({
      success: true,
      message: 'Login successful',
      data: { accessToken: 'old-access', username: 'alice', role: 'CUSTOMER' }
    });
  }

  it('uses one refresh request for concurrent 401 responses and retries all requests', () => {
    login();
    const results: string[] = [];
    http.get<{ value: string }>('/api/v1/first').subscribe(value => results.push(value.value));
    http.get<{ value: string }>('/api/v1/second').subscribe(value => results.push(value.value));

    const first = httpTestingController.expectOne('/api/v1/first');
    const second = httpTestingController.expectOne('/api/v1/second');
    expect(first.request.headers.get('Authorization')).toBe('Bearer old-access');
    expect(second.request.headers.get('Authorization')).toBe('Bearer old-access');
    first.flush({}, { status: 401, statusText: 'Unauthorized' });
    second.flush({}, { status: 401, statusText: 'Unauthorized' });

    const refreshRequests = httpTestingController.match(`${environment.apiUrl}/auth/refresh`);
    expect(refreshRequests.length).toBe(1);
    refreshRequests[0].flush({
      success: true,
      message: 'Token refreshed',
      data: { accessToken: 'new-access', username: 'alice', role: 'CUSTOMER' }
    });

    const firstRetry = httpTestingController.expectOne('/api/v1/first');
    const secondRetry = httpTestingController.expectOne('/api/v1/second');
    expect(firstRetry.request.headers.get('Authorization')).toBe('Bearer new-access');
    expect(secondRetry.request.headers.get('Authorization')).toBe('Bearer new-access');
    firstRetry.flush({ value: 'first' });
    secondRetry.flush({ value: 'second' });
    expect(results).toEqual(['first', 'second']);
  });

  it('fails every waiting request and clears the cookie once when refresh fails', () => {
    login();
    let failures = 0;
    http.get('/api/v1/first').subscribe({ error: () => failures++ });
    http.get('/api/v1/second').subscribe({ error: () => failures++ });

    httpTestingController.expectOne('/api/v1/first')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    httpTestingController.expectOne('/api/v1/second')
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    const refresh = httpTestingController.expectOne(`${environment.apiUrl}/auth/refresh`);
    refresh.flush({}, { status: 401, statusText: 'Unauthorized' });

    const logoutRequests = httpTestingController.match(`${environment.apiUrl}/auth/logout`);
    expect(logoutRequests.length).toBe(1);
    logoutRequests[0].flush({ success: true, message: 'Logout successful', data: null });
    expect(failures).toBe(2);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
