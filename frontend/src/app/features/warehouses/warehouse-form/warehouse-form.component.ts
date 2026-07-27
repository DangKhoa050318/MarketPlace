import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { WarehouseService } from '../../../core/services/warehouse.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-warehouse-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatIconModule
  ],
  template: `
    <div class="form-page-container">
      <div class="page-header">
        <h1 class="page-title">
          <mat-icon class="title-icon">warehouse</mat-icon> {{ isEdit ? 'Edit Warehouse' : 'New Storage Warehouse' }}
        </h1>
        <p class="page-subtitle">Configure warehouse identity, location, and operational status</p>
      </div>

      <div class="surface-card form-card">
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="custom-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Warehouse Name</mat-label>
            <input matInput formControlName="name" placeholder="e.g. Central Warehouse A">
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Warehouse Code</mat-label>
            <input matInput formControlName="code" placeholder="e.g. WH-HCM-01">
            <mat-hint *ngIf="isEdit">Warehouse code is fixed and cannot be modified.</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Address / Location</mat-label>
            <textarea matInput formControlName="address" rows="3" placeholder="Full facility address"></textarea>
          </mat-form-field>

          <div *ngIf="isEdit" class="toggle-row">
            <mat-slide-toggle formControlName="active" color="primary">Operational Active Status</mat-slide-toggle>
          </div>

          <div class="form-actions">
            <button mat-raised-button class="btn-solid-primary" type="submit" [disabled]="form.invalid || loading">
              <mat-icon>save</mat-icon> {{ loading ? 'Saving...' : (isEdit ? 'Update Warehouse' : 'Create Warehouse') }}
            </button>
            <button mat-button class="btn-cancel" type="button" (click)="onCancel()">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .form-page-container { display: flex; flex-direction: column; gap: 24px; max-width: 650px; margin: 0 auto; }
    .page-title { display: flex; align-items: center; gap: 12px; margin: 0; font-size: 1.8rem; font-weight: 800; color: var(--text-main); }
    .title-icon { font-size: 32px; width: 32px; height: 32px; color: var(--primary); }
    .page-subtitle { color: var(--text-secondary); margin: 4px 0 0 0; }
    .custom-form { display: flex; flex-direction: column; gap: 16px; padding: 24px; }
    .full-width { width: 100%; }
    .toggle-row { margin: 8px 0; }
    .form-actions { display: flex; gap: 12px; margin-top: 16px; }
    .btn-solid-primary { background: var(--primary); color: #ffffff; border-radius: 10px; font-weight: 600; padding: 0 24px; height: 44px; }
    .btn-cancel { color: var(--text-secondary); }
  `]
})
export class WarehouseFormComponent implements OnInit {
  isEdit = false;
  loading = false;
  warehouseId: number | null = null;

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    code: ['', [Validators.required, Validators.maxLength(50)]],
    address: ['', [Validators.maxLength(500)]],
    active: [true]
  });

  constructor(
    private fb: FormBuilder,
    private warehouseService: WarehouseService,
    private route: ActivatedRoute,
    private router: Router,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.warehouseId = +id;
      this.form.get('code')?.disable();
      this.loadWarehouse();
    }
  }

  private loadWarehouse(): void {
    this.warehouseService.getWarehouseById(this.warehouseId!).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.form.patchValue({
            name: res.data.name,
            code: res.data.code,
            address: res.data.address || '',
            active: res.data.active
          });
        }
      },
      error: () => this.notification.error('Failed to load warehouse details')
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;

    if (this.isEdit) {
      const { name, address, active } = this.form.getRawValue();
      this.warehouseService.updateWarehouse(this.warehouseId!, {
        name: name!,
        address: address || undefined,
        active: active!
      }).subscribe({
        next: () => {
          this.notification.success('Warehouse updated successfully');
          this.router.navigate(['/warehouses']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Failed to update warehouse');
        }
      });
    } else {
      const { name, code, address } = this.form.getRawValue();
      this.warehouseService.createWarehouse({
        name: name!,
        code: code!,
        address: address || undefined
      }).subscribe({
        next: () => {
          this.notification.success('Warehouse created successfully');
          this.router.navigate(['/warehouses']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Failed to create warehouse');
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/warehouses']);
  }
}
