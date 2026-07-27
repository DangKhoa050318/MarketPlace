import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ProductResponse } from '../../../../core/models/product.model';
import { ProductService } from '../../../../core/services/product.service';
import { CategoryService } from '../../../../core/services/category.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { Category } from '../../../../core/models/category.model';

@Component({
  selector: 'app-admin-product-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTooltipModule
  ],
  template: `
    <div class="admin-products-container">
      <!-- Header -->
      <div class="admin-page-header">
        <div>
          <span class="eyebrow">Catalog Operations</span>
          <h1 class="page-title text-gradient-cyan">
            <mat-icon class="title-icon">inventory_2</mat-icon> Product Inventory Management
          </h1>
          <p class="page-subtitle">Control inventory items, active status, real-time stock levels and pricing</p>
        </div>

        <div class="header-actions">
          <a mat-raised-button class="btn-glowing" routerLink="/admin/products/new">
            <mat-icon>add_box</mat-icon> Create New Product
          </a>
        </div>
      </div>

      <!-- KPI Summary Cards -->
      <div class="kpi-grid">
        <div class="kpi-card glass-panel">
          <div class="kpi-icon total"><mat-icon>inventory</mat-icon></div>
          <div class="kpi-info">
            <span class="kpi-label">Total Catalog Items</span>
            <strong class="kpi-value">{{ totalElements }}</strong>
          </div>
        </div>

        <div class="kpi-card glass-panel">
          <div class="kpi-icon active-items"><mat-icon>verified</mat-icon></div>
          <div class="kpi-info">
            <span class="kpi-label">Active Listings</span>
            <strong class="kpi-value">{{ activeCount }}</strong>
          </div>
        </div>

        <div class="kpi-card glass-panel">
          <div class="kpi-icon low-stock"><mat-icon>warning_amber</mat-icon></div>
          <div class="kpi-info">
            <span class="kpi-label">Low Stock Alerts (≤ 5)</span>
            <strong class="kpi-value" [class.text-warn]="lowStockCount > 0">{{ lowStockCount }}</strong>
          </div>
        </div>
      </div>

      <!-- Main Glass Panel -->
      <div class="admin-card glass-panel">
        <!-- Filter Controls -->
        <div class="filter-toolbar">
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Search Products</mat-label>
            <input matInput [(ngModel)]="searchQuery" (input)="onFilterChange()" placeholder="Filter by name or slug...">
            <mat-icon matSuffix>search</mat-icon>
          </mat-form-field>

          <mat-form-field appearance="outline" class="filter-field">
            <mat-label>Category Filter</mat-label>
            <mat-select [(ngModel)]="selectedCategory" (selectionChange)="onFilterChange()">
              <mat-option [value]="null">All Categories</mat-option>
              <mat-option *ngFor="let cat of categories" [value]="cat.id">
                {{ cat.name }}
              </mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="filter-field">
            <mat-label>Active Status</mat-label>
            <mat-select [(ngModel)]="selectedActiveFilter" (selectionChange)="onFilterChange()">
              <mat-option value="ALL">All Statuses</mat-option>
              <mat-option value="ACTIVE">Active Only</mat-option>
              <mat-option value="INACTIVE">Inactive Only</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        <!-- Spinner -->
        <div *ngIf="loading" class="spinner-container">
          <mat-spinner diameter="44"></mat-spinner>
        </div>

        <!-- Table -->
        <div class="table-container" *ngIf="!loading">
          <table mat-table [dataSource]="filteredProducts" class="full-width">
            <!-- ID -->
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef> ID </th>
              <td mat-cell *matCellDef="let product" class="id-cell"> #{{ product.id }} </td>
            </ng-container>

            <!-- Preview -->
            <ng-container matColumnDef="imageUrl">
              <th mat-header-cell *matHeaderCellDef> Preview </th>
              <td mat-cell *matCellDef="let product">
                <img [src]="product.imageUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=100'"
                     (error)="onImageError($event)" [alt]="product.name" class="table-thumb" />
              </td>
            </ng-container>

            <!-- Name -->
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef> Product Details </th>
              <td mat-cell *matCellDef="let product">
                <div class="product-name">{{ product.name }}</div>
                <div class="product-slug">{{ product.slug }}</div>
              </td>
            </ng-container>

            <!-- Category -->
            <ng-container matColumnDef="categoryName">
              <th mat-header-cell *matHeaderCellDef> Category </th>
              <td mat-cell *matCellDef="let product">
                <span class="badge-pill badge-confirmed">{{ product.categoryName || 'General' }}</span>
              </td>
            </ng-container>

            <!-- Price -->
            <ng-container matColumnDef="price">
              <th mat-header-cell *matHeaderCellDef> Unit Price </th>
              <td mat-cell *matCellDef="let product" class="price-cell text-gradient-cyan">
                {{ product.price | currency:'USD':'symbol':'1.2-2' }}
              </td>
            </ng-container>

            <!-- Stock -->
            <ng-container matColumnDef="stock">
              <th mat-header-cell *matHeaderCellDef> Inventory </th>
              <td mat-cell *matCellDef="let product">
                <div class="stock-badge" [class.in-stock]="product.stock > 5" [class.low-stock]="product.stock > 0 && product.stock <= 5" [class.out-of-stock]="product.stock <= 0">
                  <mat-icon class="stock-icon">
                    {{ product.stock > 0 ? (product.stock <= 5 ? 'warning' : 'inventory_2') : 'highlight_off' }}
                  </mat-icon>
                  <span>{{ product.stock }} units</span>
                </div>
              </td>
            </ng-container>

            <!-- Active Status -->
            <ng-container matColumnDef="active">
              <th mat-header-cell *matHeaderCellDef> Status </th>
              <td mat-cell *matCellDef="let product">
                <span class="badge-pill" [class.badge-delivered]="product.active" [class.badge-pending]="!product.active">
                  {{ product.active ? 'ACTIVE' : 'INACTIVE' }}
                </span>
              </td>
            </ng-container>

            <!-- Actions -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef> Actions </th>
              <td mat-cell *matCellDef="let product">
                <div class="action-buttons">
                  <a mat-icon-button [routerLink]="['/admin/products', product.id, 'edit']" matTooltip="Edit Product" class="action-btn edit">
                    <mat-icon>edit</mat-icon>
                  </a>
                  <button mat-icon-button color="warn" (click)="onDelete(product)" matTooltip="Delete Product" class="action-btn delete">
                    <mat-icon>delete_forever</mat-icon>
                  </button>
                </div>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>

          <div *ngIf="filteredProducts.length === 0" class="empty-state">
            <mat-icon class="empty-icon">search_off</mat-icon>
            <p>No products found matching the criteria.</p>
          </div>
        </div>

        <mat-paginator
          [length]="totalElements"
          [pageSize]="pageSize"
          [pageSizeOptions]="[10, 20, 50]"
          (page)="onPageChange($event)"
          showFirstLastButtons>
        </mat-paginator>
      </div>
    </div>
  `,
  styles: [`
    .admin-products-container {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .admin-page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      flex-wrap: wrap;
      gap: 16px;
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
      gap: 12px;
      margin: 4px 0 0 0;
      font-size: 2rem;
      font-weight: 800;
    }

    .title-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: #38bdf8;
    }

    .page-subtitle {
      margin: 4px 0 0 0;
      color: var(--text-muted);
      font-size: 0.9rem;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    /* KPI Summary Cards */
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
    }

    .kpi-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 18px 20px;
    }

    .kpi-icon {
      display: grid;
      width: 44px;
      height: 44px;
      place-items: center;
      border-radius: 12px;
    }

    .kpi-icon.total { color: #6366f1; background: rgba(99, 102, 241, 0.12); }
    .kpi-icon.active-items { color: #10b981; background: rgba(16, 185, 129, 0.12); }
    .kpi-icon.low-stock { color: #f59e0b; background: rgba(245, 158, 11, 0.12); }

    .kpi-info {
      display: flex;
      flex-direction: column;
    }

    .kpi-label {
      font-size: 0.76rem;
      font-weight: 650;
      color: var(--text-secondary);
    }

    .kpi-value {
      font-size: 1.5rem;
      font-weight: 800;
      color: var(--text-main);
    }

    .text-warn {
      color: #ef4444;
    }

    .admin-card {
      padding: 20px;
    }

    /* Filter Toolbar */
    .filter-toolbar {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      margin-bottom: 16px;
    }

    .search-field {
      flex: 2;
      min-width: 240px;
    }

    .filter-field {
      flex: 1;
      min-width: 180px;
    }

    .spinner-container {
      display: flex;
      justify-content: center;
      padding: 48px;
    }

    .table-container {
      overflow-x: auto;
    }

    .id-cell {
      font-weight: 800;
      color: #38bdf8;
    }

    .table-thumb {
      width: 44px;
      height: 44px;
      border-radius: 10px;
      object-fit: cover;
      border: 1px solid var(--glass-border-subtle);
    }

    .product-name {
      font-weight: 700;
      color: var(--text-main);
    }

    .product-slug {
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .price-cell {
      font-weight: 800;
      font-size: 1rem;
    }

    .stock-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      border-radius: 10px;
      font-size: 0.8rem;
      font-weight: 700;
    }

    .stock-badge.in-stock {
      color: #059669;
      background: rgba(16, 185, 129, 0.1);
    }

    .stock-badge.low-stock {
      color: #d97706;
      background: rgba(245, 158, 11, 0.1);
    }

    .stock-badge.out-of-stock {
      color: #dc2626;
      background: rgba(239, 68, 68, 0.1);
    }

    .stock-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }

    .action-buttons {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .action-btn.edit {
      color: #0284c7;
    }

    .empty-state {
      text-align: center;
      padding: 48px;
      color: var(--text-muted);
    }

    .empty-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #38bdf8;
    }
  `]
})
export class AdminProductListComponent implements OnInit {
  displayedColumns = ['id', 'imageUrl', 'name', 'categoryName', 'price', 'stock', 'active', 'actions'];
  products: ProductResponse[] = [];
  filteredProducts: ProductResponse[] = [];
  categories: Category[] = [];

  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  loading = false;

