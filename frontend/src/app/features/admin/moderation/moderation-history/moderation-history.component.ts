import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ModerationAuditLog } from '../../../../core/models/moderation.model';
import { ContentType } from '../../../../core/models/vote.model';
import { ModerationService } from '../../../../core/services/moderation.service';

export interface ModerationHistoryData {
  targetType: ContentType;
  targetId: number;
}

@Component({
  selector: 'app-moderation-history',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title>Lịch sử kiểm duyệt nội dung</h2>
    <mat-dialog-content>
      @if (loading) {
        <div class="center-spinner"><mat-spinner diameter="36"></mat-spinner></div>
      } @else if (logs.length === 0) {
        <p class="empty">Chưa có lịch sử thay đổi kiểm duyệt cho mục này.</p>
      } @else {
        <div class="log-timeline">
          @for (log of logs; track log.id) {
            <div class="log-item">
              <div class="log-header">
                <span class="moderator">{{ log.moderatorName }}</span>
                <span class="date">{{ log.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
              </div>
              <div class="status-change">
                <span class="status-chip old">{{ log.oldStatus || 'NONE' }}</span>
                <span class="arrow">➜</span>
                <span class="status-chip new" [class]="log.newStatus.toLowerCase()">{{ log.newStatus }}</span>
              </div>
              @if (log.reason) {
                <p class="reason"><strong>Lý do:</strong> {{ log.reason }}</p>
              }
            </div>
          }
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="close()">Đóng</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .center-spinner { display: flex; justify-content: center; padding: 24px; }
    .empty { color: #64748b; padding: 16px 0; text-align: center; }
    .log-timeline { display: flex; flex-direction: column; gap: 12px; margin-top: 8px; }
    .log-item { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; }
    .log-header { display: flex; justify-content: space-between; margin-bottom: 6px; }
    .moderator { font-weight: 700; color: #0f172a; font-size: 0.88rem; }
    .date { font-size: 0.78rem; color: #64748b; }
    .status-change { display: flex; align-items: center; gap: 8px; margin: 4px 0; }
    .arrow { color: #94a3b8; font-size: 0.8rem; }
    .status-chip { font-size: 0.75rem; font-weight: 800; padding: 2px 8px; border-radius: 12px; background: #e2e8f0; color: #475569; }
    .status-chip.new.visible { background: #dcfce7; color: #15803d; }
    .status-chip.new.hidden { background: #fef3c7; color: #b45309; }
    .status-chip.new.rejected { background: #ffe4e6; color: #be123c; }
    .reason { font-size: 0.85rem; color: #334155; margin: 6px 0 0; }
  `]
})
export class ModerationHistoryComponent implements OnInit {
  logs: ModerationAuditLog[] = [];
  loading = true;

  constructor(
    public dialogRef: MatDialogRef<ModerationHistoryComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ModerationHistoryData,
    private moderationService: ModerationService
  ) {}

  ngOnInit(): void {
    this.moderationService.getLogs(this.data.targetType, this.data.targetId).subscribe({
      next: (res) => {
        this.logs = res.data || [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
