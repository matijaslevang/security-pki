import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SelfSignedCertificateFormComponent } from './self-signed-certificate-form/self-signed-certificate-form.component';
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
import { IntermediateCertificateFormComponent } from './intermediate-certificate-form/intermediate-certificate-form.component';
import { MatSelectModule }    from '@angular/material/select';
import { MatOptionModule }    from '@angular/material/core';
import { EndEntityCertificateFormComponent } from './end-entity-certificate-form/end-entity-certificate-form.component';



@NgModule({
  exports: [
    SelfSignedCertificateFormComponent,
    EndEntityCertificateFormComponent,
  ],
  declarations: [
    SelfSignedCertificateFormComponent,
    IntermediateCertificateFormComponent,
    EndEntityCertificateFormComponent,
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
    BrowserAnimationsModule,
    MatSelectModule,
    MatOptionModule
  ]
})
export class CertificateModule { }
