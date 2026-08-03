import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

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
    localStorage.setItem('access_token', 'access');
    localStorage.setItem('refresh_token', 'refresh');
    localStorage.setItem('username', 'alice');
    localStorage.setItem('role', 'CUSTOMER');

    service.logout();

    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('refresh_token')).toBeNull();
    expect(localStorage.getItem('username')).toBeNull();
    expect(localStorage.getItem('role')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);

    const request = httpTestingController.expectOne(`${environment.apiUrl}/auth/logout`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ refreshToken: 'refresh' });
    request.flush({ success: true, message: 'Logout successful', data: null });
  });

  it('clears local session without an HTTP call when no refresh token exists', () => {
    localStorage.setItem('access_token', 'access');

    service.logout();

    expect(localStorage.getItem('access_token')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    httpTestingController.expectNone(`${environment.apiUrl}/auth/logout`);
  });
});
