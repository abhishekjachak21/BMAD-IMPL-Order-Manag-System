---
title: "Product Brief: Order Management & Fulfillment System (OMS)"
status: "approved"
created: "2026-10-29"
updated: "2026-10-29"
review_status: "reviewed and approved"
---

# Product Brief: Order Management & Fulfillment System (OMS)

## Executive Summary

The Order Management & Fulfillment System is a backend-first platform for managing the full lifecycle of a commercial order: product selection, inventory validation, payment processing, order status tracking, fulfillment coordination, and operational recovery when upstream or downstream systems fail. The product aims to give teams a reliable, auditable way to handle transactions while reducing operational errors and customer-facing delays.

This system is designed for a realistic business scenario where product, order, payment, and fulfillment are business domains with clear responsibilities but not necessarily independent runtime deployments. The core challenge is not simply taking an order — it is ensuring that money, inventory, fulfillment, and customer communication remain consistent even under retries, partial failures, and downstream dependency issues.

The OMS should behave like a dependable transaction coordinator: it accepts orders, validates critical conditions, processes payment safely, reserves inventory, tracks order state, and drives fulfillment without allowing duplicate charges, inconsistent statuses, or silent operational drift.

## The Problem

Modern commerce operations fail when order, payment, and fulfillment state are not aligned. In many real systems, buyers can retry checkout, payment providers can timeout, inventory can be oversold, and fulfillment can be delayed by dependency failures. The result is expensive errors: duplicate charges, canceled orders with paid inventory, incorrect shipment attempts, and poor customer support because the system cannot explain what happened.

Teams managing orders need a single place to understand the true state of a transaction. Without that, they rely on fragile, manual reconciliation across separate systems. This creates operational risk, inconsistent customer experience, and slow incident response.

The product problem is therefore not only “place an order,” but “manage the transaction safely from initial intent to final delivery or cancellation, under realistic failure conditions.”

## The Solution

The OMS will provide a backend system that manages the order lifecycle across the major business domains:

- Product domain: product catalog, availability, pricing, and product metadata
- Order domain: order creation, validation, state transitions, and customer/order records
- Payment domain: payment authorizations, captures, retries, verification, and reconciliation
- Fulfillment domain: shipment and delivery coordination, inventory allocation, and post-order actions

These are treated as business responsibilities and integration boundaries, not assumptions about deployment topology. The platform will treat the order as a durable business record with explicit state transitions, not a loose collection of independent API calls. It will validate critical business conditions before proceeding, protect money movement with idempotency, support status verification when external systems are uncertain, and keep order state aligned with payment and fulfillment outcomes.

## What Makes This Different

This product is intentionally built around operational correctness rather than simpler “happy path” success. A realistic OMS must be resilient to the things that break in production: retries, timeouts, duplicate webhooks, partial outages, and inconsistent dependency responses.

The differentiator is disciplined transaction handling:
- only one active payment operation per order
- idempotent payment operations
- payment verification before retry
- timeout treated as uncertainty, not failure
- order state synchronized with payment and fulfillment outcomes
- durable reconciliation and review for unresolved cases

This makes the product suitable as a realistic backend demo and an honest foundation for real-world commerce operations.

## Who This Serves

Primary users:
- Operations teams managing order processing and exception handling
- Customer support teams resolving payment, fulfillment, and order disputes
- Commerce or business managers monitoring conversion, fulfillment health, and resolution speed
- Internal engineers and product teams building and validating order workflows

The system is built for teams that need to process orders reliably at scale without manual intervention for every anomaly. The user need is a trustworthy order and payment workflow that can explain what happened, prevent duplicate financial events, and support recovery when systems are partially unavailable.

Secondary users:
- Warehouse or fulfillment coordinators
- Finance teams involved in settlement, reconciliation, and refund review
- Internal auditing or compliance reviewers

## Success Criteria

The OMS is successful when it can reliably support the business outcomes below:
- orders are processed without duplicate charges or duplicate payment operations
- payment status is never assumed without verification when the provider is uncertain
- fulfillment does not proceed erroneously when payment or inventory is unresolved
- customer support can explain the actual state of an order using a consistent system of record
- operational exceptions are handled through defined review or reconciliation workflows rather than ad hoc fixes

Measures of success include:
- zero duplicate capture events for the same order/payment operation
- low payment verification backlog
- low rate of order state mismatch between payment, order, and fulfillment systems
- quick identification and resolution of unresolved or manual-review transactions

## Scope

### In scope for MVP
- Order creation and validation
- Product catalog and price visibility
- Inventory reservation / availability checks
- Payment initiation and payment state lifecycle
- Payment verification and reconciliation for uncertain states
- Duplicate prevention for payment operations and callbacks
- Order status progression and tracking
- Fulfillment request creation and fulfillment state tracking
- Basic exception handling and manual review workflows

