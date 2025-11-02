-- ByteBazar Database Initialization Script
-- Creates required databases and users for all microservices

-- Create bytebazar user
CREATE USER bytebazar WITH PASSWORD 'bytebazar123';

-- Create databases for each service
CREATE DATABASE bytebazar_customer OWNER bytebazar;
CREATE DATABASE bytebazar_payments OWNER bytebazar;
CREATE DATABASE bytebazar_catalog OWNER bytebazar;

-- Grant all privileges to bytebazar user
GRANT ALL PRIVILEGES ON DATABASE bytebazar_customer TO bytebazar;
GRANT ALL PRIVILEGES ON DATABASE bytebazar_payments TO bytebazar;
GRANT ALL PRIVILEGES ON DATABASE bytebazar_catalog TO bytebazar;

-- Connect to each database and grant schema permissions
\c bytebazar_customer;
GRANT ALL ON SCHEMA public TO bytebazar;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO bytebazar;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO bytebazar;

\c bytebazar_payments;
CREATE SCHEMA IF NOT EXISTS payments;
GRANT ALL ON SCHEMA public TO bytebazar;
GRANT ALL ON SCHEMA payments TO bytebazar;
ALTER DEFAULT PRIVILEGES IN SCHEMA payments GRANT ALL ON TABLES TO bytebazar;
ALTER DEFAULT PRIVILEGES IN SCHEMA payments GRANT ALL ON SEQUENCES TO bytebazar;

\c bytebazar_catalog;
GRANT ALL ON SCHEMA public TO bytebazar;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO bytebazar;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO bytebazar;

-- Create orders schema in postgres database (order-service uses postgres DB)
\c postgres;
CREATE SCHEMA IF NOT EXISTS orders;
GRANT ALL ON SCHEMA orders TO postgres;
