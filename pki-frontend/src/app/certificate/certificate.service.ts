import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../env/environment';
import { Observable } from 'rxjs';
import { CreateCertificate } from './certicifate.model';

@Injectable({
  providedIn: 'root'
})
export class CertificateService {

  url: string = environment.apiUrl + "/api/certificates"

  constructor(private httpClient: HttpClient) { }

  createSelfSignedCertificate(newCertificate: CreateCertificate): Observable<boolean> {
    return this.httpClient.post<boolean>(this.url + "/selfsigned", newCertificate);
  }
  createEndEntityCertificate(newCertificate: CreateCertificate): Observable<boolean> {
    return this.httpClient.post<boolean>(this.url + "/end-entity", newCertificate);
  }
  getEligibleUsers() {
    const params = { roles: 'admin-user,ca-user' }; 
    return this.httpClient.get<KcUser[]>('/iam/users', { params });
  }

}
export interface KcUser { id:string; username:string; email:string; firstName:string; lastName:string; }
