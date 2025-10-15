import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../env/environment';

export interface User {
  organization: string;
  id: number;
  keycloakId: string; // Ovo polje je neophodno
  displayName: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = `${environment.apiUrl}/api/users`;

  constructor(private http: HttpClient) {}

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl);
  }

  assignCertificate(userId: number, serialNumber: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/${userId}/assign-certificate`, { serialNumber });
  }
}
