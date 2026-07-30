import { expect, test } from '@playwright/test';

const envelope = (data: unknown, message = '') => ({
  success: true,
  message,
  data,
  timestamp: '2026-07-30T10:00:00'
});

const sourceProduct = {
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

const variantsByProduct: Record<number, Array<Record<string, unknown>>> = {
  1: [{
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
  }],
  2: recommendedProduct.variants
};

test('recommendation click preserves attribution through product view and add-to-cart', async ({ page }) => {
  const analyticsEvents: Array<Record<string, unknown>> = [];
  const cartRequests: Array<Record<string, unknown>> = [];
  const recommendationRequestId = '11111111-1111-4111-8111-111111111111';

  await page.addInitScript(() => {
    localStorage.setItem('access_token', 'e2e-token');
    localStorage.setItem('refresh_token', 'e2e-refresh');
    localStorage.setItem('username', 'recommendation-customer');
    localStorage.setItem('role', 'CUSTOMER');
    localStorage.setItem('analytics_consent', 'true');
    localStorage.setItem('recently_viewed_session_id', 'recommendation-e2e-session');
  });

  await page.route('**/*.{png,jpg,jpeg,webp}', route => route.fulfill({
    status: 200,
    contentType: 'image/png',
    body: Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADElEQVR42mP8z8AARQAFAgH9z9n5AAAAAElFTkSuQmCC',
      'base64'
    )
  }));

  await page.route(/\/api\/v1\/products\/(1|2)$/, route => {
    const productId = Number(route.request().url().match(/\/products\/(\d+)$/)?.[1]);
    return route.fulfill({
      json: envelope(productId === 1 ? sourceProduct : recommendedProduct)
    });
  });

  await page.route(/\/api\/v1\/products\/(1|2)\/variants$/, route => {
    const productId = Number(route.request().url().match(/\/products\/(\d+)\/variants$/)?.[1]);
    return route.fulfill({ json: envelope(variantsByProduct[productId] ?? []) });
  });

  await page.route(/\/api\/v1\/products\/(1|2)\/reviews\/summary$/, route => {
    const productId = Number(route.request().url().match(/\/products\/(\d+)\/reviews/)?.[1]);
    return route.fulfill({
      json: envelope({
        productId,
        averageRating: 0,
        totalReviews: 0,
        starCounts: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 }
      })
    });
  });

  await page.route(/\/api\/v1\/products\/(1|2)\/reviews\/eligibility$/, route =>
    route.fulfill({
      json: envelope({
        eligible: false,
        isVerifiedPurchase: false,
        message: 'No verified purchase.'
      })
    })
  );

  await page.route(/\/api\/v1\/products\/(1|2)\/reviews(\?.*)?$/, route =>
    route.fulfill({
      json: envelope({
        content: [],
        page: 0,
        size: 5,
        totalElements: 0,
        totalPages: 0,
        last: true
      })
    })
  );

  await page.route(/\/api\/v1\/products\/(1|2)\/questions(\?.*)?$/, route =>
    route.fulfill({
      json: envelope({
        content: [],
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
        last: true
      })
    })
  );

  await page.route(/\/api\/v1\/wishlist\/(1|2)\/status$/, route => {
    const productId = Number(route.request().url().match(/\/wishlist\/(\d+)\/status$/)?.[1]);
    return route.fulfill({
      json: envelope({ productId, wishlisted: false })
    });
  });

  await page.route('**/api/v1/recently-viewed', route =>
    route.fulfill({ status: 201, json: envelope({ recorded: true }) })
  );

  await page.route('**/api/v1/recommendations?*', route => {
    const url = new URL(route.request().url());
    const placement = url.searchParams.get('placement');
    const productId = Number(url.searchParams.get('productId'));
    const isSourceSimilar =
      productId === 1 && placement === 'PRODUCT_DETAIL_SIMILAR';

    return route.fulfill({
      json: envelope({
        requestId: isSourceSimilar
          ? recommendationRequestId
          : '22222222-2222-4222-8222-222222222222',
        placement,
        strategy: placement === 'PRODUCT_DETAIL_SIMILAR' ? 'SIMILAR' : 'CO_VIEWED',
        generatedAt: '2026-07-30T10:00:00Z',
        items: isSourceSimilar
          ? [{
              position: 0,
              product: recommendedProduct,
              score: 0.92,
              reason: 'same-category-brand'
            }]
          : []
      })
    });
  });

  await page.route('**/api/v1/analytics/events', async route => {
    const payload = route.request().postDataJSON() as Record<string, unknown>;
    analyticsEvents.push(payload);
    await route.fulfill({
      status: 202,
      json: envelope({
        eventId: payload['eventId'],
        status: 'ACCEPTED',
        receivedAt: '2026-07-30T10:00:00Z'
      })
    });
  });

  await page.route('**/api/v1/cart', route =>
    route.fulfill({
      json: envelope({
        userId: 7,
        items: [],
        totalAmount: 0,
        totalItems: 0
      })
    })
  );

  await page.route('**/api/v1/cart/items', async route => {
    const payload = route.request().postDataJSON() as Record<string, unknown>;
    cartRequests.push(payload);
    await route.fulfill({
      status: 200,
      json: envelope({
        userId: 7,
        items: [{
          variantId: 201,
          sku: 'ASUS-G14-OLED',
          productName: recommendedProduct.name,
          variantName: 'OLED / 32GB',
          unitPrice: 1799,
          quantity: 1,
          subtotal: 1799
        }],
        totalAmount: 1799,
        totalItems: 1
      })
    });
  });

  await page.goto('/products/1');

  await expect(page.getByRole('heading', { name: sourceProduct.name })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Sản phẩm tương tự' })).toBeVisible();

  const recommendationLink = page.getByRole('link', {
    name: `Xem ${recommendedProduct.name}`
  });
  await recommendationLink.scrollIntoViewIfNeeded();
  await expect(recommendationLink).toBeVisible();

  await expect.poll(() =>
    analyticsEvents.some(event =>
      event['type'] === 'RECOMMENDATION_IMPRESSION'
      && event['productId'] === 2
      && event['placement'] === 'PRODUCT_DETAIL_SIMILAR'
      && event['recommendationRequestId'] === recommendationRequestId
      && event['strategy'] === 'SIMILAR'
      && event['position'] === 0
    )
  ).toBe(true);

  await recommendationLink.click();
  await expect(page).toHaveURL(/\/products\/2$/);
  await expect(page.getByRole('heading', { name: recommendedProduct.name })).toBeVisible();

  await expect.poll(() =>
    analyticsEvents.some(event =>
      event['type'] === 'PRODUCT_VIEW'
      && event['productId'] === 2
      && event['source'] === 'RECOMMENDATION'
      && event['placement'] === 'PRODUCT_DETAIL_SIMILAR'
      && event['recommendationRequestId'] === recommendationRequestId
      && event['strategy'] === 'SIMILAR'
      && event['position'] === 0
    )
  ).toBe(true);

  await Promise.all([
    page.waitForResponse(response =>
      response.url().includes('/api/v1/cart/items')
      && response.request().method() === 'POST'
    ),
    page.getByRole('button', { name: /Thêm vào giỏ/i }).click()
  ]);

  await expect.poll(() =>
    analyticsEvents.some(event =>
      event['type'] === 'ADD_TO_CART'
      && event['productId'] === 2
      && event['variantId'] === 201
      && event['quantity'] === 1
      && event['source'] === 'RECOMMENDATION'
      && event['placement'] === 'PRODUCT_DETAIL_SIMILAR'
      && event['recommendationRequestId'] === recommendationRequestId
      && event['strategy'] === 'SIMILAR'
      && event['position'] === 0
    )
  ).toBe(true);

  expect(cartRequests).toContainEqual({
    variantId: 201,
    quantity: 1
  });

  const journeyTypes = analyticsEvents
    .filter(event =>
      event['recommendationRequestId'] === recommendationRequestId
      && event['productId'] === 2
    )
    .map(event => event['type']);

  expect(journeyTypes).toEqual([
    'RECOMMENDATION_IMPRESSION',
    'RECOMMENDATION_CLICK',
    'PRODUCT_VIEW',
    'ADD_TO_CART'
  ]);
});
