package com.example.retailstore.usecases.uc003_manage_suppliers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.suppliers.ProductSupplier;
import com.example.retailstore.suppliers.ProductSupplierRepository;
import com.example.retailstore.suppliers.Supplier;
import com.example.retailstore.suppliers.SupplierDetailView;
import com.example.retailstore.suppliers.SupplierListView;
import com.example.retailstore.suppliers.SupplierRepository;
import com.example.retailstore.suppliers.SupplierService;
import com.example.retailstore.usecases.TestcontainersConfig;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteParameters;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class UC003ManageSuppliersTest extends SpringBrowserlessTest {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductSupplierRepository productSupplierRepository;

    @Autowired
    private ProductService productService;

    @BeforeEach
    void clearData() {
        supplierService.deleteAll();
        productService.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void mainFlow_createEditAndAssociateProducts() {
        Supplier supplier = createSupplier("Northwind Tools", "sales@northwind.test", "555-0100");
        Long supplierId = Objects.requireNonNull(supplier.getId());
        Supplier edited = supplierService.update(
                supplierId,
                "Northwind Tools",
                "Morgan Lee",
                "orders@northwind.test",
                "555-0101",
                "12 Market Street",
                "Call before delivery");
        Product first = createProduct("SKU-101", "Claw Hammer");
        Product second = createProduct("SKU-102", "Tape Measure");

        supplierService.associateProduct(supplierId, Objects.requireNonNull(first.getId()), "NW-HAM-1", 4);
        supplierService.associateProduct(supplierId, Objects.requireNonNull(second.getId()), "NW-TAPE-2", 7);

        assertThat(edited.getContactPerson()).isEqualTo("Morgan Lee");
        assertThat(supplierService.listAll()).extracting(Supplier::getName).containsExactly("Northwind Tools");
        assertThat(supplierService.productsForSupplier(supplierId))
                .extracting(ProductSupplier::getSupplierSku)
                .containsExactly("NW-HAM-1", "NW-TAPE-2");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br01_supplierNameIsRequiredAndUniqueIgnoringCase() {
        createSupplier("Acme Supply", "contact@acme.test", "");

        assertThatThrownBy(() -> createSupplier("  ACME SUPPLY ", "other@acme.test", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unique");
        assertThatThrownBy(() -> supplierService.create(" ", "", "", "555", "", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br02_emailOrPhoneMustBeProvided() {
        assertThatThrownBy(() -> supplierService.create("No Contact", "", "", "", "", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least an email");

        assertThat(supplierService
                        .create("Phone Contact", "", "", "555-0188", "", "")
                        .getPhone())
                .isEqualTo("555-0188");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br03_oneSupplierCanSupplyMultipleProducts() {
        Long supplierId = Objects.requireNonNull(
                createSupplier("Tool Source", "", "555-0110").getId());
        Product first = createProduct("SKU-201", "Pliers");
        Product second = createProduct("SKU-202", "Wrench");

        supplierService.associateProduct(supplierId, Objects.requireNonNull(first.getId()), "TS-PL", 3);
        supplierService.associateProduct(supplierId, Objects.requireNonNull(second.getId()), "TS-WR", 5);

        assertThat(supplierService.productsForSupplier(supplierId)).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br04_oneProductCanHaveMultipleSuppliers() {
        Product product = createProduct("SKU-301", "Socket Set");
        Long productId = Objects.requireNonNull(product.getId());
        Long firstSupplier = Objects.requireNonNull(
                createSupplier("Source One", "one@source.test", "").getId());
        Long secondSupplier = Objects.requireNonNull(
                createSupplier("Source Two", "two@source.test", "").getId());

        supplierService.associateProduct(firstSupplier, productId, "S1-SS", 2);
        supplierService.associateProduct(secondSupplier, productId, "S2-SS", 6);

        assertThat(supplierService.suppliersForProduct(productId)).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br05_leadTimeMustBePositive() {
        Long supplierId = Objects.requireNonNull(
                createSupplier("Fast Parts", "parts@fast.test", "").getId());
        Product product = createProduct("SKU-401", "Utility Knife");

        assertThatThrownBy(() -> supplierService.associateProduct(
                        supplierId, Objects.requireNonNull(product.getId()), "FP-UK", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br06_supplierSkuIsRequired() {
        Long supplierId = Objects.requireNonNull(
                createSupplier("Label Works", "labels@works.test", "").getId());
        Product product = createProduct("SKU-501", "Shipping Label");

        assertThatThrownBy(() ->
                        supplierService.associateProduct(supplierId, Objects.requireNonNull(product.getId()), " ", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supplier SKU");
    }

    @Test
    @WithAnonymousUser
    void br07_supplierWritesRequireAdministrator() {
        assertThatThrownBy(() -> createSupplier("Unauthorized", "unauthorized@test.test", ""))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithAnonymousUser
    void acceptance_publicSupplierListCanSearchByNameAndContact() {
        supplierRepository.save(new Supplier("Harbor Supply", "Riley", "harbor@test.test", "555-0120", "", ""));
        supplierRepository.save(new Supplier("Metro Parts", "Casey", "metro@test.test", "555-0130", "", ""));
        navigate(SupplierListView.class);

        TextField search = find(TextField.class).single();
        List<Grid> grids = find(Grid.class).all();
        Grid<?> supplierGrid = grids.getFirst();
        assertThat(supplierGrid.getListDataView().getItemCount()).isEqualTo(2);
        search.setValue("harbor");
        assertThat(supplierGrid.getListDataView().getItemCount()).isEqualTo(1);
        search.setValue("casey");
        assertThat(supplierGrid.getListDataView().getItemCount()).isEqualTo(1);
    }

    @Test
    @WithAnonymousUser
    void acceptance_publicSupplierDetailShowsAssociatedProducts() {
        Supplier supplier = supplierRepository.save(
                new Supplier("Harbor Hardware", "Riley", "harbor@test.test", "555-0120", "", ""));
        Product product = createProduct("SKU-601", "Adjustable Wrench");
        Long supplierId = Objects.requireNonNull(supplier.getId());
        productSupplierRepository.save(
                new ProductSupplier(supplierId, Objects.requireNonNull(product.getId()), "HH-WR-6", 5));

        UI.getCurrent().navigate(SupplierDetailView.class, new RouteParameters("supplierId", supplierId.toString()));

        assertThat(find(Grid.class).single().getListDataView().getItemCount()).isEqualTo(1);
    }

    private Supplier createSupplier(String name, String email, String phone) {
        return supplierService.create(name, "", email, phone, "", "");
    }

    private Product createProduct(String sku, String name) {
        return productService.save(sku, name, "Hardware", "Inventory item", new BigDecimal("2.50"), 1, 4);
    }
}
