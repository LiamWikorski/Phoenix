package com.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

public class MainLayout extends AppLayout {

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addToNavbar(createHeaderContent());
        addToDrawer(createDrawerContent());
    }

    private Component createHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        H1 title = new H1("Phoenix Dashboard");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)");
        title.getStyle().set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(toggle, title);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);

        return header;
    }

    private Component createDrawerContent() {
        SideNav navigation = new SideNav();
        navigation.addItem(new SideNavItem("Recent Incidents", IncidentsView.class));
        navigation.addItem(new SideNavItem("Incidents per Pod", IncidentsPerPodView.class));
        navigation.addItem(new SideNavItem("GitHub Commits", CommitsView.class));
        return navigation;
    }
}