### Out of scope for MVP
- full ERP integration
- advanced CVV or fraud scoring engine beyond basic provider integration
- advanced warehouse optimization or route planning
- international tax engine and complex multi-jurisdiction compliance workflows
- customer-facing storefront UX beyond backend orchestration needs
- fully autonomous self-healing fulfillment logic

## Vision

The long-term vision is a dependable commerce operating system that can coordinate the entire lifecycle of an order from intent to fulfillment while maintaining trust across payment, logistics, and customer experience. In a successful future state, the platform will not just record transactions; it will detect anomalies early, reconcile external systems safely, and provide teams with a clear system of record for every order decision.

## Core Scope and Workflow Overview

### 1. Order Lifecycle and State Model
The order domain maintains the authoritative lifecycle for each order. For the MVP, the lifecycle should be explicit and easy to reason about, with a limited set of states that cover both successful and failed paths.

Recommended lifecycle states:
- CREATED: order has been drafted and accepted by the system.
- VALIDATED: product, pricing, and inventory inputs are valid enough to continue.
- RESERVED: inventory has been reserved against the order.
- PAYMENT_PENDING: payment is being processed or awaiting confirmation.
- PAYMENT_VERIFICATION_REQUIRED: provider status is uncertain or unreachable; verification is required before the order can advance.
- PAID: payment is confirmed captured and the order is ready for fulfillment.
- FULFILLMENT_PENDING: the order is approved for fulfillment and tasks are being created.
- FULFILLED: goods have been fulfilled and delivered or closed as complete.
- PARTIALLY_FULFILLED: only a subset of the order is fulfilled; applicable only if partial fulfillment is in scope.
- CANCELLED: the order is cancelled before completion.
- FAILED: the order is not completed due to a business or operational failure.
- RECONCILIATION_REQUIRED: the order remains unclear because of payment or dependency uncertainty and requires manual support review.

For the MVP, the key success path is: CREATED → VALIDATED → RESERVED → PAYMENT_PENDING → PAID → FULFILLMENT_PENDING → FULFILLED.

Failure and exception paths include: payment failure, inventory not available, unresolved payment state, cancellation, or internal validation failure.

### 2. Inventory Reservation, Commitment, Release, and Expiry
Inventory handling should be explicit and bounded:
- Reservation: stock is reserved when the order becomes valid enough to hold the items.
- Commitment: inventory becomes committed when the payment is confirmed and the order is approved to proceed.
- Release: reserved inventory is released when the order is cancelled, payment fails, validation fails, or the order expires without completion.
- Expiry: reserved inventory should expire after a configurable timeout window if payment is unresolved or the order remains in a non-advancing state.

The exact values for inventory expiry and unresolved-payment windows will be finalized during PRD or implementation planning. The MVP should not assume advanced inventory optimization. It should support a basic reservation policy that prevents overselling while keeping operations understandable.

### 3. Payment Remaining Unresolved While Inventory Is Reserved
If payment remains unresolved while inventory is already reserved, the order should remain in a controlled waiting state rather than silently continuing. The recommended behavior is:
- keep the order in PAYMENT_VERIFICATION_REQUIRED or PAYMENT_PENDING while the provider is being checked
- hold reservation until a configurable timeout or resolution point is reached
- if the payment is confirmed, continue normally
- if payment is definitively failed, release the reservation and cancel or fail the order
- if payment stays unresolved past the configured timeout window, move to RECONCILIATION_REQUIRED and release or hold inventory according to policy

This ensures inventory does not become an unbounded hold while the payment truth is uncertain.

### 4. Cancellation Behavior
Cancellation should be simple and reliable for the MVP:
- a customer or operations user can cancel an order before fulfillment is finalized
- if payment has not yet been captured, the order can be cancelled without a refund flow
- if payment has already been captured, the order is marked for follow-up handling instead of being automatically refunded in the MVP
- inventory reservation is released when cancellation succeeds
- cancelled orders remain auditable and supportable for later review

Complex refund logic is intentionally deferred for the MVP. The core requirement is to prevent duplicate charges and maintain a clear order state, not to build a full financial refund engine.

### 5. Payment Reconciliation for the MVP
The MVP should keep payment reconciliation intentionally narrow:
- verify payment status with the simulated provider when the system cannot determine whether a payment succeeded
- keep unresolved payments in a verification or manual-review state
- allow limited retry of status verification with backoff
- move to manual review after a configurable timeout window

This is limited to validation and manual review handling. It does not include advanced dispute management, partial refunds, automated financial reconciliation, or settlement logic.

### 6. Business Domains, Not Deployment Assumptions
Product, Order, Payment, and Fulfillment are business domains with distinct responsibilities.
They should be treated as bounded areas of ownership and logic, but the implementation is not required to deploy as four separate microservices. Architecture will decide runtime boundaries later.

### 7. Simulated Payment Provider
The initial implementation and demo will use a simulated payment provider. This keeps the project practical and focused on payment safety, reconciliation behavior, retry logic, and order integrity without depending on a live financial integration.

