import { Observable, catchError, finalize, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injector, inject } from '@angular/core';

import { AuthResponse } from '@core/models/auth.model';
import { AuthService } from '@core/auth/auth.service';
import { environment } from '@environments/environment';

let refreshInProgress$: Observable<AuthResponse> | null = null;

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const http = inject(HttpClient);
  const injector = inject(Injector);

  if (req.url.includes('/auth/')) {
    return next(req);
  }

  const token = localStorage.getItem('accessToken');
  const authedReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        return throwError(() => error);
      }

      const refreshToken = localStorage.getItem('refreshToken');

      if (!refreshToken) {
        injector.get(AuthService).logout();
        return throwError(() => error);
      }

      refreshInProgress$ ??= http
        .post<AuthResponse>(`${environment.apiUrl}/auth/refresh`, { refreshToken })
        .pipe(
          tap((response) => {
            localStorage.setItem('accessToken', response.accessToken);
            localStorage.setItem('refreshToken', response.refreshToken);
          }),
          catchError((refreshError) => {
            injector.get(AuthService).logout();
            return throwError(() => refreshError);
          }),
          finalize(() => {
            refreshInProgress$ = null;
          }),
          shareReplay(1),
        );

      return refreshInProgress$.pipe(
        switchMap((response) =>
          next(req.clone({ setHeaders: { Authorization: `Bearer ${response.accessToken}` } })),
        ),
      );
    }),
  );
};
