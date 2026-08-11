import { expect, test } from '@playwright/test';

test('customer journey records wishlist cart checkout order and dashboard funnel', async ({ page }) => {
  const trackedEvents: string[] = [];
  const envelope = (data: unknown, message = '') => ({
    success: true,
    message,
    data,
    timestamp: '2026-07-30T10:00:00'
  });
  const product = {
    id: 1,
    slug: 'mechanical-keyboard',
    name: 'Mechanical Keyboard',
    description: 'A keyboard for journey analytics.',
    categoryId: 1,
    categoryName: 'Accessories',
    unit: 'PCS',
    imageUrl: '',
    active: true,
    price: 120,
    variants: [{ id: 11, productId: 1, sku: 'KB-001', variantName: 'Black', price: 120, active: true }]
  };
  const cart = {
    userId: 7,
    items: [{
      variantId: 11,
      sku: 'KB-001',
      productName: 'Mechanical Keyboard',
      variantName: 'Black',
      unitPrice: 120,
      quantity: 1,
      subtotal: 120
    }],
    totalAmount: 120,
    totalItems: 1
  };

  await page.addInitScript(() => {
    localStorage.setItem('marketplace_session_present', 'true');
    localStorage.setItem('analytics_consent', 'true');
    localStorage.setItem('recently_viewed_session_id', 'journey-session');
  });

  await page.route('**/api/v1/auth/refresh', route => route.fulfill({
    json: envelope({ accessToken: 'e2e-token', username: 'customer', role: 'ADMIN' })
  }));

  await page.route('**/api/v1/analytics/events', async route => {
    const payload = route.request().postDataJSON();
    trackedEvents.push(payload.type);
    await route.fulfill({ status: 202, json: envelope({ status: 'ACCEPTED', eventId: payload.eventId }) });
  });
  await page.route('**/api/v1/products/1', route => route.fulfill({ json: envelope(product) }));
  await page.route('**/api/v1/products/1/variants', route => route.fulfill({
    json: envelope(product.variants)
  }));
  await page.route('**/api/v1/products/1/reviews**', route => route.fulfill({
    json: envelope({ content: [], page: 0, size: 5, totalElements: 0, totalPages: 0, last: true })
  }));
  await page.route('**/api/v1/products/1/reviews/summary', route => route.fulfill({
    json: envelope({ productId: 1, averageRating: 0, totalReviews: 0, starCounts: {} })
  }));
  await page.route('**/api/v1/products/1/reviews/eligibility', route => route.fulfill({
    json: envelope({ eligible: false, isVerifiedPurchase: false, message: 'No purchase yet' })
  }));
  await page.route('**/api/v1/products/1/questions**', route => route.fulfill({
    json: envelope({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true })
  }));
  await page.route('**/api/v1/recommendations?*', route => {
    const placement = new URL(route.request().url()).searchParams.get('placement');
    return route.fulfill({
      json: envelope({
        requestId: '33333333-3333-4333-8333-333333333333',
        placement,
        strategy: placement === 'PRODUCT_DETAIL_SIMILAR' ? 'SIMILAR' : 'CO_VIEWED',
        generatedAt: '2026-07-30T10:00:00Z',
        items: []
      })
    });
  });
  await page.route('**/api/v1/recently-viewed', route => route.fulfill({ status: 201, json: envelope({ id: 1, product }) }));
  await page.route('**/api/v1/wishlist/1/status', route => route.fulfill({ json: envelope({ productId: 1, wishlisted: false }) }));
  await page.route('**/api/v1/wishlist/1', async route => {
    await route.fulfill({ status: route.request().method() === 'POST' ? 201 : 204, json: envelope({ id: 1, product }) });
  });
  await page.route('**/api/v1/cart/items', route => route.fulfill({ json: envelope(cart) }));
  await page.route('**/api/v1/cart', route => route.fulfill({ json: envelope(cart) }));
  await page.route('**/api/v1/orders', route => route.fulfill({ status: 201, json: envelope({ id: 9001, totalAmount: 120 }) }));
  await page.route('**/api/v1/admin/dashboard/stats', route => route.fulfill({
    json: envelope({ totalOrders: 1, totalRevenue: 120, pendingOrders: 0, completedOrders: 1, totalProducts: 1, totalCustomers: 1 })
  }));
  await page.route('**/api/v1/admin/analytics/funnel**', route => route.fulfill({
    json: envelope({
      from: '2026-07-01T00:00:00Z',
      to: '2026-08-01T00:00:00Z',
      steps: [
        { step: 'PRODUCT_VIEW', count: 1, conversionRate: 1, dropOffRate: 0 },
        { step: 'ADD_TO_CART', count: 1, conversionRate: 1, dropOffRate: 0 },
        { step: 'BEGIN_CHECKOUT', count: 1, conversionRate: 1, dropOffRate: 0 },
        { step: 'ORDER_CREATED', count: 1, conversionRate: 1, dropOffRate: 0 }
      ]
    })
  }));

  await page.goto('/products/1');
  await expect(page.getByRole('heading', { name: 'Mechanical Keyboard' })).toBeVisible();
  await page.getByRole('button', { name: /Add to wishlist/i }).click();
  await Promise.all([
    page.waitForResponse(response => response.url().includes('/api/v1/cart/items') && response.request().method() === 'POST'),
    page.locator('button.add-cart-btn').click()
  ]);
  await expect.poll(() => trackedEvents).toContain('ADD_TO_CART');
  await page.goto('/cart');
  await page.getByRole('button', { name: /checkout/i }).click();
  await page.getByRole('textbox', { name: /shipping address/i }).fill('123 Journey Street');
  await Promise.all([
    page.waitForResponse(response => response.url().includes('/api/v1/orders') && response.request().method() === 'POST'),
    page.getByRole('button', { name: /place order|confirm|confirm order/i }).click()
  ]);
  await expect.poll(() => trackedEvents).toContain('ORDER_CREATED');
  await page.goto('/admin/dashboard');

  await expect(page.getByText('Storefront Funnel')).toBeVisible();
  expect(trackedEvents).toEqual(expect.arrayContaining([
    'PRODUCT_VIEW',
    'ADD_TO_WISHLIST',
    'ADD_TO_CART',
    'BEGIN_CHECKOUT',
    'ORDER_CREATED'
  ]));
});
