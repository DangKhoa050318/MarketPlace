import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError, switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  const isAuthRequest = req.url.includes('/auth/');

  if (token && !isAuthRequest) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req).pipe(
    catchError((error) => {
      if (error.status === 401 && !isAuthRequest) {
        return authService.refreshSession().pipe(
          switchMap((res) => {
            const refreshedToken = res.data?.accessToken;
            if (!res.success || !refreshedToken) {
              return throwError(() => new Error('Token refresh failed'));
            }
            return next(req.clone({
              setHeaders: { Authorization: `Bearer ${refreshedToken}` }
            }));
          })
        );
      }
      return throwError(() => error);
    })
  );
};
