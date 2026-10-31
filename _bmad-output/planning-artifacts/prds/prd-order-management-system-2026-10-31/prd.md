---
title: "PRD: Order Management & Fulfillment System (OMS)"
status: "approved"
created: "2026-10-31"
updated: "2026-10-31T21:11:15+05:30"
---

# PRD: Order Management & Fulfillment System (OMS)
*Working title — confirm.*

## 0. Document Purpose

This PRD defines the product requirements for a practical, backend-oriented Order Management & Fulfillment System for the current project. It is intended for product stakeholders, engineering leads, and downstream planning workflows. The requirements are grounded in the approved Product Brief and reflect the agreed business decisions: inventory safety, strict payment correctness, limited reconciliation for the MVP, and a simulated payment provider.

This document uses a shared business vocabulary for Product, Order, Payment, Inventory, and Fulfillment. It does not assume four independently deployed microservices; runtime boundaries remain open until architecture planning. Technical implementation decisions are intentionally deferred to later artifacts.

## 1. Vision

The OMS exists to give a business a dependable way to manage the commercial lifecycle of an order from product selection through payment confirmation, inventory commitment, and fulfillment completion. The system is designed to protect the business from the most costly operational errors: duplicate charges, inventory oversell, premature fulfillment, and unclear order states during dependency failures.

The product is not just an order form or a checkout step. It is a transaction coordinator that maintains one trustworthy view of the order across the domains that matter most: product, inventory, payment, and fulfillment. It is used by internal operators and support staff to understand what happened, what is still unresolved, and what can be safely advanced.

The immediate goal is a realistic backend demo that is credible, operationally safe, and easy to reason about. The long-term goal is a system that can handle the essential business flow of commerce operations while remaining resilient under retries, timeouts, and delayed callbacks.

## 2. Target User

### 2.1 Jobs To Be Done

- Manage product availability and price information before order acceptance.
- Create and validate an order without allowing duplicate or conflicting payment actions.
- Protect inventory from oversell while a payment or fulfillment decision is pending.
- Confirm when payment has actually succeeded and when it is still uncertain.
- Advance fulfillment only when the order is in a valid business state.
- Resolve edge-case operational issues without creating silent financial or fulfillment errors.
- Trace the record of an order and explain the business outcome to support or operations teams.

### 2.2 Non-Users (v1)

The MVP is not intended to serve:
- a customer-facing storefront experience beyond backend-order orchestration
- a sophisticated production payment gateway integration with live financial settlement
- a full warehouse optimization engine
- a multi-channel fulfillment network with advanced dispatch logic
- complex international tax, refund, and settlement workflows

### 2.3 Key User Journeys

- **UJ-1. Operations manager validates a new order before payment**
  - **Persona + context:** An internal operations lead checking that a new order is valid before payment is attempted.
  - **Entry state:** Order has been created and not yet confirmed for payment.
  - **Path:** The operator reviews the items, stock expectations, and order context; the system validates basics and returns a status indicating whether the order can proceed.
  - **Climax:** The order is accepted into the validation flow and the system prevents invalid or conflicting actions.
  - **Resolution:** The order remains in a controlled state ready for payment or cancellation.

- **UJ-2. Customer checkout attempts a payment safely**
  - **Persona + context:** A customer placing an order with a single payment attempt and a possible retry due to a timeout or browser refresh.
  - **Entry state:** Order is valid, inventory is reserved, and payment is ready to start.
  - **Path:** The system creates a payment operation with an idempotency key; duplicate requests are recognized; the provider is called once; status is persisted.
  - **Climax:** The payment either confirms capture or moves to verification due to uncertainty.
  - **Resolution:** The order advances only on confirmed payment or safely remains unresolved for review.

