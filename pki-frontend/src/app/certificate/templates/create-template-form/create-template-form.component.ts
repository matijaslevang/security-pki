import { Component, Inject } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { CreateCertTemplate, IssuingCertificate } from '../../certicifate.model';
import { CertificateService } from '../../certificate.service';
import { AuthService } from '../../../auth/auth.service';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-create-template-form',
  templateUrl: './create-template-form.component.html',
  styleUrl: './create-template-form.component.css'
})
export class CreateTemplateFormComponent {
  form: FormGroup;
  result: any;
  issuingCertificates$!: Observable<IssuingCertificate[]>;

  constructor(
    private fb: FormBuilder,
    private certificateService: CertificateService,
    public dialogRef: MatDialogRef<CreateTemplateFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { isAdmin: boolean }
  ) {
    this.form = this.fb.group({
      issuerSerialNumber: ['', Validators.required],
      templateName: ['', Validators.required],
      commonNameRegex: [''],
      sanRegex: [''],
      ttl: ['', [Validators.required, Validators.pattern('^[0-9]*$')]],
      skiaki: [false],
      digitalSignature: [false],
      nonRepudiation: [false],
      keyEncipherment: [false],
      dataEncipherment: [false],
      keyAgreement: [false],
      cRLSign: [false],
      serverAuth: [false],
      clientAuth: [false],
      codeSigning: [false],
      emailProtection: [false],
      timeStamping: [false]
    });
  }

  async ngOnInit(): Promise<void> {
    if (this.data.isAdmin) {
      this.issuingCertificates$ = this.certificateService.getAllIssuingCertificates();
    } else {
      this.issuingCertificates$ = this.certificateService.getIssuingCertificates();
    }
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

    let newTemplate: CreateCertTemplate = {
      name: this.form.value.templateName,
      serialNumber: this.form.value.issuerSerialNumber,
      commonNameRegex: this.form.value.commonNameRegex,
      sanRegex: this.form.value.sanRegex,
      ttl: this.form.value.ttl,
      skiakiDefaultValue: this.form.value.skiaki,
      keyUsageDefaultValues: [this.form.value.digitalSignature, this.form.value.nonRepudiation, this.form.value.keyEncipherment, this.form.value.dataEncipherment, this.form.value.keyAgreement, this.form.value.cRLSign],
      extKeyUsageDefaultValues: [this.form.value.serverAuth, this.form.value.clientAuth, this.form.value.codeSigning, this.form.value.emailProtection, this.form.value.timeStamping]
    }
    
    this.certificateService.createCertificateTemplate(newTemplate).subscribe({
      next: () => {
        console.log("Successfully created template")
        this.dialogRef.close()
      }
    })
  }
  
  onCancel(): void {
    this.dialogRef.close(false);
  }
}
