import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { ResendVerificationRequest } from '@core/models/auth.model';
import { environment } from '@environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class EmailVerificationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  verifyEmail(token: string): Observable<void> {
    return this.http.get<void>(`${this.baseUrl}/verify`, { params: { token } });
  }

  resendEmail(email: ResendVerificationRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/resend-verification`, email);
  }
}
