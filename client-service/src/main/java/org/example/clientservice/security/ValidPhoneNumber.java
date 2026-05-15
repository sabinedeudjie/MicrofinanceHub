package org.example.clientservice.security;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPhoneNumber.PhoneNumberValidator.class)
@Documented
public @interface ValidPhoneNumber {
    
    String message() default "Numéro de téléphone invalide";
    
    Class<?>[] groups() default {};
    
    Class<? extends jakarta.validation.Payload>[] payload() default {};
    
    class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
        
        private static final String PHONE_REGEX = "^\\+?[0-9]{9,15}$";
        
        @Override
        public boolean isValid(String phone, ConstraintValidatorContext context) {
            if (phone == null || phone.isEmpty()) {
                return true; // , laisser @NotBlank gérer
            }
            return phone.matches(PHONE_REGEX);
        }
    }
}