import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateTemplateFormComponent } from './create-template-form.component';

describe('CreateTemplateFormComponent', () => {
  let component: CreateTemplateFormComponent;
  let fixture: ComponentFixture<CreateTemplateFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CreateTemplateFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateTemplateFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
