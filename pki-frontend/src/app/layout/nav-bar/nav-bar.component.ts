import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../auth/auth.service';
import { KeycloakService } from 'keycloak-angular';

@Component({
  selector: 'app-nav-bar',
  templateUrl: './nav-bar.component.html',
  styleUrls: ['./nav-bar.component.css']
})
export class NavBarComponent implements OnInit {
  logged = false;
  isAdmin = false;

  constructor(
    public auth: AuthService,
    private keycloakService: KeycloakService
  ) {}

  async ngOnInit(): Promise<void> {
    this.logged = await this.auth.isLoggedIn();

    if (this.logged) {
      const roles = this.keycloakService.getUserRoles(true); // true = sve uloge
      this.isAdmin = roles.includes('admin-user'); // zameni sa pravim nazivom tvoje admin uloge
    }

    console.log('User logged in:', this.logged);
    console.log('Is admin:', this.isAdmin);
  }
}
