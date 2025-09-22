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
}

// import { Injectable } from '@angular/core';
// import { KeycloakService } from 'keycloak-angular';

// @Injectable({ providedIn: 'root' })
// export class AuthService {
//   constructor(private kc: KeycloakService) {}

//   login() { return this.kc.login({ redirectUri: window.location.origin + '/' }); }
//   logout() { return this.kc.logout(window.location.origin + '/'); }
//   register() { return this.kc.register({ redirectUri: window.location.origin + '/' }); }

//   isLoggedIn() { return this.kc.isLoggedIn(); }
//   async token() { return await this.kc.getToken(); }
//   async profile() { return await this.kc.loadUserProfile(); }
// }
