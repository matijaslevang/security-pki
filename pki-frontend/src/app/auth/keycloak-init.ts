import { Router } from '@angular/router';
import { KeycloakService } from "keycloak-angular";
import { environment } from "../../env/environment";

export function initializeKeycloak(kc: KeycloakService, router: Router) {
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
      loadUserProfileAtStartUp: true,
    })
    .then(async (authenticated) => {
      console.log('KC initialized');

      // Tvoja postojeća logika za logovanje ostaje, što je super za debug
      const token = await kc.getToken();
      console.log('Access Token:', token);
      const userProfile = await kc.loadUserProfile();
      console.log('User Profile:', userProfile);
      const roles = kc.getUserRoles(true); // Koristi 'true' da dobiješ sve uloge
      console.log('Roles:', roles);
      const keycloakInstance = kc.getKeycloakInstance();
      console.log('Sub (user id):', keycloakInstance.tokenParsed?.sub);

      // -----------------------------------------------------------------
      // NOVO: Logika za redirekciju na osnovu uloge
      // -----------------------------------------------------------------
      if (authenticated) {
        // Važna provera: preusmeri korisnika samo ako je na početnoj stranici ('/')
        // Ovo sprečava da ga vratiš na dashboard ako uradi refresh na nekoj drugoj stranici
        if (window.location.pathname === '/') {
          if (roles.includes('admin-user')) { // <-- Zameni 'admin' sa tvojim nazivom uloge
            console.log("Korisnik je admin, preusmeravam na /admin-dashboard...");
            router.navigate(['/admin-dashboard']);
          } else if (roles.includes('ca-user')) { // <-- Zameni 'ca' sa tvojim nazivom uloge
            console.log("Korisnik je CA, preusmeravam na /ca-dashboard...");
            router.navigate(['/ca-dashboard']);
          } else if (roles.includes('normal-user')) { // <-- Zameni 'user' sa tvojim nazivom uloge
            console.log("Korisnik je user, preusmeravam na /user-dashboard...");
            router.navigate(['/user-dashboard']);
          } else {
            // Ako korisnik nema nijednu od navedenih uloga, možeš ga ostaviti
            // na početnoj stranici ili ga preusmeriti na neku generičku.
            console.log("Korisnik nema prepoznatljivu ulogu za automatsku redirekciju.");
          }
        }
      }
      // -----------------------------------------------------------------

    })
    .catch(err => {
      console.error('KC init failed', JSON.stringify(err, null, 2));
      return false;
    });
}