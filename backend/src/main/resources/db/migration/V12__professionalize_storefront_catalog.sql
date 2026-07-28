-- Keep V11 immutable: it may already be present in developer databases.
-- This migration upgrades its stable demo SKUs into a realistic storefront catalog.
WITH catalog(sku, slug, name, description, category_code, image_url) AS (VALUES
    ('DEMO-SKU-01', 'dell-xps-14-9440', 'Dell XPS 14 9440', 'Premium OLED laptop with Intel Core Ultra, 32GB memory and a precision aluminum chassis.', 'COMP', 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-02', 'asus-rog-zephyrus-g14', 'ASUS ROG Zephyrus G14', 'Portable gaming performance with an OLED display, advanced cooling and all-day productivity.', 'GAME', 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-03', 'samsung-galaxy-s24-ultra', 'Samsung Galaxy S24 Ultra', 'Flagship smartphone with Galaxy AI, a pro-grade camera system and integrated S Pen.', 'PHONE', 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-04', 'google-pixel-9-pro', 'Google Pixel 9 Pro', 'A refined Android flagship combining an intelligent camera, clean software and long-term updates.', 'PHONE', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-05', 'ipad-pro-13-m4', 'iPad Pro 13-inch M4', 'Ultra Retina XDR tablet built for demanding creative workflows and mobile productivity.', 'COMP', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-06', 'sony-alpha-a7-iv', 'Sony Alpha 7 IV', 'Full-frame hybrid camera with 33MP imaging, dependable autofocus and professional 4K video.', 'ELEC', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-07', 'fujifilm-x100vi', 'Fujifilm X100VI', 'Compact premium camera with a 40MP sensor, stabilization and classic film simulations.', 'ELEC', 'https://images.unsplash.com/photo-1502920917128-1aa500764cbd?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-08', 'bose-quietcomfort-ultra', 'Bose QuietComfort Ultra', 'Immersive wireless headphones with spatial audio and adaptive noise cancellation.', 'AUDIO', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-09', 'marshall-stanmore-iii', 'Marshall Stanmore III', 'Room-filling home speaker with iconic design, a wide soundstage and flexible connectivity.', 'AUDIO', 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-10', 'apple-watch-ultra-2', 'Apple Watch Ultra 2', 'Rugged GPS smartwatch for endurance, exploration and advanced health tracking.', 'ELEC', 'https://images.unsplash.com/photo-1434493789847-2f02dc6ca35d?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-11', 'garmin-fenix-8', 'Garmin fēnix 8', 'Multisport GPS watch with mapping, training insights and exceptional battery life.', 'ELEC', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-12', 'nintendo-switch-oled', 'Nintendo Switch OLED', 'Versatile console with a vivid 7-inch OLED display, enhanced audio and 64GB storage.', 'GAME', 'https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-13', 'logitech-mx-master-3s', 'Logitech MX Master 3S', 'Quiet wireless mouse with an 8K sensor and precision MagSpeed scrolling.', 'COMP', 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-14', 'keychron-q1-max', 'Keychron Q1 Max', 'Premium mechanical keyboard with an aluminum body and tri-mode wireless connection.', 'COMP', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-15', 'lg-ultrafine-32-4k', 'LG UltraFine 32-inch 4K', 'Color-accurate UHD monitor with USB-C power delivery for a professional workspace.', 'COMP', 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-16', 'dyson-purifier-cool', 'Dyson Purifier Cool', 'Intelligent air purifier and fan with automatic sensing and whole-room HEPA filtration.', 'HOME', 'https://images.unsplash.com/photo-1585771724684-38269d6639fd?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-17', 'philips-hue-starter-kit', 'Philips Hue Starter Kit', 'Connected ambient lighting kit with three color bulbs, Hue Bridge and app control.', 'HOME', 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-18', 'ubiquiti-unifi-dream-router', 'Ubiquiti UniFi Dream Router', 'All-in-one Wi-Fi 6 gateway with intuitive network management and integrated security.', 'HOME', 'https://images.unsplash.com/photo-1544197150-b99a580bb7a8?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-19', 'anker-prime-20000', 'Anker Prime 20,000mAh Power Bank', 'High-capacity portable charger with a smart display and fast multi-device charging.', 'PHONE', 'https://images.unsplash.com/photo-1609592424824-6d7b2f9b17c1?auto=format&fit=crop&w=1000&q=85'),
    ('DEMO-SKU-20', 'dji-mini-4-pro', 'DJI Mini 4 Pro', 'Travel-friendly camera drone with obstacle sensing and professional 4K vertical video.', 'ELEC', 'https://images.unsplash.com/photo-1473968512647-3e447244af8f?auto=format&fit=crop&w=1000&q=85')
)
UPDATE products p
SET slug = catalog.slug,
    name = catalog.name,
    description = catalog.description,
    category_id = c.id,
    image_url = catalog.image_url,
    updated_at = NOW()
FROM product_variants v, catalog, categories c
WHERE v.product_id = p.id
  AND v.sku = catalog.sku
  AND c.code = catalog.category_code;
