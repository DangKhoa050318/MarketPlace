import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';

import { PromotionService } from '../../../../core/services/promotion.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { PromotionCodeResponse, DiscountType } from '../../../../core/models/promotion.model';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-promotion-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule, MatDialogModule, MatTooltipModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title text-gradient-cyan"><mat-icon class="title-icon">local_offer</mat-icon> Coupons</h1>
          <p class="page-subtitle">Create and manage promotional discount codes</p>
        </div>
        <button mat-raised-button class="btn-glowing" (click)="create()">
          <mat-icon>add</mat-icon> New Coupon
        </button>
      </div>

      <div class="filters glass-panel">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search code</mat-label>
          <input matInput [(ngModel)]="q" (keyup.enter)="reload()" placeholder="e.g. SUMMER" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" class="filter-field">
          <mat-label>Type</mat-label>
          <mat-select [(ngModel)]="type" (selectionChange)="reload()">
            <mat-option [value]="undefined">All</mat-option>
            <mat-option value="PERCENT">Percent</mat-option>
            <mat-option value="FIXED">Fixed</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="filter-field">
          <mat-label>Status</mat-label>
          <mat-select [(ngModel)]="active" (selectionChange)="reload()">
            <mat-option [value]="undefined">All</mat-option>
            <mat-option [value]="true">Active</mat-option>
            <mat-option [value]="false">Inactive</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <div *ngIf="loading" class="loading"><mat-spinner diameter="40"></mat-spinner></div>

      <div *ngIf="!loading" class="table-card glass-panel">
        <table class="grid-table">
          <thead>
            <tr>
              <th>Code</th><th>Type</th><th>Value</th><th>Scope</th><th>Used / Limit</th>
              <th>Window</th><th>Status</th><th class="right">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of items">
              <td><strong>{{ c.code }}</strong></td>
              <td>{{ c.discountType }}</td>
              <td>{{ c.discountType === 'PERCENT' ? c.discountValue + '%' : (c.discountValue | currency:'VND':'symbol':'1.0-0') }}
                <span *ngIf="c.maxDiscount" class="muted"> (max {{ c.maxDiscount | currency:'VND':'symbol':'1.0-0' }})</span>
              </td>
              <td>{{ c.scopeType }}</td>
              <td>{{ c.usedCount }} / {{ c.usageLimit ?? '∞' }}</td>
              <td class="muted small">
                {{ c.startsAt ? (c.startsAt | date:'short') : '—' }}<br>{{ c.expiresAt ? (c.expiresAt | date:'short') : '—' }}
              </td>
              <td><span class="badge" [class.on]="c.active" [class.off]="!c.active">{{ c.active ? 'Active' : 'Inactive' }}</span></td>
              <td class="right">
                <button mat-icon-button (click)="edit(c)" matTooltip="Edit"><mat-icon>edit</mat-icon></button>
                <button mat-icon-button color="warn" (click)="remove(c)" matTooltip="Deactivate"><mat-icon>delete_outline</mat-icon></button>
              </td>
            </tr>
            <tr *ngIf="items.length === 0"><td colspan="8" class="empty">No coupons found</td></tr>
          </tbody>
        </table>

        <div class="pager" *ngIf="totalPages > 1">
          <button mat-button [disabled]="page === 0" (click)="go(page - 1)"><mat-icon>chevron_left</mat-icon></button>
          <span>Page {{ page + 1 }} of {{ totalPages }}</span>
          <button mat-button [disabled]="page + 1 >= totalPages" (click)="go(page + 1)"><mat-icon>chevron_right</mat-icon></button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page { display: flex; flex-direction: column; gap: 20px; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .page-title { display: flex; align-items: center; gap: 12px; margin: 0; font-size: 2rem; font-weight: 800; }
    .title-icon { font-size: 30px; width: 30px; height: 30px; color: #38bdf8; }
    .page-subtitle { margin: 4px 0 0; color: var(--text-muted); font-size: .9rem; }
    .filters { display: flex; gap: 16px; padding: 12px 16px; align-items: center; flex-wrap: wrap; }
    .filters .search-field { flex: 2; min-width: 240px; }
    .filters .filter-field { flex: 1; min-width: 160px; max-width: 250px; }
    .loading { display: flex; justify-content: center; padding: 60px; }
    .table-card { padding: 8px 12px 16px; overflow-x: auto; }
    .grid-table { width: 100%; border-collapse: collapse; }
    .grid-table th, .grid-table td { text-align: left; padding: 12px 10px; border-bottom: 1px solid var(--glass-border-subtle); font-size: .88rem; }
    .grid-table th { color: var(--text-muted); text-transform: uppercase; font-size: .72rem; font-weight: 700; }
    .right { text-align: right; }
    .muted { color: var(--text-muted); }
    .small { font-size: .78rem; }
    .empty { text-align: center; color: var(--text-muted); padding: 32px; }
    .badge { padding: 2px 10px; border-radius: 999px; font-size: .72rem; font-weight: 700; }
    .badge.on { background: rgba(22,163,74,.12); color: #16a34a; }
    .badge.off { background: rgba(148,163,184,.15); color: #64748b; }
    .pager { display: flex; align-items: center; justify-content: center; gap: 12px; padding-top: 12px; }
  `]
})
export class PromotionListComponent implements OnInit {
  items: PromotionCodeResponse[] = [];
  loading = false;
  q = '';
  type?: DiscountType;
  active?: boolean;
  page = 0;
  size = 10;
  totalPages = 0;

  constructor(
    private promotionService: PromotionService,
    private notification: NotificationService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void { this.reload(); }

  reload(): void {
    this.loading = true;
    this.promotionService.list({ active: this.active, type: this.type, q: this.q, page: this.page, size: this.size })
      .subscribe({
        next: (res) => {
          this.loading = false;
          this.items = res.data?.content ?? [];
          this.totalPages = res.data?.totalPages ?? 0;
        },
        error: () => { this.loading = false; this.notification.error('Failed to load coupons'); }
      });
  }

  go(page: number): void { this.page = page; this.reload(); }
  create(): void { this.router.navigate(['/admin/coupons/new']); }
  edit(c: PromotionCodeResponse): void { this.router.navigate(['/admin/coupons', c.id, 'edit']); }

  remove(c: PromotionCodeResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Deactivate Coupon', message: `Deactivate coupon "${c.code}"? It will no longer be usable.` }
    });
    ref.afterClosed().subscribe((ok: boolean) => {
      if (!ok) return;
      this.promotionService.delete(c.id).subscribe({
        next: () => { this.notification.success('Coupon deactivated'); this.reload(); },
        error: () => this.notification.error('Failed to deactivate coupon')
      });
    });
  }
}
