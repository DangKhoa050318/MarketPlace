import { expect, test } from '@playwright/test';

test('purchased customer creates and edits a review and sees refreshed product data', async ({ page }) => {
  let review: Record<string, unknown> | undefined;

  const envelope = (data: unknown, message = '') => ({
    success: true,
    message,
    data,
    timestamp: '2026-07-28T10:00:00'
  });

  await page.addInitScript(() => {
    localStorage.setItem('access_token', 'e2e-token');
    localStorage.setItem('refresh_token', 'e2e-refresh');
    localStorage.setItem('username', 'verified-buyer');
    localStorage.setItem('role', 'CUSTOMER');
  });

  await page.route('**/api/v1/products/1', route => route.fulfill({
    json: envelope({
      id: 1,
      name: 'Mechanical Keyboard',
      slug: 'mechanical-keyboard',
      description: 'A keyboard purchased by the test customer.',
      categoryId: 1,
      categoryName: 'Accessories',
      active: true,
      variants: []
    })
  }));

  await page.route('**/api/v1/products/1/reviews/summary', route => route.fulfill({
    json: envelope({
      productId: 1,
      averageRating: review ? review.rating : 0,
      totalReviews: review ? 1 : 0,
      starCounts: {
        1: review?.rating === 1 ? 1 : 0,
        2: review?.rating === 2 ? 1 : 0,
        3: review?.rating === 3 ? 1 : 0,
        4: review?.rating === 4 ? 1 : 0,
        5: review?.rating === 5 ? 1 : 0
      }
    })
  }));

  await page.route('**/api/v1/products/1/reviews/eligibility', route => route.fulfill({
    json: envelope({
      eligible: !review,
      isVerifiedPurchase: true,
      orderItemId: 99,
      existingReview: review,
      message: review ? 'You have already reviewed this product.' : 'Eligible for a verified review.'
    })
  }));

  await page.route(/\/api\/v1\/products\/1\/reviews(\?.*)?$/, async route => {
    if (route.request().method() === 'POST') {
      const payload = route.request().postDataJSON();
      review = {
        id: 10,
        userId: 7,
        username: 'verified-buyer',
        productId: 1,
        ...payload,
        status: 'APPROVED',
        isVerifiedPurchase: true,
        isEdited: false,
        helpfulCount: 0,
        createdAt: '2026-07-28T10:00:00',
        updatedAt: '2026-07-28T10:00:00'
      };
      await route.fulfill({ status: 201, json: envelope(review) });
      return;
    }
    await route.fulfill({
      json: envelope({
        content: review ? [review] : [],
        page: 0,
        size: 5,
        totalElements: review ? 1 : 0,
        totalPages: review ? 1 : 0,
        last: true
      })
    });
  });

  await page.route('**/api/v1/reviews/10', async route => {
    const payload = route.request().postDataJSON();
    review = {
      ...review,
      ...payload,
      isEdited: true,
      updatedAt: '2026-07-28T11:00:00'
    };
    await route.fulfill({ json: envelope(review) });
  });

  await page.goto('/products/1');
  await expect(page.getByRole('heading', { name: 'Mechanical Keyboard' })).toBeVisible();

  await page.getByRole('button', { name: '5 stars' }).click();
  await page.getByLabel('Title').fill('Excellent keyboard');
  await page.getByLabel('Your review').fill('The switches feel excellent and responsive.');
  await page.getByRole('button', { name: 'Publish review' }).click();

  await expect(page.getByText('Verified Purchase')).toBeVisible();
  await expect(page.getByText('Excellent keyboard')).toBeVisible();
  await expect(page.getByText('1 review')).toBeVisible();

  await page.getByRole('button', { name: 'Edit review' }).click();
  await page.getByRole('button', { name: '4 stars' }).click();
  await page.getByLabel('Title').fill('Great after two weeks');
  await page.getByRole('button', { name: 'Update review' }).click();

  await expect(page.getByText('Great after two weeks')).toBeVisible();
  await expect(page.getByText('Edited')).toBeVisible();
  await expect(page.getByText('4.0')).toBeVisible();
});
