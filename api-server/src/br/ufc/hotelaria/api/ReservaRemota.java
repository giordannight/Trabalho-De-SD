package br.ufc.hotelaria.api;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class ReservaRemota {
    public enum Status { PENDENTE, EFETIVADA, CANCELADA }

    public record Reserva(int id, int meioHospedagemId, int hospedeId, String hospede,
                          String entrada, String saida, int unidades, Status status) {}

    private final Map<Integer, Reserva> reservas = new LinkedHashMap<>();
    private final HospedagemRemota hospedagens;

    public ReservaRemota(HospedagemRemota hospedagens) {
        this.hospedagens = hospedagens;
    }

    public synchronized int disponibilidade(int meioId, LocalDate entrada, LocalDate saida) {
        HospedagemRemota.Hospedagem meio = hospedagens.consultar(meioId);
        if (entrada == null || saida == null || !saida.isAfter(entrada))
            throw new ApiException(400, "Intervalo de datas inválido");
        int ocupadas = 0;
        for (Reserva reserva : reservas.values()) {
            if (reserva.status() == Status.CANCELADA || reserva.meioHospedagemId() != meioId)
                continue;
            LocalDate reservaEntrada = LocalDate.parse(reserva.entrada());
            LocalDate reservaSaida = LocalDate.parse(reserva.saida());
            if (reservaEntrada.isBefore(saida) && reservaSaida.isAfter(entrada))
                ocupadas += reserva.unidades();
        }
        return meio.totalUnidades() - ocupadas;
    }

    public synchronized Reserva registrar(int id, int meioId, int hospedeId, String hospede,
                                          LocalDate entrada, LocalDate saida, int unidades) {
        if (reservas.containsKey(id)) throw new ApiException(409, "ID de reserva já existente");
        if (unidades <= 0) throw new ApiException(400, "Número de unidades deve ser positivo");
        if (hospedeId <= 0 || hospede == null || hospede.isBlank())
            throw new ApiException(400, "Dados do hóspede inválidos");
        if (disponibilidade(meioId, entrada, saida) < unidades)
            throw new ApiException(409, "Unidades indisponíveis no período");
        Reserva reserva = new Reserva(id, meioId, hospedeId, hospede,
            entrada.toString(), saida.toString(), unidades, Status.PENDENTE);
        reservas.put(id, reserva);
        return reserva;
    }

    public synchronized Reserva consultar(int id) {
        Reserva reserva = reservas.get(id);
        if (reserva == null) throw new ApiException(404, "Reserva não encontrada");
        return reserva;
    }

    public synchronized Reserva cancelar(int id) {
        Reserva atual = consultar(id);
        if (atual.status() == Status.CANCELADA)
            throw new ApiException(409, "Reserva já cancelada");
        Reserva nova = new Reserva(atual.id(), atual.meioHospedagemId(), atual.hospedeId(),
            atual.hospede(), atual.entrada(), atual.saida(), atual.unidades(), Status.CANCELADA);
        reservas.put(id, nova);
        return nova;
    }

    public synchronized Reserva efetivar(int id) {
        Reserva atual = consultar(id);
        if (atual.status() != Status.PENDENTE)
            throw new ApiException(409, "Só reserva pendente pode ser efetivada");
        Reserva nova = new Reserva(atual.id(), atual.meioHospedagemId(), atual.hospedeId(),
            atual.hospede(), atual.entrada(), atual.saida(), atual.unidades(), Status.EFETIVADA);
        reservas.put(id, nova);
        return nova;
    }

    public synchronized long valorCentavos(int id) {
        Reserva reserva = consultar(id);
        HospedagemRemota.Hospedagem meio = hospedagens.consultar(reserva.meioHospedagemId());
        long diarias = ChronoUnit.DAYS.between(LocalDate.parse(reserva.entrada()),
            LocalDate.parse(reserva.saida()));
        return meio.diariaCentavos() * diarias * reserva.unidades();
    }

    public synchronized List<Reserva> listar() {
        return new ArrayList<>(reservas.values());
    }
}
