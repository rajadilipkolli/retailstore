package com.example.retailstore;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.core.domain.JavaModifier.PRIVATE;
import static com.tngtech.archunit.core.domain.JavaModifier.STATIC;

import com.enofex.taikai.Taikai;
import com.enofex.taikai.java.ImportsConfigurer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class ArchUnitTests {

    private static final String BASE_PACKAGE = "com.example.retailstore";

    @Test
    void shouldFulfillConstraints() {
        Taikai.builder()
                .namespace(BASE_PACKAGE)
                .java(java -> java.utilityClassesShouldBeFinalAndHavePrivateConstructor()
                        .imports(ImportsConfigurer::shouldHaveNoCycles)
                        .naming(naming ->
                                naming.constantsShouldFollowConventions().interfacesShouldNotHavePrefixI()))
                .logging(logging ->
                        logging.loggersShouldFollowConventions(Logger.class, "LOG", List.of(PRIVATE, STATIC, FINAL)))
                .spring(spring -> spring.noAutowiredFields()
                        .boot(boot -> boot.applicationClassShouldResideInPackage(BASE_PACKAGE))
                        .configurations(configurations -> configurations.namesShouldMatch(".+(Config|Configuration)"))
                        .services(services ->
                                services.shouldBeAnnotatedWithService().shouldNotDependOnControllers())
                        .repositories(repositories ->
                                repositories.shouldNotDependOnServices().namesShouldEndWithRepository()))
                .build()
                .checkAll();
    }
}
