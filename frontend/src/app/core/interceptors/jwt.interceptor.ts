import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError, BehaviorSubject, switchMap, filter, take } from 'rxjs';
import { AuthService } from '../services/auth.service';

let isRefreshing = false;
const refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);
  const token = authService.getToken();

  const isAuthRequest = req.url.includes('/auth/login') || req.url.includes('/auth/register') || req.url.includes('/auth/refresh-token');

  if (token && !isAuthRequest) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req).pipe(
    catchError((error) => {
      if (error.status === 401 && !isAuthRequest) {
        return handle401Error(req, next, authService, router);
      }
      return throwError(() => error);
    })
  );
};

function handle401Error(req: HttpRequest<any>, next: HttpHandlerFn, authService: AuthService, router: Router) {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    const refreshToken = localStorage.getItem('refresh_token');

    if (refreshToken) {
      return authService.refreshToken(refreshToken).pipe(
        switchMap((res) => {
          isRefreshing = false;
          if (res.success && res.data) {
            refreshTokenSubject.next(res.data.accessToken);
            return next(req.clone({
              setHeaders: { Authorization: `Bearer ${res.data.accessToken}` }
            }));
          } else {
            authService.logout();
            return throwError(() => new Error('Token refresh failed'));
          }
        }),
        catchError((err) => {
          isRefreshing = false;
          authService.logout();
          return throwError(() => err);
        })
      );
    } else {
      authService.logout();
      return throwError(() => new Error('No refresh token available'));
    }
  } else {
    return refreshTokenSubject.pipe(
      filter(token => token !== null),
      take(1),
      switchMap((token) => next(req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      })))
    );
  }
}
