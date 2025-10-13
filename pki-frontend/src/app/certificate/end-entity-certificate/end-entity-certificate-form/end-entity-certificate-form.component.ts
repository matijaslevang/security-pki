import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { tap } from 'rxjs/operators';
import { CreateCertificate } from '../../certicifate.model';
import { KcUser, CertificateService } from '../../certificate.service';
@Component({
  selector: 'app-end-entity-certificate-form',
  templateUrl: './end-entity-certificate-form.component.html',
  styleUrl: './end-entity-certificate-form.component.css'
})
export class EndEntityCertificateFormComponent implements OnInit {
  users: KcUser[] = [];
   form: FormGroup = new FormGroup({
      startDate: new FormControl(null, Validators.required),
      endDate: new FormControl(null, Validators.required),
      sanString: new FormControl(''),
      skiaki: new FormControl(false),
      issuerUserId: new FormControl(null, Validators.required)
    }, { validators: [this.dateRangeValidator] })
  
    constructor(private certificateService: CertificateService) {

    }

  ngOnInit(): void {
    this.certificateService.getEligibleUsers()
      .pipe(
        tap(list => {
          console.log('KC users:', list);                
          console.table(list.map(u => ({                 
            id: u.id, username: u.username, email: u.email,
            firstName: u.firstName, lastName: u.lastName
          })));
        })
      )
      .subscribe({
        next: list => this.users = list,
        error: err => console.error('getEligibleUsers failed:', err)
      });
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
        issuerSerialNumber: this.form.value.issuerUserId, 
        selfSigned: false,
        intermediate: false,
        skiaki: this.form.get('skiaki')?.value || false,
        sanString: this.form.get('sanString')?.value || '',
        startDate: this.form.get('startDate')?.value || undefined,
        endDate: this.form.get('endDate')?.value || undefined,
        subjectDto: null
      }
      this.certificateService.createEndEntityCertificate(newRequest).subscribe({
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
