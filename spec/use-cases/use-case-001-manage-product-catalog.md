# UC-001: Manage Product Catalog

> Inventory managers maintain the product catalog by creating, viewing, and updating product information.

---

**As a** Inventory Manager, **I want to** create and manage product information **so that** the system maintains an accurate catalog of all items in inventory.

**Status:** Pending
**Date:** 2024-01-01

---

## Main Flow

- I navigate to the Product Catalog page
- I see a list of all products with columns for SKU, Name, Category, and Unit Cost
- I click "Add Product" to create a new product
- I enter product details: SKU, name, category, description, unit cost, and reorder level
- I click Save and the product is added to the catalog
- The system shows a success message and returns me to the product list
- I can click on any product to view or edit its details

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | SKU must be unique across all products |
| BR-02 | Product name and SKU are mandatory fields |
| BR-03 | Unit cost and reorder level must be positive numbers |
| BR-04 | Reorder level must be less than or equal to initial stock quantity |
| BR-05 | Products can be edited but not deleted (archived instead if needed) |

---

## Acceptance Criteria

- [ ] User can create a new product with all required fields
- [ ] SKU uniqueness is validated on save
- [ ] User can view a list of all products with key information
- [ ] User can click a product to open a detail view for editing
- [ ] User can save changes to existing product information
- [ ] Validation errors are displayed clearly to the user
- [ ] Success feedback is shown after creating or updating a product

---

## Tests

- [ ] `ManageProductCatalogTest` — Create, edit, and view product details
- [ ] `ProductValidationTest` — SKU uniqueness and required field validation
- [ ] `ProductListTest` — Product list display and pagination

---

## Navigation

Authenticated users reach the catalog through the shared navigation panel on `/home`. The panel links to Home, Product Catalog, Stock Tracking, Suppliers, Low-Stock Alerts, and Inventory Dashboard.

The catalog list and detail views are publicly readable. Only users with the `ADMIN` role see the `Add Product`, editable fields, and `Save product` controls.

## UI / Routes

The Product Catalog interface provides a main list view with the ability to create and edit products.

| Route | Access | Notes |
|-------|--------|-------|
| `/products` | public | List all products with search and filter options; editing controls are visible only to `ADMIN` |
| `/products/:id` | public | View product details; edit controls are visible only to `ADMIN` |
| `/products/new` | public | Create a new product; form is visible only to `ADMIN` |
