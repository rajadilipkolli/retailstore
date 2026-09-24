# UC-003: Manage Suppliers

> Procurement staff maintain supplier information and associate suppliers with products.

---

**As a** Purchasing Manager, **I want to** manage supplier information and link suppliers to products **so that** I can quickly find supplier details when placing orders.

**Status:** Pending
**Date:** 2024-01-01

---

## Main Flow

- I navigate to the Suppliers page
- I see a list of all suppliers with their names and contact information
- I click "Add Supplier" to create a new supplier
- I enter supplier details: name, contact person, email, phone, address, and notes
- I click Save and the supplier is added to the system
- I can then link this supplier to products by specifying their supplier SKU and lead time
- I can click on any supplier to view all products they supply
- I can edit supplier information at any time

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Supplier name is mandatory and must be unique |
| BR-02 | At least one contact method (email or phone) must be provided |
| BR-03 | A supplier can be associated with one or more products |
| BR-04 | A product can have one or more suppliers |
| BR-05 | Lead time is in days and must be a positive number |
| BR-06 | Supplier SKU is the identifier the supplier uses for that product |

---

## Acceptance Criteria

- [ ] User can create a new supplier with required information
- [ ] Supplier name uniqueness is validated
- [ ] User can view a list of all suppliers
- [ ] User can edit supplier information
- [ ] User can associate a supplier with multiple products
- [ ] User can view which products a supplier provides
- [ ] Lead time and supplier SKU can be specified for each product-supplier relationship
- [ ] Contact information validation (at least email or phone)

---

## Tests

- [ ] `ManageSuppliersTest` — Create, edit, and view suppliers
- [ ] `SupplierValidationTest` — Name uniqueness and contact method validation
- [ ] `SupplierProductsTest` — Link and view supplier products
- [ ] `SupplierListTest` — Display supplier list and search

---

## Navigation

Authenticated users reach Supplier Management through the shared navigation panel on `/home`. The panel links to Home, Product Catalog, Stock Tracking, Suppliers, Low-Stock Alerts, and Inventory Dashboard.

Supplier information is publicly readable. Supplier creation and editing controls are restricted to users with the `ADMIN` role.

## UI / Routes

The Supplier Management interface provides supplier list, detail views, and product linking.

| Route | Access | Notes |
|-------|--------|-------|
| `/suppliers` | public | List all suppliers |
| `/suppliers/:id` | public | View supplier details and associated products |
| `/suppliers/:id/edit` | public | Edit supplier information |
| `/suppliers/new` | public | Create a new supplier |
| `/suppliers/:id/products` | public | Manage product associations for a supplier |
