-- Stable demo analytics for FEATURE-STP-04 dashboards. Event IDs are deterministic
-- so rerunning a restored development database remains idempotent.
WITH demo_products AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
      FROM products
     WHERE active = TRUE
     ORDER BY id
     LIMIT 10
), journeys AS (
    SELECT i,
           'analytics-demo-session-' || LPAD(i::text, 2, '0') AS session_id,
           (SELECT id FROM demo_products WHERE rn = ((i - 1) % 10) + 1) AS product_id,
           NOW() - INTERVAL '20 days' + (i * INTERVAL '8 hours') AS viewed_at
      FROM generate_series(1, 30) AS i
), journey_events AS (
    SELECT i, session_id, product_id, 'PRODUCT_VIEW'::varchar AS event_type, viewed_at AS occurred_at
      FROM journeys
    UNION ALL
    SELECT i, session_id, product_id, 'ADD_TO_WISHLIST', viewed_at + INTERVAL '2 minutes'
      FROM journeys WHERE i <= 12
    UNION ALL
    SELECT i, session_id, product_id, 'ADD_TO_CART', viewed_at + INTERVAL '5 minutes'
      FROM journeys WHERE i <= 18
    UNION ALL
    SELECT i, session_id, product_id, 'BEGIN_CHECKOUT', viewed_at + INTERVAL '9 minutes'
      FROM journeys WHERE i <= 10
    UNION ALL
    SELECT i, session_id, product_id, 'ORDER_CREATED', viewed_at + INTERVAL '14 minutes'
      FROM journeys WHERE i <= 6
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id, source,
    occurred_at, received_at, properties
)
SELECT md5('journey-' || event_type || '-' || i)::uuid,
       1,
       event_type,
       session_id,
       product_id,
       'CATALOG',
       occurred_at,
       occurred_at + INTERVAL '1 second',
       jsonb_build_object(
           'campaign', CASE WHEN i <= 18 THEN 'Summer Launch' ELSE 'Organic' END,
           'deviceType', CASE WHEN i % 3 = 0 THEN 'TABLET' WHEN i % 2 = 0 THEN 'MOBILE' ELSE 'DESKTOP' END
       )
  FROM journey_events
ON CONFLICT (event_id) DO NOTHING;

WITH demo_products AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
      FROM products
     WHERE active = TRUE
     ORDER BY id
     LIMIT 10
), campaign_config AS (
    SELECT 'Summer Launch'::varchar AS campaign, 'HOME_TRENDING'::varchar AS placement,
           'TRENDING'::varchar AS strategy, 100 AS impressions, 32 AS clicks, 15 AS carts, 6 AS orders
    UNION ALL
    SELECT 'Back to School', 'CART_CROSS_SELL', 'CO_PURCHASED', 70, 14, 8, 3
), impressions AS (
    SELECT c.*, i,
           md5('recommendation-request-' || c.campaign || '-' || i)::uuid AS request_id,
           (SELECT id FROM demo_products WHERE rn = ((i - 1) % 10) + 1) AS product_id,
           NOW() - INTERVAL '13 days' + (i * INTERVAL '2 hours') AS event_at
      FROM campaign_config c
      CROSS JOIN LATERAL generate_series(1, c.impressions) AS i
), recommendation_events AS (
    SELECT campaign, placement, strategy, i, request_id, product_id,
           'RECOMMENDATION_IMPRESSION'::varchar AS event_type, event_at AS occurred_at, NULL::bigint AS order_id
      FROM impressions
    UNION ALL
    SELECT campaign, placement, strategy, i, request_id, product_id,
           'RECOMMENDATION_CLICK', event_at + INTERVAL '30 seconds', NULL
      FROM impressions WHERE i <= clicks
    UNION ALL
    SELECT campaign, placement, strategy, i, request_id, product_id,
           'ADD_TO_CART', event_at + INTERVAL '3 minutes', NULL
      FROM impressions WHERE i <= carts
    UNION ALL
    SELECT campaign, placement, strategy, i, request_id, product_id,
           'PURCHASE', event_at + INTERVAL '12 minutes',
           (SELECT id FROM orders ORDER BY id LIMIT 1)
      FROM impressions WHERE i <= orders
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id, source, placement,
    recommendation_request_id, strategy, order_id, occurred_at, received_at, properties
)
SELECT md5('campaign-' || campaign || '-' || event_type || '-' || i)::uuid,
       1,
       event_type,
       'campaign-demo-' || replace(lower(campaign), ' ', '-') || '-' || i,
       product_id,
       'RECOMMENDATION',
       placement,
       request_id,
       strategy,
       order_id,
       occurred_at,
       occurred_at + INTERVAL '1 second',
       jsonb_build_object('campaign', campaign, 'deviceType', CASE WHEN i % 2 = 0 THEN 'MOBILE' ELSE 'DESKTOP' END)
  FROM recommendation_events
ON CONFLICT (event_id) DO NOTHING;
