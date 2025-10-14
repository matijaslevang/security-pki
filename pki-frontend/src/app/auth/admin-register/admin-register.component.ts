import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-admin-register',
  templateUrl: './admin-register.component.html',
  styleUrls: ['./admin-register.component.css']
})
export class AdminRegisterComponent {
  form: FormGroup;
  showModal = false;
  resultMessage: string = '';

  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.form = this.fb.group({
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      organization: ['', Validators.required],
      department: ['', Validators.required],
    },
  );
  }

  passwordMatchValidator(group: FormGroup) {
    const pw = group.get('password')?.value;
    const cpw = group.get('confirmPassword')?.value;
    return pw === cpw ? null : { passwordsMismatch: true };
  }

  openModal() { this.showModal = true; }
  closeModal() { this.showModal = false; this.form.reset(); this.resultMessage = ''; }

  registerUser() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const dto = {
      username: this.form.value.username,
      email: this.form.value.email,
      firstName: this.form.value.firstName,
      lastName: this.form.value.lastName,
      enabled: true,
      emailVerified: false,
      attributes: { organization:  this.form.value.organization,
        department: this.form.value.department
      }
    };

    this.authService.registerNewUser(dto)
      .subscribe({
        next: (response) => {
          const locationHeader = response.headers.get('Location');
          if (locationHeader) {
            const userId = locationHeader.split('/').pop();
            if (userId) {
              this.authService.sendVerificationEmail(userId).subscribe({
                next: () => {
                    console.log('Verification email sent');
                },
                error: (err) => {
                    console.error('Error sending verification email:', err);
                }
                });   
                
              this.authService.assignRoleToUser(userId, 'ca-user').subscribe({
                next: (response) => {
                    console.log('Role assigned');
                    console.log(response)
                },
                error: (err) => {
                    console.error('Error assigning role:', err);
                }
                });  
            }
          }
          this.resultMessage = `User ${dto.username} created`;
          this.form.reset();
        },
        error: err => {
          console.error(err);
          this.resultMessage = 'Error creating user. Check console.';
        }
      });
  }
}
