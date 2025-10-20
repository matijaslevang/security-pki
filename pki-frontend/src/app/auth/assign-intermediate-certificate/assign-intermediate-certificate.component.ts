import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Observable, map, of, tap } from 'rxjs';
import { User, UserService } from '../user.service';
import { CertificateInfo } from '../../certificate/certicifate.model';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-assign-intermediate-certificate',
  templateUrl: './assign-intermediate-certificate.component.html',
  styleUrls: ['./assign-intermediate-certificate.component.css']
})
export class AssignCertificateFormComponent implements OnInit {
  form: FormGroup;
  users$!: Observable<User[]>;
  
  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private authService: AuthService,
    public dialogRef: MatDialogRef<AssignCertificateFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { certificateToAssign: CertificateInfo }
  ) {
    this.form = this.fb.group({
      userId: ['', Validators.required]
    });
  }

  async ngOnInit(): Promise<void> {
    const currentKeycloakId = await this.authService.getUserId();
    console.log('1. Current Logged In User ID (Admin):', currentKeycloakId);

    console.log('2. Certificate Subject to parse:', this.data.certificateToAssign.subject);
    const certificateOrganization = this.getOrganizationFromSubject(this.data.certificateToAssign.subject);
    console.log('3. Organization parsed from certificate:', certificateOrganization);

    if (!certificateOrganization) {
      console.error("Could not determine organization from the certificate's subject. User list will be empty.");
      this.users$ = of([]);
      return;
    }

    this.users$ = this.userService.getUsers().pipe(
      tap(users => console.log('4. All users received from service:', users)),
      map(users => {
        const filteredUsers = users.filter(user =>
          user.keycloakId !== currentKeycloakId &&
          user.organization === certificateOrganization
        );
        console.log('5. Users after filtering (same organization, not admin):', filteredUsers);
        return filteredUsers;
      })
    );
  }

  private getOrganizationFromSubject(subject: string): string | null {
    const match = subject.match(/\bO=([^,]+)/);
    return match ? match[1].trim() : null;
  }

  submit(): void {
    if (this.form.invalid) return;
    
    const userId = this.form.value.userId;
    const certificateSerialNumber = this.data.certificateToAssign.serialNumber;
    
    console.log(`Submitting assignment: User ID: ${userId}, Certificate SN: ${certificateSerialNumber}`);

    this.userService.assignCertificate(userId, certificateSerialNumber).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => console.error("Failed to assign certificate", err)
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}

