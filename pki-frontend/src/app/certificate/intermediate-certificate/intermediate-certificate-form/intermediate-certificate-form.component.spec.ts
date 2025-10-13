import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IntermediateCertificateFormComponent } from './intermediate-certificate-form.component';

describe('IntermediateCertificateFormComponent', () => {
  let component: IntermediateCertificateFormComponent;
  let fixture: ComponentFixture<IntermediateCertificateFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [IntermediateCertificateFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IntermediateCertificateFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
