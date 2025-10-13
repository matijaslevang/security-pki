import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SelfSignedCertificateFormComponent } from './self-signed-certificate-form.component';

describe('SelfSignedCertificateFormComponent', () => {
  let component: SelfSignedCertificateFormComponent;
  let fixture: ComponentFixture<SelfSignedCertificateFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SelfSignedCertificateFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SelfSignedCertificateFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
