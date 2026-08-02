import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { analyticsGuard } from './analytics.guard';

describe('analyticsGuard', () => {
  const router = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);
  const authService = jasmine.createSpyObj<AuthService>('AuthService', ['getRole', 'isAuthenticated']);

  beforeEach(() => {
    router.createUrlTree.and.returnValue({} as UrlTree);
    authService.isAuthenticated.and.returnValue(true);
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: authService }
      ]
    });
  });

  it('allows admin and manager roles', () => {
    authService.getRole.and.returnValue('ADMIN');
    expect(TestBed.runInInjectionContext(() => analyticsGuard(null!, null!))).toBeTrue();

    authService.getRole.and.returnValue('MANAGER');
    expect(TestBed.runInInjectionContext(() => analyticsGuard(null!, null!))).toBeTrue();
  });

  it('redirects staff because analytics APIs do not permit that role', () => {
    authService.getRole.and.returnValue('STAFF');

    const result = TestBed.runInInjectionContext(() => analyticsGuard(null!, null!));

    expect(result).toBe(router.createUrlTree.calls.mostRecent().returnValue);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/products']);
  });
});
