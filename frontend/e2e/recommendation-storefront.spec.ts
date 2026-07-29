import { expect, test } from '@playwright/test';

const envelope = (data: unknown, message = '') => ({
  success: true,
  message,
  data,
  timestamp: '2026-07-29T10:00:00'
});

const product = {
  id: 1,
  name: 'Dell XPS 14',
  slug: 'dell-xps-14',
  description: 'Premium laptop for recommendation testing.',
  categoryId: 7,
  categoryName: 'Computers',
  imageUrl: 'https://example.test/dell-xps.jpg',
  price: 1499,
  active: true,
  brand: 'Dell',
  attributes: {},
  variants: []
};

const variant = {
  id: 101,
  productId: 1,
  sku: 'DELL-XPS-14-OLED',
  variantName: 'OLED / 32GB',
  price: 1499,
  active: true,
  stockQuantity: 8,
  color: 'Graphite',
  size: '14-inch',
  imageUrl: 'https://example.test/dell-xps-variant.jpg'
};

const recommendedProduct = {
  id: 2,
  name: 'ASUS ROG Zephyrus G14',
  slug: 'asus-rog-zephyrus-g14',
  description: 'Portable gaming laptop.',
  categoryId: 7,
  categoryName: 'Computers',
  imageUrl: 'https://example.test/asus-g14.jpg',
  price: 1799,
  active: true,
  brand: 'ASUS',
  attributes: {},
  variants: [{
    id: 201,
    productId: 2,
    sku: 'ASUS-G14-OLED',
    variantName: 'OLED / 32GB',
    price: 1799,
    active: true,
    stockQuantity: 5,
    color: 'White',
    size: '14-inch',
    imageUrl: 'https://example.test/asus-g14-variant.jpg'
  }]
};

test.describe('recommendation storefront tracking', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('recently_viewed_session_id', 'e2e-session');
    });

    await page.route('**/*.{png,jpg,jpeg,webp}', route => route.fulfill({
      status: 200,
      contentType: 'image/png',
      body: Buffer.from(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADElEQVR42mP8z8AARQAFAgH9z9n5AAAAAElFTkSuQmCC',
        'base64'
      )
    }));

    await page.route('**/api/v1/products/1', route => route.fulfill({
      json: envelope(product)
    }));
    await page.route('**/api/v1/products/1/variants', route => route.fulfill({
      json: envelope([variant])
    }));
    await page.route('**/api/v1/products/1/reviews/summary', route => route.fulfill({
      json: envelope({
        productId: 1,
        averageRating: 0,
        totalReviews: 0,
        starCounts: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 }
      })
    }));
    await page.route('**/api/v1/products/1/reviews/eligibility', route => route.fulfill({
      json: envelope({ eligible: false, isVerifiedPurchase: false, message: 'Sign in to review.' })
    }));
    await page.route(/\/api\/v1\/products\/1\/reviews(\?.*)?$/, route => route.fulfill({
      json: envelope({
        content: [],
        page: 0,
        size: 5,
        totalElements: 0,
        totalPages: 0,
        last: true
      })
    }));
    await page.route('**/api/v1/wishlist/1/status', route => route.fulfill({
      json: envelope({ productId: 1, wishlisted: false })
    }));
    await page.route('**/api/v1/recently-viewed', route => route.fulfill({
      json: envelope({ productId: 1 })
    }));
  });

  test('renders product-detail recommendations and correlates impression and click analytics', async ({ page }) => {
    const analyticsEvents: Array<Record<string, unknown>> = [];

    await page.route('**/api/v1/recommendations?*', route => {
      const url = new URL(route.request().url());
      const placement = url.searchParams.get('placement');
      const items = placement === 'PRODUCT_DETAIL_SIMILAR'
        ? [{
            position: 0,
            product: recommendedProduct,
            score: 0.92,
            reason: 'same-category-brand'
          }]
        : [];

      return route.fulfill({
        json: envelope({
          requestId: '11111111-1111-4111-8111-111111111111',
          placement,
          strategy: placement === 'PRODUCT_DETAIL_SIMILAR' ? 'SIMILAR' : 'CO_VIEWED',
          generatedAt: '2026-07-29T10:00:00Z',
          items
        })
      });
    });

    await page.route('**/api/v1/analytics/events', async route => {
      analyticsEvents.push(route.request().postDataJSON());
      await route.fulfill({ json: envelope({ status: 'ACCEPTED' }) });
    });

    await page.goto('/products/1');

    await expect(page.getByRole('heading', { name: 'Dell XPS 14' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Sáº£n pháº©m tÆ°Æ¡ng tá»±' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Xem ASUS ROG Zephyrus G14' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'KhÃ¡ch hÃ ng cÅ©ng thÆ°á»ng xem' })).toBeHidden();

    await expect.poll(() =>
      analyticsEvents.some(event =>
        event['type'] === 'RECOMMENDATION_IMPRESSION'
        && event['placement'] === 'PRODUCT_DETAIL_SIMILAR'
        && event['recommendationRequestId'] === '11111111-1111-4111-8111-111111111111'
        && event['strategy'] === 'SIMILAR'
        && event['position'] === 0
      )
    ).toBe(true);

    await page.getByRole('link', { name: 'Xem ASUS ROG Zephyrus G14' }).click();

    expect(analyticsEvents).toContainEqual(expect.objectContaining({
      type: 'RECOMMENDATION_CLICK',
      productId: 2,
      source: 'RECOMMENDATION',
      placement: 'PRODUCT_DETAIL_SIMILAR',
      recommendationRequestId: '11111111-1111-4111-8111-111111111111',
      strategy: 'SIMILAR',
      position: 0
    }));
    await expect(page).toHaveURL(/\/products\/2$/);
  });
});
