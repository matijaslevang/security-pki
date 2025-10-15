import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AssignIntermediateCertificateComponent } from './assign-intermediate-certificate.component';

describe('AssignIntermediateCertificateComponent', () => {
  let component: AssignIntermediateCertificateComponent;
  let fixture: ComponentFixture<AssignIntermediateCertificateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AssignIntermediateCertificateComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AssignIntermediateCertificateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
