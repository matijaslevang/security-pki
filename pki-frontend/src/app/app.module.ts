import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { KeycloakAngularModule, KeycloakService, KeycloakBearerInterceptor } from 'keycloak-angular';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

// === MATERIAL MODULES ===
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';

// === APP ROUTING & MAIN COMPONENT ===
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

// === AUTH & INIT ===
import { initializeKeycloak } from './auth/keycloak-init';

// === LAYOUT ===
import { NavBarComponent } from './layout/nav-bar/nav-bar.component';

// === DASHBOARDS ===
import { AdminDashboardComponent } from './dashboards/admin-dashboard/admin-dashboard.component';
import { CaDashboardComponent } from './dashboards/ca-dashboard/ca-dashboard.component';
import { UserDashboardComponent } from './dashboards/user-dashboard/user-dashboard.component';

// === CERTIFICATE COMPONENTS ===
import { CertificateModule } from './certificate/certificate.module';
import { SelfSignedCertificateTableComponent } from './certificate/self-signed-certificate/self-signed-certificate-table/self-signed-certificate-table.component';
import { IntermediateCertificateTableComponent } from './certificate/intermediate-certificate/intermediate-certificate-table/intermediate-certificate-table.component';
import { EndEntityCertificateTableComponent } from './certificate/end-entity-certificate/end-entity-certificate-table/end-entity-certificate-table.component';
import { AuthModule } from './auth/auth.module';


@NgModule({
  declarations: [
    AppComponent,
    NavBarComponent,
    AdminDashboardComponent,
    CaDashboardComponent,
    UserDashboardComponent,
    // Komponente koje su nedostajale:
    // SelfSignedCertificateTableComponent,
    // IntermediateCertificateTableComponent,
    // EndEntityCertificateTableComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    KeycloakAngularModule,
    ReactiveFormsModule,
    CertificateModule,
    MatInputModule,
    AuthModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatCheckboxModule,
    MatSelectModule,
    MatOptionModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
  ],
  providers: [
    provideAnimationsAsync(),
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      deps: [KeycloakService, Router],
      multi: true,
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: KeycloakBearerInterceptor,
      multi: true,
    },
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }