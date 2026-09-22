# Data Model

> Entity definitions and relationships. Evolves as features are added.

## Entities

| Entity          | Key Fields                                                      | Relationships                                                                        |
|-----------------|-----------------------------------------------------------------|--------------------------------------------------------------------------------------|
| Product         | id, sku, name, category, description, unitCost, reorderLevel    | Has one Stock, Has many StockHistories, Has many Suppliers (through ProductSupplier) |
| Stock           | id, productId, quantityOnHand, lastUpdated                      | Belongs to Product                                                                   |
| StockHistory    | id, productId, quantity, changeType (IN/OUT), reason, timestamp | Belongs to Product                                                                   |
| Supplier        | id, name, contactPerson, email, phone, address, notes           | Has many Products (through ProductSupplier)                                          |
| ProductSupplier | id, productId, supplierId, supplierSku, leadTimeDays            | Links Product to Supplier                                                            |
| LowStockAlert   | id, productId, alertedAt, acknowledged                          | Triggered when Stock.quantityOnHand <= Product.reorderLevel                          |
| UserAccount     | id, email, passwordHash, roles, onboarded                        | Authenticates application users; email is unique                                      |

## Relationships

- **Product** has **one Stock** record tracking current inventory
- **Product** has **many StockHistory** entries recording all quantity changes
- **Product** has **many Suppliers** through the ProductSupplier junction table
- **Supplier** has **many Products** through the ProductSupplier junction table
- **LowStockAlert** references a Product and persists until acknowledged
- **UserAccount** stores registered users in the `user_accounts` table. A user remains unavailable to Spring Security until `onboarded` is true.

## Key Attributes

### Product
- `sku`: Unique identifier for the product
- `name`: Product display name
- `category`: Product category for organization
- `description`: Detailed product information
- `unitCost`: Cost per unit for purchasing
- `reorderLevel`: Minimum quantity threshold for low-stock alerts

### Stock
- `quantityOnHand`: Current stock quantity
- `lastUpdated`: Timestamp of last stock update

### StockHistory
- `changeType`: Either "IN" (receiving) or "OUT" (shipping/usage)
- `reason`: Why the quantity changed (e.g., "Purchase Order", "Customer Sale", "Inventory Adjustment")
- `timestamp`: When the change occurred

### Supplier
- `name`: Supplier company name
- `contactPerson`: Primary contact name
- `email`, `phone`: Communication details
- `address`: Supplier location

### ProductSupplier
- `supplierSku`: The SKU assigned by the supplier for this product
- `leadTimeDays`: Expected delivery time from this supplier

### LowStockAlert
- `alertedAt`: Timestamp when alert was generated
- `acknowledged`: Whether the alert has been reviewed by a user

### UserAccount
- `email`: Unique, normalized login identity
- `passwordHash`: BCrypt password hash; raw passwords are never persisted
- `roles`: Comma-separated role values used to create Spring Security authorities
- `onboarded`: Whether an administrator has approved the account for authentication

## Persistence

- User accounts are persisted in the PostgreSQL table `user_accounts`.
- JPA creates or updates the table from `UserAccount`; the unique email constraint prevents duplicate identities.
- Spring Security calls `AccountService.loadUserByUsername`, which reads the matching `user_accounts` row and rejects rows where `onboarded` is false.
