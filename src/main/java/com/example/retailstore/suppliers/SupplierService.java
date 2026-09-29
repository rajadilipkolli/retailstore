package com.example.retailstore.suppliers;

import com.example.retailstore.catalog.ProductCatalogCleared;
import com.example.retailstore.catalog.ProductService;
import java.util.List;
import java.util.Objects;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Validates supplier details and manages supplier-product offers. */
@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final ProductService productService;

    /** Dependencies for supplier and catalog records. */
    public SupplierService(
            SupplierRepository supplierRepository,
            ProductSupplierRepository productSupplierRepository,
            ProductService productService) {
        this.supplierRepository = supplierRepository;
        this.productSupplierRepository = productSupplierRepository;
        this.productService = productService;
    }

    /** @return all suppliers ordered by name */
    @Transactional(readOnly = true)
    public List<Supplier> listAll() {
        return supplierRepository.findAllByOrderByNameAsc();
    }

    /**
     * Searches names, contact people, email addresses, and phone numbers for a case-insensitive substring.
     *
     * @param query search text, trimmed before matching; null or blank returns all suppliers
     * @return matching suppliers ordered by name
     */
    @Transactional(readOnly = true)
    public List<Supplier> search(String query) {
        String term = query == null ? "" : query.trim();
        if (term.isEmpty()) {
            return listAll();
        }
        return supplierRepository
                .findByNameContainingIgnoreCaseOrContactPersonContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrderByNameAsc(
                        term, term, term, term);
    }

    /**
     * Loads one supplier.
     *
     * @throws IllegalArgumentException if the supplier does not exist
     */
    @Transactional(readOnly = true)
    public Supplier findById(Long id) {
        return supplierRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
    }

    /**
     * Creates a supplier with trimmed details and a name unique ignoring case. Requires the ADMIN role.
     * At least one of email or phone must be supplied; other missing contact details become empty strings.
     *
     * @return the saved supplier
     * @throws IllegalArgumentException if the name is blank or taken, both email and phone are blank, a supplied email
     *     is invalid, or a field exceeds its length limit
     * @throws DataIntegrityViolationException if saving violates a constraint other than case-insensitive name uniqueness
     * @throws org.springframework.security.access.AccessDeniedException if the caller lacks the ADMIN role
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Supplier create(
            String name, String contactPerson, String email, String phone, String address, String notes) {
        SupplierDetails details = validate(name, contactPerson, email, phone, address, notes, null);
        return saveSupplier(new Supplier(
                details.name(),
                details.contactPerson(),
                details.email(),
                details.phone(),
                details.address(),
                details.notes()));
    }

    /**
     * Replaces an existing supplier's details using the same normalization and validation as {@link #create}.
     * Requires the ADMIN role.
     *
     * @return the saved supplier
     * @throws IllegalArgumentException if the supplier is missing or the details fail validation
     * @throws DataIntegrityViolationException if saving violates a constraint other than case-insensitive name uniqueness
     * @throws org.springframework.security.access.AccessDeniedException if the caller lacks the ADMIN role
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Supplier update(
            Long id, String name, String contactPerson, String email, String phone, String address, String notes) {
        Supplier supplier = findById(id);
        SupplierDetails details = validate(name, contactPerson, email, phone, address, notes, id);
        supplier.updateDetails(
                details.name(),
                details.contactPerson(),
                details.email(),
                details.phone(),
                details.address(),
                details.notes());
        return saveSupplier(supplier);
    }

    /**
     * Lists a supplier's product offers in product identifier order.
     *
     * @throws IllegalArgumentException if the supplier does not exist
     */
    @Transactional(readOnly = true)
    public List<ProductSupplier> productsForSupplier(Long supplierId) {
        findById(supplierId);
        return productSupplierRepository.findAllBySupplierIdOrderByProductIdAsc(supplierId);
    }

    /**
     * Lists offers for a product in supplier identifier order.
     *
     * @throws IllegalArgumentException if the product does not exist
     */
    @Transactional(readOnly = true)
    public List<ProductSupplier> suppliersForProduct(Long productId) {
        productService.findById(productId);
        return productSupplierRepository.findAllByProductIdOrderBySupplierIdAsc(productId);
    }

    /**
     * Adds or updates a supplier's offer for one product. Requires the ADMIN role.
     *
     * @param supplierSku the supplier's product identifier, nonblank and at most 100 characters after trimming
     * @param leadTimeDays expected delivery time in days, strictly positive
     * @return the saved offer
     * @throws IllegalArgumentException if the supplier or product is missing, or the offer is invalid
     * @throws org.springframework.security.access.AccessDeniedException if the caller lacks the ADMIN role
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProductSupplier associateProduct(Long supplierId, Long productId, String supplierSku, int leadTimeDays) {
        Supplier supplier = findById(supplierId);
        Objects.requireNonNull(supplier.getId());
        productService.findById(productId);
        String normalizedSku = validateOffer(supplierSku, leadTimeDays);
        ProductSupplier offer = productSupplierRepository
                .findBySupplierIdAndProductId(supplierId, productId)
                .orElseGet(() -> new ProductSupplier(supplierId, productId, normalizedSku, leadTimeDays));
        offer.updateOffer(normalizedSku, leadTimeDays);
        return productSupplierRepository.saveAndFlush(offer);
    }

    /**
     * Removes a supplier's association with one product, leaving both records intact.
     * Does nothing if the association is absent. Requires the ADMIN role.
     *
     * @throws IllegalArgumentException if the supplier or product does not exist
     * @throws org.springframework.security.access.AccessDeniedException if the caller lacks the ADMIN role
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void removeProduct(Long supplierId, Long productId) {
        findById(supplierId);
        productService.findById(productId);
        productSupplierRepository.deleteBySupplierIdAndProductId(supplierId, productId);
    }

    /** Removes all product offers and suppliers for isolated integration-test setup. */
    @Transactional
    public void deleteAll() {
        productSupplierRepository.deleteAllInBatch();
        supplierRepository.deleteAllInBatch();
    }

    /** Removes supplier-product links before the catalog deletes referenced products. */
    @EventListener
    @Transactional
    public void productCatalogCleared(ProductCatalogCleared event) {
        productSupplierRepository.deleteAllInBatch();
    }

    /**
     * Returns trimmed details with null optional fields converted to empty strings.
     *
     * @param existingId supplier excluded from the name uniqueness check, or null for a new supplier
     * @throws IllegalArgumentException if required contact details, name uniqueness, email format, or length limits fail
     */
    private SupplierDetails validate(
            String name,
            String contactPerson,
            String email,
            String phone,
            String address,
            String notes,
            @Nullable Long existingId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Supplier name is required.");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > 200) {
            throw new IllegalArgumentException("Supplier name must be at most 200 characters.");
        }
        boolean nameUsed = existingId == null
                ? supplierRepository.existsByNameIgnoreCase(normalizedName)
                : supplierRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, existingId);
        if (nameUsed) {
            throw new IllegalArgumentException("Supplier name must be unique.");
        }

        String normalizedEmail = trimToEmpty(email);
        String normalizedPhone = trimToEmpty(phone);
        if (normalizedEmail.isEmpty() && normalizedPhone.isEmpty()) {
            throw new IllegalArgumentException("Provide at least an email address or phone number.");
        }
        if (!normalizedEmail.isEmpty() && !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }

        String normalizedContact = trimToEmpty(contactPerson);
        String normalizedAddress = trimToEmpty(address);
        String normalizedNotes = trimToEmpty(notes);
        requireMaxLength(normalizedContact, 200, "Contact person");
        requireMaxLength(normalizedEmail, 320, "Email");
        requireMaxLength(normalizedPhone, 50, "Phone");
        requireMaxLength(normalizedAddress, 1000, "Address");
        requireMaxLength(normalizedNotes, 2000, "Notes");
        return new SupplierDetails(
                normalizedName,
                normalizedContact,
                normalizedEmail,
                normalizedPhone,
                normalizedAddress,
                normalizedNotes);
    }

    /**
     * @return the trimmed supplier SKU
     * @throws IllegalArgumentException if the SKU is blank or exceeds 100 characters after trimming, or lead time is not
     *     positive
     */
    private String validateOffer(String supplierSku, int leadTimeDays) {
        if (supplierSku == null || supplierSku.isBlank()) {
            throw new IllegalArgumentException("Supplier SKU is required.");
        }
        String normalizedSku = supplierSku.trim();
        requireMaxLength(normalizedSku, 100, "Supplier SKU");
        if (leadTimeDays <= 0) {
            throw new IllegalArgumentException("Lead time must be a positive number of days.");
        }
        return normalizedSku;
    }

    /**
     * Saves and flushes a supplier, translating the case-insensitive name constraint into a validation error.
     *
     * @throws IllegalArgumentException if the case-insensitive name constraint is violated
     * @throws DataIntegrityViolationException if another integrity constraint is violated
     */
    private Supplier saveSupplier(Supplier supplier) {
        try {
            return supplierRepository.saveAndFlush(supplier);
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException constraint
                        && "uk_suppliers_name_lower".equalsIgnoreCase(constraint.getConstraintName())) {
                    throw new IllegalArgumentException("Supplier name must be unique.", exception);
                }
            }
            throw exception;
        }
    }

    private void requireMaxLength(String value, int maxLength, String label) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(label + " must be at most " + maxLength + " characters.");
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record SupplierDetails(
            String name, String contactPerson, String email, String phone, String address, String notes) {}
}
