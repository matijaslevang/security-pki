import { Component, OnInit } from '@angular/core';
import { CertificateChain, CertificateChainDisplay, CertificateInfo, CertificateRow } from '../../certicifate.model';
import { CertificateService } from '../../certificate.service';

@Component({
  selector: 'app-self-signed-certificate-table',
  templateUrl: './self-signed-certificate-table.component.html',
  styleUrl: './self-signed-certificate-table.component.css'
})
export class SelfSignedCertificateTableComponent implements OnInit {

 chains: CertificateChainDisplay[] = [];

  constructor(private certificateService: CertificateService) { }

  ngOnInit(): void {
    this.loadAllChains();
  }

  loadAllChains(): void {
    this.certificateService.getAllCertificateChains().subscribe({
      next: (data) => this.chains = data,
      error: (err) => console.error('Greška pri učitavanju lanaca:', err)
    });
  }

  // Pomoćna funkcija da proverimo da li je sertifikat CA
  // Gleda da li je on issuer nekom drugom sertifikatu u istom lancu
  isCa(currentRow: CertificateRow, chain: CertificateChainDisplay): boolean {
      if (currentRow.certificate.status.toString() === 'REVOKED') return false;
      return chain.chainRows.some(
          otherRow => otherRow.certificate.issuer === currentRow.certificate.subject &&
                      otherRow.certificate.serialNumber !== currentRow.certificate.serialNumber
      );
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

  revokeCertificate(arg0: CertificateInfo) {
    throw new Error('Method not implemented.');
  }
}
