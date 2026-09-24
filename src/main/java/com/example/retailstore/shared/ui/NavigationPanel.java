package com.example.retailstore.shared.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/** Links to the inventory areas shown alongside the main content. */
public class NavigationPanel extends VerticalLayout {

    /** Builds the inventory navigation links. */
    public NavigationPanel() {
        addClassName("navigation-panel");
        setSpacing(true);
        setPadding(true);
        add(new H3("Inventory"));
        addLink("Home", "/home");
        addLink("Product Catalog", "/products");
        addLink("Stock Tracking", "/stock");
        addLink("Suppliers", "/suppliers");
        addLink("Low-Stock Alerts", "/alerts");
        addLink("Inventory Dashboard", "/dashboard");
    }

    /** Adds a labeled link to an inventory route. */
    private void addLink(String label, String route) {
        add(new Anchor(route, label));
    }
}
