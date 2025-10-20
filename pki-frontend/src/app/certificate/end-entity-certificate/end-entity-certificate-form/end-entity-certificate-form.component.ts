import { Component, Inject, OnInit } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { Observable } from 'rxjs';
import { CreateCertificate, IssuingCertificate, CreateCertCsrUpload, CertTemplate } from '../../certicifate.model';
import { CertificateService } from '../../certificate.service';

type Mode = 'auto' | 'csr';

@Component({
  selector: 'app-end-entity-certificate-form',
  templateUrl: './end-entity-certificate-form.component.html',
  styleUrls: ['./end-entity-certificate-form.component.css']
})
export class EndEntityCertificateFormComponent implements OnInit {
  issuingCertificates$!: Observable<IssuingCertificate[]>;
  // AUTO-GENERATE forma
  formAuto: FormGroup = new FormGroup({
    issuerSerialNumber: new FormControl(null, Validators.required),
    startDate: new FormControl(new Date(), Validators.required),
    endDate: new FormControl(null, Validators.required),
    sanString: new FormControl(''),
    skiaki: new FormControl(false),
    commonName: new FormControl('', Validators.required),
    surname: new FormControl('', Validators.required),
    givenName: new FormControl('', Validators.required),
    organization: new FormControl(''),
    department: new FormControl(''),
    email: new FormControl('', Validators.email),
    country: new FormControl('', [Validators.pattern(/^[A-Z]{2}$/)]), // ISO 3166-1 alpha-2
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
  }, { validators: [EndEntityCertificateFormComponent.dateRangeValidator('startDate', 'endDate')] });

  mode: Mode = 'auto';

  
  templates: CertTemplate[] = []
  selectedTemplate: CertTemplate = null;

  // CSR forma
  formCsr = new FormGroup({
    issuerSerialNumber: new FormControl<string | null>(null, Validators.required),
    startDate: new FormControl<Date | null>(new Date(), Validators.required),
    endDate: new FormControl<Date | null>(null, Validators.required),
    csrFile: new FormControl<File | null>(null, Validators.required),
    sanString: new FormControl(''),
    skiaki: new FormControl(false),
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
  }, { validators: [EndEntityCertificateFormComponent.dateRangeValidator('startDate', 'endDate')] });


  constructor(
    private certificateService: CertificateService,
    private dialogRef: MatDialogRef<EndEntityCertificateFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { isAdmin: boolean }
  ) {}

  ngOnInit(): void {
    this.issuingCertificates$ = this.data?.isAdmin
      ? this.certificateService.getAllIssuingCertificates()
      : this.certificateService.getIssuingCertificates();
    this.formAuto.get('issuerSerialNumber')?.valueChanges.subscribe(() => {
      this.loadTemplates(this.formAuto.get('issuerSerialNumber')?.value);
    });
  }

  static dateRangeValidator(startKey: string, endKey: string) {
    return (group: AbstractControl): ValidationErrors | null => {
      const s = group.get(startKey)?.value;
      const e = group.get(endKey)?.value;
      return (s && e && new Date(s) >= new Date(e)) ? { endBeforeStart: true } : null;
    };
  }

