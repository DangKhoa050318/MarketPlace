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

import { CampaignService } from '../../../../core/services/campaign.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { CampaignResponse, CampaignStatus } from '../../../../core/models/campaign.model';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-campaign-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule, MatDialogModule, MatTooltipModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title text-gradient-cyan"><mat-icon class="title-icon">campaign</mat-icon> Campaigns</h1>
          <p class="page-subtitle">Schedule marketing campaigns and surface a coupon</p>
        </div>
        <button mat-raised-button class="btn-glowing" (click)="create()">
          <mat-icon>add</mat-icon> New Campaign
        </button>
      </div>

      <div class="filters glass-panel">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search name</mat-label>
          <input matInput [(ngModel)]="q" (keyup.enter)="reload()" placeholder="e.g. Summer Sale" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" class="filter-field">
          <mat-label>Status</mat-label>
          <mat-select [(ngModel)]="status" (selectionChange)="reload()">
            <mat-option [value]="undefined">All</mat-option>
            <mat-option value="DRAFT">Draft</mat-option>
            <mat-option value="PUBLISHED">Published</mat-option>
            <mat-option value="ARCHIVED">Archived</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <div *ngIf="loading" class="loading"><mat-spinner diameter="40"></mat-spinner></div>

      <div *ngIf="!loading" class="table-card glass-panel">
        <table class="grid-table">
          <thead>
            <tr><th>Name</th><th>Status</th><th>Window</th><th>Coupon</th><th class="right">Actions</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of items">
              <td><strong>{{ c.name }}</strong><div class="muted small" *ngIf="c.description">{{ c.description }}</div></td>
              <td><span class="badge" [ngClass]="c.status.toLowerCase()">{{ c.status }}</span></td>
              <td class="muted small">
                {{ c.startsAt ? (c.startsAt | date:'short') : '—' }}<br>{{ c.endsAt ? (c.endsAt | date:'short') : '—' }}
              </td>
              <td>{{ c.couponCode || '—' }}</td>
              <td class="right">
                <button mat-icon-button (click)="edit(c)" matTooltip="Edit"><mat-icon>edit</mat-icon></button>
                <button mat-icon-button *ngIf="c.status !== 'PUBLISHED'" (click)="publish(c)" matTooltip="Publish"><mat-icon>publish</mat-icon></button>
                <button mat-icon-button *ngIf="c.status !== 'ARCHIVED'" (click)="archive(c)" matTooltip="Archive"><mat-icon>archive</mat-icon></button>
                <button mat-icon-button color="warn" (click)="remove(c)" matTooltip="Deactivate"><mat-icon>delete_outline</mat-icon></button>
              </td>
            </tr>
            <tr *ngIf="items.length === 0"><td colspan="5" class="empty">No campaigns found</td></tr>
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
    .badge { padding: 2px 10px; border-radius: 999px; font-size: .72rem; font-weight: 700; text-transform: capitalize; }
    .badge.published { background: rgba(22,163,74,.12); color: #16a34a; }
    .badge.draft { background: rgba(234,179,8,.15); color: #b45309; }
    .badge.archived { background: rgba(148,163,184,.15); color: #64748b; }
    .pager { display: flex; align-items: center; justify-content: center; gap: 12px; padding-top: 12px; }
  `]
})
export class CampaignListComponent implements OnInit {
  items: CampaignResponse[] = [];
  loading = false;
  q = '';
  status?: CampaignStatus;
  page = 0;
  size = 10;
  totalPages = 0;

  constructor(
    private campaignService: CampaignService,
    private notification: NotificationService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void { this.reload(); }

  reload(): void {
    this.loading = true;
    this.campaignService.list({ status: this.status, q: this.q, page: this.page, size: this.size })
      .subscribe({
        next: (res) => {
          this.loading = false;
          this.items = res.data?.content ?? [];
          this.totalPages = res.data?.totalPages ?? 0;
        },
        error: () => { this.loading = false; this.notification.error('Failed to load campaigns'); }
      });
  }

  go(page: number): void { this.page = page; this.reload(); }
  create(): void { this.router.navigate(['/admin/campaigns/new']); }
  edit(c: CampaignResponse): void { this.router.navigate(['/admin/campaigns', c.id, 'edit']); }

  publish(c: CampaignResponse): void {
    this.campaignService.publish(c.id).subscribe({
      next: () => { this.notification.success('Campaign published'); this.reload(); },
      error: () => this.notification.error('Failed to publish campaign')
    });
  }

  archive(c: CampaignResponse): void {
    this.campaignService.archive(c.id).subscribe({
      next: () => { this.notification.success('Campaign archived'); this.reload(); },
      error: () => this.notification.error('Failed to archive campaign')
    });
  }

  remove(c: CampaignResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Deactivate Campaign', message: `Deactivate campaign "${c.name}"?` }
    });
    ref.afterClosed().subscribe((ok: boolean) => {
      if (!ok) return;
      this.campaignService.delete(c.id).subscribe({
        next: () => { this.notification.success('Campaign deactivated'); this.reload(); },
        error: () => this.notification.error('Failed to deactivate campaign')
      });
    });
  }
}
