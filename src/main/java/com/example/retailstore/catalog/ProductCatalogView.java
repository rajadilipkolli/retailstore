package com.example.retailstore.catalog;

import com.example.retailstore.shared.ui.NavigationPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Shows the catalog to everyone and editing controls to administrators. */
@Route("products")
@RouteAlias("products/new")
@AnonymousAllowed
public class ProductCatalogView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProductService productService;
    private final Grid<Product> grid = new Grid<>(Product.class, false);
    private final VerticalLayout productDetails = new VerticalLayout();
    private final TextField skuField = new TextField("SKU");
    private final TextField nameField = new TextField("Name");
    private final TextField categoryField = new TextField("Category");
    private final TextArea descriptionField = new TextArea("Description");
    private final BigDecimalField unitCostField = new BigDecimalField("Unit Cost");
    private final NumberField reorderLevelField = new NumberField("Reorder Level");
    private final NumberField initialStockField = new NumberField("Initial Stock");
    private final Button saveButton = new Button("Save product");
    private final Button resetButton = new Button("Clear");
    private final Button addProductButton = new Button("Add Product");
    private final BeanValidationBinder<Product> binder = new BeanValidationBinder<>(Product.class);

    /** @param productService service used to list and edit products */
    public ProductCatalogView(ProductService productService) {
        this.productService = productService;
        configureGrid();
        setSpacing(true);
        setPadding(true);
        HorizontalLayout layout = new HorizontalLayout(new NavigationPanel(), content());
        layout.addClassName("catalog-layout");
        add(layout);
        refreshGrid();
    }

    /** Builds the catalog grid and, for administrators, the edit form. */
    private VerticalLayout content() {
        productDetails.addClassName("catalog-details");
        productDetails.setVisible(false);
        VerticalLayout content = new VerticalLayout(new H2("Product Catalog"), grid, productDetails);
        if (isAdmin()) {
            bindFields();
            content.add(toolbar(), createForm());
        }
        content.setWidthFull();
        return content;
    }

    /** Checks whether the current user has the administrator role. */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    /**
     * Loads the requested product into the form, or prepares a new product.
     *
     * @param event the navigation event
     * @param productId identifier of the requested product, if any
     */
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Long productId) {
        if (productId != null) {
            Product product;
            try {
                product = productService.findById(productId);
            } catch (IllegalArgumentException exception) {
                event.rerouteToError(NotFoundException.class);
                return;
            }
            showProductDetails(product);
            if (isAdmin()) {
                binder.setBean(product);
            }
        } else {
            productDetails.setVisible(false);
            if (isAdmin()) {
                binder.setBean(new Product("", "", "", "", BigDecimal.ONE, 1, 0));
            }
        }
    }

    /** Displays the selected product independently of the administrator's edit form. */
    private void showProductDetails(Product product) {
        productDetails.removeAll();
        productDetails.add(
                new H2(product.getName()),
                new Paragraph("SKU: " + product.getSku()),
                new Paragraph("Category: " + product.getCategory()),
                new Paragraph("Description: " + product.getDescription()),
                new Paragraph("Unit cost: " + product.getUnitCost().toPlainString()),
                new Paragraph("Reorder level: " + product.getReorderLevel()),
                new Paragraph("Initial stock: " + product.getInitialStock()));
        productDetails.setVisible(true);
    }

    /** Builds the toolbar action that starts a new product entry. */
    private HorizontalLayout toolbar() {
        addProductButton.addClickListener(click -> {
            binder.setBean(new Product("", "", "", "", BigDecimal.ONE, 1, 0));
            clearForm();
        });
        return new HorizontalLayout(addProductButton);
    }

    /** Builds the product fields and their save and clear actions. */
    private VerticalLayout createForm() {
        HorizontalLayout formLayout = new HorizontalLayout(
                skuField, nameField, categoryField, unitCostField, reorderLevelField, initialStockField);
        formLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        descriptionField.setWidthFull();
        saveButton.addClickListener(click -> saveProduct());
        resetButton.addClickListener(click -> clearForm());

        VerticalLayout form =
                new VerticalLayout(formLayout, descriptionField, new HorizontalLayout(saveButton, resetButton));
        form.setWidthFull();
        return form;
    }

    /** Connects form fields to the selected product's properties. */
    private void bindFields() {
        binder.bind(skuField, Product::getSku, Product::setSku);
        binder.bind(nameField, Product::getName, Product::setName);
        binder.bind(categoryField, Product::getCategory, Product::setCategory);
        binder.bind(descriptionField, Product::getDescription, Product::setDescription);
        binder.bind(unitCostField, Product::getUnitCost, Product::setUnitCost);
        binder.forField(reorderLevelField)
                .withConverter(integerConverter())
                .bind(Product::getReorderLevel, Product::setReorderLevel);
        binder.forField(initialStockField)
                .withConverter(integerConverter())
                .bind(Product::getInitialStock, Product::setInitialStock);
    }

    /** Rejects fractional and out-of-range values before writing an integer property. */
    private Converter<Double, Integer> integerConverter() {
        return new Converter<>() {
            @Override
            public Result<Integer> convertToModel(Double value, ValueContext context) {
                if (value == null
                        || !Double.isFinite(value)
                        || value < Integer.MIN_VALUE
                        || value > Integer.MAX_VALUE
                        || value != Math.rint(value)) {
                    return Result.error("Enter a whole number within the integer range.");
                }
                return Result.ok(value.intValue());
            }

            @Override
            public Double convertToPresentation(Integer value, ValueContext context) {
                return value == null ? 0d : value.doubleValue();
            }
        };
    }

    /** Adds product columns and selects a row for editing. */
    private void configureGrid() {
        grid.addColumn(Product::getSku).setHeader("SKU");
        grid.addColumn(Product::getName).setHeader("Name");
        grid.addColumn(Product::getCategory).setHeader("Category");
        grid.addColumn(Product::getUnitCost).setHeader("Unit Cost");
        grid.asSingleSelect().addValueChangeListener(event -> {
            Product product = event.getValue();
            if (product != null && isAdmin()) {
                binder.setBean(product);
            }
        });
    }

    /** Creates or updates the form's product and reports validation errors. */
    private void saveProduct() {
        if (!binder.validate().isOk()) {
            return;
        }
        Product draft = binder.getBean();
        if (draft == null) {
            draft = new Product("", "", "", "", BigDecimal.ONE, 1, 0);
            binder.setBean(draft);
        }

        try {
            Product saved = draft.getId() == null
                    ? productService.save(
                            draft.getSku(),
                            draft.getName(),
                            draft.getCategory(),
                            draft.getDescription(),
                            draft.getUnitCost(),
                            draft.getReorderLevel(),
                            draft.getInitialStock())
                    : productService.update(
                            draft.getId(),
                            draft.getSku(),
                            draft.getName(),
                            draft.getCategory(),
                            draft.getDescription(),
                            draft.getUnitCost(),
                            draft.getReorderLevel(),
                            draft.getInitialStock());
            Notification.show("Product saved successfully.");
            binder.setBean(saved);
            refreshGrid();
        } catch (IllegalArgumentException exception) {
            Notification.show(exception.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    /** Replaces the form's product with an empty draft. */
    private void clearForm() {
        binder.setBean(new Product("", "", "", "", BigDecimal.ONE, 1, 0));
    }

    /** Reloads the displayed products in SKU order. */
    private void refreshGrid() {
        grid.setItems(productService.listAll());
    }
}
