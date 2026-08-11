import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { StockService } from '../../../core/services/stock.service';
import { WarehouseService } from '../../../core/services/warehouse.service';
import { ProductService } from '../../../core/services/product.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Warehouse } from '../../../core/models/warehouse.model';
import { ProductVariant } from '../../../core/models/product.model';

@Component({
  selector: 'app-movement-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <div class="form-page-container">
      <div class="page-header">
        <h1 class="page-title">
          <mat-icon class="title-icon">post_add</mat-icon> Record Stock Movement
        </h1>
        <p class="page-subtitle">Create import, export, or warehouse transfer vouchers</p>
      </div>

      <div class="surface-card form-card">
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="custom-form">
          
          <!-- Movement Type -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Movement Type</mat-label>
            <mat-select formControlName="type" (selectionChange)="onTypeChange()">
              <mat-option value="IMPORT">IMPORT (Nhập kho)</mat-option>
              <mat-option value="EXPORT">EXPORT (Xuất kho)</mat-option>
              <mat-option value="TRANSFER">TRANSFER (Chuyển kho)</mat-option>
            </mat-select>
          </mat-form-field>

          <!-- Source Warehouse -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ form.value.type === 'TRANSFER' ? 'Source Warehouse (Từ Kho)' : 'Warehouse (Kho)' }}</mat-label>
            <mat-select formControlName="warehouseId">
              <mat-option *ngFor="let w of warehouses" [value]="w.id">
                {{ w.name }} ({{ w.code }})
              </mat-option>
            </mat-select>
          </mat-form-field>

          <!-- Destination Warehouse (for Transfer) -->
          <mat-form-field *ngIf="form.value.type === 'TRANSFER'" appearance="outline" class="full-width">
            <mat-label>Destination Warehouse (Đến Kho)</mat-label>
            <mat-select formControlName="destWarehouseId">
              <mat-option *ngFor="let w of warehouses" [value]="w.id">
                {{ w.name }} ({{ w.code }})
              </mat-option>
            </mat-select>
          </mat-form-field>

          <!-- Notes -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Notes / Reason</mat-label>
            <textarea matInput formControlName="notes" rows="2" placeholder="e.g. Stock replenishment from supplier"></textarea>
          </mat-form-field>

          <!-- Items Array Section -->
          <div class="items-section">
            <div class="section-header">
              <h3>Movement Items</h3>
              <button mat-stroked-button type="button" class="btn-add-item" (click)="addItem()">
                <mat-icon>add</mat-icon> Add Variant Item
              </button>
            </div>

            <div formArrayName="items" class="items-list">
              <div *ngFor="let itemForm of items.controls; let i = index" [formGroupName]="i" class="item-row">
                
                <!-- Variant Selection -->
                <mat-form-field appearance="outline" class="item-select">
                  <mat-label>Product Variant (SKU)</mat-label>
                  <mat-select formControlName="variantId">
                    <mat-option *ngFor="let v of variants" [value]="v.id">
                      {{ v.variantName }} ({{ v.sku }}) - {{ v.price | currency:'VND':'symbol':'1.0-0' }}
                    </mat-option>
                  </mat-select>
                </mat-form-field>

                <!-- Quantity -->
                <mat-form-field appearance="outline" class="item-qty">
                  <mat-label>Quantity</mat-label>
                  <input matInput type="number" formControlName="quantity" min="1">
                </mat-form-field>

                <!-- Delete Item -->
                <button mat-icon-button color="warn" type="button" (click)="removeItem(i)" [disabled]="items.length === 1">
                  <mat-icon>delete_outline</mat-icon>
                </button>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button mat-raised-button class="btn-solid-primary" type="submit" [disabled]="form.invalid || loading">
              <mat-icon>save</mat-icon> {{ loading ? 'Submitting...' : 'Create Movement' }}
            </button>
            <button mat-button class="btn-cancel" type="button" (click)="onCancel()">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .form-page-container { display: flex; flex-direction: column; gap: 24px; max-width: 800px; margin: 0 auto; }
    .page-title { display: flex; align-items: center; gap: 12px; margin: 0; font-size: 1.8rem; font-weight: 800; color: var(--text-main); }
    .title-icon { font-size: 32px; width: 32px; height: 32px; color: var(--primary); }
    .page-subtitle { color: var(--text-secondary); margin: 4px 0 0 0; }
    .custom-form { display: flex; flex-direction: column; gap: 16px; padding: 24px; }
    .full-width { width: 100%; }
    .items-section { background: var(--surface-hover); border: 1px solid var(--border-subtle); border-radius: 12px; padding: 20px; margin: 8px 0; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .section-header h3 { margin: 0; color: var(--text-main); font-size: 1.1rem; }
    .btn-add-item { color: var(--primary); border-color: var(--border-primary); }
    .items-list { display: flex; flex-direction: column; gap: 12px; }
    .item-row { display: flex; align-items: center; gap: 16px; }
    .item-select { flex: 1; }
    .item-qty { width: 130px; }
    .form-actions { display: flex; gap: 12px; margin-top: 16px; }
    .btn-solid-primary { background: var(--primary); color: #ffffff; border-radius: 10px; font-weight: 600; padding: 0 24px; height: 44px; }
    .btn-cancel { color: var(--text-secondary); }
  `]
})
export class MovementFormComponent implements OnInit {
  loading = false;
  warehouses: Warehouse[] = [];
  variants: ProductVariant[] = [];

  form = this.fb.group({
    type: ['IMPORT', [Validators.required]],
    warehouseId: [null as number | null, [Validators.required]],
    destWarehouseId: [null as number | null],
    notes: [''],
    items: this.fb.array([])
  });

  constructor(
    private fb: FormBuilder,
    private stockService: StockService,
    private warehouseService: WarehouseService,
    private productService: ProductService,
    private router: Router,
    private notification: NotificationService
  ) {}

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  ngOnInit(): void {
    this.addItem();
    this.loadWarehouses();
    this.loadVariants();
  }

  createItemFormGroup(): FormGroup {
    return this.fb.group({
      variantId: [null as number | null, [Validators.required]],
      quantity: [1, [Validators.required, Validators.min(1)]]
    });
  }

  addItem(): void {
    this.items.push(this.createItemFormGroup());
  }

  removeItem(index: number): void {
    if (this.items.length > 1) {
      this.items.removeAt(index);
    }
  }

  loadWarehouses(): void {
    this.warehouseService.getWarehouses().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.warehouses = Array.isArray(res.data) ? res.data : (res.data.content || []);
        }
      }
    });
  }

  loadVariants(): void {
    this.productService.getProducts(0, 100).subscribe({
      next: res => {
        if (res.success && res.data) {
          const allVariants: ProductVariant[] = [];
          res.data.content.forEach(p => {
            if (p.variants && p.variants.length > 0) {
              allVariants.push(...p.variants);
            }
          });
          this.variants = allVariants;
        }
      }
    });
  }

  onTypeChange(): void {
    const type = this.form.value.type;
    const destCtrl = this.form.get('destWarehouseId');
    if (type === 'TRANSFER') {
      destCtrl?.setValidators([Validators.required]);
    } else {
      destCtrl?.clearValidators();
      destCtrl?.setValue(null);
    }
    destCtrl?.updateValueAndValidity();
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;

    const val = this.form.value;
    const type = val.type;
    const items = (val.items as Array<{ variantId: number; quantity: number }>) || [];
    const itemsReq = items.map((i) => ({
      variantId: Number(i.variantId),
      quantity: Number(i.quantity)
    }));

    if (type === 'IMPORT') {
      this.stockService.createImport({
        warehouseId: Number(val.warehouseId),
        notes: val.notes || undefined,
        items: itemsReq
      }).subscribe({
        next: () => {
          this.notification.success('Stock import voucher created successfully');
          this.router.navigate(['/stock/movements']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Failed to create import voucher');
        }
      });
    } else if (type === 'EXPORT') {
      this.stockService.createExport({
        warehouseId: Number(val.warehouseId),
        notes: val.notes || undefined,
        items: itemsReq
      }).subscribe({
        next: () => {
          this.notification.success('Stock export voucher created successfully');
          this.router.navigate(['/stock/movements']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Failed to create export voucher');
        }
      });
    } else if (type === 'TRANSFER') {
      this.stockService.createTransfer({
        sourceWarehouseId: Number(val.warehouseId),
        destWarehouseId: Number(val.destWarehouseId),
        notes: val.notes || undefined,
        items: itemsReq
      }).subscribe({
        next: () => {
          this.notification.success('Stock transfer voucher created successfully');
          this.router.navigate(['/stock/movements']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Failed to create transfer voucher');
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/stock/movements']);
  }
}
