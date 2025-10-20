import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
// import { AuthGuard } from './auth/auth.guard'; // Uvezi AuthGuard servis
import { AppComponent } from './app.component';
import { AdminDashboardComponent } from './dashboards/admin-dashboard/admin-dashboard.component';
import { CaDashboardComponent } from './dashboards/ca-dashboard/ca-dashboard.component';
import { UserDashboardComponent } from './dashboards/user-dashboard/user-dashboard.component';
import { AuthGuard } from './auth/auth.guard';
import { IntermediateCertificateFormComponent } from './certificate/intermediate-certificate/intermediate-certificate-form/intermediate-certificate-form.component';


const routes: Routes = [

    { path: 'certificate-form', component: IntermediateCertificateFormComponent },
  {
    path: 'admin-dashboard',
    component: AdminDashboardComponent,
    canActivate: [AuthGuard], 
    data: {
      roles: ['admin-user']
    }
  },

  {
    path: 'ca-dashboard',
    component: CaDashboardComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['ca-user']
    }
  },

  {
    path: 'user-dashboard',
    component: UserDashboardComponent,
    canActivate: [AuthGuard],
    data: {
      roles: ['normal-user']
    }
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }