package br.ufc.hotelaria.contrato;

import br.ufc.hotelaria.modelo.Reserva;

public interface Reservas {
    Reserva registrarReserva(Reserva reserva);
    Reserva cancelarReserva(int id);
    Reserva efetivarReserva(int id);
}