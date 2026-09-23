package br.ufc.hotelaria.modelo;

import java.time.LocalDate;

public class Reserva {
    public enum Status {
        PENDENTE, EFETIVADA, CANCELADA
    }

    private int id;
    private int meioHospedagemId;
    private Hospede hospede; 
    private LocalDate entrada;
    private LocalDate saida;
    private int unidades;
    private Status status;

    public void efetivar() {
        // Implementação
    }

    public void cancelar() {
        // Implementação
    }
    
    // Getters e Setters
}