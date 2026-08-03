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

import { BannerService } from '../../../../core/services/banner.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { BannerResponse, BannerStatus } from '../../../../core/models/banner.model';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-banner-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule, MatDialogModule, MatTooltipModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title text-gradient-cyan"><mat-icon class="title-icon">view_carousel</mat-icon> Banners</h1>
          <p class="page-subtitle">Merchandising banners by slot, with schedule</p>
        </div>
        <button mat-raised-button class="btn-glowing" (click)="create()">
          <mat-icon>add</mat-icon> New Banner
        </button>
      </div>

      <div class="filters glass-panel">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Position</mat-label>
          <input matInput [(ngModel)]="position" (keyup.enter)="reload()" placeholder="e.g. HOME_HERO" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" class="filter-field">
          <mat-label>Status</mat-label>
          <mat-select [(ngModel)]="status" (selectionChange)="reload()">
            <mat-option [value]="undefined">All</mat-option>
            <mat-option value="DRAFT">Draft</mat-option>
            <mat-option value="PUBLISHED">Published</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <div *ngIf="loading" class="loading"><mat-spinner diameter="40"></mat-spinner></div>

      <div *ngIf="!loading" class="table-card glass-panel">
        <table class="grid-table">
          <thead>
            <tr><th>Preview</th><th>Title</th><th>Position</th><th>Order</th><th>Status</th><th>Window</th><th class="right">Actions</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let b of items">
              <td><img *ngIf="b.imageUrlDesktop" [src]="b.imageUrlDesktop" [alt]="b.altText" class="thumb" /></td>
              <td><strong>{{ b.title }}</strong></td>
              <td><code>{{ b.position }}</code></td>
              <td>{{ b.displayOrder }}</td>
              <td><span class="badge" [ngClass]="b.status.toLowerCase()">{{ b.status }}</span></td>
              <td class="muted small">
                {{ b.startsAt ? (b.startsAt | date:'short') : '—' }}<br>{{ b.endsAt ? (b.endsAt | date:'short') : '—' }}
              </td>
              <td class="right">
                <button mat-icon-button (click)="edit(b)" matTooltip="Edit"><mat-icon>edit</mat-icon></button>
                <button mat-icon-button *ngIf="b.status !== 'PUBLISHED'" (click)="publish(b)" matTooltip="Publish"><mat-icon>publish</mat-icon></button>
                <button mat-icon-button *ngIf="b.status === 'PUBLISHED'" (click)="unpublish(b)" matTooltip="Unpublish"><mat-icon>unpublished</mat-icon></button>
                <button mat-icon-button color="warn" (click)="remove(b)" matTooltip="Deactivate"><mat-icon>delete_outline</mat-icon></button>
              </td>
            </tr>
            <tr *ngIf="items.length === 0"><td colspan="7" class="empty">No banners found</td></tr>
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
    .thumb { width: 84px; height: 44px; object-fit: cover; border-radius: 6px; }
    .right { text-align: right; }
    .muted { color: var(--text-muted); }
    .small { font-size: .78rem; }
    code { font-size: .78rem; background: rgba(148,163,184,.12); padding: 2px 6px; border-radius: 4px; }
    .empty { text-align: center; color: var(--text-muted); padding: 32px; }
    .badge { padding: 2px 10px; border-radius: 999px; font-size: .72rem; font-weight: 700; text-transform: capitalize; }
    .badge.published { background: rgba(22,163,74,.12); color: #16a34a; }
    .badge.draft { background: rgba(234,179,8,.15); color: #b45309; }
    .pager { display: flex; align-items: center; justify-content: center; gap: 12px; padding-top: 12px; }
  `]
})
export class BannerListComponent implements OnInit {
  items: BannerResponse[] = [];
  loading = false;
  position = '';
  status?: BannerStatus;
  page = 0;
  size = 10;
  totalPages = 0;

  constructor(
    private bannerService: BannerService,
    private notification: NotificationService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void { this.reload(); }

  reload(): void {
    this.loading = true;
    this.bannerService.list({ status: this.status, position: this.position, page: this.page, size: this.size })
      .subscribe({
        next: (res) => {
          this.loading = false;
          this.items = res.data?.content ?? [];
          this.totalPages = res.data?.totalPages ?? 0;
        },
        error: () => { this.loading = false; this.notification.error('Failed to load banners'); }
      });
  }

  go(page: number): void { this.page = page; this.reload(); }
  create(): void { this.router.navigate(['/admin/banners/new']); }
  edit(b: BannerResponse): void { this.router.navigate(['/admin/banners', b.id, 'edit']); }

  publish(b: BannerResponse): void {
    this.bannerService.publish(b.id).subscribe({
      next: () => { this.notification.success('Banner published'); this.reload(); },
      error: () => this.notification.error('Failed to publish banner')
    });
  }

  unpublish(b: BannerResponse): void {
    this.bannerService.unpublish(b.id).subscribe({
      next: () => { this.notification.success('Banner unpublished'); this.reload(); },
      error: () => this.notification.error('Failed to unpublish banner')
    });
  }

  remove(b: BannerResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Deactivate Banner', message: `Deactivate banner "${b.title}"?` }
    });
    ref.afterClosed().subscribe((ok: boolean) => {
      if (!ok) return;
      this.bannerService.delete(b.id).subscribe({
        next: () => { this.notification.success('Banner deactivated'); this.reload(); },
        error: () => this.notification.error('Failed to deactivate banner')
      });
    });
  }
}
