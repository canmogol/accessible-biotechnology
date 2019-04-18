package dev.canm.ab.config;

import com.google.common.base.Predicates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

/**
 * Swagger configuration.
 */
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {


    @Value("${api.path}")
    private String api;
    @Value("${swagger.title}")
    private String title;
    @Value("${swagger.description}")
    private String description;
    @Value("${swagger.version}")
    private String version;
    @Value("${swagger.termsUrl}")
    private String termsOfServiceUrl;
    @Value("${swagger.contact.name}")
    private String contactName;
    @Value("${swagger.contact.url}")
    private String contactUrl;
    @Value("${swagger.contact.email}")
    private String contactEmail;
    @Value("${swagger.license.name}")
    private String license;
    @Value("${swagger.license.url}")
    private String licenseUrl;


    /**
     * Creates the Docket bean for configuration.
     *
     * @return Docket
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.any())
            // other then the "Basic Error Controller", basic-error-controller
            .paths(Predicates.not(PathSelectors.regex("/error.*")))
            // other then the "Actuator"
            .paths(Predicates.not(PathSelectors.regex("/actuator.*")))
            .build()
            .apiInfo(apiInfo())
            .useDefaultResponseMessages(false);
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
            title,
            description,
            version,
            termsOfServiceUrl,
            new Contact(contactName, contactUrl, contactEmail),
            license, licenseUrl, Collections.emptyList());
    }

}
