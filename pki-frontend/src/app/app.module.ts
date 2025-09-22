import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { APP_INITIALIZER, NgModule } from '@angular/core';
import { KeycloakAngularModule, KeycloakService, KeycloakBearerInterceptor } from 'keycloak-angular';
import { environment } from '../env/environment';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';
import { CertificateModule } from './certificate/certificate.module';
import { provideHttpClient } from '@angular/common/http';
import { NavBarComponent } from './layout/nav-bar/nav-bar.component';
import { initializeKeycloak } from './auth/keycloak-init';


@NgModule({
  declarations: [
    AppComponent,
    NavBarComponent
  ],
  imports: [
    CertificateModule,
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
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
    provideHttpClient(),
    KeycloakAngularModule,
    KeycloakService,
    HttpClientModule,
    { provide: APP_INITIALIZER, useFactory: initializeKeycloak, deps: [KeycloakService], multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: KeycloakBearerInterceptor, multi: true },
  ],
  
  bootstrap: [AppComponent]
})
export class AppModule { }
