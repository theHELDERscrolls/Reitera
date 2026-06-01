import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';
import { AuthResponse, LoginRequest, RegisterRequest } from '@core/models/auth.model';
import { User } from '@core/models/user.model';

const AUTH_URL = 'http://localhost:8080/api/v1/auth';
const USERS_URL = 'http://localhost:8080/api/v1/users';

const mockUser: User = {
  id: 'user-1',
  username: 'alumno_reitera',
  email: 'alumno@reitera.app',
  firstName: 'Alumno',
  lastName: 'Reitera',
  avatarId: null,
  createdAt: '2024-01-01T00:00:00',
};

const mockAuthResponse: AuthResponse = {
  accessToken: 'access-abc',
  refreshToken: 'refresh-xyz',
  message: 'OK',
};

const mockLogin: LoginRequest = { email: 'alumno@reitera.app', password: 'secret' };

const mockRegister: RegisterRequest = {
  username: 'alumno_reitera',
  email: 'alumno@reitera.app',
  password: 'secret',
  confirmPassword: 'secret',
  firstName: 'Alumno',
  lastName: 'Reitera',
};

const configureTestBed = () => {
  const routerSpy = { navigate: vi.fn() };
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: Router, useValue: routerSpy },
    ],
  });
  const httpMock = TestBed.inject(HttpTestingController);
  return { httpMock, routerSpy };
};

describe('AuthService', () => {
  afterEach(() => {
    localStorage.clear();
  });

  describe('constructor', () => {
    it('does NOT make GET /users/me when localStorage is empty', () => {
      const { httpMock } = configureTestBed();
      TestBed.inject(AuthService);

      httpMock.expectNone(`${USERS_URL}/me`);
      httpMock.verify();
    });

    it('makes GET /users/me when accessToken is in localStorage', () => {
      localStorage.setItem('accessToken', 'pre-existing-token');
      const { httpMock } = configureTestBed();
      TestBed.inject(AuthService);

      httpMock.expectOne(`${USERS_URL}/me`).flush(mockUser);
      httpMock.verify();
    });
  });

  describe('without prior session', () => {
    let service: AuthService;
    let httpMock: HttpTestingController;
    let routerSpy: { navigate: ReturnType<typeof vi.fn> };

    beforeEach(() => {
      localStorage.clear();
      const setup = configureTestBed();
      httpMock = setup.httpMock;
      routerSpy = setup.routerSpy;
      service = TestBed.inject(AuthService);
    });

    afterEach(() => {
      httpMock.verify();
    });

    describe('login()', () => {
      it('makes POST to /auth/login with credentials', () => {
        service.login(mockLogin).subscribe();

        const req = httpMock.expectOne(`${AUTH_URL}/login`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(mockLogin);

        req.flush(mockAuthResponse);
        httpMock.expectOne(`${USERS_URL}/me`).flush(mockUser);
      });

      it('stores accessToken and refreshToken in localStorage after successful login', () => {
        service.login(mockLogin).subscribe();

        httpMock.expectOne(`${AUTH_URL}/login`).flush(mockAuthResponse);
        httpMock.expectOne(`${USERS_URL}/me`).flush(mockUser);

        expect(localStorage.getItem('accessToken')).toBe(mockAuthResponse.accessToken);
        expect(localStorage.getItem('refreshToken')).toBe(mockAuthResponse.refreshToken);
      });

      it('updates currentUser and isLoggedIn after receiving user data', () => {
        service.login(mockLogin).subscribe();

        httpMock.expectOne(`${AUTH_URL}/login`).flush(mockAuthResponse);
        httpMock.expectOne(`${USERS_URL}/me`).flush(mockUser);

        expect(service.currentUser()).toEqual(mockUser);
        expect(service.isLoggedIn()).toBe(true);
      });
    });

    describe('register()', () => {
      it('makes POST to /auth/register with form data', () => {
        service.register(mockRegister).subscribe();

        const req = httpMock.expectOne(`${AUTH_URL}/register`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(mockRegister);

        req.flush(null);
      });

      it('does NOT store tokens or update currentUser on register', () => {
        service.register(mockRegister).subscribe();
        httpMock.expectOne(`${AUTH_URL}/register`).flush(null);

        expect(localStorage.getItem('accessToken')).toBeNull();
        expect(service.isLoggedIn()).toBe(false);
      });
    });

    describe('logout()', () => {
      it('makes POST to /auth/logout when refreshToken is in localStorage', () => {
        localStorage.setItem('refreshToken', 'refresh-token');
        service.logout();

        const req = httpMock.expectOne(`${AUTH_URL}/logout`);
        expect(req.request.method).toBe('POST');
        req.flush(null);
      });

      it('clears localStorage and navigates to /auth/login on successful logout', () => {
        localStorage.setItem('accessToken', 'tok');
        localStorage.setItem('refreshToken', 'rtok');
        service.logout();

        httpMock.expectOne(`${AUTH_URL}/logout`).flush(null);

        expect(localStorage.getItem('accessToken')).toBeNull();
        expect(localStorage.getItem('refreshToken')).toBeNull();
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
      });

      it('clears the session even when the logout POST fails', () => {
        localStorage.setItem('refreshToken', 'rtok');
        service.logout();

        httpMock
          .expectOne(`${AUTH_URL}/logout`)
          .flush('Server error', { status: 500, statusText: 'Internal Server Error' });

        expect(localStorage.getItem('refreshToken')).toBeNull();
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
      });

      it('calls clearSession directly without POST when no refreshToken exists', () => {
        service.logout();

        httpMock.expectNone(`${AUTH_URL}/logout`);
        expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
      });
    });

    describe('getAccessToken()', () => {
      it('returns the token when it exists in localStorage', () => {
        localStorage.setItem('accessToken', 'my-token');
        expect(service.getAccessToken()).toBe('my-token');
      });

      it('returns null when no token is in localStorage', () => {
        expect(service.getAccessToken()).toBeNull();
      });
    });

    describe('refreshCurrentUser()', () => {
      it('makes GET /users/me and updates the currentUser signal', () => {
        service.refreshCurrentUser();

        httpMock.expectOne(`${USERS_URL}/me`).flush(mockUser);

        expect(service.currentUser()).toEqual(mockUser);
      });
    });

    describe('isLoggedIn computed', () => {
      it('is false on initial state', () => {
        expect(service.isLoggedIn()).toBe(false);
      });

      it('is true after fetchCurrentUser sets the user', () => {
        service.refreshCurrentUser();
        httpMock.expectOne(`${USERS_URL}/me`).flush(mockUser);

        expect(service.isLoggedIn()).toBe(true);
      });
    });
  });
});
