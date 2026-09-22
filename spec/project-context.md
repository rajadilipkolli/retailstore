# Project Context

> High-level context for the project: the problem being solved, who it's for, what's in scope, and what constraints apply.

## 1. Vision

The Inventory Management System helps businesses maintain optimal stock levels across their product catalog, streamline supplier interactions, and proactively manage inventory by alerting users when stock falls below critical thresholds. The system enables data-driven decision-making about purchasing and product availability, reducing costs from overstocking or stockouts.

## 2. Users

- **Inventory Manager**: Responsible for monitoring stock levels, managing the product catalog, and reviewing supplier information. Can view all products, update stock quantities, manage suppliers, and review low-stock alerts.
- **Warehouse Staff**: Performs stock counts and updates, receives incoming inventory from suppliers. Can update product stock levels and view product information.
- **Purchasing Manager**: Reviews low-stock alerts and supplier information to place orders and maintain adequate inventory. Can view products, stock status, supplier details, and low-stock alerts.

## 3. Scope

**In Scope:**
- Product catalog with detailed product information (name, SKU, unit cost, category, description)
- Stock tracking with quantity on hand, reorder level, and stock history
- Supplier management with contact information and product associations
- Low-stock alert system that triggers when inventory falls below reorder level
- Dashboard showing current inventory status and alerts

**Out of Scope:**
- External supplier integrations or automated ordering
- Multi-location/warehouse support
- Financial reporting or cost analysis
- User authentication and role-based access control (assume system is internally accessible)

## 4. Constraints

- Single-location inventory system
- Manual stock updates (no barcode scanning or RFID integration)
- In-memory or file-based persistence (no external database requirement at launch)
- Vaadin 24+ framework for UI
- All functionality contained within the application (no external API integrations)

---

# Related Documents

- [Spec README](README.md) — process overview and workflow
- [Architecture](architecture.md) — technology stack and application structure
- [Design System](design-system.md) — theme, component usage, and visual standards
- [Use Case Template](use-cases/use-case-template.md) — template for feature specifications
- [Skills](skills/) — implementation, testing, and visual verification guides
