import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private snackBar: MatSnackBar) {}

  success(message: string): void {
    this.snackBar.open(message, 'DISMISS', {
      duration: 3500,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['custom-toast', 'success-toast']
    });
  }

  error(message: string): void {
    this.snackBar.open(message, 'DISMISS', {
      duration: 5000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['custom-toast', 'error-toast']
    });
  }

  info(message: string): void {
    this.snackBar.open(message, 'DISMISS', {
      duration: 3500,
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['custom-toast', 'info-toast']
    });
  }
}
