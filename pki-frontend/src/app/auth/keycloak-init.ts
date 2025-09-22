import { KeycloakService } from 'keycloak-angular';
import { environment } from '../../env/environment';

export function initializeKeycloak(kc: KeycloakService) {
  return () =>
    kc.init({
      config: {
        url: environment.keycloakUrl,
        realm: environment.keycloakRealm,
        clientId: environment.keycloakClient,
      },
      initOptions: {
        onLoad: 'login-required',      // umesto 'check-sso'
        pkceMethod: 'S256',
        flow: 'standard',
        responseMode: 'fragment',
        checkLoginIframe: false,
        silentCheckSsoFallback: true,  // može ostati
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html'
      },
      loadUserProfileAtStartUp: false,
    }).catch(err => { console.error('KC init failed', err); return true; });
}