### 8. Measurable Non-Functional Requirements
For the MVP, non-functional requirements should be practical and measurable without inventing unrealistic production SLAs.

- Reliability: the system should successfully process 95% or more of standard order flows without state inconsistency during normal demo conditions. This is a normal-flow operational metric and is intentionally separate from the strict correctness guarantees for payment integrity and no-uncertain-fulfillment rules.
- Resilience: duplicate payment attempts and provider uncertainty should not lead to duplicate charge creation or silent order-state drift.
- Security: payment and customer data must be protected in transit and at rest; secrets should never be hardcoded or exposed in logs.
- Observability: every order and payment state transition should be traceable through logs or persisted status history; unresolved payment states must be visible to operations staff.
- Correctness: duplicate payment operations, duplicate webhooks, and concurrent order actions should be prevented or safely rejected. Correctness is a strict invariant and not a measured SLA target.

These are practical measures for a demo-grade backend, not a full enterprise guarantee.

## Main End-to-End Business Workflows

### Workflow 1: Create and validate an order
1. Customer selects products and quantity.
2. Product and pricing validation checks catalog, price, and inventory availability.
3. Inventory reservation is created or checked.
4. Order record enters a valid state for payment.

### Workflow 2: Process payment safely
1. Order domain creates a payment operation with a unique idempotency key.
2. Payment domain validates that no active payment operation exists for the order.
3. Simulated provider is called once for the operation.
4. If the provider confirms capture, the order is synchronized to a paid state.
5. If the provider is unreachable or returns an uncertain status, the payment enters PAYMENT_VERIFICATION_REQUIRED and verification begins.

### Workflow 3: Fulfill an order
1. Order is confirmed paid and inventory is committed.
2. Fulfillment domain creates shipping or delivery tasks.
3. Order status updates as fulfillment progresses.
4. If fulfillment fails or is delayed, order state reflects the pending exception and supports support or retry workflows.

### Workflow 4: Handle exceptions and recovery
1. Duplicate request is detected and treated as the same operation.
2. Duplicate webhook is ignored after deduplication.
3. Verification queue resolves provider status and repairs order state.
4. Manual review handles long-lived unresolved status cases.

### Workflow 5: Fulfillment progression and completion
1. Shipment creation marks the order as ready for dispatch.
2. Shipment dispatch indicates the goods have been handed off to the carrier or fulfillment partner.
3. Delivery completion is the final completion event and closes the order as FULFILLED.
4. A shipment may exist before the final delivery completion event, but the FULFILLED state represents the agreed final completion step.
5. If a shipment is created but not yet dispatched, the order remains in a fulfillment-pending or in-flight state rather than FULFILLED.

## Important Business Rules and Failure Scenarios

### Payment rules
- A timeout is not treated as payment failure.
- A payment already captured must never be charged again.
- Retry is allowed only for eligible failed payments.
- Duplicate requests for the same payment operation return the same result.
- Different payment requests while one is active are rejected as “already in progress.”
- Payment correctness is a strict invariant: no duplicate captures and no fulfillment based on uncertain payment status.
- For the MVP, payment reconciliation is limited to basic verification and manual-review handling.

### Order rules
- Orders must not advance to fulfillment without a valid payment and inventory state.
- Order status must reflect the true current business state, never just the last API response.
- Inventory reservation must be consistent with order state and fulfillment actions.

### Inventory rules
- Reserved inventory is held only while the order remains in an active decision path.
- Inventory should be released on cancellation, payment failure, expiry, or validation failure.
- Unresolved payment conditions should not leave inventory held indefinitely without a defined route to release or escalation.
- Inventory expiry and unresolved-payment windows are configurable and will be finalized during PRD or implementation planning.

### Fulfillment rules
- A fulfillment action must be based on a confirmed order state.
- Delayed or failed fulfillment must not silently mark an order as complete.
- Shipment creation, shipment dispatch, and delivery completion are distinct events; FULFILLED represents the final completion event.
- A shipment or delivery failure should route to exception handling rather than a false success state.

### Operational rules
- Provider callbacks and webhooks are deduplicated before state updates.
- Long-running “unknown” payments move to reconciliation or manual review.
- Support and operations teams need an auditable trail of status transitions.

## High-Level Domain Responsibilities

### Product domain
- Manage product catalog and metadata
- Hold pricing and product-level rules
- Provide product availability information to order and inventory decisions

### Order domain
- Create and track orders
- Manage order status transitions
- Coordinate with payment and fulfillment processes
- Maintain order history and dispute context

### Payment domain
- Handle payment operations and idempotency
- Verify provider status for uncertain states
- Maintain payment lifecycle and exceptions
- Support payment retries only when allowed and safe
- Use the simulated provider for demo and validation

### Fulfillment domain
- Create and track fulfillment tasks
- Validate shipping readiness against order state
- Handle shipping delays or cancellations according to operational policy


