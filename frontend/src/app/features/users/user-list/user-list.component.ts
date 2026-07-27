import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { User, UserService } from '../user.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-user-list',
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
    MatDialogModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="admin-users-container">
      <!-- Page Header -->
      <div class="admin-page-header">
        <div>
          <span class="eyebrow">Identity & Access Management</span>
          <h1 class="page-title text-gradient-cyan">
            <mat-icon class="title-icon">manage_accounts</mat-icon> User Accounts Control
          </h1>
          <p class="page-subtitle">Review registered customer profiles, administrative roles and account statuses</p>
        </div>

        <div class="header-actions">
          <a mat-raised-button class="btn-glowing" routerLink="/admin/users/new">
            <mat-icon>person_add</mat-icon> Create New User
          </a>
        </div>
      </div>

      <!-- KPI Summary Cards -->
      <div class="kpi-grid">
        <div class="kpi-card glass-panel">
          <div class="kpi-icon total"><mat-icon>group</mat-icon></div>
          <div class="kpi-info">
            <span class="kpi-label">Total Accounts</span>
            <strong class="kpi-value">{{ totalElements }}</strong>
          </div>
        </div>

        <div class="kpi-card glass-panel">
          <div class="kpi-icon active-users"><mat-icon>how_to_reg</mat-icon></div>
          <div class="kpi-info">
            <span class="kpi-label">Active Users</span>
            <strong class="kpi-value">{{ activeCount }}</strong>
          </div>
        </div>

        <div class="kpi-card glass-panel">
          <div class="kpi-icon admins"><mat-icon>shield</mat-icon></div>
          <div class="kpi-info">
            <span class="kpi-label">Administrators</span>
            <strong class="kpi-value">{{ adminCount }}</strong>
          </div>
        </div>
      </div>

      <!-- Glass Panel -->
      <div class="admin-card glass-panel">
        <!-- Filter Controls -->
        <div class="filter-toolbar">
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Search Accounts</mat-label>
            <input matInput [(ngModel)]="searchQuery" (input)="onFilterChange()" placeholder="Search by name, username or email...">
            <mat-icon matSuffix>search</mat-icon>
          </mat-form-field>

          <mat-form-field appearance="outline" class="filter-field">
            <mat-label>Role Filter</mat-label>
            <mat-select [(ngModel)]="selectedRole" (selectionChange)="onFilterChange()">
              <mat-option value="ALL">All Roles</mat-option>
              <mat-option value="ADMIN">ADMIN</mat-option>
              <mat-option value="USER">USER</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="filter-field">
            <mat-label>Active Status</mat-label>
            <mat-select [(ngModel)]="selectedStatus" (selectionChange)="onFilterChange()">
              <mat-option value="ALL">All Statuses</mat-option>
              <mat-option value="ACTIVE">Active</mat-option>
              <mat-option value="INACTIVE">Inactive</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        <!-- Spinner -->
        <div *ngIf="loading" class="spinner-container">
          <mat-spinner diameter="44"></mat-spinner>
        </div>

        <!-- Table -->
        <div class="table-container" *ngIf="!loading">
          <table mat-table [dataSource]="filteredUsers" class="full-width">
            <!-- ID -->
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef> ID </th>
              <td mat-cell *matCellDef="let user" class="id-cell"> #{{ user.id }} </td>
            </ng-container>

            <!-- User Info (Avatar + Name + Handle) -->
            <ng-container matColumnDef="fullName">
              <th mat-header-cell *matHeaderCellDef> Account Profile </th>
              <td mat-cell *matCellDef="let user">
                <div class="user-profile-cell">
                  <div class="user-avatar" [class.admin-avatar]="user.role === 'ADMIN'">
                    {{ (user.fullName || user.username || 'U')[0].toUpperCase() }}
                  </div>
                  <div>
                    <div class="user-name">{{ user.fullName || user.username }}</div>
                    <div class="user-handle">&#64;{{ user.username }}</div>
                  </div>
                </div>
              </td>
            </ng-container>

            <!-- Email -->
            <ng-container matColumnDef="email">
              <th mat-header-cell *matHeaderCellDef> Email Address </th>
              <td mat-cell *matCellDef="let user" class="email-cell">
                <mat-icon class="email-icon">alternate_email</mat-icon>
                <span>{{ user.email }}</span>
              </td>
            </ng-container>

            <!-- Role -->
            <ng-container matColumnDef="role">
              <th mat-header-cell *matHeaderCellDef> Access Level </th>
              <td mat-cell *matCellDef="let user">
                <span class="role-badge" [class.badge-admin]="user.role === 'ADMIN'" [class.badge-user]="user.role === 'USER'">
                  <mat-icon class="role-icon">{{ user.role === 'ADMIN' ? 'admin_panel_settings' : 'person' }}</mat-icon>
                  {{ user.role }}
                </span>
              </td>
            </ng-container>

            <!-- Status -->
            <ng-container matColumnDef="active">
              <th mat-header-cell *matHeaderCellDef> Status </th>
              <td mat-cell *matCellDef="let user">
                <span class="badge-pill" [class.badge-delivered]="user.active" [class.badge-pending]="!user.active">
                  {{ user.active ? 'ACTIVE' : 'INACTIVE' }}
                </span>
              </td>
            </ng-container>

            <!-- Actions -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef> Actions </th>
              <td mat-cell *matCellDef="let user">
                <div class="action-buttons">
                  <a mat-icon-button [routerLink]="['/admin/users', user.id, 'edit']" matTooltip="Edit User" class="action-btn edit">
                    <mat-icon>edit</mat-icon>
                  </a>
                  <button mat-icon-button color="warn" (click)="onDelete(user)" matTooltip="Delete User" class="action-btn delete">
                    <mat-icon>delete</mat-icon>
                  </button>
                </div>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>

          <div *ngIf="filteredUsers.length === 0" class="empty-state">
            <mat-icon class="empty-icon">person_off</mat-icon>
            <p>No user accounts match the search criteria.</p>
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
    .admin-users-container {
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
    }

    /* KPI Grid */
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
    .kpi-icon.active-users { color: #10b981; background: rgba(16, 185, 129, 0.12); }
    .kpi-icon.admins { color: #8b5cf6; background: rgba(139, 92, 246, 0.12); }

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
      min-width: 160px;
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

    .user-profile-cell {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .user-avatar {
      display: grid;
      width: 36px;
      height: 36px;
      place-items: center;
      border-radius: 10px;
      color: #fff;
      background: linear-gradient(135deg, #06b6d4, #3b82f6);
      font-weight: 800;
      font-size: 0.85rem;
    }

    .user-avatar.admin-avatar {
      background: linear-gradient(135deg, #8b5cf6, #6366f1);
    }

    .user-name {
      font-weight: 700;
      color: var(--text-main);
    }

    .user-handle {
      font-size: 0.75rem;
      color: var(--text-muted);
    }

    .email-cell {
      display: flex;
      align-items: center;
      gap: 6px;
      color: var(--text-secondary);
    }

    .email-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      color: var(--text-muted);
    }

    .role-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 4px 10px;
      border-radius: 8px;
      font-size: 0.75rem;
      font-weight: 800;
    }

    .badge-admin {
      color: #7c3aed;
      background: rgba(124, 58, 237, 0.1);
      border: 1px solid rgba(124, 58, 237, 0.2);
    }

    .badge-user {
      color: #0284c7;
      background: rgba(2, 132, 199, 0.1);
      border: 1px solid rgba(2, 132, 199, 0.2);
    }

    .role-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
    }

    .action-buttons {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .action-btn.edit { color: #0284c7; }

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
export class UserListComponent implements OnInit {
  users: User[] = [];
  filteredUsers: User[] = [];
  displayedColumns = ['id', 'fullName', 'email', 'role', 'active', 'actions'];
  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  loading = false;

  searchQuery = '';
  selectedRole = 'ALL';
  selectedStatus = 'ALL';

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private userService: UserService,
    private notification: NotificationService,
    private dialog: MatDialog
  ) {}

  get activeCount(): number {
    return this.users.filter(u => u.active).length;
  }

  get adminCount(): number {
    return this.users.filter(u => u.role === 'ADMIN').length;
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAll(this.currentPage, this.pageSize).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.data) {
          this.users = res.data.content;
          this.totalElements = res.data.totalElements;
          this.applyFilters();
        }
      },
      error: () => {
        this.loading = false;
        this.notification.error('Failed to load users');
      }
    });
  }

  applyFilters(): void {
    let result = [...this.users];

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(u =>
        u.username.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        (u.fullName && u.fullName.toLowerCase().includes(q))
      );
    }

    if (this.selectedRole !== 'ALL') {
      result = result.filter(u => u.role === this.selectedRole);
    }

    if (this.selectedStatus === 'ACTIVE') {
      result = result.filter(u => u.active);
    } else if (this.selectedStatus === 'INACTIVE') {
      result = result.filter(u => !u.active);
    }

    this.filteredUsers = result;
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadUsers();
  }

  onDelete(user: User): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete User Account', message: `Are you sure you want to delete user "${user.username}"?` }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.userService.delete(user.id).subscribe({
          next: () => {
            this.notification.success('User account deleted');
            this.loadUsers();
          },
          error: () => this.notification.error('Failed to delete user')
        });
      }
    });
  }
}
