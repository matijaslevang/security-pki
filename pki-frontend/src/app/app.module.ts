// src/app/app.module.ts

import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { KeycloakAngularModule, KeycloakService, KeycloakBearerInterceptor } from 'keycloak-angular';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { initializeKeycloak } from './auth/keycloak-init';
import { NavBarComponent } from './layout/nav-bar/nav-bar.component';
import { AdminDashboardComponent } from './dashboards/admin-dashboard/admin-dashboard.component';
import { CaDashboardComponent } from './dashboards/ca-dashboard/ca-dashboard.component';
import { UserDashboardComponent } from './dashboards/user-dashboard/user-dashboard.component';
import { CertificateModule } from './certificate/certificate.module';

// Uvezi sve Angular Material module
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';


@NgModule({
  declarations: [
    AppComponent,
    NavBarComponent,
    AdminDashboardComponent,
    CaDashboardComponent,
    UserDashboardComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    KeycloakAngularModule, // Ključno za Keycloak
    CertificateModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatCheckboxModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
  ],
  providers: [
    provideAnimationsAsync(),
    // Omogući `HttpClient` sa podrškom za interseptore
    provideHttpClient(withInterceptorsFromDi()),
    {
      // `APP_INITIALIZER` osigurava da se Keycloak inicijalizuje na startu aplikacije
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      deps: [KeycloakService],
      multi: true,
    },
    {
      // `KeycloakBearerInterceptor` automatski dodaje token u zaglavlje zahteva
      provide: HTTP_INTERCEPTORS,
      useClass: KeycloakBearerInterceptor,
      multi: true,
    },
    // `KeycloakService` je automatski obezbeđen od strane `KeycloakAngularModule`
    // Stari `HttpClientModule` nije potreban sa `provideHttpClient`
    // Stari način: provideHttpClient() je moderniji
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }