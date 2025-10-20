import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EndEntityCertificateAutogenerateComponent } from './end-entity-certificate-autogenerate.component';

describe('EndEntityCertificateAutogenerateComponent', () => {
  let component: EndEntityCertificateAutogenerateComponent;
  let fixture: ComponentFixture<EndEntityCertificateAutogenerateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [EndEntityCertificateAutogenerateComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EndEntityCertificateAutogenerateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
