package com.example.context;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.example.MainLayout;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "context", layout = MainLayout.class)
@PageTitle("AI Context")
public class ContextView extends VerticalLayout {

    private final ContextAssemblerService assemblerService;
    private final ObjectMapper mapper;
    private final TextArea output;

    @Autowired
    public ContextView(ContextAssemblerService assemblerService) {
        this.assemblerService = assemblerService;
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.output = new TextArea("Context JSON");

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        Button generate = new Button("Build AI context", event -> build());
        generate.setWidth("160px");

        output.setWidthFull();
        output.setHeight("600px");
        output.setReadOnly(true);
        output.getStyle().set("font-family", "monospace");

        add(generate, output);
        expand(output);
    }

    private void build() {
        try {
            ContextPayload payload = assemblerService.buildPayload();
            output.setValue(mapper.writeValueAsString(payload));
        } catch (Exception ex) {
            Notification.show("Failed to build context: " + ex.getMessage());
        }
    }
}
