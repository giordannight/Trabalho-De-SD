package br.ufc.hotelaria.modelo;

public abstract class MeioHospedagem {
    private int id;
    private String nome;
    private String endereco;
    private long diariaCentavos;
    private int totalUnidades;

    public abstract String getTipo();
    
    // Getters e Setters
}