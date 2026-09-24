package com.example.retailstore;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(Application.class);

    @Test
    void verifiesModularStructure() {
        MODULES.verify();
        new Documenter(MODULES).writeDocumentation();
    }
}
