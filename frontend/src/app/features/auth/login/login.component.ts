import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule
  ],
  template: `
    <div class="auth-form">
      <header>
        <span class="section-label">Welcome back</span>
        <h2>Sign in to your account</h2>
        <p>Enter your credentials to continue to OrderFlow.</p>
      </header>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
        <div class="field-group">
          <label for="login-username">Username</label>
          <mat-form-field appearance="outline" subscriptSizing="fixed">
            <mat-icon matPrefix>person_outline</mat-icon>
            <input
              id="login-username"
              matInput
              formControlName="username"
              autocomplete="username"
              placeholder="Enter your username">
            @if (form.controls.username.touched && form.controls.username.hasError('required')) {
              <mat-error>Username is required</mat-error>
            }
          </mat-form-field>
        </div>

        <div class="field-group">
          <div class="field-heading">
            <label for="login-password">Password</label>
          </div>
          <mat-form-field appearance="outline" subscriptSizing="fixed">
            <mat-icon matPrefix>lock_outline</mat-icon>
            <input
              id="login-password"
              matInput
              [type]="hidePassword ? 'password' : 'text'"
              formControlName="password"
              autocomplete="current-password"
              placeholder="Enter your password">
            <button
              mat-icon-button
              matSuffix
              type="button"
              [attr.aria-label]="hidePassword ? 'Show password' : 'Hide password'"
              (click)="hidePassword = !hidePassword">
              <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
            </button>
            @if (form.controls.password.touched && form.controls.password.hasError('required')) {
              <mat-error>Password is required</mat-error>
            }
          </mat-form-field>
        </div>

        <button class="submit-button" type="submit" [disabled]="form.invalid || loading">
          @if (loading) {
            <span class="spinner" aria-hidden="true"></span>
            Signing in...
          } @else {
            Sign in
            <mat-icon>arrow_forward</mat-icon>
          }
        </button>
      </form>

      <div class="divider"><span>New to OrderFlow?</span></div>
      <p class="auth-switch">
        Create your account and start shopping.
        <a routerLink="/register">Create account <mat-icon>north_east</mat-icon></a>
      </p>
    </div>
  `,
  styleUrls: ['../auth-form.scss']
})
export class LoginComponent {
  loading = false;
  hidePassword = true;

  form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private notification: NotificationService
  ) {}

  onSubmit(): void {
    if (this.form.invalid || this.loading) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.notification.success('Welcome back to OrderFlow');
        this.router.navigate([this.authService.getRole() === 'ADMIN' ? '/admin/dashboard' : '/products']);
      },
      error: (err) => {
        this.loading = false;
        this.notification.error(err.error?.message || 'Unable to sign in. Please check your credentials.');
      }
    });
  }
}