- **UJ-3. Support agent investigates an unresolved payment**
  - **Persona + context:** A support agent handling a customer concern about an order that may or may not have been paid.
  - **Entry state:** Payment is in PAYMENT_VERIFICATION_REQUIRED or RECONCILIATION_REQUIRED.
  - **Path:** Agent retrieves the order timeline, sees provider verification status, and reviews order/inventory state.
  - **Climax:** The system surfaces the payment result or escalation state clearly and prevents additional accidental charges.
  - **Resolution:** The order is either finalized, cancelled, or paused for manual review.

- **UJ-4. Fulfillment operator completes a valid shipment**
  - **Persona + context:** A warehouse or fulfillment operator processing a confirmed, paid order.
  - **Entry state:** Order is in PAID or FULFILLMENT_PENDING with committed inventory.
  - **Path:** Shipment is created, dispatch is recorded, and the final delivery completion event closes the order.
  - **Climax:** The order reaches FULFILLED only after the final completion event is recorded.
  - **Resolution:** Inventory is considered settled for the order and the support timeline remains auditable.

## 3. Glossary

- **Order** — The business record representing a customer purchase request and its lifecycle from creation through completion or cancellation.
- **Product** — A sellable item with catalog data, price, and current availability information.
- **Inventory Reservation** — A temporary hold against stock to prevent oversell while an order is in an active decision path.
- **Inventory Commitment** — The transition of reserved stock into a committed state when payment is confirmed and fulfillment may proceed.
- **Payment Operation** — A business-level payment intent or action, identified by a payment operation ID and idempotency key.
- **Idempotency Key** — A stable client- or business-generated key used to ensure repeated requests refer to the same payment operation.
- **Payment Verification Required** — The state entered when a provider cannot confirm payment success or the transaction status is uncertain.
- **Fulfillment Task** — A unit of work representing a shipment, dispatch action, or delivery-related action for an order.
- **Shipment Creation** — The first fulfillment event that marks an order as ready for dispatch.
- **Shipment Dispatch** — The event indicating that goods have been handed off to the carrier or fulfillment partner.
- **Delivery Completion** — The final completion event that closes the order as FULFILLED.
- **Reconciliation Required** — The state entered when a payment or dependency issue remains unresolved beyond the configured review window.
- **Simulated Payment Provider** — The non-production provider used for demo and validation, not a live financial gateway.
- **Duplicate Event** — An event or callback that repeats the same transaction or provider outcome and must be rejected or deduplicated.

## 3.1 State Transition Tables

### Order lifecycle

| Current state | Valid next states | Invalid transitions | Notes |
| --- | --- | --- | --- |
| CREATED | VALIDATED, CANCELLED, FAILED | FULFILLED, PAID | Initial order record prior to validation |
| VALIDATED | RESERVED, CANCELLED, FAILED | FULFILLED, PAYMENT_PENDING without reservation | Validation passes and suitability is checked |
| RESERVED | PAYMENT_PENDING, CANCELLED, FAILED | PAID without payment start | Inventory is held while payment is decided |
| PAYMENT_PENDING | PAID, PAYMENT_VERIFICATION_REQUIRED, CANCELLED, FAILED | FULFILLED | Payment is being processed or awaiting confirmation |
| PAYMENT_VERIFICATION_REQUIRED | PAID, FAILED, RECONCILIATION_REQUIRED, CANCELLED | FULFILLED | Provider status is uncertain or unreachable; timeout does not imply failure |
| PAID | FULFILLMENT_PENDING, CANCELLED, RECONCILIATION_REQUIRED | FAILED | Payment is confirmed captured |
| FULFILLMENT_PENDING | DISPATCHED, CANCELLED, RECONCILIATION_REQUIRED | FULFILLED before completion | Shipment preparation or handoff is in progress |
| DISPATCHED | DELIVERED, CANCELLED, RECONCILIATION_REQUIRED | PAID, CREATED | Shipment is in transit |
| DELIVERED | FULFILLED | PAID, CREATED | Final completion event |
| CANCELLED | none | any active state after cancellation | Terminal for the order unless reopened via exceptions |
| FAILED | none | any active state | Terminal failure state |
| RECONCILIATION_REQUIRED | PAID, FAILED, CANCELLED | FULFILLED without resolution | Awaiting human or operational decision; unresolved payment beyond timeout releases inventory |
| FULFILLED | none | any other active state | Final terminal state |

