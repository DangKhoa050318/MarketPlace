import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ProductService } from '../../../core/services/product.service';
import { CategoryService } from '../../../core/services/category.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Category } from '../../../core/models/category.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="product-form-container">
      <div class="form-header">
        <div>
          <span class="eyebrow">Inventory Catalog</span>
          <h1 class="page-title text-gradient-cyan">
            <mat-icon class="title-icon">{{ isEdit ? 'edit' : 'add_box' }}</mat-icon>
            {{ isEdit ? 'Edit Catalog Product' : 'Create New Product' }}
          </h1>
          <p class="page-subtitle">Configure product properties, pricing, stock levels and category bindings</p>
        </div>
        <a mat-button class="back-link" routerLink="/admin/products">
          <mat-icon>arrow_back</mat-icon> Back to Catalog
        </a>
      </div>

      <div class="form-card glass-panel">
        <div *ngIf="initialLoading" class="loading-container">
          <mat-spinner diameter="40"></mat-spinner>
        </div>

        <form *ngIf="!initialLoading" [formGroup]="form" (ngSubmit)="onSubmit()" class="product-form">
          <!-- Image Preview Header -->
          <div class="image-preview-bar" *ngIf="form.get('imageUrl')?.value">
            <img [src]="form.get('imageUrl')?.value" (error)="onImageError($event)" alt="Preview" class="preview-img" />
            <div class="preview-info">
              <strong>Image Preview</strong>
              <small>{{ form.get('name')?.value || 'New Product Item' }}</small>
            </div>
          </div>

          <!-- Name & Slug -->
          <div class="form-row">
            <mat-form-field appearance="outline" class="half-width">
              <mat-label>Product Name</mat-label>
              <input matInput formControlName="name" placeholder="e.g. Smartphone Pro 15" (input)="onNameInput()">
              <mat-error *ngIf="form.get('name')?.hasError('required')">Product name is required</mat-error>
              <mat-error *ngIf="form.get('name')?.hasError('minlength')">Must be at least 2 characters</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="half-width">
              <mat-label>Product Slug</mat-label>
              <input matInput formControlName="slug" placeholder="e.g. smartphone-pro-15">
              <mat-error *ngIf="form.get('slug')?.hasError('required')">Product slug is required</mat-error>
            </mat-form-field>
          </div>

          <!-- Category -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Category Assignment</mat-label>
            <mat-select formControlName="categoryId">
              <mat-option *ngFor="let category of categories" [value]="category.id">
                {{ category.name }}
              </mat-option>
            </mat-select>
            <mat-error *ngIf="form.get('categoryId')?.hasError('required')">Category is required</mat-error>
          </mat-form-field>

          <!-- Price & Stock Row -->
          <div class="form-row">
            <mat-form-field appearance="outline" class="half-width">
              <mat-label>Price ($ USD)</mat-label>
              <input matInput type="number" step="0.01" formControlName="price" placeholder="0.00">
              <mat-error *ngIf="form.get('price')?.hasError('required')">Price is required</mat-error>
              <mat-error *ngIf="form.get('price')?.hasError('min')">Price must be greater than 0</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="half-width">
              <mat-label>Stock Quantity</mat-label>
              <input matInput type="number" formControlName="stock" placeholder="0">
              <mat-error *ngIf="form.get('stock')?.hasError('required')">Stock is required</mat-error>
              <mat-error *ngIf="form.get('stock')?.hasError('min')">Stock cannot be negative</mat-error>
            </mat-form-field>
          </div>

          <!-- Description -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Detailed Description</mat-label>
            <textarea matInput formControlName="description" rows="3" placeholder="Enter specifications, features, details..."></textarea>
          </mat-form-field>

          <!-- Image URL -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Image URL</mat-label>
            <input matInput formControlName="imageUrl" placeholder="https://images.unsplash.com/photo-...">
          </mat-form-field>

          <!-- Active Status (Edit Mode) -->
          <div *ngIf="isEdit" class="toggle-container">
            <mat-slide-toggle formControlName="active" color="primary">
              Listing Active Status
            </mat-slide-toggle>
          </div>

          <!-- Actions -->
          <div class="form-actions">
            <button mat-raised-button class="btn-glowing" type="submit" [disabled]="form.invalid || submitting">
              <mat-icon>{{ isEdit ? 'save' : 'add' }}</mat-icon>
              {{ submitting ? 'Saving...' : (isEdit ? 'Update Product' : 'Create Product') }}
            </button>
            <button mat-button type="button" (click)="onCancel()" [disabled]="submitting">
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .product-form-container {
      max-width: 800px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .form-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
    }

    .eyebrow {
      color: #38bdf8;
      font-size: .7rem;
      font-weight: 800;
      letter-spacing: .13em;
      text-transform: uppercase;
    }

    .page-title {
      display: flex;
      align-items: center;
      gap: 10px;
      margin: 4px 0 0 0;
      font-size: 1.8rem;
      font-weight: 800;
    }

    .title-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
      color: #38bdf8;
    }

    .page-subtitle {
      margin: 4px 0 0 0;
      color: var(--text-muted);
      font-size: 0.88rem;
    }

    .back-link {
      color: var(--text-secondary);
    }

    .form-card {
      padding: 24px;
    }

    .image-preview-bar {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 12px;
      border-radius: 12px;
      background: rgba(99, 102, 241, 0.06);
      border: 1px solid rgba(99, 102, 241, 0.15);
      margin-bottom: 16px;
    }

    .preview-img {
      width: 50px;
      height: 50px;
      border-radius: 8px;
      object-fit: cover;
    }

    .preview-info strong, .preview-info small {
      display: block;
    }

    .preview-info strong {
      font-size: 0.85rem;
      color: var(--text-main);
    }

    .preview-info small {
      color: var(--text-muted);
      font-size: 0.75rem;
    }

    .product-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .full-width {
      width: 100%;
    }

    .form-row {
      display: flex;
      gap: 16px;
    }

    .half-width {
      flex: 1;
    }

    .toggle-container {
      margin: 8px 0 16px 0;
    }

    .loading-container {
      display: flex;
      justify-content: center;
      padding: 48px;
    }

    .form-actions {
      display: flex;
      gap: 12px;
      margin-top: 16px;
    }
  `]
})
export class ProductFormComponent implements OnInit {
  isEdit = false;
  productId: number | null = null;
  categories: Category[] = [];
  initialLoading = false;
  submitting = false;

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private categoryService: CategoryService,
    private notificationService: NotificationService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
      slug: ['', [Validators.required, Validators.maxLength(255)]],
      description: [''],
      price: [null, [Validators.required, Validators.min(0.01)]],
      stock: [null, [Validators.required, Validators.min(0)]],
      categoryId: [null, [Validators.required]],
      imageUrl: [''],
      active: [true]
    });
  }

  ngOnInit(): void {
    this.loadCategories();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.productId = +idParam;
      this.loadProduct();
    }
  }

  loadCategories(): void {
    this.categoryService.getCategories().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.categories = res.data;
        }
      },
      error: () => this.notificationService.error('Failed to load categories')
    });
  }

  loadProduct(): void {
    if (!this.productId) return;
    this.initialLoading = true;
    this.productService.getProductById(this.productId).subscribe({
      next: (res) => {
        this.initialLoading = false;
        if (res.success && res.data) {
          this.form.patchValue({
            name: res.data.name,
            slug: res.data.slug,
            description: res.data.description,
            price: res.data.price,
            stock: res.data.stock,
            categoryId: res.data.categoryId,
            imageUrl: res.data.imageUrl,
            active: res.data.active
          });
        }
      },
      error: () => {
        this.initialLoading = false;
        this.notificationService.error('Failed to load product details');
        this.router.navigate(['/admin/products']);
      }
    });
  }

  onNameInput(): void {
    if (!this.isEdit && (!this.form.get('slug')?.value || this.form.get('slug')?.pristine)) {
      const name = this.form.get('name')?.value || '';
      const slug = name
        .toLowerCase()
        .trim()
        .replace(/[^a-z0-9\s-]/g, '')
        .replace(/[\s-]+/g, '-');
      this.form.patchValue({ slug });
    }
  }

  onImageError(event: Event): void {
    (event.target as HTMLImageElement).src = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=100';
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.submitting = true;
    const formValue = this.form.getRawValue();

    if (this.isEdit && this.productId) {
      this.productService.updateProduct(this.productId, formValue).subscribe({
        next: (res) => {
          this.submitting = false;
          if (res.success) {
            this.notificationService.success('Product updated successfully');
            this.router.navigate(['/admin/products']);
          }
        },
        error: (err) => {
          this.submitting = false;
          this.notificationService.error(err.error?.message || 'Failed to update product');
        }
      });
    } else {
      this.productService.createProduct(formValue).subscribe({
        next: (res) => {
          this.submitting = false;
          if (res.success) {
            this.notificationService.success('Product created successfully');
            this.router.navigate(['/admin/products']);
          }
        },
        error: (err) => {
          this.submitting = false;
          this.notificationService.error(err.error?.message || 'Failed to create product');
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/admin/products']);
  }
}
