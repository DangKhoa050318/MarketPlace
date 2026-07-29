import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ContentType } from '../../../../core/models/vote.model';
import { ModerationStatus } from '../../../../core/models/question.model';
import { ModerationItem } from '../../../../core/models/moderation.model';
import { ModerationService } from '../../../../core/services/moderation.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ModerationDialogComponent } from '../moderation-dialog/moderation-dialog.component';
import { ModerationHistoryComponent } from '../moderation-history/moderation-history.component';

@Component({
  selector: 'app-moderation-queue',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule, MatTableModule,
    MatPaginatorModule, MatSelectModule, MatInputModule, MatFormFieldModule,
    MatProgressSpinnerModule, MatDialogModule
  ],
  template: `
    <div class="moderation-page">
      <div class="page-header">
        <div>
          <h1>Kiểm duyệt nội dung</h1>
          <p class="subtitle">Quản lý và kiểm duyệt Đánh giá, Câu hỏi & Câu trả lời người dùng</p>
        </div>
      </div>

      <!-- Filters -->
      <div class="filter-card surface-card">
        <mat-form-field appearance="outline" class="filter-field">
          <mat-label>Loại nội dung</mat-label>
          <mat-select [(ngModel)]="targetType" (selectionChange)="onFilterChange()">
            <mat-option value="QUESTION">Câu hỏi (Question)</mat-option>
            <mat-option value="ANSWER">Câu trả lời (Answer)</mat-option>
            <mat-option value="REVIEW">Đánh giá (Review)</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="filter-field">
          <mat-label>Trạng thái</mat-label>
          <mat-select [(ngModel)]="status" (selectionChange)="onFilterChange()">
            <mat-option [value]="undefined">Tất cả trạng thái</mat-option>
            <mat-option value="VISIBLE">Hiển thị (VISIBLE)</mat-option>
            <mat-option value="HIDDEN">Đã ẩn (HIDDEN)</mat-option>
            <mat-option value="PENDING_REVIEW">Chờ duyệt (PENDING)</mat-option>
            <mat-option value="REJECTED">Từ chối (REJECTED)</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="filter-field">
          <mat-label>Mã sản phẩm (ID)</mat-label>
          <input matInput type="number" [(ngModel)]="productId" (change)="onFilterChange()" placeholder="VD: 1">
        </mat-form-field>

        <button mat-stroked-button class="reset-btn" (click)="resetFilters()">
          <mat-icon>refresh</mat-icon> Đặt lại
        </button>
      </div>

      <!-- Table Queue -->
      <div class="table-card surface-card">
        @if (loading) {
          <div class="center-spinner"><mat-spinner diameter="40"></mat-spinner></div>
        } @else if (items.length === 0) {
          <div class="empty-state">
            <mat-icon>verified_user</mat-icon>
            <p>Không tìm thấy nội dung nào phù hợp với bộ lọc hiện tại.</p>
          </div>
        } @else {
          <table mat-table [dataSource]="items" class="moderation-table">
            <!-- Content Column -->
            <ng-container matColumnDef="content">
              <th mat-header-cell *matHeaderCellDef>Nội dung</th>
              <td mat-cell *matCellDef="let element">
                <div class="content-cell">
                  <span class="product-tag">{{ element.productName }} (ID: {{ element.productId }})</span>
                  <p class="text">{{ element.content }}</p>
                  <small class="author">Tác giả: {{ element.authorName }} | {{ element.createdAt | date:'dd/MM/yyyy HH:mm' }}</small>
                </div>
              </td>
            </ng-container>

            <!-- Status Column -->
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Trạng thái</th>
              <td mat-cell *matCellDef="let element">
                <span class="status-badge" [class]="element.status.toLowerCase()">
                  {{ element.status }}
                </span>
              </td>
            </ng-container>

            <!-- Actions Column -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef class="actions-header">Thao tác kiểm duyệt</th>
              <td mat-cell *matCellDef="let element">
                <div class="action-buttons">
                  @if (element.status !== 'VISIBLE') {
                    <button mat-flat-button color="primary" class="action-btn sm" (click)="updateStatus(element, 'VISIBLE')">
                      <mat-icon>check_circle</mat-icon> Duyệt
                    </button>
                  }
                  @if (element.status !== 'HIDDEN') {
                    <button mat-stroked-button color="accent" class="action-btn sm" (click)="updateStatus(element, 'HIDDEN')">
                      <mat-icon>visibility_off</mat-icon> Ẩn
                    </button>
                  }
                  @if (element.status !== 'REJECTED') {
                    <button mat-stroked-button color="warn" class="action-btn sm" (click)="updateStatus(element, 'REJECTED')">
                      <mat-icon>cancel</mat-icon> Từ chối
                    </button>
                  }
                  <button mat-icon-button color="basic" (click)="openHistory(element)" title="Xem lịch sử kiểm duyệt">
                    <mat-icon>history</mat-icon>
                  </button>
                </div>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>

          <mat-paginator
            [length]="totalElements"
            [pageSize]="pageSize"
            [pageIndex]="page"
            (page)="onPageChange($event)"
            showFirstLastButtons>
          </mat-paginator>
        }
      </div>
    </div>
  `,
  styles: [`
    .moderation-page { padding: 24px; }
    .page-header h1 { margin: 0 0 4px; font-size: 1.8rem; font-weight: 800; color: #0f172a; }
    .subtitle { margin: 0 0 20px; color: #64748b; font-size: 0.95rem; }
    .filter-card { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; padding: 16px 20px; border-radius: 12px; margin-bottom: 20px; background: #fff; border: 1px solid #e2e8f0; }
    .filter-field { width: 220px; margin-bottom: -1.25em; }
    .reset-btn { height: 48px; border-radius: 8px; }
    .table-card { border-radius: 12px; background: #fff; border: 1px solid #e2e8f0; overflow: hidden; }
    .center-spinner { display: flex; justify-content: center; padding: 40px; }
    .empty-state { display: flex; flex-direction: column; align-items: center; padding: 60px 0; color: #64748b; }
    .empty-state mat-icon { font-size: 48px; width: 48px; height: 48px; opacity: 0.4; margin-bottom: 12px; }
    .moderation-table { width: 100%; }
    .content-cell { padding: 12px 0; }
    .product-tag { font-size: 0.75rem; font-weight: 700; background: #e0f2fe; color: #0369a1; padding: 2px 8px; border-radius: 4px; }
    .text { margin: 6px 0 4px; font-size: 0.95rem; color: #1e293b; font-weight: 600; line-height: 1.4; }
    .author { font-size: 0.8rem; color: #64748b; }
    .status-badge { font-size: 0.78rem; font-weight: 800; padding: 4px 10px; border-radius: 12px; }
    .status-badge.visible { background: #dcfce7; color: #15803d; }
    .status-badge.hidden { background: #fef3c7; color: #b45309; }
    .status-badge.pending_review { background: #e0f2fe; color: #0369a1; }
    .status-badge.rejected { background: #ffe4e6; color: #be123c; }
    .actions-header { text-align: right; }
    .action-buttons { display: flex; justify-content: flex-end; align-items: center; gap: 8px; }
    .action-btn.sm { height: 32px; line-height: 32px; padding: 0 10px; font-size: 0.8rem; }
    .action-btn mat-icon { font-size: 16px; width: 16px; height: 16px; margin-right: 4px; }
  `]
})
export class ModerationQueueComponent implements OnInit {
  targetType: ContentType = 'QUESTION';
  status: ModerationStatus | undefined = undefined;
  productId: number | undefined = undefined;

