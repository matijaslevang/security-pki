import { Component, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { SelfSignedCertificateTableComponent } from '../../certificate/self-signed-certificate/self-signed-certificate-table/self-signed-certificate-table.component';
import { IntermediateCertificateFormComponent } from '../../certificate/intermediate-certificate/intermediate-certificate-form/intermediate-certificate-form.component';
import { SelfSignedCertificateFormComponent } from '../../certificate/self-signed-certificate/self-signed-certificate-form/self-signed-certificate-form.component';
import { CreateTemplateFormComponent } from '../../certificate/templates/create-template-form/create-template-form.component';
import { EndEntityCertificateFormComponent } from '../../certificate/end-entity-certificate/end-entity-certificate-form/end-entity-certificate-form.component';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css'],
})
export class AdminDashboardComponent {

  @ViewChild('certList') certListComponent!: SelfSignedCertificateTableComponent;

  constructor(private dialog: MatDialog) {}

  openCreateIntermediateForm(): void {
    const dialogRef = this.dialog.open(IntermediateCertificateFormComponent, {
      width: '800px',
      disableClose: true,
      data: { isAdmin: true }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.certListComponent.loadAllChains();
      }
    });
  }

  openCreateRootForm(): void {
    const dialogRef = this.dialog.open(SelfSignedCertificateFormComponent, {
      width: '800px',
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.certListComponent.loadAllChains();
      }
    });
  }

  openCreateEndEntityForm(): void {
    const dialogRef = this.dialog.open(EndEntityCertificateFormComponent, {
      width: '800px',
      disableClose: true,
      data: { isAdmin: true } 
    });
    dialogRef.afterClosed().subscribe(ok => { if (ok) this.certListComponent.loadAllChains(); });
  }

  openTemplateForm(): void {
    const dialogRef = this.dialog.open(CreateTemplateFormComponent, {
      width: '800px',
      disableClose: true,
      data: { isAdmin: true }
    });
  }
  
}
