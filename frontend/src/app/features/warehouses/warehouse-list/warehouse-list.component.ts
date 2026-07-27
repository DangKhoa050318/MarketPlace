import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { WarehouseService } from '../../../core/services/warehouse.service';
import { Warehouse } from '../../../core/models/warehouse.model';

@Component({
  selector: 'app-warehouse-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatTableModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="warehouse-page-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">
            <mat-icon class="title-icon">warehouse</mat-icon> Warehouses & Storage Locations
          </h1>
          <p class="page-subtitle">Manage distribution centers, capacity, and active status</p>
        </div>
        <div *ngIf="canManage">
          <a mat-raised-button class="btn-solid-primary" routerLink="/warehouses/create">
            <mat-icon>add</mat-icon> New Warehouse
          </a>
        </div>
      </div>

      <div *ngIf="loading" class="loading-state surface-card">
        <mat-spinner diameter="44"></mat-spinner>
      </div>

      <div *ngIf="!loading">
        <div *ngIf="warehouses.length === 0" class="empty-state surface-card">
          <mat-icon class="empty-icon">store_mall_directory</mat-icon>
          <h3>No Warehouses Registered</h3>
          <p>Click "New Warehouse" to add your first storage facility.</p>
        </div>

        <div *ngIf="warehouses.length > 0" class="surface-card table-container">
          <table mat-table [dataSource]="warehouses" class="custom-table">
            
            <!-- Code -->
            <ng-container matColumnDef="code">
              <th mat-header-cell *matHeaderCellDef>Code</th>
              <td mat-cell *matCellDef="let w">
                <span class="code-badge">{{ w.code }}</span>
              </td>
            </ng-container>

            <!-- Name -->
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Warehouse Name</th>
              <td mat-cell *matCellDef="let w">
                <strong class="warehouse-name">{{ w.name }}</strong>
              </td>
            </ng-container>

            <!-- Address -->
            <ng-container matColumnDef="address">
              <th mat-header-cell *matHeaderCellDef>Address</th>
              <td mat-cell *matCellDef="let w" class="text-muted">
                {{ w.address || '—' }}
              </td>
            </ng-container>

            <!-- Status -->
            <ng-container matColumnDef="active">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let w">
                <span class="status-pill" [class.active]="w.active" [class.inactive]="!w.active">
                  {{ w.active ? 'Active' : 'Inactive' }}
                </span>
              </td>
            </ng-container>

            <!-- Actions -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Actions</th>
              <td mat-cell *matCellDef="let w">
                <button *ngIf="canManage" mat-icon-button class="action-btn" [routerLink]="['/warehouses', w.id, 'edit']">
                  <mat-icon>edit</mat-icon>
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
    .warehouse-page-container { display: flex; flex-direction: column; gap: 24px; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .page-title { display: flex; align-items: center; gap: 12px; margin: 0; font-size: 1.8rem; font-weight: 800; color: var(--text-main); }
    .title-icon { font-size: 32px; width: 32px; height: 32px; color: var(--primary); }
    .page-subtitle { color: var(--text-secondary); margin: 4px 0 0 0; }
    .loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px; text-align: center; gap: 16px; }
    .empty-icon { font-size: 56px; width: 56px; height: 56px; color: var(--text-muted); }
    .custom-table { width: 100%; background: transparent; }
    .code-badge { font-family: monospace; background: var(--primary-subtle); color: var(--primary-text); border: 1px solid var(--border-subtle); padding: 4px 8px; border-radius: 6px; font-weight: 600; }
    .warehouse-name { color: var(--text-main); font-size: 1rem; }
    .text-muted { color: var(--text-secondary); }
    .status-pill { display: inline-flex; align-items: center; padding: 4px 12px; border-radius: 9999px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; }
    .status-pill.active { background: var(--success-subtle); color: var(--success-text); border: 1px solid var(--success); }
    .status-pill.inactive { background: var(--danger-subtle); color: var(--danger-text); border: 1px solid var(--danger); }
    .action-btn { color: var(--text-secondary); }
    .action-btn:hover { color: var(--primary); }
    .table-row:hover { background: var(--surface-hover); }
    .btn-solid-primary { background: var(--primary); color: #ffffff; border-radius: 10px; font-weight: 600; padding: 0 20px; }
  `]
})
export class WarehouseListComponent implements OnInit {
  displayedColumns = ['code', 'name', 'address', 'active', 'actions'];
  warehouses: Warehouse[] = [];
  loading = false;

  constructor(
    private warehouseService: WarehouseService,
    private notification: NotificationService,
    private authService: AuthService
  ) {}

  get canManage(): boolean {
    const role = this.authService.getRole();
    return role === 'ADMIN' || role === 'MANAGER';
  }

  ngOnInit(): void {
    this.loadWarehouses();
  }

  loadWarehouses(): void {
    this.loading = true;
    this.warehouseService.getWarehouses().subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.success && res.data) {
          this.warehouses = Array.isArray(res.data) ? res.data : (res.data.content || []);
        }
      },
      error: () => {
        this.loading = false;
        this.notification.error('Failed to load warehouse list');
      }
    });
  }
}
