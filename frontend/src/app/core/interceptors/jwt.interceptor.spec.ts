import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '@core/auth/auth.service';
import { AuthResponse } from '@core/models/auth.model';

const API = 'http://localhost:8080/api/v1';

const mockRefreshResponse: AuthResponse = {
  accessToken: 'new-access-token',
  refreshToken: 'new-refresh-token',
  message: 'OK',
};

describe('jwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let logoutSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    localStorage.clear();
    logoutSpy = vi.fn();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { logout: logoutSpy } },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('/auth/ routes — passthrough', () => {
    it('does NOT add Authorization header to requests to /auth/*', () => {
      localStorage.setItem('accessToken', 'existing-token');
      http.post(`${API}/auth/login`, {}).subscribe({ error: () => undefined });

      const req = httpMock.expectOne(`${API}/auth/login`);
      expect(req.request.headers.has('Authorization')).toBe(false);
      req.flush({});
    });
  });

  describe('protected routes', () => {
    it('adds Bearer token header when accessToken is in localStorage', () => {
      localStorage.setItem('accessToken', 'my-token');
      http.get(`${API}/decks`).subscribe();

      const req = httpMock.expectOne(`${API}/decks`);
      expect(req.request.headers.get('Authorization')).toBe('Bearer my-token');
      req.flush([]);
    });

    it('does NOT add Authorization header when no token is in localStorage', () => {
      http.get(`${API}/decks`).subscribe({ error: () => undefined });

      const req = httpMock.expectOne(`${API}/decks`);
      expect(req.request.headers.has('Authorization')).toBe(false);
      req.flush(null, { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('401 error handling', () => {
    it('attempts token refresh on 401 when refreshToken exists', () => {
      localStorage.setItem('accessToken', 'old-token');
      localStorage.setItem('refreshToken', 'refresh-token');

      http.get(`${API}/decks`).subscribe({ error: () => undefined });

      httpMock.expectOne(`${API}/decks`).flush(null, { status: 401, statusText: 'Unauthorized' });

      const refreshReq = httpMock.expectOne(`${API}/auth/refresh`);
      expect(refreshReq.request.method).toBe('POST');
      expect(refreshReq.request.body).toEqual({ refreshToken: 'refresh-token' });

      refreshReq.flush(mockRefreshResponse);
      httpMock.expectOne(`${API}/decks`).flush([]);
    });

    it('retries the original request with the new token after successful refresh', () => {
      localStorage.setItem('accessToken', 'old-token');
      localStorage.setItem('refreshToken', 'refresh-token');

      let responseData: unknown;
      http.get(`${API}/decks`).subscribe((data) => (responseData = data));

      httpMock.expectOne(`${API}/decks`).flush(null, { status: 401, statusText: 'Unauthorized' });
      httpMock.expectOne(`${API}/auth/refresh`).flush(mockRefreshResponse);

      const retryReq = httpMock.expectOne(`${API}/decks`);
      expect(retryReq.request.headers.get('Authorization')).toBe(
        `Bearer ${mockRefreshResponse.accessToken}`,
      );
      retryReq.flush([{ id: 1 }]);

      expect(responseData).toEqual([{ id: 1 }]);
    });

    it('updates tokens in localStorage after successful refresh', () => {
      localStorage.setItem('accessToken', 'old-token');
      localStorage.setItem('refreshToken', 'old-refresh');

      http.get(`${API}/decks`).subscribe({ error: () => undefined });

      httpMock.expectOne(`${API}/decks`).flush(null, { status: 401, statusText: 'Unauthorized' });
      httpMock.expectOne(`${API}/auth/refresh`).flush(mockRefreshResponse);
      httpMock.expectOne(`${API}/decks`).flush([]);

      expect(localStorage.getItem('accessToken')).toBe(mockRefreshResponse.accessToken);
      expect(localStorage.getItem('refreshToken')).toBe(mockRefreshResponse.refreshToken);
    });

    it('calls AuthService.logout() on 401 when no refreshToken exists', () => {
      localStorage.setItem('accessToken', 'old-token');

      http.get(`${API}/decks`).subscribe({ error: () => undefined });

      httpMock.expectOne(`${API}/decks`).flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(logoutSpy).toHaveBeenCalledTimes(1);
    });

    it('calls AuthService.logout() when the refresh request itself fails', () => {
      localStorage.setItem('accessToken', 'old-token');
      localStorage.setItem('refreshToken', 'expired-refresh');

      http.get(`${API}/decks`).subscribe({ error: () => undefined });

      httpMock.expectOne(`${API}/decks`).flush(null, { status: 401, statusText: 'Unauthorized' });
      httpMock
        .expectOne(`${API}/auth/refresh`)
        .flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(logoutSpy).toHaveBeenCalledTimes(1);
    });

    it('does NOT attempt refresh for non-401 errors', () => {
      localStorage.setItem('accessToken', 'token');
      localStorage.setItem('refreshToken', 'refresh-token');

      http.get(`${API}/decks`).subscribe({ error: () => undefined });

      httpMock
        .expectOne(`${API}/decks`)
        .flush(null, { status: 500, statusText: 'Internal Server Error' });

      httpMock.expectNone(`${API}/auth/refresh`);
    });
  });
});
