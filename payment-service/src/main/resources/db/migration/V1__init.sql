-- Enable extension for UUID generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Ensure schema
CREATE SCHEMA IF NOT EXISTS payments;

-- Payment table
CREATE TABLE IF NOT EXISTS payments.payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PENDING','COMPLETED','FAILED')),
    provider TEXT NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payment_order_id ON payments.payment(order_id);
