import { Component, OnInit } from '@angular/core';
import { CertificateChain, CertificateChainDisplay, CertificateInfo, CertificateRow } from '../../certicifate.model';
import { CertificateService } from '../../certificate.service';
import { AssignCertificateFormComponent } from '../../../auth/assign-intermediate-certificate/assign-intermediate-certificate.component';
import { MatDialog } from '@angular/material/dialog';
import { RevocationDialogComponent } from '../../revocation-dialog/revocation-dialog.component';
@Component({
  selector: 'app-self-signed-certificate-table',
  templateUrl: './self-signed-certificate-table.component.html',
  styleUrl: './self-signed-certificate-table.component.css'
})
export class SelfSignedCertificateTableComponent implements OnInit {

 chains: CertificateChainDisplay[] = [];
 CertificateStatus: any;

  constructor(private certificateService: CertificateService, private dialog: MatDialog) { }

  ngOnInit(): void {
    this.loadAllChains();
  }

  loadAllChains(): void {
    this.certificateService.getAllCertificateChains().subscribe({
      next: (data) => this.chains = data,
      error: (err) => console.error('Greška pri učitavanju lanaca:', err)
    });
  }

  isCa(currentRow: CertificateRow): boolean {
    return currentRow.certificate.isCa;
  }

  // Metoda za download je ista kao i pre, nema potrebe da se menja
  downloadCertificate(serialNumber: string): void {
    this.certificateService.downloadCertificate(serialNumber).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${serialNumber}.p12`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      },
      error: (err) => {
        console.error('Greška pri preuzimanju sertifikata:', err);
      }
    });
  }

  revokeCertificate(cert: CertificateInfo): void {
    const ref = this.dialog.open(RevocationDialogComponent, { data: { serial: cert.serialNumber } });
    ref.afterClosed().subscribe(res => {
      if (!res) return;
      this.certificateService.revokeCertificate(cert.serialNumber, res.reason, res.comment).subscribe({
        next: () => this.loadAllChains(),
        error: err => console.error('revoke failed', err)
      });
    });
  }

  openAssignDialog(certificateToAssign: CertificateInfo): void {
    const dialogRef = this.dialog.open(AssignCertificateFormComponent, {
      width: '500px',
      disableClose: true,
      data: { certificateToAssign: certificateToAssign }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        console.log('Certificate was assigned successfully!');
      }
    });
  }
  
}
