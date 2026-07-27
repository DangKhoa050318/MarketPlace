import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AddToCartRequest, Cart, UpdateCartItemRequest } from '../models/cart.model';

@Injectable({ providedIn: 'root' })
export class CartService {
  private apiUrl = `${environment.apiUrl}/cart`;
  private cartSubject = new BehaviorSubject<Cart | null>(null);

  public cart$ = this.cartSubject.asObservable();
  public cartCount$ = this.cart$.pipe(
    map(cart => cart ? cart.totalItems : 0)
  );

  constructor(private http: HttpClient) {}

  getCart(): Observable<ApiResponse<Cart>> {
    return this.http.get<ApiResponse<Cart>>(this.apiUrl).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.cartSubject.next(res.data);
        }
      })
    );
  }

  addToCart(productId: number, quantity = 1): Observable<ApiResponse<Cart>> {
    const payload: AddToCartRequest = { productId, quantity };
    return this.http.post<ApiResponse<Cart>>(`${this.apiUrl}/items`, payload).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.cartSubject.next(res.data);
        }
      })
    );
  }

  updateQuantity(productId: number, quantity: number): Observable<ApiResponse<Cart>> {
    const payload: UpdateCartItemRequest = { quantity };
    return this.http.put<ApiResponse<Cart>>(`${this.apiUrl}/items/${productId}`, payload).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.cartSubject.next(res.data);
        }
      })
    );
  }

  removeItem(productId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/items/${productId}`).pipe(
      tap(() => this.getCart().subscribe())
    );
  }

  clearCart(): Observable<void> {
    return this.http.delete<void>(this.apiUrl).pipe(
      tap(() => {
        this.cartSubject.next({
          userId: 0,
          items: [],
          totalAmount: 0,
          totalItems: 0
        });
      })
    );
  }
}
