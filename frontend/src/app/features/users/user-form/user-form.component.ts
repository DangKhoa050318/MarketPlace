import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { UserService } from '../user.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="user-form-container">
      <!-- Form Header -->
      <div class="form-header">
        <div>
          <span class="eyebrow">User Administration</span>
          <h1 class="page-title text-gradient-cyan">
            <mat-icon class="title-icon">{{ isEdit ? 'manage_accounts' : 'person_add' }}</mat-icon>
            {{ isEdit ? 'Edit User Account' : 'Create New User' }}
          </h1>
          <p class="page-subtitle">Configure account credentials, profile details and system permissions</p>
        </div>
        <a mat-button class="back-link" routerLink="/admin/users">
          <mat-icon>arrow_back</mat-icon> Back to Users
        </a>
      </div>

      <!-- Glass Card -->
      <div class="form-card glass-panel">
        <div *ngIf="initialLoading" class="loading-container">
          <mat-spinner diameter="40"></mat-spinner>
        </div>

        <form *ngIf="!initialLoading" [formGroup]="form" (ngSubmit)="onSubmit()" class="user-form">
          <!-- Username & Email Row -->
          <div class="form-row">
            <mat-form-field appearance="outline" class="half-width">
              <mat-label>Username Handle</mat-label>
              <input matInput formControlName="username" placeholder="e.g. john_doe">
              <mat-error *ngIf="form.get('username')?.hasError('required')">Username is required</mat-error>
              <mat-error *ngIf="form.get('username')?.hasError('minlength')">Must be at least 3 characters</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="half-width">
              <mat-label>Email Address</mat-label>
              <input matInput type="email" formControlName="email" placeholder="e.g. john@example.com">
              <mat-error *ngIf="form.get('email')?.hasError('required')">Email is required</mat-error>
              <mat-error *ngIf="form.get('email')?.hasError('email')">Please enter a valid email</mat-error>
            </mat-form-field>
          </div>

          <!-- Full Name -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Full Name</mat-label>
            <input matInput formControlName="fullName" placeholder="e.g. John Doe">
            <mat-error *ngIf="form.get('fullName')?.hasError('required')">Full name is required</mat-error>
          </mat-form-field>

          <!-- Password (Only for Create Mode) -->
          <mat-form-field *ngIf="!isEdit" appearance="outline" class="full-width">
            <mat-label>Account Password</mat-label>
            <input matInput type="password" formControlName="password" placeholder="Enter secure password...">
            <mat-error *ngIf="form.get('password')?.hasError('required')">Password is required</mat-error>
            <mat-error *ngIf="form.get('password')?.hasError('minlength')">Password must be at least 6 characters</mat-error>
          </mat-form-field>

          <!-- Active Toggle (Edit Mode) -->
          <div *ngIf="isEdit" class="toggle-container">
            <mat-slide-toggle formControlName="active" color="primary">
              Account Active Status
            </mat-slide-toggle>
          </div>

          <!-- Actions -->
          <div class="form-actions">
            <button mat-raised-button class="btn-glowing" type="submit" [disabled]="form.invalid || loading">
              <mat-icon>{{ isEdit ? 'save' : 'person_add' }}</mat-icon>
              {{ loading ? 'Saving...' : (isEdit ? 'Update User' : 'Create Account') }}
            </button>
            <button mat-button type="button" (click)="onCancel()" [disabled]="loading">
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .user-form-container {
      max-width: 700px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .form-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
    }

    .eyebrow {
      color: #38bdf8;
      font-size: .7rem;
      font-weight: 800;
      letter-spacing: .13em;
      text-transform: uppercase;
    }

    .page-title {
      display: flex;
      align-items: center;
      gap: 10px;
      margin: 4px 0 0 0;
      font-size: 1.8rem;
      font-weight: 800;
    }

    .title-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
      color: #38bdf8;
    }

    .page-subtitle {
      margin: 4px 0 0 0;
      color: var(--text-muted);
      font-size: 0.88rem;
    }

    .back-link {
      color: var(--text-secondary);
    }

    .form-card {
      padding: 24px;
    }

    .user-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .full-width {
      width: 100%;
    }

    .form-row {
      display: flex;
      gap: 16px;
    }

    .half-width {
      flex: 1;
    }

    .toggle-container {
      margin: 8px 0 16px 0;
    }

    .loading-container {
      display: flex;
      justify-content: center;
      padding: 48px;
    }

    .form-actions {
      display: flex;
      gap: 12px;
      margin-top: 16px;
    }
  `]
})
export class UserFormComponent implements OnInit {
  isEdit = false;
  loading = false;
  initialLoading = false;
  userId: number | null = null;

  form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    fullName: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(6)]],
    active: [true]
  });

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private route: ActivatedRoute,
    private router: Router,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.userId = +id;
      this.form.get('password')?.clearValidators();
      this.form.get('username')?.disable();
      this.loadUser();
    }
  }

  loadUser(): void {
    this.initialLoading = true;
    this.userService.getById(this.userId!).subscribe({
      next: (res) => {
        this.initialLoading = false;
        if (res.success && res.data) {
          this.form.patchValue({
            username: res.data.username,
            email: res.data.email,
            fullName: res.data.fullName,
            active: res.data.active
          });
        }
      },
      error: () => {
        this.initialLoading = false;
        this.notification.error('Failed to load user');
        this.router.navigate(['/admin/users']);
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;

    if (this.isEdit) {
      const { email, fullName, active } = this.form.getRawValue();
      this.userService.update(this.userId!, { email: email!, fullName: fullName!, active: active! }).subscribe({
        next: () => {
          this.notification.success('User updated successfully');
          this.router.navigate(['/admin/users']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Update failed');
        }
      });
    } else {
      const { username, email, password, fullName } = this.form.getRawValue();
      this.userService.create({ username: username!, email: email!, password: password!, fullName: fullName! }).subscribe({
        next: () => {
          this.notification.success('User created successfully');
          this.router.navigate(['/admin/users']);
        },
        error: (err) => {
          this.loading = false;
          this.notification.error(err.error?.message || 'Create failed');
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/admin/users']);
  }
}
