import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap, tap } from 'rxjs';
import { Router } from '@angular/router';

import { environment } from '@environments/environment';
import { LoginRequest, AuthResponse, RegisterRequest } from '@core/models/auth.model';
import { User } from '@core/models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = `${environment.apiUrl}/auth`;
  private readonly usersUrl = `${environment.apiUrl}/users`;
  private readonly _currentUser = signal<User | null>(null);

  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this._currentUser() !== null);

  constructor() {
    if (localStorage.getItem('accessToken')) {
      this.fetchCurrentUser().subscribe();
    }
  }

  login(data: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, data)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<void>(`${this.apiUrl}/register`, data)
      .pipe(switchMap(() => this.login({ email: data.email, password: data.password })));
  }

  /**
   * Revokes the refresh token on the backend and clears local session.
   */
  logout(): void {
    const refreshToken = localStorage.getItem('refreshToken');

    if (refreshToken) {
      this.http.post<void>(`${this.apiUrl}/logout`, { refreshToken }).subscribe({
        complete: () => this.clearSession(),
        error: () => this.clearSession(),
      });
    } else {
      this.clearSession();
    }
  }

  /**
   * Returns the stored access token, used by the JWT interceptor.
   */
  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  /**
   * Fetches the full user profile from the backend and updates the signal.
   * Called after login/register and on page reload when a token is present.
   */
  private fetchCurrentUser(): Observable<User> {
    return this.http
      .get<User>(`${this.usersUrl}/me`)
      .pipe(tap((user) => this._currentUser.set(user)));
  }

  /**
   * Stores tokens after a successful auth response, then fetches the full user profile.
   */
  private handleAuthSuccess(response: AuthResponse): void {
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    this.fetchCurrentUser().subscribe();
  }

  /**
   * Clears tokens from storage, resets the user signal, and redirects to login.
   */
  private clearSession(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this._currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }
}
