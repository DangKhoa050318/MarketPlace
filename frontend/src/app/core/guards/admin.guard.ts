import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authService = inject(AuthService);
  const role = authService.getRole();

  // Allow back-office management roles (ADMIN, MANAGER, STAFF)
  if (authService.isAuthenticated() && (role === 'ADMIN' || role === 'MANAGER' || role === 'STAFF')) {
    return true;
  }

  router.navigate(['/products']);
  return false;
};
