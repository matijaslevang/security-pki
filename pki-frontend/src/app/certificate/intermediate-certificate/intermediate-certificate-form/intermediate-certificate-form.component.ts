import { Component, Inject, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { AuthService } from '../../../auth/auth.service';
import { IssuingCertificate } from '../../certicifate.model';
import { CertificateService } from '../../certificate.service';

@Component({
  selector: 'app-intermediate-certificate-form',
  templateUrl: './intermediate-certificate-form.component.html',
  styleUrls: ['./intermediate-certificate-form.component.css']
})
export class IntermediateCertificateFormComponent implements OnInit {
  form: FormGroup;
  result: any;
  issuingCertificates$!: Observable<IssuingCertificate[]>;
  currentUserUuid: string | null = null;

  constructor(
    private fb: FormBuilder,
    private certificateService: CertificateService,
    private authService: AuthService,
    public dialogRef: MatDialogRef<IntermediateCertificateFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { isAdmin: boolean }
  ) {
    this.form = this.fb.group({
      issuerSerialNumber: ['', Validators.required],
      subject: this.fb.group({
        givenName: ['', Validators.required],
        surname: ['', Validators.required],
        organization: ['', Validators.required],
        department: [''],
        country: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
        email: ['', [Validators.required, Validators.email]]
      }),
      startDate: [new Date(), Validators.required],
      endDate: ['', Validators.required],
    }, { validators: this.dateValidator });
  }

  async ngOnInit(): Promise<void> {
    if (this.data.isAdmin) {
      this.issuingCertificates$ = this.certificateService.getAllIssuingCertificates();
    } else {
      this.issuingCertificates$ = this.certificateService.getIssuingCertificates();
    }
    this.currentUserUuid = await this.authService.getUserId();
  }

  dateValidator(group: AbstractControl): ValidationErrors | null {
    const start = group.get('startDate')?.value;
    const end = group.get('endDate')?.value;
    if (start && end && new Date(start) >= new Date(end)) {
      return { endBeforeStart: true };
    }
    return null;
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const dto = {
      issuerUuid: this.currentUserUuid,
      issuerSerialNumber: this.form.value.issuerSerialNumber,
      subjectDto: this.form.value.subject,
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate,
      intermediate: true,
      selfSigned: false,
      skiaki: true,
      sanString: ''
    };

    this.certificateService.createIntermediateCertificate(dto)
      .subscribe({
        next: res => {
          this.result = 'Certificate created successfully!';
          this.dialogRef.close(true);
        },
        error: err => {
          this.result = `Error: ${err.message || 'Failed to create certificate.'}`;
          console.error(err);
        }
      });
  }
  
  onCancel(): void {
    this.dialogRef.close(false);
  }
}
