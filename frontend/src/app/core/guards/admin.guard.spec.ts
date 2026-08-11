import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { adminGuard } from './admin.guard';

describe('adminGuard', () => {
  let router: jasmine.SpyObj<Router>;
  let authService: jasmine.SpyObj<AuthService>;
  let redirect: UrlTree;

  beforeEach(() => {
    redirect = {} as UrlTree;
    router = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getRole', 'isAuthenticated']);
    router.createUrlTree.and.returnValue(redirect);
    authService.isAuthenticated.and.returnValue(true);

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: authService }
      ]
    });
  });

  it('allows all back-office roles when a route has no narrower role contract', () => {
    const route = routeWithRoles();

    for (const role of ['ADMIN', 'MANAGER', 'STAFF']) {
      authService.getRole.and.returnValue(role);
      expect(runGuard(route)).toBeTrue();
    }
  });

  it('uses route role metadata to block staff and manager from admin-only screens', () => {
    const route = routeWithRoles(['ADMIN']);

    for (const role of ['MANAGER', 'STAFF']) {
      authService.getRole.and.returnValue(role);
      expect(runGuard(route)).toBe(redirect);
    }

    expect(router.createUrlTree).toHaveBeenCalledWith(['/products']);
  });

  it('allows manager into manager-or-admin screens', () => {
    authService.getRole.and.returnValue('MANAGER');

    expect(runGuard(routeWithRoles(['ADMIN', 'MANAGER']))).toBeTrue();
  });

  it('redirects unauthenticated users even when their stale role is allowed', () => {
    authService.isAuthenticated.and.returnValue(false);
    authService.getRole.and.returnValue('ADMIN');

    expect(runGuard(routeWithRoles(['ADMIN']))).toBe(redirect);
  });

  function routeWithRoles(roles?: string[]): ActivatedRouteSnapshot {
    return { data: roles ? { roles } : {} } as ActivatedRouteSnapshot;
  }

  function runGuard(route: ActivatedRouteSnapshot): boolean | UrlTree {
    return TestBed.runInInjectionContext(() => adminGuard(route, null!)) as boolean | UrlTree;
  }
});
