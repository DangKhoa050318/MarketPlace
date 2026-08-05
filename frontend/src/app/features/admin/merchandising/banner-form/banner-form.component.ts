import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { BannerService } from '../../../../core/services/banner.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { CreateBannerRequest } from '../../../../core/models/banner.model';

@Component({
  selector: 'app-banner-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatButtonToggleModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <button mat-icon-button (click)="back()"><mat-icon>arrow_back</mat-icon></button>
        <h1 class="page-title">{{ isEdit ? 'Edit Banner' : 'New Banner' }}</h1>
      </div>

      <div class="layout" *ngIf="!loading">
        <form [formGroup]="form" (ngSubmit)="submit()" class="form glass-panel">
          <mat-form-field appearance="outline">
            <mat-label>Title</mat-label>
            <input matInput formControlName="title" maxlength="150" />
            <mat-error *ngIf="form.get('title')?.hasError('required')">Title is required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Desktop image URL</mat-label>
            <input matInput formControlName="imageUrlDesktop" />
            <mat-error *ngIf="form.get('imageUrlDesktop')?.hasError('required')">Desktop image is required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Mobile image URL (optional)</mat-label>
            <input matInput formControlName="imageUrlMobile" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Alt text</mat-label>
            <input matInput formControlName="altText" maxlength="255" />
            <mat-error *ngIf="form.get('altText')?.hasError('required')">Alt text is required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Target URL (optional)</mat-label>
            <input matInput formControlName="targetUrl" />
          </mat-form-field>

          <div class="row">
            <mat-form-field appearance="outline">
              <mat-label>Position (slot)</mat-label>
              <input matInput formControlName="position" placeholder="HOME_HERO" maxlength="40" />
              <mat-error *ngIf="form.get('position')?.hasError('required')">Position is required</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Display order</mat-label>
              <input matInput type="number" formControlName="displayOrder" />
            </mat-form-field>
          </div>

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

          <div class="actions">
            <button mat-button type="button" (click)="back()">Cancel</button>
            <button mat-raised-button class="btn-glowing" type="submit" [disabled]="form.invalid || saving">
              <mat-icon>save</mat-icon> {{ isEdit ? 'Save' : 'Create' }}
            </button>
          </div>
        </form>

        <div class="preview glass-panel">
          <div class="preview-head">
            <span>Preview</span>
            <mat-button-toggle-group [value]="device" (change)="device = $event.value" hideSingleSelectionIndicator>
              <mat-button-toggle value="desktop"><mat-icon>desktop_windows</mat-icon></mat-button-toggle>
              <mat-button-toggle value="mobile"><mat-icon>smartphone</mat-icon></mat-button-toggle>
            </mat-button-toggle-group>
          </div>
          <div class="frame" [class.mobile]="device === 'mobile'">
            <img *ngIf="previewSrc()" [src]="previewSrc()" [alt]="form.get('altText')?.value || ''" />
            <div *ngIf="!previewSrc()" class="ph"><mat-icon>image</mat-icon><span>Image preview</span></div>
          </div>
          <p class="alt muted small" *ngIf="form.get('altText')?.value">alt: "{{ form.get('altText')?.value }}"</p>
        </div>
      </div>

      <div *ngIf="loading" class="loading"><mat-spinner diameter="40"></mat-spinner></div>
    </div>
  `,
  styles: [`
    .page { display: flex; flex-direction: column; gap: 16px; }
    .page-header { display: flex; align-items: center; gap: 8px; }
    .page-title { margin: 0; font-size: 1.6rem; font-weight: 800; }
    .layout { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr); gap: 20px; align-items: start; }
    @media (max-width: 900px) { .layout { grid-template-columns: 1fr; } }
    .form { display: flex; flex-direction: column; gap: 6px; padding: 24px; }
    .row { display: flex; gap: 16px; }
    .row mat-form-field { flex: 1; }
    .actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
    .preview { padding: 16px; position: sticky; top: 16px; }
    .preview-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; font-weight: 700; }
    .frame { border: 1px dashed var(--glass-border-subtle); border-radius: 10px; overflow: hidden; display: flex; align-items: center; justify-content: center; min-height: 180px; background: rgba(148,163,184,.06); }
    .frame.mobile { max-width: 320px; margin: 0 auto; }
    .frame img { width: 100%; height: auto; display: block; }
    .ph { display: flex; flex-direction: column; align-items: center; gap: 6px; color: var(--text-muted); padding: 40px; }
    .ph mat-icon { font-size: 40px; width: 40px; height: 40px; }
    .muted { color: var(--text-muted); } .small { font-size: .78rem; }
    .alt { margin: 10px 2px 0; }
    .loading { display: flex; justify-content: center; padding: 60px; }
  `]
})
export class BannerFormComponent implements OnInit {
  form: FormGroup;
  isEdit = false;
  editId!: number;
  loading = false;
  saving = false;
  device: 'desktop' | 'mobile' = 'desktop';

  constructor(
    private fb: FormBuilder,
    private bannerService: BannerService,
    private notification: NotificationService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group({
      title: ['', Validators.required],
      imageUrlDesktop: ['', Validators.required],
      imageUrlMobile: [null],
      altText: ['', Validators.required],
      targetUrl: [null],
      position: ['', Validators.required],
      displayOrder: [0],
      startsAt: [null],
      endsAt: [null]
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.editId = +id;
      this.loading = true;
      this.bannerService.getById(this.editId).subscribe({
        next: (res) => {
          this.loading = false;
          const b = res.data;
          this.form.patchValue({
            title: b.title,
            imageUrlDesktop: b.imageUrlDesktop,
            imageUrlMobile: b.imageUrlMobile ?? null,
            altText: b.altText,
            targetUrl: b.targetUrl ?? null,
            position: b.position,
            displayOrder: b.displayOrder ?? 0,
            startsAt: this.toLocalInput(b.startsAt),
            endsAt: this.toLocalInput(b.endsAt)
          });
        },
        error: () => { this.loading = false; this.notification.error('Failed to load banner'); }
      });
    }
  }

  previewSrc(): string | null {
    const desktop = this.form.get('imageUrlDesktop')?.value;
    const mobile = this.form.get('imageUrlMobile')?.value;
    return this.device === 'mobile' ? (mobile || desktop || null) : (desktop || null);
  }

  submit(): void {
    if (this.form.invalid) return;
    this.saving = true;
    const req: CreateBannerRequest = this.form.value;
    const done = {
      next: () => { this.saving = false; this.notification.success(this.isEdit ? 'Banner updated' : 'Banner created'); this.back(); },
      error: () => { this.saving = false; this.notification.error('Failed to save banner'); }
    };
    if (this.isEdit) {
      this.bannerService.update(this.editId, req).subscribe(done);
    } else {
      this.bannerService.create(req).subscribe(done);
    }
  }

  back(): void { this.router.navigate(['/admin/banners']); }

  private toLocalInput(value?: string): string | null {
    return value ? value.substring(0, 16) : null;
  }
}
