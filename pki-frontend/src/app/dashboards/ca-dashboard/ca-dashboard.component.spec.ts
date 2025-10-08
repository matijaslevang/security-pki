import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CaDashboardComponent } from './ca-dashboard.component';

describe('CaDashboardComponent', () => {
  let component: CaDashboardComponent;
  let fixture: ComponentFixture<CaDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CaDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CaDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
