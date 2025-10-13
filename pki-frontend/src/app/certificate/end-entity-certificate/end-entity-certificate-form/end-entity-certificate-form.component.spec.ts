import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EndEntityCertificateFormComponent } from './end-entity-certificate-form.component';

describe('EndEntityCertificateFormComponent', () => {
  let component: EndEntityCertificateFormComponent;
  let fixture: ComponentFixture<EndEntityCertificateFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [EndEntityCertificateFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EndEntityCertificateFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
