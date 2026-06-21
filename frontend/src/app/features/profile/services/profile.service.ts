import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@environments/environment';
import { UpdateUserRequest, User } from '@core/models/user.model';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  updateMe(data: UpdateUserRequest): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/me`, data);
  }
}
