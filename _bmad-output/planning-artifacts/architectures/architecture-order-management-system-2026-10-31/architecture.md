---
title: "Architecture: Order Management & Fulfillment System (OMS)"
status: "approved"
created: "2026-10-31"
updated: "2026-10-31T21:12:49+05:30"
---

# Architecture: Order Management & Fulfillment System (OMS)

## 1. Architecture Summary

This architecture follows the approved PRD and keeps the system practical for a backend demo: a single Spring Boot application in Java 21 using Maven and PostgreSQL. The codebase is organized by business domain using package boundaries, not by multiple Maven modules. Product, Order, Inventory, Payment, and Fulfillment remain explicit domains within one deployable application, sharing a single transactional database and a consistent domain model.

This is intentionally not a microservice architecture. The system avoids Kafka, Kubernetes, and distributed transaction complexity because the product requirements prioritize correctness, operational clarity, and a realistic demo implementation over unnecessary enterprise infrastructure.

## 2. Architectural Principles

- Keep business domains explicit: Product, Order, Inventory, Payment, Fulfillment.
- Use one shared relational database as the source of truth.
- Use database transactions and unique constraints for consistency and concurrency control.
- Treat payment correctness as a strict invariant.
- Treat timeout as uncertainty, never as success.
- Block fulfillment on uncertain payment state.
- Separate business workflow rules from transport concerns.
- Favor boring, well-understood infrastructure: Spring Boot + PostgreSQL + Maven.

## 3. System Context

The OMS supports a backend-leaning workflow for:
- validating catalog and pricing data
- creating and tracking orders
- reserving and releasing inventory
- creating and verifying payment operations
- creating shipments and final delivery completion
- handling unresolved or exception states with auditability

This is designed for internal operations and demo workflows, not a public storefront or live financial integration.

## 4. High-Level Architecture

### 4.1 Runtime model

- One Spring Boot application
- One PostgreSQL database
- Domain-based packages inside the single application
- REST API layer for application operations
- Scheduled jobs for expiry, verification, and reconciliation
- Simulated payment provider adapter

### 4.2 Deployment shape

- App server: Spring Boot 3.x, Java 21
- Build: Single Maven project
- Database: PostgreSQL 15+
- Internal access: application-to-database via JDBC / Spring Data JPA
- External provider: simulated provider adapter (HTTP stub or local service boundary)

### 4.3 Why this structure

A single Spring Boot app with domain packages matches the PRD well:
- business domains are clearly separated by package boundaries
- the system can still enforce transactional consistency across domains
- it avoids overengineering and deployment complexity
- it remains easy to demo and reason about as a backend service

## 5. Domain Package Layout

Suggested package layout under a single Maven project:

- com.oms
  - api
  - catalog
  - order
  - inventory
  - payment
  - fulfillment
  - support
  - shared
  - infrastructure
  - config

### 5.1 api
Purpose:
- REST controllers and request/response contracts
- API validation boundaries
- OpenAPI documentation if used in demo
- support and operational endpoints

### 5.2 catalog
Responsibilities:
- product catalog
- price and availability lookup
- validation of product and quantity constraints

### 5.3 order
Responsibilities:
- order creation and mutation
- order state tracking
- order event timeline
- cancellation and review states

### 5.4 inventory
Responsibilities:
- inventory availability
- reservation creation and expiry
- release and commitment logic
- reservation timeout orchestration

### 5.5 payment
Responsibilities:
- payment operation lifecycle
- idempotency enforcement
- provider requests and status verification
- duplicate callback handling
- reconciliation decisions

### 5.6 fulfillment
Responsibilities:
- shipment creation
- dispatch and delivery completion
- final fulfillment completion
- fulfillment exception handling

### 5.7 support
Responsibilities:
- investigation and review workflows
- order timeline and unresolved-state queries
- reconciliation queue support

### 5.8 shared
Responsibilities:
- common utility code
- request IDs and correlation IDs
- time utilities
- error response standardization
- shared security and audit metadata

### 5.9 infrastructure
Responsibilities:
- JPA repositories
- PostgreSQL access
- external provider client adapter
- scheduled jobs
- logging and metrics

## 6. Database Design

A single PostgreSQL database stores all transactional state. Shared tables and explicit domain tables keep the system clear and auditable.

### 6.1 Core relational model

#### products
- id UUID PK
- sku VARCHAR unique
- name VARCHAR
- description TEXT
- unit_price NUMERIC(19,4)
- active BOOLEAN
- created_at TIMESTAMP
- updated_at TIMESTAMP

