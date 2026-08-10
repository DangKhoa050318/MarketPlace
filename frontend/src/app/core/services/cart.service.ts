import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AddToCartRequest, Cart, UpdateCartItemRequest } from '../models/cart.model';
import { PromotionService } from './promotion.service';

@Injectable({ providedIn: 'root' })
export class CartService {
  private apiUrl = `${environment.apiUrl}/cart`;
  private cartSubject = new BehaviorSubject<Cart | null>(null);

  public cart$ = this.cartSubject.asObservable();
  public cartCount$ = this.cart$.pipe(
    map(cart => cart ? cart.totalItems : 0)
  );

  constructor(
    private http: HttpClient,
    private promotionService: PromotionService
  ) {}

  getCart(): Observable<ApiResponse<Cart>> {
    return this.http.get<ApiResponse<Cart>>(this.apiUrl).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.cartSubject.next(res.data);
          this.revalidateCoupon();
        }
      })
    );
  }

  addToCart(variantId: number, quantity = 1): Observable<ApiResponse<Cart>> {
    const payload: AddToCartRequest = { variantId, quantity };
    return this.http.post<ApiResponse<Cart>>(`${this.apiUrl}/items`, payload).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.cartSubject.next(res.data);
          this.revalidateCoupon();
        }
      })
    );
  }

  updateQuantity(variantId: number, quantity: number): Observable<ApiResponse<Cart>> {
    const payload: UpdateCartItemRequest = { quantity };
    return this.http.put<ApiResponse<Cart>>(`${this.apiUrl}/items/${variantId}`, payload).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.cartSubject.next(res.data);
          this.revalidateCoupon();
        }
      })
    );
  }

  removeItem(variantId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/items/${variantId}`).pipe(
      tap(() => {
        this.getCart().subscribe();
      })
    );
  }

  clearCart(): Observable<void> {
    return this.http.delete<void>(this.apiUrl).pipe(
      tap(() => {
        this.promotionService.clearApplied();
        this.cartSubject.next({
          userId: 0,
          items: [],
          totalAmount: 0,
          totalItems: 0
        });
      })
    );
  }

  markCheckoutComplete(): void {
    this.promotionService.clearApplied();
    this.cartSubject.next({
      userId: 0,
      items: [],
      totalAmount: 0,
      totalItems: 0
    });
  }

  private revalidateCoupon(): void {
    this.promotionService.revalidateApplied().subscribe({ error: () => undefined });
  }
}
