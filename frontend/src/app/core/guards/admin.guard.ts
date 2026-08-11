import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

const backOfficeRoles = ['ADMIN', 'MANAGER', 'STAFF'];

export const adminGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  const authService = inject(AuthService);
  const role = authService.getRole();
  const allowedRoles = (route.data?.['roles'] as string[] | undefined) ?? backOfficeRoles;

  if (authService.isAuthenticated() && role && allowedRoles.includes(role)) {
    return true;
  }

  return router.createUrlTree(['/products']);
};
