import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { environment } from '../../../env/environment';

@Component({
  selector: 'app-intermediate-certificate-form',
  templateUrl: './intermediate-certificate-form.component.html',
  styleUrl: './intermediate-certificate-form.component.css'
})
export class IntermediateCertificateFormComponent implements OnInit{
form: FormGroup;
  result: any;

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.form = this.fb.group({
      token: ['', Validators.required],
      issuerUuid: ['', Validators.required],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      skiaki: [true]
    }, { validators: this.dateValidator });
  }

  ngOnInit(): void {}

  dateValidator(group: AbstractControl): ValidationErrors | null {
    const start = group.get('startDate')?.value;
    const end = group.get('endDate')?.value;
    if (start && end && new Date(start) > new Date(end)) {
      return { endBeforeStart: true };
    }
    return null;
  }

  submit() {
    if (this.form.invalid) return;

    const dto = {
      issuerUuid: this.form.value.issuerUuid,
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate,
      selfSigned: false,
      intermediate: true,
      skiaki: this.form.value.skiaki,
      sanString: ''
    };

    const headers = new HttpHeaders({ Authorization: `Bearer ${this.form.value.token}` });

    this.http.post<boolean>(`${environment.apiUrl}/api/certificates/intermediate`, dto, { headers })
      .subscribe({
        next: res => this.result = res,
        error: err => this.result = err.message || err
      });
  }
}