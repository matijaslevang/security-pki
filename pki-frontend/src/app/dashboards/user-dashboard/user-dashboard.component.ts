import { Component, ViewChild } from '@angular/core';
import { CertificateService } from '../../certificate/certificate.service';
import { AuthService } from '../../auth/auth.service';
import { EndEntityCertificateFormComponent } from '../../certificate/end-entity-certificate/end-entity-certificate-form/end-entity-certificate-form.component';
import { MatDialog } from '@angular/material/dialog';
import { EndEntityCertificateTableComponent } from '../../certificate/end-entity-certificate/end-entity-certificate-table/end-entity-certificate-table.component';
@Component({
  selector: 'app-user-dashboard',
  templateUrl: './user-dashboard.component.html',
  styleUrl: './user-dashboard.component.css'
})
export class UserDashboardComponent {
  @ViewChild('chainsList') chainsListComponent!: EndEntityCertificateTableComponent;
  constructor(private certificateService: CertificateService, private authService: AuthService, private dialog: MatDialog) {

  }
  create() {
    this.authService.getToken().then(token => this.certificateService.createIntermediateCertificate(null))
  }
   openCreateEndEntityForm(): void {
    const dialogRef = this.dialog.open(EndEntityCertificateFormComponent, {
      width: '800px',
      disableClose: true,
      data: { isAdmin: false }  // ca-user vidi samo svoje CA sertifikate kao issuere
    });
    dialogRef.afterClosed().subscribe(ok => { if (ok) this.chainsListComponent.loadCertificates(); });
  }
}
