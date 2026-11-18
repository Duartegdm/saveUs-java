package br.com.fiap.saveus.refugio.auth;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Senha de 8 a 100 caracteres, com maiúscula, minúscula e número.
 * Devolve só a primeira regra que falhar, para o app mostrar uma mensagem clara de cada vez.
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SenhaForte.Validador.class)
public @interface SenhaForte {

    String message() default "A senha não atende aos requisitos.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<SenhaForte, String> {
        @Override
        public boolean isValid(String senha, ConstraintValidatorContext contexto) {
            if (senha == null || senha.isEmpty()) {
                return true; // @NotBlank cuida do vazio
            }
            String erro = null;
            if (senha.length() < 8 || senha.length() > 100) {
                erro = "A senha deve ter entre 8 e 100 caracteres.";
            } else if (senha.chars().noneMatch(c -> c >= 'A' && c <= 'Z')) {
                erro = "A senha deve ter uma letra maiúscula.";
            } else if (senha.chars().noneMatch(c -> c >= 'a' && c <= 'z')) {
                erro = "A senha deve ter uma letra minúscula.";
            } else if (senha.chars().noneMatch(c -> c >= '0' && c <= '9')) {
                erro = "A senha deve ter um número.";
            }
            if (erro == null) {
                return true;
            }
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(erro).addConstraintViolation();
            return false;
        }
    }
}
