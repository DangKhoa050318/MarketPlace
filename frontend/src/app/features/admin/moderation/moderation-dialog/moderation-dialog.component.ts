import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { ModerationStatus } from '../../../../core/models/question.model';

export interface ModerationDialogData {
  title: string;
  action: ModerationStatus;
  requireReason: boolean;
}

@Component({
  selector: 'app-moderation-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      @if (data.requireReason) {
        <p class="instruction">Vui lòng nhập lý do cụ thể cho thao tác này (bắt buộc):</p>
        <textarea
          [(ngModel)]="reason"
          rows="4"
          placeholder="Nhập lý do ẩn/từ chối nội dung..."
          class="form-control"></textarea>
      } @else {
        <p>Bạn có chắc chắn muốn duyệt cho hiển thị công khai nội dung này?</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Hủy</button>
      <button
        mat-raised-button
        [color]="data.action === 'VISIBLE' ? 'primary' : 'warn'"
        [disabled]="data.requireReason && !reason.trim()"
        (click)="onConfirm()">
        Xác nhận
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .instruction { font-size: 0.9rem; color: #475569; margin-bottom: 12px; }
    .form-control { width: 100%; border: 1px solid #cbd5e1; border-radius: 8px; padding: 10px; font-family: inherit; font-size: 0.95rem; resize: vertical; box-sizing: border-box; }
    .form-control:focus { outline: none; border-color: #0284c7; }
  `]
})
export class ModerationDialogComponent {
  reason = '';

  constructor(
    public dialogRef: MatDialogRef<ModerationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ModerationDialogData
  ) {}

  onCancel(): void {
    this.dialogRef.close();
  }

  onConfirm(): void {
    this.dialogRef.close({ confirmed: true, reason: this.reason.trim() });
  }
}
