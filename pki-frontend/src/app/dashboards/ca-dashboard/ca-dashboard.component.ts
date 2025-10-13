import { Component, ViewChild } from '@angular/core';
import { IntermediateCertificateTableComponent } from '../../certificate/intermediate-certificate/intermediate-certificate-table/intermediate-certificate-table.component';
import { MatDialog } from '@angular/material/dialog';
import { IntermediateCertificateFormComponent } from '../../certificate/intermediate-certificate/intermediate-certificate-form/intermediate-certificate-form.component';

@Component({
  selector: 'app-ca-dashboard',
  templateUrl: './ca-dashboard.component.html',
  styleUrl: './ca-dashboard.component.css'
})
export class CaDashboardComponent {

  @ViewChild('chainsList') chainsListComponent!: IntermediateCertificateTableComponent;

  constructor(private dialog: MatDialog) {}

  openCreateIntermediateForm(): void {
    const dialogRef = this.dialog.open(IntermediateCertificateFormComponent, {
      width: '800px', // Širina modala
      disableClose: true, // Sprečava zatvaranje klikom van modala
      data: { isAdmin: false } 
    });

    // Nakon što se modal zatvori...
    dialogRef.afterClosed().subscribe(result => {
      // Ako je forma uspešno poslata, 'result' će biti 'true' (pogledaj korak 3)
      if (result) {
        console.log('Certificate created, refreshing list...');
        // Pozivamo metodu za osvežavanje podataka u tabeli
        this.chainsListComponent.loadChains();
      }
    });
  }
}
