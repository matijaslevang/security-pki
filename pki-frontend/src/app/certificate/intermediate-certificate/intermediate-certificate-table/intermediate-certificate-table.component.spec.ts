import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IntermediateCertificateTableComponent } from './intermediate-certificate-table.component';

describe('IntermediateCertificateTableComponent', () => {
  let component: IntermediateCertificateTableComponent;
  let fixture: ComponentFixture<IntermediateCertificateTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [IntermediateCertificateTableComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IntermediateCertificateTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