### Inventory lifecycle

| Inventory state | Meaning | Allowed transitions |
| --- | --- | --- |
| AVAILABLE | Stock is free for allocation | RESERVED |
| RESERVED | Stock held for an active order | COMMITTED, RELEASED, EXPIRED |
| COMMITTED | Stock tied to a confirmed payment and fulfillment-ready order | RELEASED, DISPATCHED, CANCELLED |
| RELEASED | Stock released after failure, cancellation, or expiry | AVAILABLE |
| EXPIRED | Reservation expired after timeout | RELEASED, AVAILABLE |

### Payment lifecycle

| Payment state | Meaning | Allowed transitions |
| --- | --- | --- |
| CREATED | Payment operation initialized | INITIATED, FAILED |
| INITIATED | Provider request is being made | PENDING, FAILED, PAYMENT_VERIFICATION_REQUIRED |
| PENDING | Provider accepted the request but result is not yet confirmed | CAPTURED, FAILED, PAYMENT_VERIFICATION_REQUIRED |
| PAYMENT_VERIFICATION_REQUIRED | Provider status is uncertain or unreachable | CAPTURED, FAILED, RECONCILIATION_REQUIRED |
| CAPTURED | Payment confirmed and final for this operation | RECONCILIATION_REQUIRED |
| FAILED | Payment definitively failed and retry eligibility is checked | RECONCILIATION_REQUIRED |
| RECONCILIATION_REQUIRED | Human or operational review required | CAPTURED, FAILED, CANCELLED |
| CANCELLED | Payment operation cancelled by business rule | none |

Note: A retry after FAILED must create a brand-new payment operation with a new operation ID and new idempotency key; the original failed operation is never resumed or retried.

### Fulfillment lifecycle

| Fulfillment state | Meaning | Allowed transitions |
| --- | --- | --- |
| PENDING | Order is ready for fulfillment work | CREATED, CANCELLED, RECONCILIATION_REQUIRED |
| SHIPMENT_CREATED | Shipment ready for dispatch | DISPATCHED, CANCELLED |
| DISPATCHED | Goods handed to transport partner | DELIVERED, CANCELLED |
| DELIVERED | Goods arrived at destination | FULFILLED |
| FULFILLED | Final completion event reached | none |
| CANCELLED | Fulfillment cancelled | none |
| RECONCILIATION_REQUIRED | Fulfillment uncertain or blocked | SHIPMENT_CREATED, CANCELLED |

## 4. Features

### 4.1 Product and Pricing Validation
**Description:** The system must maintain a reliable product catalog and validate product, price, and availability conditions before an order is allowed to move forward. This feature realizes UJ-1 and supports all downstream flows. It is a core product and order validation boundary.

**Functional Requirements:**

#### FR-1: Product catalog values are available and valid
A Product can be created or updated only with required identifiers, price data, and product metadata needed for ordering and display. Realizes UJ-1.

**Consequences (testable):**
- A product without a valid identifier or pricing metadata cannot be accepted for order creation.
- The catalog returns the current price and availability state for a product.

#### FR-2: Product availability is checked before order acceptance
The system must validate product availability and the requested quantity before the order progresses to inventory reservation or payment. Realizes UJ-1.

**Consequences (testable):**
- If the quantity requested exceeds available stock, the system rejects the order step or marks it for review.
- Availability checks are re-evaluated for order validation when the order changes.

#### FR-3: Price changes are treated as order-level decisions
The system must capture the effective price at order creation and treat price mismatches as validation issues rather than silently updating the order in place. Realizes UJ-1.

**Consequences (testable):**
- An order retains its validated price for the payment flow.
- Price changes after order creation do not silently alter a paid or reserved order.

