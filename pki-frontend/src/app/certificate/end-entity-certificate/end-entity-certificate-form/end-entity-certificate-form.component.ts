import { Component, Inject, OnInit } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { CreateCertificate, IssuingCertificate } from '../../certicifate.model';
import { CertificateService } from '../../certificate.service';

@Component({
  selector: 'app-end-entity-certificate-form',
  templateUrl: './end-entity-certificate-form.component.html',
  styleUrls: ['./end-entity-certificate-form.component.css']
})
export class EndEntityCertificateFormComponent implements OnInit {
  issuingCertificates$!: Observable<IssuingCertificate[]>;

  form: FormGroup = new FormGroup({
    issuerSerialNumber: new FormControl(null, Validators.required),
    startDate: new FormControl(new Date(), Validators.required),
    endDate: new FormControl(null, Validators.required),
    sanString: new FormControl(''),
    skiaki: new FormControl(false),
    surname: new FormControl('', Validators.required),
    givenName: new FormControl('', Validators.required),
    organization: new FormControl(''),
    department: new FormControl(''),
    email: new FormControl('', Validators.email),
    country: new FormControl('', [Validators.pattern(/^[A-Z]{2}$/)]) // ISO 3166-1 alpha-2
  }, { validators: [EndEntityCertificateFormComponent.dateRangeValidator] });

  constructor(
    private certificateService: CertificateService,
    private dialogRef: MatDialogRef<EndEntityCertificateFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { isAdmin: boolean }
  ) {}

  ngOnInit(): void {
    this.issuingCertificates$ = this.data?.isAdmin
      ? this.certificateService.getAllIssuingCertificates()
      : this.certificateService.getIssuingCertificates();
  }

  static dateRangeValidator(control: AbstractControl): ValidationErrors | null {
    const s = control.get('startDate')?.value;
    const e = control.get('endDate')?.value;
    return (s && e && new Date(s) >= new Date(e)) ? { endBeforeStart: true } : null;
  }

  submit(): void {
    if (!this.form.valid) { 
      this.form.markAllAsTouched(); return; }
    
    const req: CreateCertificate = {
      issuerSerialNumber: this.form.value.issuerSerialNumber, // <-- serial issuer CA sertifikata
      selfSigned: false,
      intermediate: false,
      skiaki: !!this.form.value.skiaki,
      sanString: this.form.value.sanString || '',
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate,
      subjectDto: {
      surname: this.form.value.surname,
      givenName: this.form.value.givenName,
      organization: this.form.value.organization,
      department: this.form.value.department,
      email: this.form.value.email,
      country: this.form.value.country
    }
    };
    this.certificateService.createEndEntityCertificate(req).subscribe({
      next: ok => {
        if (ok) this.dialogRef.close(true);
      },
      error: err => console.error('createEndEntityCertificate failed:', err)
    });

  }
  onCancel(): void {
    this.dialogRef.close(false);
  } 
}
