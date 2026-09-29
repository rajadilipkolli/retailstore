package com.example.retailstore.suppliers;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence queries for suppliers. */
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /** Checks whether a supplier name is already used, ignoring case. */
    boolean existsByNameIgnoreCase(String name);

    /** Checks whether another supplier has the requested name. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Lists suppliers alphabetically by name. */
    List<Supplier> findAllByOrderByNameAsc();

    /** Searches supplier name and contact details without regard to case. */
    List<Supplier>
            findByNameContainingIgnoreCaseOrContactPersonContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrderByNameAsc(
                    String name, String contactPerson, String email, String phone);
}