**Notes:**
- `[ASSUMPTION: Product pricing can be represented as a simple catalog price and validated at checkout without complex discount logic in the MVP.]`

### 4.2 Order Creation and Validation
**Description:** Orders are created as first-class business records with explicit state transitions and a clear lifecycle. This feature realizes UJ-1 and UJ-2.

**Functional Requirements:**

#### FR-4: Orders are created only with valid item and quantity data
An Order can be created only when the request contains valid items, quantities, and a valid order context. Realizes UJ-1.

**Consequences (testable):**
- A zero-quantity or malformed order is rejected before it enters the lifecycle.
- The system records the order creation event and initial state.

#### FR-5: Orders enter a controlled lifecycle with explicit states
The system must maintain an explicit order lifecycle and prevent silent state jumps. Realizes UJ-1, UJ-2, UJ-4.

**Consequences (testable):**
- An order cannot move directly from CREATED to FULFILLED without passing through valid intermediate states.
- The system stores the current state and a timeline of transitions.

#### FR-6: Orders are rejected or paused when critical validation fails
If product validation, inventory validation, or payment prerequisites fail, the order must not continue as if it were valid. Realizes UJ-1, UJ-2.

**Consequences (testable):**
- Invalid orders are rejected or moved to a non-advancing state.
- The reason for blockage is persisted and visible to operations and support.

**Notes:**
- `[NOTE FOR PM: Whether customer-visible order states are limited or expanded should be decided before public launch.]`

### 4.3 Inventory Reservation and Release
**Description:** The OMS must protect inventory from oversell with a reservation model. The system reserves stock for valid orders and releases it when the order is cancelled, expires, or cannot continue. This feature realizes UJ-1 and UJ-2.

**Functional Requirements:**

#### FR-7: Inventory is reserved when an order is valid to proceed
The system must reserve stock for an order when the order passes initial validation and is ready for payment or commitment. Realizes UJ-1, UJ-2.

**Consequences (testable):**
- Inventory is deducted from available stock once the reservation is created.
- A second valid order cannot consume the same reserved stock unless the reservation is released or expired.

#### FR-8: Inventory is released on cancellation or failed validation
Reserved inventory must be released when an order is cancelled, rejected, timed out, or cannot proceed. Realizes UJ-1, UJ-2.

**Consequences (testable):**
- Inventory is restored when the order exits a valid business path.
- No reserved stock remains held indefinitely for a cancelled or failed order.
- A cancelled order cannot keep inventory reserved beyond the configured release window.

#### FR-9: Inventory expiry is configurable and enforced
The system must support configurable inventory expiry and ensure that reserved stock expires after the configured timeout window when an order remains unresolved. Realizes UJ-2.

**Consequences (testable):**
- Expired reservations are released automatically or by scheduled processing.
- An expired reservation triggers an order state change or administrative review.
- Inventory is not automatically committed to an order while payment remains uncertain.

#### FR-9A: Uncertain payment holds inventory only temporarily
When payment is uncertain, inventory may remain reserved only until the configured timeout window, then it is released or escalated. Realizes UJ-2, UJ-3.

**Consequences (testable):**
- A payment in PAYMENT_VERIFICATION_REQUIRED does not advance to fulfillment without confirmed payment.
- After the timeout window, the system releases the reservation and moves the order to RECONCILIATION_REQUIRED.
- The MVP inventory policy for unresolved payment beyond the reservation timeout is: release the reservation and mark the order for review; do not auto-commit stock or auto-create a second charge based solely on the timeout.

#### FR-9B: Post-dispatch inventory behavior is controlled and explicit
After shipment dispatch, inventory is treated as committed and should not be released by a routine payment timeout or retry path. Realizes UJ-4.

**Consequences (testable):**
- A payment verification event after dispatch does not silently release dispatched inventory unless a defined exception workflow is in effect.
- Post-dispatch exceptions are routed to reconciliation or operational review rather than automatic stock restoration.

**Notes:**
- `[ASSUMPTION: Exact timeout values are configurable and will be finalized during PRD or implementation planning.]`

