import { HttpClient, HttpHeaders } from '@angular/common/http';
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

  createIntermediateCertificate(dto: CreateCertificate, token: string): Observable<boolean> {
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
    console.log("ASDDDDDDDDDD")
    console.log(token)
    return this.httpClient.post<boolean>(`${this.url}/intermediate`, dto, { headers });
  }
}
