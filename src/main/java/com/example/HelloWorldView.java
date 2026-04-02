package com.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("")
@PageTitle("Hello World")
public class HelloWorldView extends VerticalLayout {

    public HelloWorldView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 heading = new H1("Hello, World!");
        heading.getStyle().set("font-family", "system-ui, sans-serif");

        add(heading);

        UI.getCurrent().getPage().setTitle("Hello World");
    }
}