### 4.4 Payment Processing and Idempotency
**Description:** The OMS must safely manage payment requests with strong idempotency and strict correctness guarantees. This feature realizes UJ-2 and UJ-3.

**Functional Requirements:**

#### FR-10: Payment operations require a unique idempotency key
Each payment action must be associated with a payment operation ID and idempotency key. Repeated requests with the same key must resolve to the same payment operation. Realizes UJ-2.

**Consequences (testable):**
- A duplicate payment request with the same idempotency key does not create a second payment record.
- The system returns the existing operation status for the repeated request.
- A request with an invalid or missing idempotency payload is rejected before provider submission.

#### FR-11: Only one active payment operation may exist per order
The system must enforce a single active payment operation per order. Realizes UJ-2.

**Consequences (testable):**
- A second payment request with a different idempotency key while a payment is active is rejected as “already in progress.”
- The system prevents concurrent charge attempts from creating separate payment operations.
- Two concurrent requests for the same order and same idempotency key resolve to the same operation and do not create separate attempts.

#### FR-12: Payment capture is only created once and never retried after capture
Once a payment is captured, no subsequent retry may create a new charge for the same order and operation. Realizes UJ-2, UJ-3.

**Consequences (testable):**
- A captured payment remains final in the payment state model.
- A retry flow may verify status or re-route to manual review, but not create another charge.
- A definitive FAILED payment may start a new payment operation only with a new operation ID and new idempotency key; the original failed operation is not restarted.
- A retry after FAILED does not transition back to INITIATED within the same operation; it creates a separate payment operation record.

#### FR-13: Timeout does not imply failure
The system must treat payment timeout or provider uncertainty as an unresolved payment status, not as a positive or negative final outcome. Realizes UJ-2, UJ-3.

**Consequences (testable):**
- A request that times out is re-checked via provider verification before being marked failed.
- The system does not advance the order to a successful paid state from timeout alone.
- When the provider is unreachable, the order is kept in PAYMENT_VERIFICATION_REQUIRED rather than advancing to paid or failed.

#### FR-14: Unresolved payments enter a verification or reconciliation state
When the provider is unreachable or returns uncertain status, the system must transition the payment to a verification state or an escalation state. Realizes UJ-2, UJ-3.

**Consequences (testable):**
- The order is blocked from fulfillment while the payment truth is uncertain.
- A defined manual-review or reconciliation workflow handles unresolved states.
- A duplicate callback or duplicate provider event is rejected or deduplicated without changing state.
- If a late payment success callback arrives after the order has been cancelled, the callback is treated as stale and must not reopen or reinstate the cancelled order without an explicit exception workflow.

#### FR-14A: Duplicate provider events are detected and ignored
The system must deduplicate repeated payment provider callbacks, retries, or webhook events before changing order or payment state.

**Consequences (testable):**
- A provider event with the same event ID or transaction reference is ignored once it has been processed.
- Duplicate events do not create second payment state transitions or duplicate order updates.

#### FR-14B: Idempotency payload validation occurs before provider submission
The system must reject or normalize payment requests that do not include valid idempotency data, inconsistent operation references, or malformed state payloads before the provider call is attempted.

**Consequences (testable):**
- A malformed idempotency key, missing operation reference, or mismatched order amount is rejected with a validation error and not submitted to the provider.
- Invalid payment payloads are treated as validation failures rather than triggering a charge attempt.

**Notes:**
- `[ASSUMPTION: Payment verification and reconciliation will be limited to provider status checks and manual review in the MVP; advanced settlement, refunds, and dispute workflows are deferred.]`

### 4.5 Fulfillment Creation, Dispatch, and Completion
**Description:** Fulfillment begins only when the order is in a valid state. It should track shipment creation, dispatch, and final completion as distinct milestones. This feature realizes UJ-4.

**Functional Requirements:**

