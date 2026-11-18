package br.com.fiap.saveus.common.erro;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converte toda exceção em ProblemDetail (application/problem+json), no formato combinado com o app:
 * title e detail para a mensagem, errors (campo em camelCase → mensagens) para marcar o formulário.
 */
@RestControllerAdvice
public class TratamentoGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratamentoGlobalDeErros.class);

    static final String MSG_DATA = "Data em formato inválido. Use yyyy-MM-dd, como 2026-11-15.";
    static final String MSG_HORA = "Horário em formato inválido. Use HH:mm, como 08:00.";
    static final String MSG_INTEIRO = "Informe um número inteiro.";
    static final String MSG_BOOLEANO = "Use true ou false.";
    static final String MSG_FORMATO = "Valor em formato inválido.";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> tratarApiException(ApiException ex) {
        return responder(Problemas.criar(ex.getStatus(), ex.getTitulo(), ex.getMessage()));
    }

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ProblemDetail> tratarDadosInvalidos(DadosInvalidosException ex) {
        return responder(Problemas.validacao(ex.getCampo(), ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> tratarConstraintViolation(ConstraintViolationException ex) {
        Map<String, List<String>> erros = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String caminho = v.getPropertyPath().toString();
            String campo = caminho.contains(".") ? caminho.substring(caminho.lastIndexOf('.') + 1) : caminho;
            erros.computeIfAbsent(campo, c -> new ArrayList<>()).add(v.getMessage());
        });
        return responder(Problemas.validacao(erros));
    }

    /** Qualquer outra exceção: 500 sem expor stack trace nem mensagem interna. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> tratarInesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return responder(Problemas.criar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente em instantes."));
    }

    /** Bean Validation no corpo (@Valid @RequestBody). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, List<String>> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> adicionar(erros, e.getField(), e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> adicionar(erros, "corpo", e.getDefaultMessage()));
        return responderObjeto(Problemas.validacao(erros));
    }

    /** Bean Validation em @RequestParam / @PathVariable (ex.: pagina, tamanho). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, List<String>> erros = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> {
            if (resultado instanceof ParameterErrors errosDoObjeto) {
                errosDoObjeto.getFieldErrors().forEach(e -> adicionar(erros, e.getField(), e.getDefaultMessage()));
                return;
            }
            String nome = resultado.getMethodParameter().getParameterName();
            resultado.getResolvableErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .forEach(m -> adicionar(erros, nome == null ? "parametro" : nome, m));
        });
        return responderObjeto(Problemas.validacao(erros));
    }

    /** JSON malformado ou com tipo errado ("capacidade": "20", "tipo": "XYZ", "abre": "8h"). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Throwable causa = ex.getCause();
        if (causa instanceof JsonMappingException mapeamento && !mapeamento.getPath().isEmpty()) {
            String campo = mapeamento.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(n -> n != null)
                    .collect(Collectors.joining("."));
            return responderObjeto(Problemas.validacao(campo.isBlank() ? "corpo" : campo, MSG_FORMATO));
        }
        if (causa instanceof JsonProcessingException) {
            return responderObjeto(Problemas.validacao("corpo", "O corpo da requisição não é um JSON válido."));
        }
        return responderObjeto(Problemas.validacao("corpo", "Envie o corpo da requisição em JSON."));
    }

    /** Query param ou path variable que não converte (data=15-11-2026, pagina=abc, ativo=talvez). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            @NonNull TypeMismatchException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        String campo = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        return responderObjeto(Problemas.validacao(campo == null ? "parametro" : campo, mensagemDoTipo(ex.getRequiredType())));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            @NonNull MissingServletRequestParameterException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        return responderObjeto(Problemas.validacao(ex.getParameterName(), "Informe este parâmetro."));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            @NonNull HttpMediaTypeNotSupportedException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        return responderObjeto(Problemas.criar(status, "Requisição inválida",
                "Envie o corpo com Content-Type: application/json."));
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            @NonNull NoResourceFoundException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        return responderObjeto(Problemas.criar(HttpStatus.NOT_FOUND, "Recurso não encontrado",
                "O endereço pedido não existe."));
    }

    /** Demais exceções do Spring MVC (405, 406, 413...): mesmo formato, com título em português. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            @NonNull Exception ex, Object body, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode, @NonNull WebRequest request) {
        String detalhe = statusCode.value() == 405
                ? "Este método HTTP não é aceito neste endereço."
                : "A requisição não pôde ser processada.";
        ProblemDetail problema = Problemas.criar(statusCode, Problemas.tituloPadrao(statusCode.value()), detalhe);
        return ResponseEntity.status(statusCode).headers(headers).body(problema);
    }

    private static String mensagemDoTipo(Class<?> tipo) {
        if (tipo == null) return MSG_FORMATO;
        if (LocalDate.class.equals(tipo)) return MSG_DATA;
        if (LocalTime.class.equals(tipo)) return MSG_HORA;
        if (Boolean.class.equals(tipo) || boolean.class.equals(tipo)) return MSG_BOOLEANO;
        if (Integer.class.equals(tipo) || int.class.equals(tipo) || Long.class.equals(tipo) || long.class.equals(tipo)) {
            return MSG_INTEIRO;
        }
        return MSG_FORMATO;
    }

    private static void adicionar(Map<String, List<String>> erros, String campo, String mensagem) {
        List<String> mensagens = erros.computeIfAbsent(campo, c -> new ArrayList<>());
        if (mensagem != null && !mensagens.contains(mensagem)) {
            mensagens.add(mensagem);
        }
    }

    private static ResponseEntity<ProblemDetail> responder(ProblemDetail problema) {
        return ResponseEntity.status(problema.getStatus()).body(problema);
    }

    private static ResponseEntity<Object> responderObjeto(ProblemDetail problema) {
        return ResponseEntity.status(problema.getStatus()).body(problema);
    }

}
