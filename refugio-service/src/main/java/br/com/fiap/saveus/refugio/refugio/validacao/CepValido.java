package br.com.fiap.saveus.refugio.refugio.validacao;

import br.com.fiap.saveus.refugio.common.CepFormato;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 8 dígitos, com ou sem hífen na 6ª posição. Vazio fica por conta do @NotBlank, para sair uma mensagem só. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CepValido.Validador.class)
public @interface CepValido {

    String message() default CepFormato.MENSAGEM;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<CepValido, String> {
        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            return valor == null || valor.isBlank() || CepFormato.ehValido(valor);
        }
    }
}
