import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { EMPTY, Observable, of, shareReplay, tap, throwError } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { PromotionService } from './promotion.service';

export interface AuthResponse {
  accessToken: string;
  username: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private journeyApiUrl = `${environment.apiUrl}/journey`;
  private sessionStorageKey = 'recently_viewed_session_id';
  private sessionHintKey = 'marketplace_session_present';
  private accessToken: string | null = null;
  private username: string | null = null;
  private role: string | null = null;
  private refreshInFlight$: Observable<ApiResponse<AuthResponse>> | null = null;

  constructor(
    private http: HttpClient,
    private router: Router,
    private promotionService: PromotionService
  ) {}

  login(credentials: { username: string; password: string }): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(
      `${this.apiUrl}/login`, credentials, { withCredentials: true }
    ).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.storeSession(res.data);
          this.mergeAnonymousJourney();
        }
      })
    );
  }

  register(data: { username: string; email: string; password: string; fullName: string }): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(
      `${this.apiUrl}/register`, data, { withCredentials: true }
    ).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.storeSession(res.data);
          this.mergeAnonymousJourney();
        }
      })
    );
  }

  refreshSession(): Observable<ApiResponse<AuthResponse>> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http.post<ApiResponse<AuthResponse>>(
        `${this.apiUrl}/refresh`,
        {},
        { withCredentials: true }
      ).pipe(
        tap(res => {
          if (res.success && res.data) {
            this.storeSession(res.data);
          }
        }),
        catchError(error => {
          this.expireSession();
          return throwError(() => error);
        }),
        finalize(() => {
          this.refreshInFlight$ = null;
        }),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.refreshInFlight$;
  }

  restoreSession(): Observable<boolean> {
    if (this.isAuthenticated()) {
      return of(true);
    }
    return this.refreshSession().pipe(
      map(res => !!(res.success && res.data?.accessToken)),
      catchError(() => of(false))
    );
  }

  initializeSession(): Observable<boolean> {
    this.clearLegacyAuthStorage();
    if (this.isAuthenticated()) {
      return of(true);
    }
    if (localStorage.getItem(this.sessionHintKey) !== 'true') {
      return of(false);
    }
    return this.restoreSession();
  }

  logout(): void {
    this.clearSession();
    this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true }).pipe(
      catchError(() => EMPTY)
    ).subscribe();
    this.router.navigate(['/login']);
  }

  private clearSession(): void {
    this.promotionService.clearApplied();
    this.accessToken = null;
    this.username = null;
    this.role = null;
    localStorage.removeItem(this.sessionHintKey);
    this.clearLegacyAuthStorage();
  }

  private clearLegacyAuthStorage(): void {
    // Remove values written by older builds during the migration to memory-only auth state.
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
  }

  getToken(): string | null {
    return this.accessToken;
  }

  getUsername(): string | null {
    return this.username;
  }

  getRole(): string | null {
    return this.role;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  private storeSession(auth: AuthResponse): void {
    this.accessToken = auth.accessToken;
    this.username = auth.username;
    this.role = auth.role;
    this.clearLegacyAuthStorage();
    localStorage.setItem(this.sessionHintKey, 'true');
  }

  private expireSession(): void {
    this.clearSession();
    this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true }).pipe(
      catchError(() => EMPTY)
    ).subscribe();
    this.router.navigate(['/login']);
  }

  private mergeAnonymousJourney(): void {
    const sessionId = localStorage.getItem(this.sessionStorageKey);
    if (!sessionId) return;

    this.http.post(`${this.journeyApiUrl}/merge`, { sessionId }).pipe(
      catchError(() => EMPTY)
    ).subscribe();
  }
}
