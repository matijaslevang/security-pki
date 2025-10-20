import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EndEntityCertificateTableComponent } from './end-entity-certificate-table.component';

describe('EndEntityCertificateTableComponent', () => {
  let component: EndEntityCertificateTableComponent;
  let fixture: ComponentFixture<EndEntityCertificateTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [EndEntityCertificateTableComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EndEntityCertificateTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
