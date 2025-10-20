import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SelfSignedCertificateTableComponent } from './self-signed-certificate-table.component';

describe('SelfSignedCertificateTableComponent', () => {
  let component: SelfSignedCertificateTableComponent;
  let fixture: ComponentFixture<SelfSignedCertificateTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SelfSignedCertificateTableComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SelfSignedCertificateTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
