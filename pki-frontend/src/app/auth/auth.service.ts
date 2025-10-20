import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../env/environment';
import { Observable, switchMap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {

  url: string = environment.apiUrl + "/api/certificates"

  constructor(private kc: KeycloakService, private httpClient: HttpClient) {}

  login(redirect = window.location.href) {
    return this.kc.login({ redirectUri: redirect });
  }

  register(redirect = window.location.href) {
    return this.kc.register({ redirectUri: redirect }); // OTVARA KC registraciju
  }

  logout(redirect = window.location.origin) {
    return this.kc.logout(redirect);
  }

  isLoggedIn() { return this.kc.isLoggedIn(); }
  getToken() { return this.kc.getToken(); }
  getProfile() { return this.kc.loadUserProfile(); }

  async getUserId(): Promise<string | null> {
    if (!await this.isLoggedIn()) {
      return null; 
    }
    
    const userProfile = await this.getProfile();
    return userProfile.id ?? null;
  }

  registerNewUser(user: Object) :Observable<any>{
    return this.httpClient.post(`${environment.keycloakUrl}/admin/realms/${environment.keycloakRealm}/users`, user, { observe: 'response' });
  }

  sendVerificationEmail(userId: string): Observable<any> {
    return this.httpClient.put(`${environment.keycloakUrl}/admin/realms/${environment.keycloakRealm}/users/${userId}/execute-actions-email`,
      ['VERIFY_EMAIL', 'UPDATE_PASSWORD']);
  }

  assignRoleToUser(userId: string, roleName: string): Observable<any> {
    console.log(roleName)
    return this.httpClient.get<any[]>(`${environment.keycloakUrl}/admin/realms/${environment.keycloakRealm}/roles`).pipe(
      switchMap(roles => {
        const role = roles.find(r => r.name === roleName);
        console.log(roles)
        if (!role) throw new Error(`Role ${roleName} not found`);
        return this.httpClient.post(`${environment.keycloakUrl}/admin/realms/${environment.keycloakRealm}/users/${userId}/role-mappings/realm`, [role]);
      })
    );
  }

  getAllRoles(): Observable<any[]> {
    return this.httpClient.get<any[]>(`${environment.keycloakUrl}/admin/realms/${environment.keycloakRealm}/roles`);
  }
}

