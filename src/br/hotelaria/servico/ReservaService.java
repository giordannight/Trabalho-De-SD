package br.ufc.hotelaria.servico;

import br.ufc.hotelaria.contrato.Reservas;
import br.ufc.hotelaria.modelo.Reserva;
import java.time.LocalDate;

public class ReservaService implements Reservas {
    private HospedagemService hospedagemService; 

    public synchronized int disponibilidade(int meioId, LocalDate entrada, LocalDate saida) {
        // Implementação
        return 0;
    }

    public long valorReservaCentavos(int id) {
        // Implementação
        return 0L;
    }

    @Override
    public synchronized Reserva registrarReserva(Reserva reserva) {
        // Implementação
        return null;
    }

    @Override
    public Reserva cancelarReserva(int id) {
        // Implementação
        return null;
    }

    @Override
    public Reserva efetivarReserva(int id) {
        // Implementação
        return null;
    }
}