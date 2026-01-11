-- Initialize database with user having all privileges
-- This script runs automatically when the container is first created

-- Grant all privileges on the database to pogrejab user
GRANT ALL PRIVILEGES ON DATABASE pogrejab_db TO pogrejab;

-- Grant all privileges on all tables in public schema
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO pogrejab;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO pogrejab;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO pogrejab;

-- Grant privileges for future tables/sequences/functions
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO pogrejab;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO pogrejab;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON FUNCTIONS TO pogrejab;
