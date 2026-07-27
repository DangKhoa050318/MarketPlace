-- V1: users (merged). Role set = ADMIN | MANAGER | STAFF | CUSTOMER.
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    email    VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- Seed accounts (password for all = 'admin123')
INSERT INTO users (username, email, password, full_name, role, active) VALUES
('admin',    'admin@marketplace.com',    '$2a$10$ZC4eiPn25JMEntSoCC/0uuUlxO.dmifem0QW0LrY5ImHaUaBQwSFe', 'Admin User',    'ADMIN',    true),
('manager',  'manager@marketplace.com',  '$2a$10$ZC4eiPn25JMEntSoCC/0uuUlxO.dmifem0QW0LrY5ImHaUaBQwSFe', 'Warehouse Manager', 'MANAGER', true),
('staff',    'staff@marketplace.com',    '$2a$10$ZC4eiPn25JMEntSoCC/0uuUlxO.dmifem0QW0LrY5ImHaUaBQwSFe', 'Warehouse Staff',   'STAFF',   true),
('customer', 'customer@marketplace.com', '$2a$10$ZC4eiPn25JMEntSoCC/0uuUlxO.dmifem0QW0LrY5ImHaUaBQwSFe', 'Sample Customer',   'CUSTOMER', true)
ON CONFLICT (username) DO NOTHING;
