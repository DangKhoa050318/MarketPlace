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

import { CollectionService } from '../../../../core/services/collection.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { CollectionResponse, PublishStatus } from '../../../../core/models/collection.model';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-collection-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule, MatDialogModule, MatTooltipModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title text-gradient-cyan"><mat-icon class="title-icon">collections_bookmark</mat-icon> Collections</h1>
          <p class="page-subtitle">Curated, ordered product lists</p>
        </div>
        <button mat-raised-button class="btn-glowing" (click)="create()">
          <mat-icon>add</mat-icon> New Collection
        </button>
      </div>

      <div class="filters glass-panel">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search name</mat-label>
          <input matInput [(ngModel)]="q" (keyup.enter)="reload()" placeholder="e.g. Featured" />
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
            <tr><th>Name</th><th>Slug</th><th>Items</th><th>Status</th><th class="right">Actions</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of items" [class.row-inactive]="!c.active">
              <td><strong>{{ c.name }}</strong></td>
              <td><code>{{ c.slug }}</code></td>
              <td>{{ c.items.length }}</td>
              <td>
                <span class="badge" [ngClass]="c.status.toLowerCase()">{{ c.status }}</span>
                <span class="badge inactive" *ngIf="!c.active">Inactive</span>
              </td>
              <td class="right">
                <button mat-icon-button (click)="edit(c)" matTooltip="Edit / manage items"><mat-icon>edit</mat-icon></button>
                <ng-container *ngIf="c.active">
                  <button mat-icon-button *ngIf="c.status !== 'PUBLISHED'" (click)="publish(c)" matTooltip="Publish"><mat-icon>publish</mat-icon></button>
                  <button mat-icon-button *ngIf="c.status === 'PUBLISHED'" (click)="unpublish(c)" matTooltip="Unpublish"><mat-icon>unpublished</mat-icon></button>
                  <button mat-icon-button color="warn" (click)="remove(c)" matTooltip="Deactivate"><mat-icon>delete_outline</mat-icon></button>
                </ng-container>
                <button mat-icon-button *ngIf="!c.active" color="primary" (click)="activate(c)" matTooltip="Activate"><mat-icon>restore_from_trash</mat-icon></button>
                <button mat-icon-button *ngIf="!c.active" color="warn" (click)="hardRemove(c)" matTooltip="Delete permanently"><mat-icon>delete_forever</mat-icon></button>
              </td>
            </tr>
            <tr *ngIf="items.length === 0"><td colspan="5" class="empty">No collections found</td></tr>
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
    code { font-size: .78rem; background: rgba(148,163,184,.12); padding: 2px 6px; border-radius: 4px; }
    .empty { text-align: center; color: var(--text-muted); padding: 32px; }
    .badge { padding: 2px 10px; border-radius: 999px; font-size: .72rem; font-weight: 700; text-transform: capitalize; }
    .badge.published { background: rgba(22,163,74,.12); color: #16a34a; }
    .badge.draft { background: rgba(234,179,8,.15); color: #b45309; }
    .badge.inactive { background: rgba(148,163,184,.18); color: #64748b; margin-left: 6px; }
    tr.row-inactive { opacity: .55; }
    .pager { display: flex; align-items: center; justify-content: center; gap: 12px; padding-top: 12px; }
  `]
})
export class CollectionListComponent implements OnInit {
  items: CollectionResponse[] = [];
  loading = false;
  q = '';
  status?: PublishStatus;
  page = 0;
  size = 10;
  totalPages = 0;

  constructor(
    private collectionService: CollectionService,
    private notification: NotificationService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void { this.reload(); }

  reload(): void {
    this.loading = true;
    this.collectionService.list({ status: this.status, q: this.q, page: this.page, size: this.size })
      .subscribe({
        next: (res) => {
          this.loading = false;
          this.items = res.data?.content ?? [];
          this.totalPages = res.data?.totalPages ?? 0;
        },
        error: () => { this.loading = false; this.notification.error('Failed to load collections'); }
      });
  }

  go(page: number): void { this.page = page; this.reload(); }
  create(): void { this.router.navigate(['/admin/collections/new']); }
  edit(c: CollectionResponse): void { this.router.navigate(['/admin/collections', c.id, 'edit']); }

  publish(c: CollectionResponse): void {
    this.collectionService.publish(c.id).subscribe({
      next: () => { this.notification.success('Collection published'); this.reload(); },
      error: () => this.notification.error('Failed to publish collection')
    });
  }

  unpublish(c: CollectionResponse): void {
    this.collectionService.unpublish(c.id).subscribe({
      next: () => { this.notification.success('Collection unpublished'); this.reload(); },
      error: () => this.notification.error('Failed to unpublish collection')
    });
  }

  remove(c: CollectionResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Deactivate Collection', message: `Deactivate collection "${c.name}"?` }
    });
    ref.afterClosed().subscribe((ok: boolean) => {
      if (!ok) return;
      this.collectionService.delete(c.id).subscribe({
        next: () => { this.notification.success('Collection deactivated'); this.reload(); },
        error: () => this.notification.error('Failed to deactivate collection')
      });
    });
  }

  activate(c: CollectionResponse): void {
    this.collectionService.restore(c.id).subscribe({
      next: () => { this.notification.success('Collection activated'); this.reload(); },
      error: () => this.notification.error('Failed to activate collection')
    });
  }

  hardRemove(c: CollectionResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Collection Permanently', message: `Permanently delete "${c.name}"? This cannot be undone.` }
    });
    ref.afterClosed().subscribe((ok: boolean) => {
      if (!ok) return;
      this.collectionService.hardDelete(c.id).subscribe({
        next: () => { this.notification.success('Collection permanently deleted'); this.reload(); },
        error: (err) => this.notification.error(err?.error?.message || 'Cannot delete: collection still has links')
      });
    });
  }
}
