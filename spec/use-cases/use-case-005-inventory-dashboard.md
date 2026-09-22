# UC-005: Inventory Dashboard

> Users access a dashboard providing a high-level overview of inventory status, key metrics, and alerts.

---

**As a** Inventory Manager, **I want to** see a dashboard with key inventory metrics and status at a glance **so that** I can quickly assess the health of inventory and identify issues.

**Status:** Pending
**Date:** 2024-01-01

---

## Main Flow

- I log in or open the application
- The dashboard page is displayed as the home/default view
- I see the following sections:
  - Total product count and categories
  - Total stock value (quantity × unit cost sum)
  - Number of low-stock alerts
  - Number of products with zero stock
  - Recent stock transactions (last 10 changes)
  - Top 10 most valuable inventory items (by total value)
- I can click on any metric to drill down to detailed information
- I can click on an alert summary to jump to the alerts page
- Dashboard updates in real-time as stock levels change

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Stock value is calculated as quantityOnHand × unitCost |
| BR-02 | Low-stock count includes only unacknowledged alerts |
| BR-03 | Zero-stock products are highlighted separately |
| BR-04 | Dashboard metrics are recalculated when stock changes |
| BR-05 | Only current data is shown; historical comparisons are not included |

---

## Acceptance Criteria

- [ ] Dashboard displays total product count
- [ ] Dashboard shows total inventory value (sum of all stock × cost)
- [ ] Dashboard shows count of low-stock alerts
- [ ] Dashboard shows count of zero-stock products
- [ ] Recent stock transactions are displayed with timestamp and reason
- [ ] Top inventory value items are ranked and displayed
- [ ] All metrics update when stock is adjusted
- [ ] Clicking metrics navigates to appropriate detail pages
- [ ] Dashboard is responsive and loads quickly

---

## Tests

- [ ] `InventoryDashboardTest` — Verify dashboard displays and calculates metrics correctly
- [ ] `DashboardMetricsTest` — Test metric calculations for stock value and counts
- [ ] `DashboardNavigationTest` — Verify drill-down links work correctly
- [ ] `DashboardUpdateTest` — Verify real-time updates when stock changes

---

## UI / Routes

The dashboard is the main landing page and overview.

| Route | Access | Notes |
|-------|--------|-------|
| `/` | public | Main dashboard with inventory metrics and alerts |
| `/dashboard` | public | Same as root (alternative route) |
