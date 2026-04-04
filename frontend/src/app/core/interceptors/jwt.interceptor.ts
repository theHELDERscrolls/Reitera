import { catchError, switchMap, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthResponse } from '@core/models/auth.model';
import { AuthService } from '@core/auth/auth.service';
import { environment } from '@environments/environment';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
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
        inject(AuthService).logout();
        return throwError(() => error);
      }

      return inject(HttpClient)
        .post<AuthResponse>(`${environment.apiUrl}/auth/refresh`, { refreshToken })
        .pipe(
          switchMap((response) => {
            localStorage.setItem('accessToken', response.accessToken);
            localStorage.setItem('refreshToken', response.refreshToken);
            return next(
              req.clone({ setHeaders: { Authorization: `Bearer ${response.accessToken}` } }),
            );
          }),
          catchError((refreshError) => {
            inject(AuthService).logout();
            return throwError(() => refreshError);
          }),
        );
    }),
  );
};
