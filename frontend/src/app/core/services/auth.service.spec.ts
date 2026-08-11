import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthResponse, AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpTestingController: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService, { provide: Router, useValue: router }]
    });
    service = TestBed.inject(AuthService);
    httpTestingController = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpTestingController.verify();
    localStorage.clear();
  });

  it('revokes the refresh token and clears local session on logout', () => {
    service.login({ username: 'alice', password: 'secret' }).subscribe();
    const login = httpTestingController.expectOne(`${environment.apiUrl}/auth/login`);
    login.flush({
      success: true,
      message: 'Login successful',
      data: { accessToken: 'access', username: 'alice', role: 'CUSTOMER' }
    });

    service.logout();

    expect(service.getToken()).toBeNull();
    expect(service.getUsername()).toBeNull();
    expect(service.getRole()).toBeNull();
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(localStorage.getItem('username')).toBeNull();
    expect(localStorage.getItem('role')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);

    const request = httpTestingController.expectOne(`${environment.apiUrl}/auth/logout`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    expect(request.request.withCredentials).toBeTrue();
    request.flush({ success: true, message: 'Logout successful', data: null });
  });

  it('always asks the backend to clear the HttpOnly cookie on logout', () => {
    localStorage.setItem('access_token', 'legacy-access');
    localStorage.setItem('refresh_token', 'legacy-refresh');

    service.logout();

    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    const request = httpTestingController.expectOne(`${environment.apiUrl}/auth/logout`);
    request.flush({ success: true, message: 'Logout successful', data: null });
  });

  it('stores authentication state in memory without writing tokens or role to localStorage', () => {
    service.login({ username: 'alice', password: 'secret' }).subscribe();

    const request = httpTestingController.expectOne(`${environment.apiUrl}/auth/login`);
    expect(request.request.withCredentials).toBeTrue();
    request.flush({
      success: true,
      message: 'Login successful',
      data: { accessToken: 'access', username: 'alice', role: 'CUSTOMER' }
    });

    expect(service.getToken()).toBe('access');
    expect(service.getUsername()).toBe('alice');
    expect(service.getRole()).toBe('CUSTOMER');
    expect(localStorage.getItem('marketplace_session_present')).toBe('true');
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(localStorage.getItem('username')).toBeNull();
    expect(localStorage.getItem('role')).toBeNull();
  });

  it('removes legacy authentication values during application initialization', () => {
    localStorage.setItem('access_token', 'legacy-access');
    localStorage.setItem('refresh_token', 'legacy-refresh');
    localStorage.setItem('username', 'legacy-user');
    localStorage.setItem('role', 'ADMIN');

    let initialized = true;
    service.initializeSession().subscribe(value => initialized = value);

    expect(initialized).toBeFalse();
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(localStorage.getItem('username')).toBeNull();
    expect(localStorage.getItem('role')).toBeNull();
    httpTestingController.expectNone(`${environment.apiUrl}/auth/refresh`);
  });

  it('shares one refresh request between concurrent subscribers', () => {
    const first: ApiResponse<AuthResponse>[] = [];
    const second: ApiResponse<AuthResponse>[] = [];

    service.refreshSession().subscribe(value => first.push(value));
    service.refreshSession().subscribe(value => second.push(value));

    const requests = httpTestingController.match(`${environment.apiUrl}/auth/refresh`);
    expect(requests.length).toBe(1);
    expect(requests[0].request.withCredentials).toBeTrue();
    requests[0].flush({
      success: true,
      message: 'Token refreshed',
      data: { accessToken: 'new-access', username: 'alice', role: 'CUSTOMER' }
    });

    expect(first[0].data?.accessToken).toBe('new-access');
    expect(second[0].data?.accessToken).toBe('new-access');
    expect(service.getToken()).toBe('new-access');
  });
});
