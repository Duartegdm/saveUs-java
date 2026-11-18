package br.com.fiap.saveus.common.erro;

/** Erro de validação de um campo específico, fora do Bean Validation (ex.: CEP no path). Vira 400 com errors[campo]. */
public class DadosInvalidosException extends RuntimeException {

    private final String campo;

    public DadosInvalidosException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
