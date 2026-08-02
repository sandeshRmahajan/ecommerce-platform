CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS order_payment;
CREATE SCHEMA IF NOT EXISTS notification;

CREATE USER auth_user WITH PASSWORD 'auth_password';
CREATE USER catalog_user WITH PASSWORD 'catalog_password';
CREATE USER order_payment_user WITH PASSWORD 'order_payment_password';
CREATE USER notification_user WITH PASSWORD 'notification_password';

GRANT ALL PRIVILEGES ON SCHEMA auth TO auth_user;
GRANT ALL PRIVILEGES ON SCHEMA catalog TO catalog_user;
GRANT ALL PRIVILEGES ON SCHEMA order_payment TO order_payment_user;
GRANT ALL PRIVILEGES ON SCHEMA notification TO notification_user;

REVOKE ALL ON SCHEMA catalog, order_payment, notification FROM auth_user;
REVOKE ALL ON SCHEMA auth, order_payment, notification FROM catalog_user;
REVOKE ALL ON SCHEMA auth, catalog, notification FROM order_payment_user;
REVOKE ALL ON SCHEMA auth, catalog, order_payment FROM notification_user;

CREATE EXTENSION IF NOT EXISTS pgcrypto;