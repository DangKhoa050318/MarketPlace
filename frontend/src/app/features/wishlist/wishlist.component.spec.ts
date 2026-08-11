import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { CartService } from '../../core/services/cart.service';
import { NotificationService } from '../../core/services/notification.service';
import { WishlistService } from '../../core/services/wishlist.service';
import { WishlistComponent } from './wishlist.component';

describe('WishlistComponent', () => {
  let fixture: ComponentFixture<WishlistComponent>;
  let component: WishlistComponent;
  let wishlistService: jasmine.SpyObj<WishlistService>;
  let cartService: jasmine.SpyObj<CartService>;
  let notification: jasmine.SpyObj<NotificationService>;

  const item = {
    id: 100,
    createdAt: '2026-07-30T10:00:00',
    product: {
      id: 10,
      slug: 'phone',
      name: 'Phone',
      description: 'Desc',
      categoryId: 1,
      categoryName: 'Devices',
      unit: 'PCS',
      imageUrl: '',
      active: true,
      variants: [{ id: 20, productId: 10, sku: 'SKU-20', variantName: 'Default', price: 500, active: true }]
    }
  };

  beforeEach(async () => {
    wishlistService = jasmine.createSpyObj('WishlistService', ['list', 'remove']);
    cartService = jasmine.createSpyObj('CartService', ['addToCart']);
    notification = jasmine.createSpyObj('NotificationService', ['success', 'error']);
    wishlistService.list.and.returnValue(of({
      success: true,
      message: '',
      timestamp: '',
      data: { content: [item], page: 0, size: 12, totalElements: 1, totalPages: 1, last: true }
    }));

    await TestBed.configureTestingModule({
      imports: [WishlistComponent, RouterTestingModule, NoopAnimationsModule],
      providers: [
        { provide: WishlistService, useValue: wishlistService },
        { provide: CartService, useValue: cartService },
        { provide: NotificationService, useValue: notification }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WishlistComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads paged wishlist items', () => {
    expect(wishlistService.list).toHaveBeenCalledWith(0, 12);
    expect(component.items.length).toBe(1);
    expect(component.items[0].product.name).toBe('Phone');
  });

  it('moves a wishlist item to cart and removes it from wishlist', () => {
    cartService.addToCart.and.returnValue(of({ success: true, message: '', timestamp: '', data: {} as never }));
    wishlistService.remove.and.returnValue(of(undefined));

    component.moveToCart(item);

    expect(cartService.addToCart).toHaveBeenCalledWith(20, 1);
    expect(wishlistService.remove).toHaveBeenCalledWith(10);
  });

  it('shows an error when product has no variant to move', () => {
    component.moveToCart({ ...item, product: { ...item.product, variants: [] } });

    expect(cartService.addToCart).not.toHaveBeenCalled();
    expect(notification.error).toHaveBeenCalledWith('This product has no available variant');
  });

  it('reports remove failures', () => {
    wishlistService.remove.and.returnValue(throwError(() => new Error('nope')));

    component.remove(item);

    expect(notification.error).toHaveBeenCalledWith('Could not remove product');
  });
});
