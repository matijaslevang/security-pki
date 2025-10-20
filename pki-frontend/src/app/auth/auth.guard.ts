// src/app/auth/auth.guard.ts
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { KeycloakAuthGuard, KeycloakService } from 'keycloak-angular';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard extends KeycloakAuthGuard {
  constructor(protected override router: Router, protected keycloakService: KeycloakService) {
    super(router, keycloakService);
  }

  public async isAccessAllowed(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Promise<boolean | UrlTree> {
    
    const requiredRoles = route.data['roles'];
    console.log(requiredRoles)
    if (!requiredRoles || requiredRoles.length === 0) {
      return true; 
    }

    const hasRequiredRole = requiredRoles.some((role: string) => this.roles.includes(role));

    if (hasRequiredRole) {
      return true;
    } else {
      console.error('Pristup odbijen. Korisnik nema potrebnu ulogu.');
      this.router.navigate(['/login']); 
      return false;
    }
  }
}