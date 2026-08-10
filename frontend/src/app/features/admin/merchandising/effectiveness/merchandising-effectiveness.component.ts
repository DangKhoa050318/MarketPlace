import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MerchandisingService } from '../../../../core/services/merchandising.service';
import { NotificationService } from '../../../../core/services/notification.service';
import {
  MerchandisingSummaryResponse,
  MerchandisingTargetType
} from '../../../../core/models/merchandising.model';

/**
 * F-406: merchandising effectiveness report. Picks a target (campaign/collection/banner) and a
 * time range, then shows impressions, clicks, CTR and attributed orders from the B-408 summary.
 */
@Component({
  selector: 'app-merchandising-effectiveness',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1 class="page-title text-gradient-cyan">
            <mat-icon class="title-icon">insights</mat-icon> Merchandising Effectiveness
          </h1>
          <p class="page-subtitle">Impressions, clicks, CTR and attributed orders for a target over a time range</p>
        </div>
      </div>

      <form class="filters glass-panel" [formGroup]="form" (ngSubmit)="load()">
        <mat-form-field appearance="outline" class="field">
          <mat-label>Target type</mat-label>
          <mat-select formControlName="targetType">
            <mat-option value="CAMPAIGN">Campaign</mat-option>
            <mat-option value="COLLECTION">Collection</mat-option>
            <mat-option value="BANNER">Banner</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="field">
          <mat-label>Target ID</mat-label>
          <input matInput type="number" min="1" formControlName="targetId" placeholder="e.g. 1" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="field">
          <mat-label>From</mat-label>
          <input matInput type="datetime-local" formControlName="from" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="field">
          <mat-label>To</mat-label>
          <input matInput type="datetime-local" formControlName="to" />
        </mat-form-field>
        <button mat-raised-button class="btn-glowing" type="submit" [disabled]="form.invalid || loading">
          <mat-icon>query_stats</mat-icon> Run report
        </button>
      </form>

      <div *ngIf="loading" class="loading"><mat-spinner diameter="40"></mat-spinner></div>

      <div *ngIf="!loading && result" class="metrics">
        <div class="metric glass-panel">
          <span class="label">Impressions</span>
          <strong class="value">{{ result.impressions | number }}</strong>
        </div>
        <div class="metric glass-panel">
          <span class="label">Clicks</span>
          <strong class="value">{{ result.clicks | number }}</strong>
        </div>
        <div class="metric glass-panel">
          <span class="label">CTR</span>
          <strong class="value">{{ result.ctr | percent:'1.0-2' }}</strong>
        </div>
        <div class="metric glass-panel">
          <span class="label">Attributed orders</span>
          <strong class="value">{{ result.attributedOrders | number }}</strong>
        </div>
      </div>

      <div *ngIf="!loading && !result" class="empty glass-panel">
        Choose a target and range, then run the report.
      </div>
    </div>
  `,
  styles: [`
    .page { display: flex; flex-direction: column; gap: 20px; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .page-title { display: flex; align-items: center; gap: 12px; margin: 0; font-size: 2rem; font-weight: 800; }
    .title-icon { font-size: 30px; width: 30px; height: 30px; color: #38bdf8; }
    .page-subtitle { margin: 4px 0 0; color: var(--text-muted); font-size: .9rem; }
    .filters { display: flex; gap: 14px; padding: 16px; align-items: center; flex-wrap: wrap; }
    .filters .field { min-width: 170px; }
    .loading { display: flex; justify-content: center; padding: 60px; }
    .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; }
    .metric { padding: 20px; display: flex; flex-direction: column; gap: 6px; border-radius: 14px; }
    .metric .label { color: var(--text-muted); text-transform: uppercase; font-size: .72rem; font-weight: 700; letter-spacing: .05em; }
    .metric .value { font-size: 1.9rem; font-weight: 850; color: var(--text-main); }
    .empty { text-align: center; color: var(--text-muted); padding: 40px; border-radius: 14px; }
  `]
})
export class MerchandisingEffectivenessComponent {
  form: FormGroup;
  loading = false;
  result?: MerchandisingSummaryResponse;

  constructor(
    private fb: FormBuilder,
    private merchandising: MerchandisingService,
    private notification: NotificationService
  ) {
    const now = new Date();
    const from = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
    this.form = this.fb.group({
      targetType: ['CAMPAIGN' as MerchandisingTargetType, Validators.required],
      targetId: [null, [Validators.required, Validators.min(1)]],
      from: [this.toLocalInput(from), Validators.required],
      to: [this.toLocalInput(now), Validators.required]
    });
  }

  load(): void {
    if (this.form.invalid) return;
    const { targetType, targetId, from, to } = this.form.value;
    this.loading = true;
    this.result = undefined;
    this.merchandising.summary(targetType, Number(targetId), from, to).subscribe({
      next: res => {
        this.loading = false;
        if (res.success) {
          this.result = res.data;
        } else {
          this.notification.error(res.message || 'Failed to load summary');
        }
      },
      error: () => {
        this.loading = false;
        this.notification.error('Failed to load merchandising summary');
      }
    });
  }

  private toLocalInput(date: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
      + `T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
}
