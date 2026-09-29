package com.example.retailstore.suppliers;

import com.example.retailstore.shared.persistence.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/** A supplier and its purchasing contact information. */
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

    @Id
    @Tsid
    private @Nullable Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "contact_person", nullable = false, length = 200)
    private String contactPerson;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(nullable = false, length = 1000)
    private String address;

    @Column(nullable = false, length = 2000)
    private String notes;

    protected Supplier() {}

    /** Creates a supplier with validated details. */
    public Supplier(String name, String contactPerson, String email, String phone, String address, String notes) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.notes = notes;
    }

    /** @return the generated supplier identifier */
    public @Nullable Long getId() {
        return id;
    }

    /** @return the supplier's unique name */
    public String getName() {
        return name;
    }

    /** @return the main contact person */
    public String getContactPerson() {
        return contactPerson;
    }

    /** @return the email address, or an empty string when not supplied */
    public String getEmail() {
        return email;
    }

    /** @return the phone number, or an empty string when not supplied */
    public String getPhone() {
        return phone;
    }

    /** @return the supplier's postal address */
    public String getAddress() {
        return address;
    }

    /** @return purchasing notes */
    public String getNotes() {
        return notes;
    }

    /** Replaces supplier details after service validation. */
    void updateDetails(String name, String contactPerson, String email, String phone, String address, String notes) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.notes = notes;
    }
}
