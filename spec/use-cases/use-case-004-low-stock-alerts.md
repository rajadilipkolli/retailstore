# UC-004: Manage Low-Stock Alerts

> The system automatically generates alerts when product stock falls below reorder level, enabling proactive procurement.

---

**As a** Purchasing Manager, **I want to** see which products have fallen below their reorder level **so that** I can initiate purchase orders before stockouts occur.

**Status:** Pending
**Date:** 2024-01-01

---

## Main Flow

- I navigate to the Low-Stock Alerts page
- The system displays all products currently below their reorder level
- Each alert shows: Product name, SKU, current quantity, reorder level, and last stock update
- I can click on an alert to see supplier options and suggested suppliers for ordering
- I acknowledge the alert after reviewing it
- Acknowledged alerts move to a separate view but can be retrieved for reference
- I can filter alerts by status (unacknowledged, all, or by date)
- I can re-open an acknowledged alert if stock hasn't been replenished

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | An alert is triggered automatically when quantityOnHand <= reorderLevel |
| BR-02 | Only unacknowledged alerts are shown in the main alert list by default |
| BR-03 | An alert remains until stock is replenished above the reorder level |
| BR-04 | Acknowledging an alert only marks it as reviewed, it does not resolve it |
| BR-05 | Alert list is sorted by date (most recent first) |
| BR-06 | Alert should suggest the supplier with the shortest lead time |

---

## Acceptance Criteria

- [ ] Unacknowledged low-stock alerts are displayed prominently
- [ ] Each alert shows product details and stock information
- [ ] Alert list is sortable by product name, current quantity, and date
- [ ] User can acknowledge an alert to mark it as reviewed
- [ ] User can view acknowledged alerts in a separate section
- [ ] Alerts are automatically cleared when stock is replenished above reorder level
- [ ] Suggested suppliers are shown with lead times
- [ ] Alert count badge is displayed in navigation (optional but recommended)

---

## Tests

- [ ] `LowStockAlertTest` — Alert generation when stock falls below reorder level
- [ ] `AlertAcknowledgmentTest` — Acknowledge and view alert status
- [ ] `AlertClearanceTest` — Verify alerts clear when stock is replenished
- [ ] `AlertSortingTest` — Sort alerts by various criteria
- [ ] `AlertSupplierSuggestionTest` — Verify correct supplier suggestions by lead time

---

## Navigation

Authenticated users reach Low-Stock Alerts through the shared navigation panel on `/home`. The panel links to Home, Product Catalog, Stock Tracking, Suppliers, Low-Stock Alerts, and Inventory Dashboard. The active alert count may be shown as a badge on the Low-Stock Alerts link.

Alert acknowledgement controls are restricted to users with the `ADMIN` role.

## UI / Routes

The Low-Stock Alerts interface displays active alerts and allows users to review and acknowledge them.

| Route | Access | Notes |
|-------|--------|-------|
| `/alerts` | public | View all unacknowledged low-stock alerts |
| `/alerts/history` | public | View acknowledged and historical alerts |
| `/alerts/:id` | public | View alert details with supplier options |
| `/alerts/:id/acknowledge` | public | Mark alert as acknowledged |