#### FR-15: Fulfillment tasks are created only for valid, paid orders
The system must not create fulfillment tasks for orders that are not paid or otherwise not ready for shipment. Realizes UJ-4.

**Consequences (testable):**
- An unpaid or cancelled order cannot move to fulfillment creation.
- Inventory commitment is aligned with fulfillment readiness.

#### FR-16: Shipment creation, dispatch, and delivery completion are distinct events
The system must distinguish shipment creation, dispatch, and final delivery completion as separate fulfillment events. Realizes UJ-4.

**Consequences (testable):**
- An order can be ready for dispatch without being FULFILLED.
- FULFILLED is only reached at the final completion event.

#### FR-17: FULFILLED is the final completion event
The final, stable fulfillment status is FULFILLED, reached only after delivery completion or the agreed equivalent completion event. Realizes UJ-4.

**Consequences (testable):**
- An order in a created or dispatched state is not treated as fully completed.
- The system keeps a timeline of fulfillment events leading to final completion.

### 4.6 Order Exception Handling and Support
**Description:** Unclear payment, expired reservations, and failed fulfillment conditions require a structured support and review path. This feature supports UJ-3 and UJ-4.

**Functional Requirements:**

#### FR-18: Unresolved orders are visible and actionable
The system must surface unresolved payment, inventory, or fulfillment states to operations and support. Realizes UJ-3.

**Consequences (testable):**
- A support agent can view the order timeline and current state.
- A review queue exists for unresolved or timed-out business decisions.
- The order timeline shows payment verification attempts, inventory release events, and final reconciliation state.

#### FR-19: Cancellation releases inventory and preserves auditability
When an order is cancelled, the system must release reserved or committed inventory according to the order state while preserving an auditable record. Realizes UJ-1, UJ-3.

**Consequences (testable):**
- Cancellation prevents the order from continuing into payment or fulfillment.
- The order remains available for support and exception review.
- A cancellation after dispatch is handled as an exception and does not silently release inventory after fulfillment has already completed.

#### FR-20: Refunds are out of scope for the MVP decision flow
The MVP must not create a refund workflow as part of normal order completion. Complex refund logic is explicitly deferred. Realizes UJ-3.

**Consequences (testable):**
- A captured payment does not trigger automated refund processing in the MVP flow.
- The system instead marks the order for review or manual follow-up.

#### FR-21: Concurrency, retries, and failure scenarios have explicit behavior
The system must behave predictably when concurrent requests, duplicate callbacks, retries, and failed dependency calls occur. Realizes UJ-2, UJ-3, UJ-4.

**Consequences (testable):**
- Two identical payment requests with the same idempotency key yield the same result.
- Two different payment requests for the same active order are rejected with “already in progress.”
- A provider timeout does not result in a second charge attempt before verification.
- A definitive FAILED payment can trigger a new payment operation only with a new operation ID and new idempotency key; it cannot restart the original failed operation.
- A failed or expired payment state does not allow fulfillment to proceed.
- A repeated webhook or callback is ignored after first processing.
- A late success callback after cancellation is ignored or routed to exception review and does not reopen the cancelled order automatically.
- When payment remains unresolved beyond the configured timeout, the reservation is released and the order is moved to RECONCILIATION_REQUIRED.

#### FR-22: Security and operational logs are sufficient for review and investigation
The system must keep structured operational records that make payment, inventory, and fulfillment decisions auditable without exposing sensitive secrets. Realizes UJ-3.

**Consequences (testable):**
- Logs or records capture key state transitions without storing raw secrets or full payment tokens.
- Investigators can trace an order through payment uncertainty, inventory release, and final resolution.
- Failed validation and duplicate event events are recorded with enough detail for investigation without exposing sensitive data.

## 5. Non-Goals (Explicit)

- Full financial settlement and refund engine for complex refund scenarios
- Real payment provider integration or live gateway processing
- Advanced fraud detection or risk scoring beyond simple validation rules
- Full ERP or warehouse optimization integration
- Real-time autonomous fulfillment orchestration across large distribution networks
- Customer-facing storefront UX beyond backend orchestration requirements
- Advanced tax, customs, or regional compliance automation beyond initial validation boundaries

