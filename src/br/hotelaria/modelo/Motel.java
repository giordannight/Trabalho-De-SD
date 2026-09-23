package br.ufc.hotelaria.modelo;

public class Motel extends MeioHospedagem {
    private boolean garagemPrivativa;

    @Override
    public String getTipo() {
        return "Motel";
    }
}