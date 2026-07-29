package com.training.marketplace.dto;

import com.training.marketplace.dto.request.CreateProductRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductRecommendationMetadataValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void metadata_acceptsBoundedBrandAndAttributes() {
        CreateProductRequest request = new CreateProductRequest(
                "laptop",
                "Laptop",
                "Description",
                1L,
                "Acme",
                Map.of("processor", "X1", "memory", "16GB"),
                "PCS",
                null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void metadata_rejectsBlankAttributeNamesAndValues() {
        CreateProductRequest request = new CreateProductRequest(
                "laptop",
                "Laptop",
                "Description",
                1L,
                "Acme",
                Map.of(" ", "16GB", "processor", " "),
                "PCS",
                null);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getMessage().contains("cannot be blank"));
    }

    @Test
    void metadata_rejectsMoreThanFiftyAttributes() {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (int index = 0; index < 51; index++) {
            attributes.put("attribute-" + index, "value-" + index);
        }
        CreateProductRequest request = new CreateProductRequest(
                "laptop",
                "Laptop",
                "Description",
                1L,
                "Acme",
                attributes,
                "PCS",
                null);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("attributes"));
    }

    @Test
    void legacyConstructor_defaultsAttributesToEmptyMap() {
        CreateProductRequest request =
                new CreateProductRequest("laptop", "Laptop", null, 1L, "PCS", null);

        assertThat(request.brand()).isNull();
        assertThat(request.attributes()).isEmpty();
    }
}