#### orders
- id UUID PK
- order_number VARCHAR unique
- customer_id VARCHAR
- status VARCHAR
- currency VARCHAR
- subtotal NUMERIC(19,4)
- total_amount NUMERIC(19,4)
- created_at TIMESTAMP
- updated_at TIMESTAMP
- cancelled_at TIMESTAMP
- failed_at TIMESTAMP
- fulfilled_at TIMESTAMP

#### order_items
- id UUID PK
- order_id UUID FK -> orders.id
- product_id UUID FK -> products.id
- quantity INTEGER
- unit_price NUMERIC(19,4)
- line_total NUMERIC(19,4)
- created_at TIMESTAMP

Constraints:
- unique(order_id, product_id)
- quantity > 0

#### inventory_items
- id UUID PK
- product_id UUID FK -> products.id
- total_quantity INTEGER NOT NULL
- available_quantity INTEGER NOT NULL
- reserved_quantity INTEGER NOT NULL
- committed_quantity INTEGER NOT NULL
- version BIGINT NOT NULL
- updated_at TIMESTAMP

Consistency rules:
- total_quantity >= 0
- available_quantity >= 0
- reserved_quantity >= 0
- committed_quantity >= 0
- available_quantity + reserved_quantity + committed_quantity = total_quantity
- inventory mutations must be performed in a single transaction and validated against the current version to prevent oversell or stale quantity updates

#### inventory_reservations
- id UUID PK
- order_id UUID FK -> orders.id
- product_id UUID FK -> products.id
- quantity INTEGER
- status VARCHAR
- reserved_at TIMESTAMP
- expires_at TIMESTAMP
- released_at TIMESTAMP
- created_by VARCHAR
- version BIGINT NOT NULL

Indexes:
- order_id
- product_id + status
- expires_at

#### payment_operations
- id UUID PK
- order_id UUID FK -> orders.id
- provider_operation_id VARCHAR
- idempotency_key VARCHAR
- status VARCHAR
- payment_attempt_number INTEGER
- amount NUMERIC(19,4)
- currency VARCHAR
- provider_status VARCHAR
- started_at TIMESTAMP
- completed_at TIMESTAMP
- last_verified_at TIMESTAMP
- created_at TIMESTAMP
- updated_at TIMESTAMP

Unique constraints:
- (order_id, idempotency_key) when active or active+historical if needed for dedupe
- provider_operation_id unique where populated

#### payment_events
- id UUID PK
- payment_operation_id UUID FK -> payment_operations.id
- external_event_id VARCHAR
- source VARCHAR
- event_type VARCHAR
- payload JSONB
- processed_at TIMESTAMP
- dedupe_hash VARCHAR unique

Purpose:
- store provider callbacks/webhooks
- ignore duplicate events safely
- preserve auditability

#### fulfillment_tasks
- id UUID PK
- order_id UUID FK -> orders.id
- status VARCHAR
- shipment_reference VARCHAR
- created_at TIMESTAMP
- dispatched_at TIMESTAMP
- delivered_at TIMESTAMP
- completed_at TIMESTAMP

#### order_events
- id UUID PK
- order_id UUID FK -> orders.id
- aggregate_type VARCHAR
- aggregate_id UUID
- event_type VARCHAR
- event_version BIGINT
- payload JSONB
- created_at TIMESTAMP

Purpose:
- timeline of business state changes
- operational support and investigation

### 6.2 Database consistency strategy

Core safeguards:
- unique constraints on idempotency keys and provider event IDs
- row-level locking for active payment operations per order
- optimistic locking with version fields on inventory and order state rows
- transactional updates for order + inventory + payment state changes
- rejection of duplicate external event data via dedupe hash
- explicit separation between payment-operation persistence and external provider calls

### 6.3 Payment transaction boundaries

The architecture explicitly separates the internal payment-operation persistence from the external provider call:

1. Start a database transaction.
2. Validate idempotency and check the active payment-operation record for the order.
3. Persist or update the payment operation row and record the internal state change.
4. Commit the transaction once the payment operation state is durable.
5. Invoke the external provider call outside the database transaction.
6. Persist the provider result in a subsequent transaction, then update the payment state and order state.

This ensures that:
- the OMS records the payment intent before the external dependency is contacted
- a timeout or provider outage does not create a false payment success
- retries can safely verify provider status without reusing or recharging the same operation
- duplicate callbacks are deduplicated without mutating state twice

