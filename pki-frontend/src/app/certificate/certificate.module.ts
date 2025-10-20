import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { IntermediateCertificateFormComponent } from './intermediate-certificate/intermediate-certificate-form/intermediate-certificate-form.component';
import { MatSelectModule }    from '@angular/material/select';
import { MatOptionModule }    from '@angular/material/core';
import { IntermediateCertificateTableComponent } from './intermediate-certificate/intermediate-certificate-table/intermediate-certificate-table.component';
import { SelfSignedCertificateTableComponent } from './self-signed-certificate/self-signed-certificate-table/self-signed-certificate-table.component';
import { SelfSignedCertificateFormComponent } from './self-signed-certificate/self-signed-certificate-form/self-signed-certificate-form.component';
import { EndEntityCertificateTableComponent } from './end-entity-certificate/end-entity-certificate-table/end-entity-certificate-table.component';
import { EndEntityCertificateFormComponent } from './end-entity-certificate/end-entity-certificate-form/end-entity-certificate-form.component';
import { CreateTemplateFormComponent } from './templates/create-template-form/create-template-form.component';
import { RevocationDialogComponent } from './revocation-dialog/revocation-dialog.component';
import { MatTabsModule } from '@angular/material/tabs';

@NgModule({
  exports: [
    SelfSignedCertificateFormComponent,
    EndEntityCertificateFormComponent,
    EndEntityCertificateTableComponent,
    SelfSignedCertificateTableComponent,
    IntermediateCertificateTableComponent,
    IntermediateCertificateFormComponent,
  ],
  declarations: [
    SelfSignedCertificateFormComponent,
    IntermediateCertificateFormComponent,
    EndEntityCertificateFormComponent,
    IntermediateCertificateTableComponent,
    SelfSignedCertificateTableComponent,
    EndEntityCertificateTableComponent,
    CreateTemplateFormComponent,
    RevocationDialogComponent,
    
  ],
  imports: [
    CommonModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatCheckboxModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatTabsModule,
    BrowserAnimationsModule,
    MatSelectModule,
    MatOptionModule
  ]
})
export class CertificateModule { }
