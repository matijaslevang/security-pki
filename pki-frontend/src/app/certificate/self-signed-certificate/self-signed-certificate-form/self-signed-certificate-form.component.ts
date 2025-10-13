import { Component } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { CertificateService } from '../../certificate.service';
import { CreateCertificate } from '../../certicifate.model';


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
    skiaki: new FormControl(false)
  }, { validators: [this.dateRangeValidator] })

  constructor(private certificateService: CertificateService) {

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
        issuerUuid: null,
        selfSigned: true,
        intermediate: false,
        skiaki: this.form.get('skiaki')?.value || false,
        sanString: this.form.get('sanString')?.value || '',
        startDate: this.form.get('startDate')?.value || undefined,
        endDate: this.form.get('endDate')?.value || undefined,
        subjectDto: null
      }
      this.certificateService.createSelfSignedCertificate(newRequest).subscribe({
        next: (created: boolean) => {
          if (created) {
            console.log("Created")
          }
        }
      });
    } else {
      this.form.markAllAsTouched();
    }
  }
}
