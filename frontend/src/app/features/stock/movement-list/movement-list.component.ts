import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { StockService } from '../../../core/services/stock.service';
import { WarehouseService } from '../../../core/services/warehouse.service';
import { NotificationService } from '../../../core/services/notification.service';
import { StockMovement } from '../../../core/models/stock.model';
import { Warehouse } from '../../../core/models/warehouse.model';

@Component({
  selector: 'app-movement-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="movement-page-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">
            <mat-icon class="title-icon">swap_horiz</mat-icon> Stock Movements & Logs
          </h1>
          <p class="page-subtitle">Track imports, exports, adjustments, and warehouse transfers</p>
        </div>
        <div>
          <a mat-raised-button class="btn-solid-primary" routerLink="/stock/movements/new">
            <mat-icon>add</mat-icon> New Movement
          </a>
        </div>
      </div>

      <!-- Filter Controls -->
      <div class="filter-bar surface-card">
        <div class="filter-group">
          <label for="warehouse-select">Warehouse:</label>
          <select id="warehouse-select" class="custom-select" [(ngModel)]="selectedWarehouseId" (change)="loadMovements()">
            <option [value]="null">All Warehouses</option>
            <option *ngFor="let w of warehouses" [value]="w.id">{{ w.name }}</option>
          </select>
        </div>

        <div class="filter-group">
          <label for="type-select">Type:</label>
          <select id="type-select" class="custom-select" [(ngModel)]="selectedType" (change)="loadMovements()">
            <option value="">All Types</option>
            <option value="IMPORT">IMPORT</option>
            <option value="EXPORT">EXPORT</option>
            <option value="TRANSFER">TRANSFER</option>
            <option value="ADJUSTMENT">ADJUSTMENT</option>
          </select>
        </div>
      </div>

      <div *ngIf="loading" class="loading-state surface-card">
        <mat-spinner diameter="44"></mat-spinner>
      </div>

      <div *ngIf="!loading">
        <div *ngIf="movements.length === 0" class="empty-state surface-card">
          <mat-icon class="empty-icon">history</mat-icon>
          <h3>No Movement Records Found</h3>
          <p>Create an import or transfer movement to record stock changes.</p>
        </div>

        <div *ngIf="movements.length > 0" class="surface-card table-container">
          <table mat-table [dataSource]="movements" class="custom-table">
            
            <!-- Reference No -->
            <ng-container matColumnDef="referenceNo">
              <th mat-header-cell *matHeaderCellDef>Ref No.</th>
              <td mat-cell *matCellDef="let m">
                <span class="ref-badge">{{ m.referenceNo }}</span>
              </td>
            </ng-container>

            <!-- Type -->
            <ng-container matColumnDef="type">
              <th mat-header-cell *matHeaderCellDef>Type</th>
              <td mat-cell *matCellDef="let m">
                <span class="type-pill" [class]="m.type.toLowerCase()">{{ m.type }}</span>
              </td>
            </ng-container>

            <!-- Status -->
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let m">
                <span class="status-pill" [class]="m.status.toLowerCase()">{{ m.status }}</span>
              </td>
            </ng-container>

            <!-- Source Warehouse -->
            <ng-container matColumnDef="warehouseName">
              <th mat-header-cell *matHeaderCellDef>Source Warehouse</th>
              <td mat-cell *matCellDef="let m">{{ m.warehouseName || m.warehouseCode || '—' }}</td>
            </ng-container>

            <!-- Destination Warehouse -->
            <ng-container matColumnDef="destWarehouseName">
              <th mat-header-cell *matHeaderCellDef>Destination</th>
              <td mat-cell *matCellDef="let m">{{ m.destWarehouseName || m.destWarehouseCode || '—' }}</td>
            </ng-container>

            <!-- Created At -->
            <ng-container matColumnDef="createdAt">
              <th mat-header-cell *matHeaderCellDef>Date</th>
              <td mat-cell *matCellDef="let m" class="text-muted">
                {{ m.createdAt | date:'medium' }}
              </td>
            </ng-container>

            <!-- Actions -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Actions</th>
              <td mat-cell *matCellDef="let m">
                <button *ngIf="m.status === 'APPROVED' || m.status === 'DRAFT'"
                        mat-stroked-button class="btn-complete" (click)="onComplete(m)">
                  <mat-icon>task_alt</mat-icon> Complete
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;" class="table-row"></tr>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .movement-page-container { display: flex; flex-direction: column; gap: 24px; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .page-title { display: flex; align-items: center; gap: 12px; margin: 0; font-size: 1.8rem; font-weight: 800; color: var(--text-main); }
    .title-icon { font-size: 32px; width: 32px; height: 32px; color: var(--primary); }
    .page-subtitle { color: var(--text-secondary); margin: 4px 0 0 0; }
    .filter-bar { display: flex; gap: 24px; align-items: center; padding: 16px 24px; }
    .filter-group { display: flex; align-items: center; gap: 12px; color: var(--text-main); font-weight: 600; }
    .custom-select { background: #ffffff; border: 1px solid var(--border-subtle); color: var(--text-main); padding: 8px 16px; border-radius: 8px; font-size: 0.95rem; outline: none; }
    .loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px; text-align: center; gap: 16px; }
    .empty-icon { font-size: 56px; width: 56px; height: 56px; color: var(--text-muted); }
    .custom-table { width: 100%; background: transparent; }
    .ref-badge { font-family: monospace; background: var(--primary-subtle); color: var(--primary-text); border: 1px solid var(--border-subtle); padding: 4px 8px; border-radius: 6px; font-weight: 600; }
    .type-pill { display: inline-flex; padding: 4px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 800; }
    .type-pill.import { background: var(--success-subtle); color: var(--success-text); }
    .type-pill.export { background: var(--danger-subtle); color: var(--danger-text); }
    .type-pill.transfer { background: var(--primary-subtle); color: var(--primary-text); }
    .type-pill.adjustment { background: var(--warning-subtle); color: var(--warning-text); }
    .status-pill { display: inline-flex; padding: 4px 10px; border-radius: 9999px; font-size: 0.75rem; font-weight: 700; }
    .status-pill.completed { background: var(--success-subtle); color: var(--success-text); }
    .status-pill.approved { background: var(--secondary-subtle); color: var(--secondary-hover); }
    .status-pill.draft { background: var(--surface-hover); color: var(--text-secondary); }
    .text-muted { color: var(--text-secondary); }
    .btn-complete { color: var(--success-text); border-color: var(--success); }
    .btn-solid-primary { background: var(--primary); color: #ffffff; border-radius: 10px; font-weight: 600; padding: 0 20px; }
    .table-row:hover { background: var(--surface-hover); }
  `]
})
export class MovementListComponent implements OnInit {
  displayedColumns = ['referenceNo', 'type', 'status', 'warehouseName', 'destWarehouseName', 'createdAt', 'actions'];
  movements: StockMovement[] = [];
  warehouses: Warehouse[] = [];
  selectedWarehouseId: number | null = null;
  selectedType = '';
  loading = false;

  constructor(
    private stockService: StockService,
    private warehouseService: WarehouseService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadWarehouses();
    this.loadMovements();
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

  loadMovements(): void {
    this.loading = true;
    const wId = this.selectedWarehouseId ? Number(this.selectedWarehouseId) : undefined;
    const type = this.selectedType || undefined;

    this.stockService.getMovements(wId, type).subscribe({
      next: res => {
        this.loading = false;
        if (res.success && res.data) {
          this.movements = res.data.content;
        }
      },
      error: () => {
        this.loading = false;
        this.notification.error('Failed to load stock movements');
      }
    });
  }

  onComplete(movement: StockMovement): void {
    this.stockService.completeMovement(movement.id).subscribe({
      next: () => {
        this.notification.success(`Movement ${movement.referenceNo} completed successfully`);
        this.loadMovements();
      },
      error: (err) => {
        this.notification.error(err.error?.message || 'Failed to complete movement');
      }
    });
  }
}
