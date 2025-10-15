import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

// === MATERIAL MODULES ===
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';

// === COMPONENTS ===
import { AdminRegisterComponent } from './admin-register/admin-register.component';
import { AssignCertificateFormComponent } from './assign-intermediate-certificate/assign-intermediate-certificate.component';

@NgModule({
  declarations: [
    AdminRegisterComponent,
    AssignCertificateFormComponent
  ],
  imports: [
    // Angular Modules
    CommonModule,
    ReactiveFormsModule,
    // Standalone Component
    // Material Modules
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatDialogModule
  ],
  exports: [
    AdminRegisterComponent // Eksportujemo da se može koristiti u drugim delovima aplikacije (npr. u nav-bar)
  ]
})
export class AuthModule { }
