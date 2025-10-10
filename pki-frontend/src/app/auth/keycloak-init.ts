import { KeycloakService } from "keycloak-angular";
import { environment } from "../../env/environment";

export function initializeKeycloak(kc: KeycloakService) {
  return () =>
    kc.init({
      config: {
        url: environment.keycloakUrl,
        realm: environment.keycloakRealm,
        clientId: environment.keycloakClient,
      },
      initOptions: {
        onLoad: 'login-required',
        pkceMethod: 'S256',
        checkLoginIframe: false
      },
      loadUserProfileAtStartUp: true, // dobavlja username, email i role
    })
    .then(async () => {
      console.log('KC initialized');

      // Access token
      const token = await kc.getToken();
      console.log('Access Token:', token);

      // User info iz Keycloak-a
      const userProfile = await kc.loadUserProfile();
      console.log('User Profile:', userProfile);

      // Roles
      const roles = kc.getUserRoles();
      console.log('Roles:', roles);

      // Ako želiš sub iz tokena:
      const keycloakInstance = kc.getKeycloakInstance();
      console.log('Sub (user id):', keycloakInstance.tokenParsed?.sub);
    })
    .catch(err => {
      console.error('KC init failed', JSON.stringify(err, null, 2));
      return false;
    });
}
