import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PromotionService } from '../../../../core/services/promotion.service';
import { CategoryService } from '../../../../core/services/category.service';
import { ProductService } from '../../../../core/services/product.service';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  CreatePromotionCodeRequest, PromotionScopeType, ScopeRef, UpdatePromotionCodeRequest
} from '../../../../core/models/promotion.model';

@Component({
  selector: 'app-promotion-form',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatSlideToggleModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <button mat-icon-button (click)="back()"><mat-icon>arrow_back</mat-icon></button>
        <h1 class="page-title text-gradient-cyan">{{ editId ? 'Edit Coupon' : 'New Coupon' }}</h1>
      </div>

      <div *ngIf="loading" class="loading"><mat-spinner diameter="40"></mat-spinner></div>

      <form *ngIf="!loading" [formGroup]="form" (ngSubmit)="submit()" class="form glass-panel">
        <div class="grid">
          <mat-form-field appearance="outline">
            <mat-label>Code</mat-label>
            <input matInput formControlName="code" placeholder="SUMMER10" />
            <mat-hint *ngIf="editId">Code is immutable</mat-hint>
            <mat-error *ngIf="form.get('code')?.hasError('required')">Code is required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Discount type</mat-label>
            <mat-select formControlName="discountType">
              <mat-option value="PERCENT">Percent (%)</mat-option>
              <mat-option value="FIXED">Fixed amount</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Discount value</mat-label>
            <input matInput type="number" formControlName="discountValue" />
            <mat-error *ngIf="form.get('discountValue')?.hasError('required')">Value is required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" *ngIf="form.get('discountType')?.value === 'PERCENT'">
            <mat-label>Max discount (cap)</mat-label>
            <input matInput type="number" formControlName="maxDiscount" placeholder="optional" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Minimum order amount</mat-label>
            <input matInput type="number" formControlName="minOrderAmount" placeholder="0" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Scope</mat-label>
            <mat-select formControlName="scopeType" (selectionChange)="onScopeChange()">
              <mat-option value="CART">Whole cart</mat-option>
              <mat-option value="PRODUCT">Specific products</mat-option>
              <mat-option value="CATEGORY">Specific categories</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="span-2" *ngIf="form.get('scopeType')?.value === 'CATEGORY'">
            <mat-label>Categories ({{ form.get('scopeIds')?.value?.length || 0 }} selected)</mat-label>
            <mat-select formControlName="scopeIds" multiple panelClass="scope-select-panel" (openedChange)="scopeSearch = ''">
              <div class="scope-search" tabindex="0" (click)="$event.stopPropagation()" (keydown.enter)="$event.stopPropagation()">
                <mat-icon>search</mat-icon>
                <input type="text" [(ngModel)]="scopeSearch" [ngModelOptions]="{ standalone: true }"
                       placeholder="Search categories..." (keydown)="$event.stopPropagation()" />
              </div>
              <mat-option *ngFor="let c of filteredCategories()" [value]="c.id">{{ c.name }}</mat-option>
              <div *ngIf="filteredCategories().length === 0" class="scope-empty">No matches</div>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="span-2" *ngIf="form.get('scopeType')?.value === 'PRODUCT'">
            <mat-label>Products ({{ form.get('scopeIds')?.value?.length || 0 }} selected)</mat-label>
            <mat-select formControlName="scopeIds" multiple panelClass="scope-select-panel" (openedChange)="scopeSearch = ''">
              <div class="scope-search" tabindex="0" (click)="$event.stopPropagation()" (keydown.enter)="$event.stopPropagation()">
                <mat-icon>search</mat-icon>
                <input type="text" [(ngModel)]="scopeSearch" [ngModelOptions]="{ standalone: true }"
                       placeholder="Search products..." (keydown)="$event.stopPropagation()" />
              </div>
              <mat-option *ngFor="let p of filteredProducts()" [value]="p.id">{{ p.name }}</mat-option>
              <div *ngIf="filteredProducts().length === 0" class="scope-empty">No matches</div>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Total usage limit</mat-label>
            <input matInput type="number" formControlName="usageLimit" placeholder="unlimited" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Per-user limit</mat-label>
            <input matInput type="number" formControlName="perUserLimit" placeholder="unlimited" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Starts at</mat-label>
            <input matInput type="datetime-local" formControlName="startsAt" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Expires at</mat-label>
            <input matInput type="datetime-local" formControlName="expiresAt" />
          </mat-form-field>
        </div>

        <mat-slide-toggle formControlName="active" class="toggle">Active</mat-slide-toggle>

        <div class="actions">
          <button mat-button type="button" (click)="back()">Cancel</button>
          <button mat-raised-button class="btn-glowing" type="submit" [disabled]="form.invalid || saving">
            {{ saving ? 'Saving...' : (editId ? 'Update Coupon' : 'Create Coupon') }}
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .page { display: flex; flex-direction: column; gap: 20px; max-width: 860px; }
    .page-header { display: flex; align-items: center; gap: 8px; }
    .page-title { margin: 0; font-size: 1.8rem; font-weight: 800; }
    .loading { display: flex; justify-content: center; padding: 60px; }
    .form { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .span-2 { grid-column: 1 / -1; }
    .toggle { margin: 4px 0; }
    .actions { display: flex; justify-content: flex-end; gap: 12px; }
    @media (max-width: 720px) { .grid { grid-template-columns: 1fr; } }

    /* Search box inside the scope dropdown (panel renders in an overlay -> ::ng-deep + panelClass). */
    ::ng-deep .scope-select-panel .scope-search {
      position: sticky; top: 0; z-index: 2;
      display: flex; align-items: center; gap: 6px;
      padding: 8px 10px; background: #fff; border-bottom: 1px solid #e5e7eb;
    }
    ::ng-deep .scope-select-panel .scope-search mat-icon { color: #94a3b8; font-size: 20px; width: 20px; height: 20px; }
    ::ng-deep .scope-select-panel .scope-search input {
      flex: 1; box-sizing: border-box; padding: 6px 8px;
      border: 1px solid #cbd5e1; border-radius: 8px; font-size: .9rem; outline: none;
    }
    ::ng-deep .scope-select-panel .scope-empty { padding: 12px; text-align: center; color: #94a3b8; font-size: .85rem; }
  `]
})
export class PromotionFormComponent implements OnInit {
  form: FormGroup;
  editId: number | null = null;
  loading = false;
  saving = false;
  categories: { id: number; name: string }[] = [];
  products: { id: number; name: string }[] = [];
  scopeSearch = '';

  constructor(
    private fb: FormBuilder,
    private promotionService: PromotionService,
    private categoryService: CategoryService,
    private productService: ProductService,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      code: ['', Validators.required],
      discountType: ['PERCENT', Validators.required],
      discountValue: [null, [Validators.required, Validators.min(0.01)]],
      maxDiscount: [null],
      minOrderAmount: [0],
      scopeType: ['CART', Validators.required],
      scopeIds: [[]],
      usageLimit: [null],
      perUserLimit: [null],
      startsAt: [null],
      expiresAt: [null],
      active: [true]
    });
  }

  ngOnInit(): void {
    this.categoryService.getAll().subscribe(res => this.categories = (res.data ?? []).map(c => ({ id: c.id, name: c.name })));
    this.productService.getProducts(0, 200).subscribe(res => this.products = (res.data?.content ?? []).map(p => ({ id: p.id, name: p.name })));

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId = +id;
      this.loadCoupon(this.editId);
    }
  }

  loadCoupon(id: number): void {
    this.loading = true;
    this.promotionService.getById(id).subscribe({
      next: (res) => {
        this.loading = false;
        const c = res.data;
        if (!c) return;
        this.form.patchValue({
          code: c.code,
          discountType: c.discountType,
          discountValue: c.discountValue,
          maxDiscount: c.maxDiscount ?? null,
          minOrderAmount: c.minOrderAmount ?? 0,
          scopeType: c.scopeType,
          scopeIds: (c.scopes ?? []).map(s => s.refId),
          usageLimit: c.usageLimit ?? null,
          perUserLimit: c.perUserLimit ?? null,
          startsAt: c.startsAt ? c.startsAt.substring(0, 16) : null,
          expiresAt: c.expiresAt ? c.expiresAt.substring(0, 16) : null,
          active: c.active
        });
        this.form.get('code')?.disable();
      },
      error: () => { this.loading = false; this.notification.error('Failed to load coupon'); }
    });
  }

  onScopeChange(): void {
    this.form.get('scopeIds')?.setValue([]);
    this.scopeSearch = '';
  }

  filteredCategories(): { id: number; name: string }[] {
    const term = this.scopeSearch.trim().toLowerCase();
    return term ? this.categories.filter(c => c.name.toLowerCase().includes(term)) : this.categories;
  }

  filteredProducts(): { id: number; name: string }[] {
    const term = this.scopeSearch.trim().toLowerCase();
    return term ? this.products.filter(p => p.name.toLowerCase().includes(term)) : this.products;
  }

  submit(): void {
    const v = this.form.getRawValue();
    const scopeType: PromotionScopeType = v.scopeType;
    const scopes: ScopeRef[] | null = scopeType === 'CART'
      ? null
      : (v.scopeIds || []).map((id: number) => ({ refType: scopeType, refId: id }));

    if (scopeType !== 'CART' && (!scopes || scopes.length === 0)) {
      this.notification.error('Please select at least one ' + scopeType.toLowerCase());
      return;
    }

    const num = (x: unknown) => (x === null || x === '' || x === undefined ? null : Number(x));
    const base = {
      discountType: v.discountType,
      discountValue: Number(v.discountValue),
      maxDiscount: v.discountType === 'PERCENT' ? num(v.maxDiscount) : null,
      minOrderAmount: num(v.minOrderAmount) ?? 0,
      scopeType,
      scopes,
      usageLimit: num(v.usageLimit),
      perUserLimit: num(v.perUserLimit),
      startsAt: v.startsAt || null,
      expiresAt: v.expiresAt || null,
      active: v.active
    };

    this.saving = true;
    if (this.editId) {
      this.promotionService.update(this.editId, base as UpdatePromotionCodeRequest).subscribe({
        next: () => { this.saving = false; this.notification.success('Coupon updated'); this.back(); },
        error: (err) => { this.saving = false; this.notification.error(err.error?.message || 'Failed to update coupon'); }
      });
    } else {
      const req: CreatePromotionCodeRequest = { code: v.code, ...base };
      this.promotionService.create(req).subscribe({
        next: () => { this.saving = false; this.notification.success('Coupon created'); this.back(); },
        error: (err) => { this.saving = false; this.notification.error(err.error?.message || 'Failed to create coupon'); }
      });
    }
  }

  back(): void { this.router.navigate(['/admin/coupons']); }
}
