import { Component } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { CertificateService } from '../../certificate.service';
import { CreateCertificate } from '../../certicifate.model';
import { MatDialogRef } from '@angular/material/dialog';


@Component({
  selector: 'app-self-signed-certificate-form',
  templateUrl: './self-signed-certificate-form.component.html',
  styleUrl: './self-signed-certificate-form.component.css'
})
export class SelfSignedCertificateFormComponent {

  form: FormGroup = new FormGroup({
    startDate: new FormControl(null, Validators.required),
    endDate: new FormControl(null, Validators.required),
    sanString: new FormControl(''),
    skiaki: new FormControl(false),
    commonName: new FormControl('', Validators.required),
    givenName: new FormControl('', Validators.required),
    surname: new FormControl('', Validators.required),
    organization: new FormControl('', Validators.required),
    department: new FormControl('', Validators.required),
    country: new FormControl('', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    digitalSignature: new FormControl(false),
    nonRepudiation: new FormControl(false),
    keyEncipherment: new FormControl(false),
    dataEncipherment: new FormControl(false),
    keyAgreement: new FormControl(false),
    cRLSign: new FormControl(false),
    serverAuth: new FormControl(false),
    clientAuth: new FormControl(false),
    codeSigning: new FormControl(false),
    emailProtection: new FormControl(false),
    timeStamping: new FormControl(false)
  }, { validators: [this.dateRangeValidator] })

  constructor(private certificateService: CertificateService, public dialogRef: MatDialogRef<SelfSignedCertificateFormComponent>) {

  }

  dateRangeValidator(control: AbstractControl): ValidationErrors | null {
    const start = control.get('startDate')?.value;
    const end = control.get('endDate')?.value;

    if (start && end && end <= start) {
      return { endBeforeStart: true };
    }

    return null;
  }

  submit(): void {
    if (this.form.valid) {
      console.log('Form submitted:', this.form.value);
      let newRequest: CreateCertificate = {
        issuerSerialNumber: null,
        selfSigned: true,
        intermediate: false,
        skiaki: this.form.get('skiaki')?.value || false,
        sanString: this.form.get('sanString')?.value || '',
        startDate: this.form.get('startDate')?.value || undefined,
        endDate: this.form.get('endDate')?.value || undefined,
        subjectDto: {
          commonName: this.form.get('commonName')?.value || '',
          surname: this.form.get('surname')?.value || '',
          givenName: this.form.get('givenName')?.value || '',
          organization: this.form.get('organization')?.value || '',
          department: this.form.get('givenName')?.value || '',
          email: this.form.get('email')?.value || '',
          country: this.form.get('country')?.value || ''
        },
        keyUsageValues: [this.form.value.digitalSignature, this.form.value.nonRepudiation, this.form.value.keyEncipherment, this.form.value.dataEncipherment, this.form.value.keyAgreement, this.form.value.cRLSign],
        extKeyUsageValues: [this.form.value.serverAuth, this.form.value.clientAuth, this.form.value.codeSigning, this.form.value.emailProtection, this.form.value.timeStamping]
      }
      console.log(newRequest)
      this.certificateService.createSelfSignedCertificate(newRequest).subscribe({
        next: (created: boolean) => {
          if (created) {
            console.log("Created")
            this.dialogRef.close(true);
          }
        }
      });
    } else {
      this.form.markAllAsTouched();
    }
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
