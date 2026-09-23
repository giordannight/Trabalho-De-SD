package br.ufc.hotelaria.modelo;

public class Hotel extends MeioHospedagem {
    private int estrelas;

    @Override
    public String getTipo() {
        return "Hotel";
    }
}