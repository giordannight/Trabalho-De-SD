package br.ufc.hotelaria.modelo;

public class Pousada extends MeioHospedagem {
    private boolean cafeDaManhaIncluso;

    @Override
    public String getTipo() {
        return "Pousada";
    }
}