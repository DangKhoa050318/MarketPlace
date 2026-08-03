import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CampaignService } from '../../../../core/services/campaign.service';
import { PromotionService } from '../../../../core/services/promotion.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { CreateCampaignRequest } from '../../../../core/models/campaign.model';
import { PromotionCodeResponse } from '../../../../core/models/promotion.model';

@Component({
  selector: 'app-campaign-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <button mat-icon-button (click)="back()"><mat-icon>arrow_back</mat-icon></button>
        <h1 class="page-title">{{ isEdit ? 'Edit Campaign' : 'New Campaign' }}</h1>
      </div>

      <form [formGroup]="form" (ngSubmit)="submit()" class="form glass-panel" *ngIf="!loading">
        <mat-form-field appearance="outline">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" maxlength="150" />
          <mat-error *ngIf="form.get('name')?.hasError('required')">Name is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="3"></textarea>
        </mat-form-field>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Starts at</mat-label>
            <input matInput type="datetime-local" formControlName="startsAt" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Ends at</mat-label>
            <input matInput type="datetime-local" formControlName="endsAt" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline">
          <mat-label>Linked coupon (optional)</mat-label>
          <mat-select formControlName="promotionCodeId">
            <mat-option [value]="null">— None —</mat-option>
            <mat-option *ngFor="let c of coupons" [value]="c.id">{{ c.code }}</mat-option>
          </mat-select>
          <mat-hint>The campaign surfaces this coupon; it is not auto-applied.</mat-hint>
        </mat-form-field>

        <div class="actions">
          <button mat-button type="button" (click)="back()">Cancel</button>
          <button mat-raised-button class="btn-glowing" type="submit" [disabled]="form.invalid || saving">
            <mat-icon>save</mat-icon> {{ isEdit ? 'Save' : 'Create' }}
          </button>
        </div>
      </form>

      <div *ngIf="loading" class="loading"><mat-spinner diameter="40"></mat-spinner></div>
    </div>
  `,
  styles: [`
    .page { display: flex; flex-direction: column; gap: 16px; max-width: 720px; }
    .page-header { display: flex; align-items: center; gap: 8px; }
    .page-title { margin: 0; font-size: 1.6rem; font-weight: 800; }
    .form { display: flex; flex-direction: column; gap: 6px; padding: 24px; }
    .row { display: flex; gap: 16px; }
    .row mat-form-field { flex: 1; }
    .actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
    .loading { display: flex; justify-content: center; padding: 60px; }
  `]
})
export class CampaignFormComponent implements OnInit {
  form: FormGroup;
  isEdit = false;
  editId!: number;
  loading = false;
  saving = false;
  coupons: PromotionCodeResponse[] = [];

  constructor(
    private fb: FormBuilder,
    private campaignService: CampaignService,
    private promotionService: PromotionService,
    private notification: NotificationService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      description: [null],
      startsAt: [null],
      endsAt: [null],
      promotionCodeId: [null]
    });
  }

  ngOnInit(): void {
    this.promotionService.list({ active: true, size: 200 }).subscribe({
      next: (res) => { this.coupons = res.data?.content ?? []; },
      error: () => {}
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.editId = +id;
      this.loading = true;
      this.campaignService.getById(this.editId).subscribe({
        next: (res) => {
          this.loading = false;
          const c = res.data;
          this.form.patchValue({
            name: c.name,
            description: c.description ?? null,
            startsAt: this.toLocalInput(c.startsAt),
            endsAt: this.toLocalInput(c.endsAt),
            promotionCodeId: c.promotionCodeId ?? null
          });
        },
        error: () => { this.loading = false; this.notification.error('Failed to load campaign'); }
      });
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    this.saving = true;
    const req: CreateCampaignRequest = this.form.value;
    const done = {
      next: () => { this.saving = false; this.notification.success(this.isEdit ? 'Campaign updated' : 'Campaign created'); this.back(); },
      error: () => { this.saving = false; this.notification.error('Failed to save campaign'); }
    };
    if (this.isEdit) {
      this.campaignService.update(this.editId, req).subscribe(done);
    } else {
      this.campaignService.create(req).subscribe(done);
    }
  }

  back(): void { this.router.navigate(['/admin/campaigns']); }

  /** Trim a backend ISO datetime to the "YYYY-MM-DDTHH:mm" a datetime-local input expects. */
  private toLocalInput(value?: string): string | null {
    return value ? value.substring(0, 16) : null;
  }
}
