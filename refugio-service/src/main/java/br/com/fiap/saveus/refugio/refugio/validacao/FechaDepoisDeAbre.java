package br.com.fiap.saveus.refugio.refugio.validacao;

import br.com.fiap.saveus.refugio.refugio.dto.RefugioRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validação de classe: fecha depois de abre. O erro é ligado ao campo "fecha",
 * para sair em errors.fecha e o app marcar o campo certo do formulário.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FechaDepoisDeAbre.Validador.class)
public @interface FechaDepoisDeAbre {

    String message() default "O horário de fechamento deve ser depois da abertura.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<FechaDepoisDeAbre, RefugioRequest> {
        @Override
        public boolean isValid(RefugioRequest r, ConstraintValidatorContext contexto) {
            if (r == null || r.abre() == null || r.fecha() == null || r.fecha().isAfter(r.abre())) {
                return true;
            }
            contexto.disableDefaultConstraintViolation();
            contexto.buildConstraintViolationWithTemplate(contexto.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("fecha")
                    .addConstraintViolation();
            return false;
        }
    }
}
