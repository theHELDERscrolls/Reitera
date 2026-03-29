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
  private readonly _currentUser = signal<User | null>(null);

  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this._currentUser() !== null);

  constructor() {
    const token = localStorage.getItem('accessToken');
    if (token) {
      this._currentUser.set(this.decodeUserFromToken(token));
    }
  }

  /**
   * Sends login credentials to the backend.
   * On success, stores tokens and loads the user profile.
   */
  login(data: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, data)
      .pipe(tap((response) => this.handleAuthSuccess(response)));
  }

  /**
   * Registers a new user, then logs in automatically with the same credentials.
   * The backend register endpoint returns only user data, not tokens.
   */
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
   * Stores tokens and updates the current user signal from the JWT payload.
   */
  private handleAuthSuccess(response: AuthResponse): void {
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    this._currentUser.set(this.decodeUserFromToken(response.accessToken));
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

  /**
   * Decodes the JWT payload to extract basic user info without a library.
   * JWTs are base64url-encoded — the payload is the second segment.
   */
  private decodeUserFromToken(token: string): User | null {
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload));

      return {
        id: decoded.sub,
        username: decoded.sub,
        email: '',
        firstName: '',
        lastName: '',
        roleName: decoded.role ?? '',
      };
    } catch {
      return null;
    }
  }
}
