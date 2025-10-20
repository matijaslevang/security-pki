import { Component, Inject, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { AuthService } from '../../../auth/auth.service';
import { CertTemplate, CreateCertificate, IssuingCertificate } from '../../certicifate.model';
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
  templates: CertTemplate[] = []
  selectedTemplate: CertTemplate = null;

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
        commonName: ['', Validators.required],
        givenName: ['', Validators.required],
        surname: ['', Validators.required],
        organization: ['', Validators.required],
        department: [''],
        country: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
        email: ['', [Validators.required, Validators.email]]
      }),
      startDate: [new Date(), Validators.required],
      endDate: ['', Validators.required],
      sanString: [''],
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
    }, { validators: this.dateValidator });
  }

  async ngOnInit(): Promise<void> {
    if (this.data.isAdmin) {
      this.issuingCertificates$ = this.certificateService.getAllIssuingCertificates();
    } else {
      this.issuingCertificates$ = this.certificateService.getIssuingCertificates();
    }
    this.currentUserUuid = await this.authService.getUserId();
    this.form.get('issuerSerialNumber')?.valueChanges.subscribe(() => {
      this.loadTemplates(this.form.get('issuerSerialNumber')?.value);
    });
  }

  loadTemplates(serial: string): void {
    this.certificateService.getAllTemplatesForCertificateSerial(serial).subscribe({
      next: (templates: CertTemplate[]) => {
        this.templates = [{
          id: -1,
          name: "No template",
          serialNumber: '',
          commonNameRegex: '',
          sanRegex: '',
          ttl: 0,
          skiakiDefaultValue: false,
          keyUsageDefaultValues: [],
          extKeyUsageDefaultValues: []
        },
        ...templates]
      }
    })
  }

  applyTemplate(): void {
    if (this.selectedTemplate && this.selectedTemplate.id === -1) {
      this.form.get('subject.commonName')?.setValidators([Validators.required]);
      this.form.get('sanString')?.clearValidators();

      this.form.get('subject.commonName')?.updateValueAndValidity();
      this.form.get('sanString')?.updateValueAndValidity();

      this.form.patchValue({
        digitalSignature: false,
        nonRepudiation: false,
        keyEncipherment: false,
        dataEncipherment: false,
        keyAgreement: false,
        cRLSign: false,
        serverAuth: false,
        clientAuth: false,
        codeSigning: false,
        emailProtection: false,
        timeStamping: false
      })
    } else if (this.selectedTemplate && this.selectedTemplate.id > -1) {
      this.form.get('subject.commonName')?.setValidators([
        Validators.required,
        Validators.pattern(new RegExp(this.selectedTemplate.commonNameRegex))
      ]);
      this.form.get('sanString')?.setValidators([
        Validators.pattern(new RegExp(this.selectedTemplate.sanRegex))
      ]);
      this.form.get('subject.commonName')?.updateValueAndValidity();
      this.form.get('sanString')?.updateValueAndValidity();

      this.form.patchValue({
        digitalSignature: this.selectedTemplate.keyUsageDefaultValues[0],
        nonRepudiation: this.selectedTemplate.keyUsageDefaultValues[1],
        keyEncipherment: this.selectedTemplate.keyUsageDefaultValues[2],
        dataEncipherment: this.selectedTemplate.keyUsageDefaultValues[3],
        keyAgreement: this.selectedTemplate.keyUsageDefaultValues[4],
        cRLSign: this.selectedTemplate.keyUsageDefaultValues[5],
        serverAuth: this.selectedTemplate.extKeyUsageDefaultValues[0],
        clientAuth: this.selectedTemplate.extKeyUsageDefaultValues[1],
        codeSigning: this.selectedTemplate.extKeyUsageDefaultValues[2],
        emailProtection: this.selectedTemplate.extKeyUsageDefaultValues[3],
        timeStamping: this.selectedTemplate.extKeyUsageDefaultValues[4]
      })
    } else {
      this.form.get('subject.commonName')?.setValidators([Validators.required]);
      this.form.get('sanString')?.clearValidators();

      this.form.get('subject.commonName')?.updateValueAndValidity();
      this.form.get('sanString')?.updateValueAndValidity();

      this.form.patchValue({
        digitalSignature: false,
        nonRepudiation: false,
        keyEncipherment: false,
        dataEncipherment: false,
        keyAgreement: false,
        cRLSign: false,
        serverAuth: false,
        clientAuth: false,
        codeSigning: false,
        emailProtection: false,
        timeStamping: false
      })
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

    const dto: CreateCertificate = {
      //issuerUuid: this.currentUserUuid,
      issuerSerialNumber: this.form.value.issuerSerialNumber,
      subjectDto: this.form.value.subject,
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate,
      intermediate: true,
      selfSigned: false,
      skiaki: this.form.value.skiaki,
      sanString: this.form.value.sanString,
      keyUsageValues: [this.form.value.digitalSignature, this.form.value.nonRepudiation, this.form.value.keyEncipherment, this.form.value.dataEncipherment, this.form.value.keyAgreement, this.form.value.cRLSign],
      extKeyUsageValues: [this.form.value.serverAuth, this.form.value.clientAuth, this.form.value.codeSigning, this.form.value.emailProtection, this.form.value.timeStamping]
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
