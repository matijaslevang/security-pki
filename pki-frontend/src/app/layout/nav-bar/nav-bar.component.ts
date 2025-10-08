import { Component } from '@angular/core';
import { AuthService } from '../../auth/auth.service';
@Component({
  selector: 'app-nav-bar',
  templateUrl: './nav-bar.component.html',
  styleUrls: ['./nav-bar.component.css']
})
export class NavBarComponent {
logged = false;
constructor(public auth: AuthService) {}

ngOnInit() { this.logged = this.auth.isLoggedIn();
  console.log(this.auth.isLoggedIn())
 }

}
