import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { StockService } from '../../../core/services/stock.service';
import { WarehouseService } from '../../../core/services/warehouse.service';
import { NotificationService } from '../../../core/services/notification.service';
import { StockLevel, StockSummary } from '../../../core/models/stock.model';
import { Warehouse } from '../../../core/models/warehouse.model';

@Component({
  selector: 'app-stock-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="stock-page-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">
            <mat-icon class="title-icon">inventory_2</mat-icon> Inventory & Stock Levels
          </h1>
          <p class="page-subtitle">Real-time variant quantities, warehouse reservations, and low stock alerts</p>
        </div>
        <div class="header-actions">
          <a mat-raised-button class="btn-solid-primary" routerLink="/stock/movements/new">
            <mat-icon>post_add</mat-icon> Create Movement
          </a>
          <a mat-stroked-button class="btn-secondary" routerLink="/stock/movements">
            <mat-icon>history</mat-icon> Movement Logs
          </a>
        </div>
      </div>

      <!-- Warehouse Filter Bar -->
      <div class="filter-bar surface-card">
        <div class="filter-item">
          <label class="filter-label" for="stock-warehouse-select">Filter by Warehouse:</label>
          <select id="stock-warehouse-select" class="custom-select" [(ngModel)]="selectedWarehouseId" (change)="onFilterChange()">
            <option [value]="null">All Warehouses</option>
            <option *ngFor="let w of warehouses" [value]="w.id">{{ w.name }} ({{ w.code }})</option>
          </select>
        </div>
      </div>

      <mat-tab-group class="custom-tabs surface-card" animationDuration="0ms">
        
        <!-- Tab 1: Detailed Stock Levels -->
        <mat-tab label="Detailed Stock Levels">
          <div class="tab-content">
            <div *ngIf="loadingLevels" class="loading-box">
              <mat-spinner diameter="40"></mat-spinner>
            </div>
            
            <div *ngIf="!loadingLevels && levels.length === 0" class="empty-box">
              <mat-icon class="empty-icon">inventory</mat-icon>
              <p>No stock levels registered for this selection.</p>
            </div>

            <table *ngIf="!loadingLevels && levels.length > 0" mat-table [dataSource]="levels" class="custom-table">
              <ng-container matColumnDef="sku">
                <th mat-header-cell *matHeaderCellDef>SKU</th>
                <td mat-cell *matCellDef="let item">
                  <span class="sku-tag">{{ item.sku }}</span>
                </td>
              </ng-container>

              <ng-container matColumnDef="variantName">
                <th mat-header-cell *matHeaderCellDef>Variant / Product</th>
                <td mat-cell *matCellDef="let item">
                  <strong class="item-name">{{ item.variantName }}</strong>
                </td>
              </ng-container>

              <ng-container matColumnDef="warehouseName">
                <th mat-header-cell *matHeaderCellDef>Warehouse</th>
                <td mat-cell *matCellDef="let item">{{ item.warehouseName }}</td>
              </ng-container>

              <ng-container matColumnDef="quantity">
                <th mat-header-cell *matHeaderCellDef>Total Qty</th>
                <td mat-cell *matCellDef="let item" class="text-right">
                  <strong>{{ item.quantity }}</strong>
                </td>
              </ng-container>

              <ng-container matColumnDef="reservedQuantity">
                <th mat-header-cell *matHeaderCellDef>Reserved</th>
                <td mat-cell *matCellDef="let item" class="text-right text-warning">
                  {{ item.reservedQuantity }}
                </td>
              </ng-container>

              <ng-container matColumnDef="availableQuantity">
                <th mat-header-cell *matHeaderCellDef>Available</th>
                <td mat-cell *matCellDef="let item" class="text-right">
                  <span class="qty-badge" [class.low]="item.availableQuantity <= (item.minStock || 5)">
                    {{ item.availableQuantity }}
                  </span>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="levelColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: levelColumns;" class="table-row"></tr>
            </table>
          </div>
        </mat-tab>

        <!-- Tab 2: Stock Summary -->
        <mat-tab label="Stock Summary By SKU">
          <div class="tab-content">
            <div *ngIf="loadingSummary" class="loading-box">
              <mat-spinner diameter="40"></mat-spinner>
            </div>

            <table *ngIf="!loadingSummary" mat-table [dataSource]="summaries" class="custom-table">
              <ng-container matColumnDef="sku">
                <th mat-header-cell *matHeaderCellDef>SKU</th>
                <td mat-cell *matCellDef="let s">
                  <span class="sku-tag">{{ s.sku }}</span>
                </td>
              </ng-container>

              <ng-container matColumnDef="variantName">
                <th mat-header-cell *matHeaderCellDef>Variant Name</th>
                <td mat-cell *matCellDef="let s">
                  <strong class="item-name">{{ s.variantName }}</strong>
                </td>
              </ng-container>

              <ng-container matColumnDef="totalQuantity">
                <th mat-header-cell *matHeaderCellDef>Global Stock</th>
                <td mat-cell *matCellDef="let s" class="text-right">
                  <strong>{{ s.totalQuantity }}</strong>
                </td>
              </ng-container>

              <ng-container matColumnDef="totalReserved">
                <th mat-header-cell *matHeaderCellDef>Global Reserved</th>
                <td mat-cell *matCellDef="let s" class="text-right text-warning">
                  {{ s.totalReserved }}
                </td>
              </ng-container>

              <ng-container matColumnDef="totalAvailable">
                <th mat-header-cell *matHeaderCellDef>Global Available</th>
                <td mat-cell *matCellDef="let s" class="text-right">
                  <span class="qty-badge">{{ s.totalAvailable }}</span>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="summaryColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: summaryColumns;" class="table-row"></tr>
            </table>
          </div>
        </mat-tab>

        <!-- Tab 3: Low Stock Alerts -->
        <mat-tab label="Low Stock Alerts">
          <div class="tab-content">
            <div *ngIf="loadingLow" class="loading-box">
              <mat-spinner diameter="40"></mat-spinner>
            </div>

            <div *ngIf="!loadingLow && lowLevels.length === 0" class="empty-box">
              <mat-icon class="empty-icon text-success">check_circle_outline</mat-icon>
              <h3>All Stock Levels Healthy</h3>
              <p>No items currently below minimum stock threshold.</p>
            </div>

            <table *ngIf="!loadingLow && lowLevels.length > 0" mat-table [dataSource]="lowLevels" class="custom-table">
              <ng-container matColumnDef="sku">
                <th mat-header-cell *matHeaderCellDef>SKU</th>
                <td mat-cell *matCellDef="let item"><span class="sku-tag warning">{{ item.sku }}</span></td>
              </ng-container>

              <ng-container matColumnDef="variantName">
                <th mat-header-cell *matHeaderCellDef>Variant Name</th>
                <td mat-cell *matCellDef="let item"><strong>{{ item.variantName }}</strong></td>
              </ng-container>

              <ng-container matColumnDef="warehouseName">
                <th mat-header-cell *matHeaderCellDef>Warehouse</th>
                <td mat-cell *matCellDef="let item">{{ item.warehouseName }}</td>
              </ng-container>

              <ng-container matColumnDef="availableQuantity">
                <th mat-header-cell *matHeaderCellDef>Available</th>
                <td mat-cell *matCellDef="let item" class="text-right">
                  <span class="qty-badge low">{{ item.availableQuantity }}</span>
                </td>
              </ng-container>

              <ng-container matColumnDef="minStock">
                <th mat-header-cell *matHeaderCellDef>Min Threshold</th>
                <td mat-cell *matCellDef="let item" class="text-right">{{ item.minStock || 5 }}</td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="lowColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: lowColumns;" class="table-row"></tr>
            </table>
          </div>
        </mat-tab>

      </mat-tab-group>
    </div>
  `,
  styles: [`
    .stock-page-container { display: flex; flex-direction: column; gap: 20px; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .page-title { display: flex; align-items: center; gap: 12px; margin: 0; font-size: 1.8rem; font-weight: 800; color: var(--text-main); }
    .title-icon { font-size: 32px; width: 32px; height: 32px; color: var(--primary); }
    .page-subtitle { color: var(--text-secondary); margin: 4px 0 0 0; }
    .header-actions { display: flex; gap: 12px; }
    .filter-bar { display: flex; align-items: center; gap: 16px; padding: 16px 24px; }
    .filter-item { display: flex; align-items: center; gap: 12px; }
    .filter-label { color: var(--text-main); font-weight: 600; }
    .custom-select { background: #ffffff; border: 1px solid var(--border-subtle); color: var(--text-main); padding: 8px 16px; border-radius: 8px; font-size: 0.95rem; outline: none; }
    .custom-tabs { padding: 0; overflow: hidden; }
    .tab-content { padding: 24px; }
    .custom-table { width: 100%; background: transparent; }
    .sku-tag { font-family: monospace; background: var(--primary-subtle); color: var(--primary-text); border: 1px solid var(--border-subtle); padding: 4px 8px; border-radius: 6px; font-weight: 600; }
    .sku-tag.warning { background: var(--warning-subtle); color: var(--warning-text); border-color: var(--warning); }
    .item-name { color: var(--text-main); }
    .text-right { text-align: right; }
    .text-warning { color: var(--warning-text); }
    .qty-badge { display: inline-flex; align-items: center; justify-content: center; min-width: 32px; padding: 4px 10px; border-radius: 9999px; background: var(--success-subtle); color: var(--success-text); border: 1px solid var(--success); font-weight: 700; }
    .qty-badge.low { background: var(--danger-subtle); color: var(--danger-text); border: 1px solid var(--danger); }
    .loading-box, .empty-box { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; text-align: center; gap: 12px; color: var(--text-secondary); }
    .empty-icon { font-size: 48px; width: 48px; height: 48px; color: var(--text-muted); }
    .text-success { color: var(--success) !important; }
    .btn-solid-primary { background: var(--primary); color: #ffffff; border-radius: 10px; font-weight: 600; padding: 0 20px; }
    .btn-secondary { color: var(--text-main); border-color: var(--border-strong); border-radius: 10px; }
    .table-row:hover { background: var(--surface-hover); }
  `]
})
export class StockListComponent implements OnInit {
  levelColumns = ['sku', 'variantName', 'warehouseName', 'quantity', 'reservedQuantity', 'availableQuantity'];
  summaryColumns = ['sku', 'variantName', 'totalQuantity', 'totalReserved', 'totalAvailable'];
  lowColumns = ['sku', 'variantName', 'warehouseName', 'availableQuantity', 'minStock'];

  warehouses: Warehouse[] = [];
  selectedWarehouseId: number | null = null;

  levels: StockLevel[] = [];
  summaries: StockSummary[] = [];
  lowLevels: StockLevel[] = [];

  loadingLevels = false;
  loadingSummary = false;
  loadingLow = false;

  constructor(
    private stockService: StockService,
    private warehouseService: WarehouseService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadWarehouses();
    this.loadLevels();
    this.loadSummary();
    this.loadLowStock();
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

  onFilterChange(): void {
    this.loadLevels();
    this.loadLowStock();
  }

  loadLevels(): void {
    this.loadingLevels = true;
    const wId = this.selectedWarehouseId ? Number(this.selectedWarehouseId) : undefined;
    this.stockService.getStockLevels(wId).subscribe({
      next: res => {
        this.loadingLevels = false;
        if (res.success && res.data) {
          this.levels = res.data.content;
        }
      },
      error: () => {
        this.loadingLevels = false;
        this.notification.error('Failed to load stock levels');
      }
    });
  }

  loadSummary(): void {
    this.loadingSummary = true;
    this.stockService.getStockSummary().subscribe({
      next: res => {
        this.loadingSummary = false;
        if (res.success && res.data) {
          this.summaries = res.data.content;
        }
      },
      error: () => {
        this.loadingSummary = false;
      }
    });
  }

  loadLowStock(): void {
    this.loadingLow = true;
    const wId = this.selectedWarehouseId ? Number(this.selectedWarehouseId) : undefined;
    this.stockService.getLowStock(wId).subscribe({
      next: res => {
        this.loadingLow = false;
        if (res.success && res.data) {
          this.lowLevels = res.data.content;
        }
      },
      error: () => {
        this.loadingLow = false;
      }
    });
  }
}
