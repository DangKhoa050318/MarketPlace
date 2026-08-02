import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const analyticsGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const role = authService.getRole();

  return authService.isAuthenticated() && (role === 'ADMIN' || role === 'MANAGER')
    ? true
    : router.createUrlTree(['/products']);
};
