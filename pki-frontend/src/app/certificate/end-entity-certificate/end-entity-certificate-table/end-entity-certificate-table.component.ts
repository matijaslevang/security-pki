import { Component, OnInit } from '@angular/core';
import { CertificateService } from '../../certificate.service';
import { KeycloakService } from 'keycloak-angular';
import { MatDialog } from '@angular/material/dialog';
import { RevocationDialogComponent } from '../../revocation-dialog/revocation-dialog.component';

@Component({
  selector: 'app-end-entity-certificate-table',
  templateUrl: './end-entity-certificate-table.component.html',
  styleUrls: ['./end-entity-certificate-table.component.css']
})
export class EndEntityCertificateTableComponent implements OnInit {

  certificates: any[] = [];
  userRole: string = '';

  constructor(
    private certificateService: CertificateService,
    private keycloakService: KeycloakService,
    private dialog: MatDialog) {}

  async ngOnInit(): Promise<void> {
    await this.loadUserRole();
    this.loadCertificates();
  }

  async loadUserRole(): Promise<void> {
    try {
      const roles = this.keycloakService.getUserRoles(true); // true = all realm roles
      console.log('User roles:', roles);

      if (roles.includes('admin-user')) {
        this.userRole = 'ADMIN';
      } else if (roles.includes('ca-user')) {
        this.userRole = 'CA';
      } else if (roles.includes('normal-user')) {
        this.userRole = 'USER';
      } else {
        this.userRole = 'UNKNOWN';
      }

    } catch (error) {
      console.error('Error loading user role:', error);
      this.userRole = 'UNKNOWN';
    }
  }

  loadCertificates(): void {
    this.certificateService.getMyCertificates().subscribe({
      next: (data) => {
        this.certificates = data;
        console.log('Certificates successfully loaded:', this.certificates);
      },
      error: (err) => {
        console.error('Error loading certificates:', err);
      }
    });
  }

  downloadCertificate(serialNumber: string): void {
    this.certificateService.downloadCertificate(serialNumber).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${serialNumber}.p12`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      },
      error: (err) => {
        console.error('Error downloading certificate:', err);
      }
    });
  }
}