## 6. MVP Scope

### 6.1 In Scope
- Product catalog and pricing validation
- Order creation, tracking, and lifecycle transitions
- Inventory reservation, release, expiry, and basic commitment logic
- Payment operation creation, idempotency, and single-active-operation enforcement
- Simulated provider integration and verification-based reconciliation for uncertain payment status
- Fulfillment creation, dispatch, and final completion event handling
- Cancellation and manual review handling for unresolved payment or fulfillment states
- Support-facing order timeline and state auditability

### 6.2 Out of Scope for MVP
- Automated refund processing and settlement workflows
- Advanced warehouse or route optimization
- Cross-border tax and complex compliance automation
- Large-scale external fulfillment integrator orchestration
- Live financial ecosystem integration beyond the simulated provider
- Full customer self-service portal

## 7. Success Metrics

### Primary
- **SM-1: Payment correctness** — Zero duplicate captures across the same order/payment operation. Validates FR-10, FR-11, FR-12, FR-13.
- **SM-2: No uncertain fulfillment** — Zero fulfillment actions advanced from an order with uncertain payment status. Validates FR-14, FR-15, FR-17.
- **SM-3: Inventory safety** — Zero oversell events caused by reservation or expiry logic errors. Validates FR-7, FR-8, FR-9.
- **SM-4: Operational clarity** — Support teams can identify unresolved orders and review the timeline without manual data reconstruction. Validates FR-18, FR-19.

### Secondary
- **SM-5: Normal flow reliability** — 95% or more of standard demo order flows complete without state inconsistency. Validates FR-4, FR-5, FR-7, FR-15.
- **SM-6: Verification efficiency** — Timely completion of payment verification and escalation workflows for uncertain payments. Validates FR-13, FR-14.

### Counter-metrics
- **SM-C1: Do not optimize for speed at the expense of correctness** — Avoid auto-approving uncertain payments or advancing fulfillment before payment validity is confirmed.
- **SM-C2: Do not optimize for throughput at the expense of duplicate prevention** — Avoid duplicate charge attempts or incomplete idempotency handling.

## 8. Open Questions

1. What exact timeout values should be used for inventory reservation expiry and unresolved payment windows during the initial demo? [ASSUMPTION: values are configurable and finalize during PRD/implementation planning.]
2. Should support-visible order states be limited to a small internal set, or should a broader customer-visible set be introduced in a later release?
3. Who should own the manual review queue for unresolved payment states during the MVP: operations, finance, or support?
4. Should fulfillment exceptions be limited to "reconcile and retry" behavior, or should the MVP include explicit cancellation rules for partially created shipments?
5. Should the product backlog explicitly separate refund handling from cancellation handling after the MVP is validated?

## 9. Assumptions Index

- [ASSUMPTION: Product pricing can be represented as a simple catalog price and validated at checkout without complex discount logic in the MVP.] — from §4.1.
- [ASSUMPTION: Exact timeout values are configurable and will be finalized during PRD or implementation planning.] — from §4.3 and §4.4.
- [ASSUMPTION: Payment verification and reconciliation will be limited to provider status checks and manual review in the MVP; advanced settlement, refunds, and dispute workflows are deferred.] — from §4.4.
- [ASSUMPTION: FULFILLED represents the final completion event and is not reached before dispatch or delivery completion.] — from §4.5.
- [NOTE FOR PM: Whether customer-visible order states are limited or expanded should be decided before public launch.] — from §4.2.
- [ASSUMPTION: A malformed payment payload or duplicate callback does not create a financial effect and is treated as validation or deduplication failure.] — from §4.4 and §4.6.

This PRD is intentionally practical, backend-focused, and scoped for a realistic engineering demo. The product decisions above are consistent with the approved brief and keep the system safe, auditable, and operationally understandable without introducing unnecessary complexity.
