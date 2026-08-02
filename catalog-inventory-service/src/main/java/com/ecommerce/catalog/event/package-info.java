/**
 * Kafka event contract shared conceptually with order-payment-service. Changes here have
 * cross-service implications and should be made carefully, keeping payload shapes backward
 * compatible with what order-payment-service actually publishes/consumes.
 */
package com.ecommerce.catalog.event;
