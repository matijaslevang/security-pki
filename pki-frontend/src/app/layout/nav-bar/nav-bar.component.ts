import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../auth/auth.service';
import { KeycloakService } from 'keycloak-angular';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../env/environment';

@Component({
  selector: 'app-nav-bar',
  templateUrl: './nav-bar.component.html',
  styleUrls: ['./nav-bar.component.css']
})
export class NavBarComponent implements OnInit {
  logged = false;
  isAdmin = false;

  url: string = environment.apiUrl + "/api/certificates"

  constructor(
    public auth: AuthService,
    private keycloakService: KeycloakService,
    private httpClient: HttpClient
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