  items: ModerationItem[] = [];
  loading = false;
  totalElements = 0;
  page = 0;
  pageSize = 10;

  displayedColumns = ['content', 'status', 'actions'];

  constructor(
    private moderationService: ModerationService,
    private notificationService: NotificationService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadQueue();
  }

  loadQueue(): void {
    this.loading = true;
    this.moderationService.getQueue({
      targetType: this.targetType,
      status: this.status,
      productId: this.productId,
      page: this.page,
      size: this.pageSize
    }).subscribe({
      next: (res) => {
        if (res.data) {
          this.items = res.data.content || [];
          this.totalElements = res.data.totalElements || 0;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Không thể tải danh sách kiểm duyệt');
      }
    });
  }

  onFilterChange(): void {
    this.page = 0;
    this.loadQueue();
  }

  resetFilters(): void {
    this.targetType = 'QUESTION';
    this.status = undefined;
    this.productId = undefined;
    this.page = 0;
    this.loadQueue();
  }

  updateStatus(item: ModerationItem, newStatus: ModerationStatus): void {
    const requireReason = newStatus === 'HIDDEN' || newStatus === 'REJECTED';
    const actionText = newStatus === 'VISIBLE' ? 'Duyệt' : (newStatus === 'HIDDEN' ? 'Ẩn' : 'Từ chối');

    const dialogRef = this.dialog.open(ModerationDialogComponent, {
      width: '450px',
      data: {
        title: `${actionText} nội dung ID #${item.id}`,
        action: newStatus,
        requireReason
      }
    });

    dialogRef.afterClosed().subscribe((res) => {
      if (res && res.confirmed) {
        this.moderationService.updateStatus({
          targetType: item.targetType,
          targetId: item.targetId,
          newStatus,
          reason: res.reason
        }).subscribe({
          next: () => {
            this.notificationService.success(`Đã cập nhật trạng thái sang ${newStatus}`);
            this.loadQueue();
          },
          error: (err) => {
            this.notificationService.error(err?.error?.message || 'Không thể cập nhật trạng thái');
          }
        });
      }
    });
  }

  openHistory(item: ModerationItem): void {
    this.dialog.open(ModerationHistoryComponent, {
      width: '500px',
      data: {
        targetType: item.targetType,
        targetId: item.targetId
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.page = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadQueue();
  }
}
