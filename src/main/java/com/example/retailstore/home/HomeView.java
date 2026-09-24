package com.example.retailstore.home;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route("home")
@PermitAll
public class HomeView extends VerticalLayout {

    /**
     * Creates the authenticated inventory landing page.
     */
    public HomeView() {
        addClassName("home-view");
        add(new H1("Inventory home"), new Paragraph("You are signed in and ready to manage inventory."));
    }
}
