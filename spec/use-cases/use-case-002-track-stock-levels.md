# UC-002: Track Stock Levels

> Warehouse staff and inventory managers update and monitor current stock quantities for each product.

---

**As a** Warehouse Staff, **I want to** update stock quantities when receiving or using inventory **so that** the system always reflects accurate stock levels.

**Status:** Pending
**Date:** 2024-01-01

---

## Main Flow

- I navigate to the Stock Tracking page
- I see a list of products with their current stock quantities and reorder levels
- I search for a specific product by SKU or name
- I find the product I need to update and click on it
- I enter the quantity change: I specify whether it's an incoming shipment or outage
- I enter the reason for the change (e.g., "Purchase Order PO-123", "Customer Order SO-456")
- I click Submit and the stock is updated
- The system shows the new quantity and logs the change to history
- The product's last updated timestamp is recorded

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Stock quantity cannot be negative |
| BR-02 | All stock changes must have a reason/reference |
| BR-03 | Stock history is immutable (changes cannot be deleted, only new entries added) |
| BR-04 | Change type is either IN (receiving) or OUT (usage/shipment) |
| BR-05 | Quantity on hand is updated in real-time when changes are recorded |

---

## Acceptance Criteria

- [ ] User can view current stock quantity for all products
- [ ] User can record an incoming stock adjustment (IN)
- [ ] User can record an outgoing stock adjustment (OUT)
- [ ] Stock changes require a reason/reference text
- [ ] Quantity cannot be changed to a negative value
- [ ] Stock history is visible showing all past changes with timestamp and reason
- [ ] Last updated timestamp reflects when the stock was last changed
- [ ] Search/filter works by SKU and product name

---

## Tests

- [ ] `TrackStockTest` — Update stock IN and OUT transactions
- [ ] `StockValidationTest` — Negative quantity and required reason validation
- [ ] `StockHistoryTest` — Verify history entries are logged correctly
- [ ] `StockSearchTest` — Search by SKU and product name

---

## Navigation

Authenticated users reach Stock Tracking through the shared navigation panel on `/home`. The panel links to Home, Product Catalog, Stock Tracking, Suppliers, Low-Stock Alerts, and Inventory Dashboard.

Stock information is publicly readable. Any stock adjustment controls are restricted to authenticated users with the `ADMIN` role until a dedicated warehouse-staff permission is implemented.

## UI / Routes

The Stock Tracking interface provides a product list with stock information and an interface to record changes.

| Route | Access | Notes |
|-------|--------|-------|
| `/stock` | public | View all products with current stock levels |
| `/stock/:productId` | public | View product stock detail and history |
| `/stock/:productId/adjust` | public | Record a stock adjustment |