## 7. API Contracts

The architecture uses REST endpoints that align to business actions, not complex internal microservice boundaries.

### 7.1 Product APIs
- GET /api/products/{id}
- GET /api/products?sku=
- POST /api/products
- PUT /api/products/{id}

### 7.2 Order APIs
- POST /api/orders
- GET /api/orders/{id}
- GET /api/orders/{id}/timeline
- POST /api/orders/{id}/cancel
- POST /api/orders/{id}/validate

### 7.3 Inventory APIs
- POST /api/inventory/reservations
- DELETE /api/inventory/reservations/{id}
- POST /api/inventory/reservations/{id}/expire
- GET /api/inventory/products/{productId}

### 7.4 Payment APIs
- POST /api/orders/{orderId}/payments
  Request:
  - orderId
  - amount
  - currency
  - idempotencyKey
  - operationId (optional if server generates it)
  - provider reference if supplied

  Response:
  - paymentOperationId
  - status
  - message
  - alreadyInProgress flag

- GET /api/payments/{paymentOperationId}
- POST /api/payments/{paymentOperationId}/verify
- POST /api/payments/provider/webhook

### 7.5 Fulfillment APIs
- POST /api/orders/{orderId}/shipments
- POST /api/orders/{orderId}/shipments/{shipmentId}/dispatch
- POST /api/orders/{orderId}/shipments/{shipmentId}/deliver

### 7.6 Support APIs
- GET /api/support/orders/{id}/summary
- GET /api/support/reconciliation

## 8. Key Workflows

### 8.1 Order placement workflow
1. Validate product and quantity.
2. Create order row with CREATED or VALIDATED state.
3. Reserve inventory.
4. Create payment operation with idempotency key.
5. Submit payment attempt to provider.
6. Record outcome and update order state.
7. If payment captured, mark order PAID and allow fulfillment progression.
8. If payment uncertain, set PAYMENT_VERIFICATION_REQUIRED and stop fulfillment.

### 8.2 Payment submission workflow
1. Validate idempotency key and payment payload.
2. Check if an active payment operation exists for the order.
3. If same idempotency key: return the existing result.
4. If different key while active: reject as already in progress.
5. If previous payment is definitively failed: create a new payment operation with a new ID and new idempotency key.
6. Submit request to simulated provider.
7. Persist provider response and order/payment state change.
8. If timeout or unknown response: mark PAYMENT_VERIFICATION_REQUIRED and trigger verification workflow.

### 8.3 Verification and timeout handling workflow
1. When provider is unreachable or status unknown, do not treat as a successful or failed payment.
2. Mark payment as PAYMENT_VERIFICATION_REQUIRED.
3. Start retry verification with backoff.
4. If provider confirms capture: mark payment CAPTURED and synchronize order to PAID.
5. If provider confirms failure: evaluate if retry is allowed; otherwise move to reconciliation review.
6. If unresolved beyond timeout: release reservation and move order to RECONCILIATION_REQUIRED.

### 8.4 Cancellation workflow
1. Prevent new payment or fulfillment progression.
2. Release reserved inventory when allowed.
3. Mark payment as CANCELLED or RECONCILIATION_REQUIRED as needed.
4. Preserve audit timeline.

### 8.5 Fulfillment workflow
1. Only allow shipment creation if order is paid and valid.
2. Record shipment creation event.
3. Record dispatch event.
4. Record delivery completion event.
5. Only then transition to FULFILLED.

## 9. Payment and Inventory Consistency Safeguards

### 9.1 Payment invariants
- A payment is never retried after capture.
- A timeout does not imply success or failure.
- A duplicate provider callback must not create duplicate state transitions.
- A payment request with the same idempotency key returns the same result.
- Only one active payment operation may exist per order.
- A definitive FAILED payment creates a new operation, not a restarted one.

### 9.2 Inventory invariants
- Inventory is reserved before payment confirmation when the order enters the payment path.
- Payment uncertainty does not authorize fulfillment.
- Unresolved payment beyond the reservation timeout releases the reservation and moves the order to RECONCILIATION_REQUIRED.
- Post-dispatch inventory is treated as committed and is not silently released by routine payment verification.

