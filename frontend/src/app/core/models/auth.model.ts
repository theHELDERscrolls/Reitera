export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  message: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface ResendVerificationRequest {
  email: string;
}
