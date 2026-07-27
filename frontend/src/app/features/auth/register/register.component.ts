import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

interface RegisterFormValue {
  username: string;
  email: string;
  fullName: string;
  password: string;
}

@Component({
  selector: 'app-register',
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
    <div class="auth-form register-form">
      <header>
        <span class="section-label">Get started</span>
        <h2>Create your account</h2>
        <p>Join OrderFlow and keep every purchase beautifully organized.</p>
      </header>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
        <div class="two-column">
          <div class="field-group">
            <label for="register-full-name">Full name</label>
            <mat-form-field appearance="outline" subscriptSizing="fixed">
              <mat-icon matPrefix>badge</mat-icon>
              <input id="register-full-name" matInput formControlName="fullName"
                autocomplete="name" placeholder="Your full name">
              @if (form.controls.fullName.touched && form.controls.fullName.hasError('required')) {
                <mat-error>Full name is required</mat-error>
              }
            </mat-form-field>
          </div>

          <div class="field-group">
            <label for="register-username">Username</label>
            <mat-form-field appearance="outline" subscriptSizing="fixed">
              <mat-icon matPrefix>alternate_email</mat-icon>
              <input id="register-username" matInput formControlName="username"
                autocomplete="username" placeholder="Choose a username">
              @if (form.controls.username.touched && form.controls.username.hasError('required')) {
                <mat-error>Username is required</mat-error>
              } @else if (form.controls.username.touched && form.controls.username.hasError('minlength')) {
                <mat-error>Use at least 3 characters</mat-error>
              }
            </mat-form-field>
          </div>
        </div>

        <div class="field-group">
          <label for="register-email">Email address</label>
          <mat-form-field appearance="outline" subscriptSizing="fixed">
            <mat-icon matPrefix>mail_outline</mat-icon>
            <input id="register-email" matInput type="email" formControlName="email"
              autocomplete="email" placeholder="you@example.com">
            @if (form.controls.email.touched && form.controls.email.hasError('required')) {
              <mat-error>Email address is required</mat-error>
            } @else if (form.controls.email.touched && form.controls.email.hasError('email')) {
              <mat-error>Enter a valid email address</mat-error>
            }
          </mat-form-field>
        </div>

        <div class="field-group">
          <label for="register-password">Password</label>
          <mat-form-field appearance="outline" subscriptSizing="fixed">
            <mat-icon matPrefix>lock_outline</mat-icon>
            <input id="register-password" matInput [type]="hidePassword ? 'password' : 'text'"
              formControlName="password" autocomplete="new-password"
              placeholder="At least 6 characters">
            <button mat-icon-button matSuffix type="button"
              [attr.aria-label]="hidePassword ? 'Show password' : 'Hide password'"
              (click)="hidePassword = !hidePassword">
              <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
            </button>
            @if (form.controls.password.touched && form.controls.password.hasError('required')) {
              <mat-error>Password is required</mat-error>
            } @else if (form.controls.password.touched && form.controls.password.hasError('minlength')) {
              <mat-error>Use at least 6 characters</mat-error>
            }
          </mat-form-field>
          <div class="password-hint">
            <span [class.met]="form.controls.password.value.length >= 6"></span>
            Use 6 or more characters
          </div>
        </div>

        <button class="submit-button" type="submit" [disabled]="form.invalid || loading">
          @if (loading) {
            <span class="spinner" aria-hidden="true"></span>
            Creating account...
          } @else {
            Create account
            <mat-icon>arrow_forward</mat-icon>
          }
        </button>
      </form>

      <div class="divider"><span>Already have an account?</span></div>
      <p class="auth-switch">
        Return to your OrderFlow workspace.
        <a routerLink="/login">Sign in <mat-icon>north_east</mat-icon></a>
      </p>
    </div>
  `,
  styleUrls: ['../auth-form.scss']
})
export class RegisterComponent {
  loading = false;
  hidePassword = true;

  form = this.fb.nonNullable.group({
    fullName: ['', Validators.required],
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]]
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
    const request: RegisterFormValue = this.form.getRawValue();
    this.authService.register(request).subscribe({
      next: () => {
        this.notification.success('Your OrderFlow account is ready');
        this.router.navigate(['/products']);
      },
      error: (err) => {
        this.loading = false;
        this.notification.error(err.error?.message || 'Unable to create your account. Please try again.');
      }
    });
  }
}