  searchQuery = '';
  selectedCategory: number | null = null;
  selectedActiveFilter = 'ALL';

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private productService: ProductService,
    private categoryService: CategoryService,
    private notification: NotificationService,
    private dialog: MatDialog
  ) {}

  get activeCount(): number {
    return this.products.filter(p => p.active).length;
  }

  get lowStockCount(): number {
    return this.products.filter(p => p.stock <= 5).length;
  }

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.categoryService.getCategories().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.categories = res.data;
        }
      }
    });
  }

  loadProducts(): void {
    this.loading = true;
    this.productService.getProducts(this.currentPage, this.pageSize).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.data) {
          this.products = res.data.content;
          this.totalElements = res.data.totalElements;
          this.applyFilters();
        }
      },
      error: () => {
        this.loading = false;
        this.notification.error('Failed to load products');
      }
    });
  }

  applyFilters(): void {
    let result = [...this.products];

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(p => p.name.toLowerCase().includes(q) || p.slug.toLowerCase().includes(q));
    }

    if (this.selectedCategory !== null) {
      result = result.filter(p => p.categoryId === this.selectedCategory);
    }

    if (this.selectedActiveFilter === 'ACTIVE') {
      result = result.filter(p => p.active);
    } else if (this.selectedActiveFilter === 'INACTIVE') {
      result = result.filter(p => !p.active);
    }

    this.filteredProducts = result;
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
  }

  onImageError(event: Event): void {
    (event.target as HTMLImageElement).src = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=100';
  }

  onDelete(product: ProductResponse): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete Product',
        message: `Are you sure you want to delete product "${product.name}"?`
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.productService.deleteProduct(product.id).subscribe({
          next: () => {
            this.notification.success('Product deleted successfully');
            this.loadProducts();
          },
          error: () => this.notification.error('Failed to delete product')
        });
      }
    });
  }
}
