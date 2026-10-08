package com.wildlife.wildlife_conservationbackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class Utf8SizeValidator implements ConstraintValidator<Utf8Size, CharSequence> {
    private int max;

    @Override
    public void initialize(Utf8Size constraint) {
        max = constraint.max();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || value.toString().getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
