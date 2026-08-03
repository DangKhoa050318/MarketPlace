-- Complete recommendation demo journeys with a product view before each impression.
WITH demo_products AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
      FROM products
     WHERE active = TRUE
     ORDER BY id
     LIMIT 10
), campaign_config AS (
    SELECT 'Summer Launch'::varchar AS campaign, 100 AS impressions
    UNION ALL
    SELECT 'Back to School', 70
), views AS (
    SELECT c.campaign,
           i,
           (SELECT id FROM demo_products WHERE rn = ((i - 1) % 10) + 1) AS product_id,
           NOW() - INTERVAL '13 days' + (i * INTERVAL '2 hours') - INTERVAL '1 minute' AS event_at
      FROM campaign_config c
      CROSS JOIN LATERAL generate_series(1, c.impressions) AS i
)
INSERT INTO analytics_events (
    event_id, schema_version, event_type, session_id, product_id, source,
    occurred_at, received_at, properties
)
SELECT md5('campaign-view-' || campaign || '-' || i)::uuid,
       1,
       'PRODUCT_VIEW',
       'campaign-demo-' || replace(lower(campaign), ' ', '-') || '-' || i,
       product_id,
       'RECOMMENDATION',
       event_at,
       event_at + INTERVAL '1 second',
       jsonb_build_object('campaign', campaign, 'deviceType', CASE WHEN i % 2 = 0 THEN 'MOBILE' ELSE 'DESKTOP' END)
  FROM views
ON CONFLICT (event_id) DO NOTHING;
