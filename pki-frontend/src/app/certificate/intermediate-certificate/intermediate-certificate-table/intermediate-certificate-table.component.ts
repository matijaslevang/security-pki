import { Component, OnInit } from '@angular/core';
import { CertificateChain } from '../../certicifate.model';
import { CertificateService } from '../../certificate.service';
import { RevocationDialogComponent } from '../../revocation-dialog/revocation-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { CertificateInfo } from '../../certicifate.model';
@Component({
  selector: 'app-intermediate-certificate-table',
  templateUrl: './intermediate-certificate-table.component.html',
  styleUrl: './intermediate-certificate-table.component.css'
})
export class IntermediateCertificateTableComponent implements OnInit { 
  chains: CertificateChain[] = [];

  constructor(private certificateService: CertificateService, private dialog: MatDialog) { }

  ngOnInit(): void {
    this.loadChains();
  }

  loadChains(): void {
    this.certificateService.getMyChains().subscribe({
      next: (data) => {
        this.chains = data;
        console.log('Lanci uspešno učitani:', this.chains);
      },
      error: (err) => {
        console.error('Greška pri učitavanju lanaca:', err);
      }
    });
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
        next: () => this.loadChains(),
        error: err => console.error('revoke failed', err)
      });
    });
  }
}
