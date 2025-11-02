-- Enable useful extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Create immutable wrapper function for unaccent
CREATE OR REPLACE FUNCTION immutable_unaccent(text) 
RETURNS text AS $$
BEGIN
    RETURN unaccent($1);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    attributes JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    search_tsv tsvector GENERATED ALWAYS AS (
        to_tsvector('english',
            coalesce(immutable_unaccent(title), '') || ' ' || coalesce(immutable_unaccent(description), '')
        )
    ) STORED
);

-- GIN index for full-text search
CREATE INDEX IF NOT EXISTS idx_products_search_tsv ON products USING GIN (search_tsv);

-- Helpful index for seller filtering
CREATE INDEX IF NOT EXISTS idx_products_seller ON products (seller_id);
