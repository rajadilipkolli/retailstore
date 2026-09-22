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

## Relationships

- **Product** has **one Stock** record tracking current inventory
- **Product** has **many StockHistory** entries recording all quantity changes
- **Product** has **many Suppliers** through the ProductSupplier junction table
- **Supplier** has **many Products** through the ProductSupplier junction table
- **LowStockAlert** references a Product and persists until acknowledged

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
