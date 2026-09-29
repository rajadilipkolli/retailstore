package com.example.retailstore.stock;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.shared.ui.NavigationPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;

/** ADMIN-only form for recording a stock quantity adjustment. */
@Route("stock/:productId/adjust")
@RolesAllowed("ADMIN")
public class StockAdjustmentView extends VerticalLayout implements BeforeEnterObserver {

    private final ProductService productService;
    private final StockService stockService;
    private final ComboBox<StockChangeType> changeTypeField = new ComboBox<>("Change type");
    private final IntegerField quantityField = new IntegerField("Quantity");
    private final TextField reasonField = new TextField("Reason or reference");
    private final Paragraph productHeading = new Paragraph();
    private final Paragraph currentQuantity = new Paragraph();
    private final Anchor cancelLink = new Anchor("/stock", "Cancel");
    private @Nullable Long productId;

    /** Dependencies for the restricted adjustment form. */
    public StockAdjustmentView(ProductService productService, StockService stockService) {
        this.productService = productService;
        this.stockService = stockService;
        setSpacing(true);
        setPadding(true);
        VerticalLayout form = form();
        HorizontalLayout layout = new HorizontalLayout(new NavigationPanel(), form);
        layout.addClassName("stock-layout");
        layout.setWidthFull();
        layout.setFlexGrow(1, form);
        add(layout);
    }

    private VerticalLayout form() {
        changeTypeField.setItems(Arrays.asList(StockChangeType.values()));
        changeTypeField.setItemLabelGenerator(
                type -> type == StockChangeType.IN ? "IN - Receiving" : "OUT - Usage or shipment");
        changeTypeField.setRequired(true);
        quantityField.setMin(1);
        quantityField.setStepButtonsVisible(true);
        quantityField.setRequiredIndicatorVisible(true);
        reasonField.setMaxLength(500);
        reasonField.setRequiredIndicatorVisible(true);
        Button submitButton = new Button("Submit adjustment");
        submitButton.addClickListener(event -> submit());
        VerticalLayout form = new VerticalLayout(
                new H2("Adjust stock"),
                productHeading,
                currentQuantity,
                changeTypeField,
                quantityField,
                reasonField,
                new HorizontalLayout(submitButton, cancelLink));
        form.setWidthFull();
        return form;
    }

    /** Loads the product and balance, rerouting missing or invalid product identifiers and missing balances to not found. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        try {
            productId = Long.valueOf(event.getRouteParameters()
                    .get("productId")
                    .orElseThrow(() -> new IllegalArgumentException("Product identifier is required.")));
            Product product = productService.findById(productId);
            productHeading.setText(product.getName() + " · " + product.getSku());
            currentQuantity.setText("Current quantity: "
                    + stockService.findByProductId(productId).getQuantityOnHand());
            cancelLink.setHref("/stock/" + productId);
        } catch (IllegalArgumentException exception) {
            event.rerouteToError(NotFoundException.class);
        }
    }

    /**
     * Records a complete adjustment, updates the displayed balance, and clears quantity and reason on success.
     * Shows incomplete input and service validation failures as notifications.
     */
    private void submit() {
        if (productId == null || changeTypeField.isEmpty() || quantityField.isEmpty() || reasonField.isEmpty()) {
            Notification.show("Complete all adjustment fields.", 4000, Notification.Position.MIDDLE);
            return;
        }
        Integer quantity = quantityField.getValue();
        if (quantity == null || quantity <= 0) {
            Notification.show("Quantity must be a positive whole number.", 4000, Notification.Position.MIDDLE);
            return;
        }
        try {
            Stock stock = stockService.adjust(productId, changeTypeField.getValue(), quantity, reasonField.getValue());
            currentQuantity.setText("Current quantity: " + stock.getQuantityOnHand());
            quantityField.clear();
            reasonField.clear();
            Notification.show("Stock adjustment recorded.");
        } catch (IllegalArgumentException exception) {
            Notification.show(exception.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }
}