### 9.3 Database safeguards
- Use a unique constraint on payment idempotency keys and dedupe hashes.
- Use row-level lock or select-for-update when starting or updating an active payment operation.
- Use optimistic locking on inventory rows and order rows.
- Enforce state transition validation in application logic and repository checks.
- Write all domain transitions to `order_events` for investigation.

### 9.4 Duplicate webhook handling
- Hash provider callback payloads and store dedupe signature.
- Ignore any duplicate callback with same dedupe hash.
- Idempotent processing in payment and order domains.

### 9.5 Operational safeguards
- Keep structured logs with correlation IDs and order IDs.
- Mark failure and timeout paths separately.
- Calendar or scheduled job for inventory expiry and payment verification.
- Reconciliation queue for unresolved states.

## 10. Technology Choices

### Java 21
- modern language features and stable LTS baseline
- suited to a backend demo and strong Spring Boot ecosystem support

### Spring Boot
- fast application bootstrapping
- easy REST controller usage
- mature transaction management and ORM support

### PostgreSQL
- strong relational modeling for inventory and payment state
- reliable constraints and transactions
- straightforward for a single-system demo

### Maven
- single Maven project for the entire Spring Boot application
- clear dependency and build ordering for one deployable backend service
- fits the project size and delivery pace without introducing multi-module complexity

### Simulated payment provider
- local HTTP stub or fake provider service
- returns success, failure, timeout, and unknown-status outcomes for testing
- keeps developer demo realistic without live payment complexity

## 11. Service and Module Interactions

- Order module creates order record and validates item content.
- Inventory module checks stock and reserves items.
- Payment module creates payment operation and enforces idempotency and single-active-operation checks.
- Order + Payment modules coordinate on transition to PAID or PAYMENT_VERIFICATION_REQUIRED.
- Fulfillment module only acts on orders with valid payment state.
- Shared infrastructure records domain events and implements external provider communication.

This keeps module boundaries aligned with business responsibilities while still allowing a single application to coordinate them transactionally.

## 12. Operational and Reliability Requirements

### Reliability
- Transactions cover order and inventory updates for the core state path.
- Payment operations are persisted before provider submission.
- Uncertain outcomes are never treated as successful without verification.

### Security
- Do not persist raw card data or provider secrets.
- Mask and hash provider references and identity data as needed.
- Restrict operational endpoints to internal roles.

### Resilience
- Verification retries use bounded backoff.
- Unknown status is treated as retryable uncertainty, not a charge.
- Reconciliation queue catches unresolved cases after timeout.

### Observability
- correlation IDs for each order and payment operation
- structured logs for state transitions and failure classes
- metrics for payment attempts, verification retries, and unresolved states
- support timeline endpoint for troubleshooting

## 13. Non-Functional Targets

These are practical demo targets rather than unrealistic production SLAs:
- API success for normal order flow: 95% or better under standard demo conditions
- Duplicate charge prevention: zero duplicate captures within same operation and order
- Fulfillment gating: zero fulfillment actions from uncertain payment state
- Payment verification retry coverage: all uncertain provider states retried with backoff
- Support auditability: full order timeline available within the operational interface

## 14. Key Risks and Trade-Offs

- A single database means stronger consistency but requires disciplined transaction boundaries.
- Payment provider uncertainty must be treated conservatively to avoid false success.
- Inventory timeout rules need explicit operational handling after reservation expiry.
- The architecture intentionally avoids complex distributed patterns because they would exceed the approved PRD scope.

## 15. Recommended Implementation Order

1. Domain model and state enums
2. PostgreSQL schema and migration scripts
3. Product and order modules
4. Inventory reservation + release logic
5. Payment operation + idempotency + dedupe logic
6. Provider simulation and status verification job
7. Fulfillment creation and completion logic
8. Support timeline and reconciliation queue
9. End-to-end demo validation

## 16. Decision Summary

Accepted architecture decisions:
- Modular monolith with Java 21 + Spring Boot + PostgreSQL + Maven
- Single database as the source of truth
- Business-domain modules inside one application
- Payment correctness and idempotency as hard invariants
- Inventory release on unresolved payment timeout with order moved to RECONCILIATION_REQUIRED
- Simulated payment provider for demo and validation
- No Kafka, no Kubernetes, no distributed microservice deployment for MVP

Open for review before implementation:
- exact timeout values for inventory reservation and payment verification windows
- final ownership of reconciliation queue and support workflow
- formatting and endpoint standards for the demo API

This architecture is intentionally implementation-ready for a practical backend demo while staying faithful to the approved PRD and not over-engineering the system.
