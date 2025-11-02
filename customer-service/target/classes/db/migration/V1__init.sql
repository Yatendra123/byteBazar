-- Enable extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Customers table
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Case-insensitive unique email
CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_email_unique ON customers (lower(email));

-- Helpful name index
CREATE INDEX IF NOT EXISTS idx_customers_name ON customers (last_name, first_name);
