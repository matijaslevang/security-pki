import { Component } from '@angular/core';
import { CertificateService } from '../../certificate/certificate.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-user-dashboard',
  templateUrl: './user-dashboard.component.html',
  styleUrl: './user-dashboard.component.css'
})
export class UserDashboardComponent {

  constructor(private certificateService: CertificateService, private authService: AuthService) {

  }
  create() {
    this.authService.getToken().then(token => this.certificateService.createIntermediateCertificate(null, token))
  }
}
