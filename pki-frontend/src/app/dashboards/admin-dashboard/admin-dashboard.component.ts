import { Component } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';
import { AuthService } from '../../auth/auth.service';
import { CertificateService } from '../../certificate/certificate.service';
import { CreateCertificate, Subject } from '../../certificate/certicifate.model';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent {

  isUserAdmin: boolean = false;

  constructor(private kc: KeycloakService, private certificateService: CertificateService, private authService: AuthService) {
    this.isUserAdmin = this.kc.isUserInRole('administrator');
  }

    async fakJu() {
      const token = await this.authService.getToken();
      const exampleSubject: Subject = {
          surname: "Ivan",
          givenName: "Zvonko",
          organization: "MyCompany",
          department: "IT",
          email: "zvonko.ivan@example.com",
          country: "RS"
      };

      const exampleCertificate: CreateCertificate = {
          issuerUuid: "123e4567-e89b-12d3-a456-426614174000",
          selfSigned: false,
          intermediate: true,
          skiaki: true,
          sanString: "www.example.com, example.com",
          startDate: new Date("2025-10-08T00:00:00Z"),
          endDate: new Date("2026-10-08T00:00:00Z"),
          subjectDto: exampleSubject
      };
      this.certificateService.createIntermediateCertificate(exampleCertificate, token).subscribe({
        next: (asd: any) => {
          console.log("IZVRSENO:", asd)
        }
      }
      );

  }
}
