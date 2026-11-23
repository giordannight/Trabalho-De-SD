package br.ufc.hotelaria.api;

public final class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
