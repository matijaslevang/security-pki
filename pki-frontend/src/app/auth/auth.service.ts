import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private kc: KeycloakService) {}

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
}

