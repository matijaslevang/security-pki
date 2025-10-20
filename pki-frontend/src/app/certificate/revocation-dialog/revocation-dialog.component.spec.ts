import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RevocationDialogComponent } from './revocation-dialog.component';

describe('RevocationDialogComponent', () => {
  let component: RevocationDialogComponent;
  let fixture: ComponentFixture<RevocationDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [RevocationDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RevocationDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
