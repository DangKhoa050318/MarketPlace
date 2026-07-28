import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ProductReview, ReviewPayload } from '../../../core/models/review.model';

@Component({
  selector: 'app-review-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule
  ],
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" class="review-form">
      <h3>{{ review ? 'Edit your review' : 'Write a review' }}</h3>
      <div class="star-selector" role="radiogroup" aria-label="Rating">
        @for (star of stars; track star) {
          <button type="button" class="star-button" (click)="setRating(star)"
                  [attr.aria-checked]="form.controls.rating.value === star"
                  [attr.aria-label]="star + ' stars'">
            <mat-icon>{{ star <= (form.controls.rating.value || 0) ? 'star' : 'star_border' }}</mat-icon>
          </button>
        }
      </div>
      @if (form.controls.rating.touched && form.controls.rating.invalid) {
        <p class="error">Choose a rating from 1 to 5.</p>
      }
      <mat-form-field appearance="outline">
        <mat-label>Title</mat-label>
        <input matInput formControlName="title" maxlength="100">
        <mat-hint align="end">{{ form.controls.title.value.length }}/100</mat-hint>
        <mat-error>Title must contain 3–100 characters.</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Your review</mat-label>
        <textarea matInput formControlName="content" rows="6" maxlength="1000"></textarea>
        <mat-hint align="end">{{ form.controls.content.value.length }}/1000</mat-hint>
        <mat-error>Review must contain 10–1000 characters.</mat-error>
      </mat-form-field>
      @if (error) { <p class="error" role="alert">{{ error }}</p> }
      <div class="actions">
        @if (review) { <button mat-button type="button" (click)="cancel.emit()">Cancel</button> }
        <button mat-raised-button color="primary" type="submit" [disabled]="saving">
          {{ saving ? 'Saving…' : (review ? 'Update review' : 'Publish review') }}
        </button>
      </div>
    </form>
  `,
  styles: [`
    .review-form { display:flex; flex-direction:column; gap:12px; padding:20px; border:1px solid #e2e8f0; border-radius:14px; }
    h3 { margin:0; }
    .star-selector { display:flex; }
    .star-button { border:0; background:transparent; color:#f59e0b; cursor:pointer; padding:2px; }
    .star-button mat-icon { font-size:30px; width:30px; height:30px; }
    .actions { display:flex; justify-content:flex-end; gap:8px; }
    .error { color:#dc2626; margin:0; font-size:.85rem; }
  `]
})
export class ReviewFormComponent implements OnChanges {
  @Input() review?: ProductReview;
  @Input() saving = false;
  @Input() error = '';
  @Output() save = new EventEmitter<ReviewPayload>();
  @Output() cancel = new EventEmitter<void>();
  readonly stars = [1, 2, 3, 4, 5];
  readonly form = this.fb.nonNullable.group({
    rating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    content: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]
  });

  constructor(private fb: FormBuilder) {}

  ngOnChanges(): void {
    if (this.review) {
      this.form.reset({
        rating: this.review.rating,
        title: this.review.title,
        content: this.review.content
      });
    }
  }

  setRating(rating: number): void {
    this.form.controls.rating.setValue(rating);
    this.form.controls.rating.markAsTouched();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.save.emit({
      rating: value.rating,
      title: value.title.trim(),
      content: value.content.trim()
    });
  }
}
