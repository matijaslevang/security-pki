import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../env/environment';
import { Observable } from 'rxjs';
import { CertificateChain, CertificateChainDisplay, CertificateInfo, CertTemplate, CreateCertificate, CreateCertTemplate, IssuingCertificate } from './certicifate.model';

@Injectable({
  providedIn: 'root'
})
export class CertificateService {

  url: string = environment.apiUrl + "/api/certificates"

  constructor(private httpClient: HttpClient) { }
  
  createSelfSignedCertificate(newCertificate: CreateCertificate): Observable<boolean> {
    return this.httpClient.post<boolean>(this.url + "/selfsigned", newCertificate);
  }

  createIntermediateCertificate(dto: CreateCertificate): Observable<boolean> {
    return this.httpClient.post<boolean>(`${this.url}/intermediate`, dto);
  }

  createEndEntityCertificate(newCertificate: CreateCertificate): Observable<boolean> {
    return this.httpClient.post<boolean>(this.url + "/end-entity", newCertificate);
  }

  getIssuingCertificates(): Observable<IssuingCertificate[]> {
    return this.httpClient.get<IssuingCertificate[]>(`${this.url}/my-issuing-certificates`);
  }

  getAllIssuingCertificates(): Observable<IssuingCertificate[]> {
    return this.httpClient.get<IssuingCertificate[]>(`${this.url}/all-issuing-certificates`);
  }

  getEligibleUsers() {
    const params = { roles: 'admin-user,ca-user' }; 
    return this.httpClient.get<KcUser[]>('/iam/users', { params });
  }

  getMyCertificates(): Observable<CertificateInfo[]> {
    return this.httpClient.get<any[]>(`${this.url}/my-certificates`);
  }

  // Metoda za preuzimanje .p12 fajla
  downloadCertificate(serialNumber: string): Observable<Blob> {
    // Važno: responseType mora biti 'blob' da bi se preuzeo fajl
    return this.httpClient.get(`${this.url}/${serialNumber}/download`, {
      responseType: 'blob'
    });
  }

  getMyChains(): Observable<CertificateChain[]> {
    return this.httpClient.get<CertificateChain[]>(`${this.url}/my-chains`);
  }

  getAllCertificateChains(): Observable<CertificateChainDisplay[]> {
    return this.httpClient.get<CertificateChainDisplay[]>(`${this.url}/all-chains`);
  }

  createCertificateTemplate(newTemplate: CreateCertTemplate): Observable<any> {
    return this.httpClient.post<any>(this.url + "/template", newTemplate);
  }

  getAllTemplatesForCertificateSerial(serial: string): Observable<CertTemplate[]> {
    return this.httpClient.get<CertTemplate[]>(this.url + "/templates/" + serial);
  }

  getAssignableIntermediateCertificates(): Observable<IssuingCertificate[]> {
    return this.httpClient.get<IssuingCertificate[]>(`${this.url}/intermediate/assignable`);
  }

}
export interface KcUser { id:string; username:string; email:string; firstName:string; lastName:string; }
