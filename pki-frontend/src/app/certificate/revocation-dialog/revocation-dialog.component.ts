import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-revocation-dialog',
  templateUrl: './revocation-dialog.component.html',
  styleUrl: './revocation-dialog.component.css'
})
export class RevocationDialogComponent {
   form: FormGroup;

  reasons = [
    { code: 0, label: 'Unspecified' },
    { code: 1, label: 'Key compromise' },
    { code: 2, label: 'CA compromise' },
    { code: 3, label: 'Affiliation changed' },
    { code: 4, label: 'Superseded' },
    { code: 5, label: 'Cessation of operation' },
    { code: 6, label: 'Certificate hold' },
    { code: 8, label: 'Remove from CRL' },
    { code: 9, label: 'Privilege withdrawn' },
    { code: 10, label: 'AA compromise' },
  ];

  constructor(
    private fb: FormBuilder,
    private ref: MatDialogRef<RevocationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { serial: string }
  ) {
    this.form = this.fb.group({
      reason: [null, Validators.required],
      comment: ['']
    });
  }

  submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.ref.close(this.form.value);
  }

  cancel() { this.ref.close(null); }
}