  loadTemplates(serial: string): void {
    this.certificateService.getAllTemplatesForCertificateSerial(serial).subscribe({
      next: (templates: CertTemplate[]) => {
        this.templates = [{
          id: -1,
          serialNumber: '',
          commonNameRegex: 'No template',
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
      this.formAuto.get('commonName')?.setValidators([Validators.required]);
      this.formAuto.get('sanString')?.clearValidators();

      this.formAuto.get('commonName')?.updateValueAndValidity();
      this.formAuto.get('sanString')?.updateValueAndValidity();

      this.formAuto.patchValue({
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
      this.formAuto.get('commonName')?.setValidators([
        Validators.required,
        Validators.pattern(new RegExp(this.selectedTemplate.commonNameRegex))
      ]);
      this.formAuto.get('sanString')?.setValidators([
        Validators.pattern(new RegExp(this.selectedTemplate.sanRegex))
      ]);
      this.formAuto.get('commonName')?.updateValueAndValidity();
      this.formAuto.get('sanString')?.updateValueAndValidity();

      this.formAuto.patchValue({
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
      this.formAuto.get('commonName')?.setValidators([Validators.required]);
      this.formAuto.get('sanString')?.clearValidators();

      this.formAuto.get('commonName')?.updateValueAndValidity();
      this.formAuto.get('sanString')?.updateValueAndValidity();

      this.formAuto.patchValue({
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

  onTabChange(ev: MatTabChangeEvent): void {
    this.mode = ev.index === 0 ? 'auto' : 'csr';
  }

  onCsrFileSelected(ev: Event): void {
    const file = (ev.target as HTMLInputElement).files?.[0] ?? null;
    this.formCsr.patchValue({ csrFile: file });
    this.formCsr.get('csrFile')?.markAsDirty();
  }

  submit(): void {
    if (this.mode === 'auto') {
      if (!this.formAuto.valid) { this.formAuto.markAllAsTouched(); return; }
      const req: CreateCertificate = {
      issuerSerialNumber: this.formAuto.value.issuerSerialNumber, // <-- serial issuer CA sertifikata
      selfSigned: false,
      intermediate: false,
      skiaki: !!this.formAuto.value.skiaki,
      sanString: this.formAuto.value.sanString || '',
      startDate: this.formAuto.value.startDate,
      endDate: this.formAuto.value.endDate,
      subjectDto: {
      commonName: this.formAuto.value.commonName,
      surname: this.formAuto.value.surname,
      givenName: this.formAuto.value.givenName,
      organization: this.formAuto.value.organization,
      department: this.formAuto.value.department,
      email: this.formAuto.value.email,
      country: this.formAuto.value.country,
    },
    keyUsageValues: [this.formAuto.value.digitalSignature, this.formAuto.value.nonRepudiation, this.formAuto.value.keyEncipherment, this.formAuto.value.dataEncipherment, this.formAuto.value.keyAgreement, this.formAuto.value.cRLSign],
    extKeyUsageValues: [this.formAuto.value.serverAuth, this.formAuto.value.clientAuth, this.formAuto.value.codeSigning, this.formAuto.value.emailProtection, this.formAuto.value.timeStamping]
    };
      this.certificateService.createEndEntityCertificate(req).subscribe({
        next: ok => { if (ok) this.dialogRef.close(true); },
        error: err => console.error('createEndEntityCertificate (auto) failed:', err)
      });
      return;
    }


    // CSR upload
    if (!this.formCsr.valid) { this.formCsr.markAllAsTouched(); return; }
    if (!this.formCsr.valid) {
    this.formCsr.markAllAsTouched();
    return;
  }
  const formValue = this.formCsr.value;

  // Izdvoj fajl iz forme
  const file = formValue.csrFile!;

  // Pripremi DTO objekat sa svim podacima
  const req: CreateCertCsrUpload = {
    issuerSerialNumber: formValue.issuerSerialNumber!,
    startDate: formValue.startDate!,
    selfSigned:false,
    intermediate:false,
    endDate: formValue.endDate!,
    skiaki: !!formValue.skiaki,
    sanString: formValue.sanString || '',
    keyUsageValues: [
      !!formValue.digitalSignature,
      !!formValue.nonRepudiation,
      !!formValue.keyEncipherment,
      !!formValue.dataEncipherment,
      !!formValue.keyAgreement,
      !!formValue.cRLSign
    ],
    extKeyUsageValues: [
      !!formValue.serverAuth,
      !!formValue.clientAuth,
      !!formValue.codeSigning,
      !!formValue.emailProtection,
      !!formValue.timeStamping
    ]
  };

  // Pozovi novu servisnu metodu
  this.certificateService.submitCsrWithExtensions(req, file).subscribe({
    next: ok => { if (ok) this.dialogRef.close(true); },
    error: err => console.error('submitCsrWithExtensions failed:', err)
  });
    // const c = this.formCsr.value;
    // const file = c.csrFile!;
    // const issuerSerial = c.issuerSerialNumber!;
    // const start = c.startDate!;
    // const end = c.endDate!;
    
    ////////////////////// -----------
    
    // const req: CreateCertCsrUpload = {
    // issuerSerialNumber: this.formCsr.value.issuerSerialNumber, 
    // selfSigned: false,
    // intermediate: false,
    // skiaki: !!this.formCsr.value.skiaki,
    // sanString: this.formCsr.value.sanString || '',
    // startDate: this.formCsr.value.startDate,
    // endDate: this.formCsr.value.endDate,
    // keyUsageValues: [this.formCsr.value.digitalSignature, this.formCsr.value.nonRepudiation, this.formCsr.value.keyEncipherment, this.formCsr.value.dataEncipherment, this.formCsr.value.keyAgreement, this.formCsr.value.cRLSign],
    // extKeyUsageValues: [this.formAuto.value.serverAuth, this.formAuto.value.clientAuth, this.formAuto.value.codeSigning, this.formAuto.value.emailProtection, this.formAuto.value.timeStamping]
    // }
    ////////////////////// -----------
    // this.certificateService.submitCsr(issuerSerial, start, end, file).subscribe({
    //   next: ok => { if (ok) this.dialogRef.close(true); },
    //   error: err => console.error('submitCsr failed:', err)
    // });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
