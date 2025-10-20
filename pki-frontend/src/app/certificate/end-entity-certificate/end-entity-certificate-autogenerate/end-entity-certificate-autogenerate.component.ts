import { Component, OnInit, Inject } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { CreateCertificate, IssuingCertificate, CertTemplate } from '../../certicifate.model';
import { CertificateService } from '../../certificate.service';
import { MatFormField } from "@angular/material/form-field";
import { MatDialogModule } from '@angular/material/dialog';
import { FormsModule } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
// Neophodni importi za Angular Material i forme
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-end-entity-certificate-autogenerate',
  standalone: true,  // <-- Komponenta je sada 'standalone'
  imports: [         // <-- Svi potrebni moduli se importuju ovde
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatCheckboxModule,
    MatButtonModule
  ],
  templateUrl: './end-entity-certificate-autogenerate.component.html',
  styleUrls: ['./end-entity-certificate-autogenerate.component.css']
})
export class EndEntityCertificateAutogenerateComponent implements OnInit {

  issuingCertificates$!: Observable<IssuingCertificate[]>;
  templates: CertTemplate[] = [];
  selectedTemplate: CertTemplate | null = null;

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
    email: new FormControl('', [Validators.required, Validators.email]),
    country: new FormControl('', [Validators.required, Validators.pattern(/^[A-Z]{2}$/)]), // ISO 3166-1 alpha-2
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
  }, { validators: [this.dateRangeValidator] });

  constructor(
    private certificateService: CertificateService,
    private dialogRef: MatDialogRef<EndEntityCertificateAutogenerateComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { isAdmin: boolean }
  ) {}

  ngOnInit(): void {
    // ca ili admin zavisi
    this.issuingCertificates$ = this.data?.isAdmin
       ? this.certificateService.getAllIssuingCertificates()
       : this.certificateService.getIssuingCertificates();
    // Kada korisnik izabere CA, učitaj templejte vezane za taj CA
    this.formAuto.get('issuerSerialNumber')?.valueChanges.subscribe((serial) => {
      if (serial) {
        this.loadTemplates(serial);
      }
    });
  }

  dateRangeValidator(group: AbstractControl): ValidationErrors | null {
    const start = group.get('startDate')?.value;
    const end = group.get('endDate')?.value;
    return (start && end && new Date(start) >= new Date(end)) ? { endBeforeStart: true } : null;
  }

  loadTemplates(serial: string): void {
    this.certificateService.getAllTemplatesForCertificateSerial(serial).subscribe({
      next: (templates: CertTemplate[]) => {
        // Dodajemo opciju "No template" na početak liste
        this.templates = [{
          id: -1, name: "No template", serialNumber: '', commonNameRegex: '',
          sanRegex: '', ttl: 0, skiakiDefaultValue: false,
          keyUsageDefaultValues: [], extKeyUsageDefaultValues: []
        }, ...templates];
        this.selectedTemplate = this.templates[0]; // Selektuj "No template" po default-u
      }
    });
  }

  applyTemplate(): void {
    if (!this.selectedTemplate || this.selectedTemplate.id === -1) {
      this.resetToDefaults();
      return;
    }

    const template = this.selectedTemplate;

    // Postavi validatore na osnovu regexa iz templejta
    this.formAuto.get('commonName')?.setValidators([Validators.required, Validators.pattern(template.commonNameRegex)]);
    this.formAuto.get('sanString')?.setValidators([Validators.pattern(template.sanRegex)]);
    this.formAuto.get('commonName')?.updateValueAndValidity();
    this.formAuto.get('sanString')?.updateValueAndValidity();

    // Postavi default vrednosti za checkbox-ove
    this.formAuto.patchValue({
      digitalSignature: template.keyUsageDefaultValues[0],
      nonRepudiation: template.keyUsageDefaultValues[1],
      keyEncipherment: template.keyUsageDefaultValues[2],
      dataEncipherment: template.keyUsageDefaultValues[3],
      keyAgreement: template.keyUsageDefaultValues[4],
      cRLSign: template.keyUsageDefaultValues[5],
      serverAuth: template.extKeyUsageDefaultValues[0],
      clientAuth: template.extKeyUsageDefaultValues[1],
      codeSigning: template.extKeyUsageDefaultValues[2],
      emailProtection: template.extKeyUsageDefaultValues[3],
      timeStamping: template.extKeyUsageDefaultValues[4]
    });
  }

  resetToDefaults(): void {
    this.formAuto.get('commonName')?.setValidators([Validators.required]);
    this.formAuto.get('sanString')?.clearValidators();
    this.formAuto.get('commonName')?.updateValueAndValidity();
    this.formAuto.get('sanString')?.updateValueAndValidity();

    this.formAuto.patchValue({
      digitalSignature: false, nonRepudiation: false, keyEncipherment: false,
      dataEncipherment: false, keyAgreement: false, cRLSign: false,
      serverAuth: false, clientAuth: false, codeSigning: false,
      emailProtection: false, timeStamping: false
    });
  }

  submit(): void {
    if (!this.formAuto.valid) {
      this.formAuto.markAllAsTouched();
      return;
    }

    const formValue = this.formAuto.value;
    const req: CreateCertificate = {
      issuerSerialNumber: formValue.issuerSerialNumber,
      selfSigned: false,
      intermediate: false,
      skiaki: !!formValue.skiaki,
      sanString: formValue.sanString || '',
      startDate: formValue.startDate,
      endDate: formValue.endDate,
      //password:'', // proveri
      subjectDto: {
        commonName: formValue.commonName,
        surname: formValue.surname,
        givenName: formValue.givenName,
        organization: formValue.organization,
        department: formValue.department,
        email: formValue.email,
        country: formValue.country,
      },
      keyUsageValues: [
        !!formValue.digitalSignature, !!formValue.nonRepudiation, !!formValue.keyEncipherment,
        !!formValue.dataEncipherment, !!formValue.keyAgreement, !!formValue.cRLSign
      ],
      extKeyUsageValues: [
        !!formValue.serverAuth, !!formValue.clientAuth, !!formValue.codeSigning,
        !!formValue.emailProtection, !!formValue.timeStamping
      ]
    };

    // Validacija za TTL ako je templejt izabran
    if (this.selectedTemplate && this.selectedTemplate.id > -1) {
      const ttlDays = (new Date(req.endDate).getTime() - new Date(req.startDate).getTime()) / (1000 * 3600 * 24);
      if (ttlDays > this.selectedTemplate.ttl) {
        this.formAuto.get('endDate')?.setErrors({ invalidTTL: true });
        // Ovde možeš dodati i neku poruku za korisnika
        console.error("Invalid TTL: The validity period exceeds the template's maximum TTL.");
        return;
      }
    }

    this.certificateService.createEndEntityCertificate(req).subscribe({
      next: ok => {
        if (ok) this.dialogRef.close(true); // Vrati `true` ako je uspešno
      },
      error: err => console.error('createEndEntityCertificate (auto) failed:', err)
    });
  }

  onCancel(): void {
    this.dialogRef.close(false); // Vrati `false` ako je otkazano
  }
}

