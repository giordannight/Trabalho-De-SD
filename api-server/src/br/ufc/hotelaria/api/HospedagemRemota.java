package br.ufc.hotelaria.api;

import java.util.*;

public final class HospedagemRemota {
    public record Hospedagem(int id, String tipo, String nome, String endereco,
                             long diariaCentavos, int totalUnidades, String detalhe) {}

    private final Map<Integer, Hospedagem> hospedagens = new LinkedHashMap<>();

    public HospedagemRemota() {
        hospedagens.put(1, new Hospedagem(1, "HOTEL", "Hotel Central",
            "Rua 10, Quixadá", 25000, 5, "4 estrelas"));
        hospedagens.put(2, new Hospedagem(2, "MOTEL", "Motel Lua",
            "Rodovia 12, Quixadá", 18000, 4, "Garagem privativa"));
        hospedagens.put(3, new Hospedagem(3, "POUSADA", "Pousada Serra",
            "Rua das Flores, Quixadá", 12000, 6, "Café da manhã incluso"));
    }

    public synchronized List<Hospedagem> listar() {
        return new ArrayList<>(hospedagens.values());
    }

    public synchronized Hospedagem consultar(int id) {
        Hospedagem hospedagem = hospedagens.get(id);
        if (hospedagem == null) throw new ApiException(404, "Hospedagem não encontrada");
        return hospedagem;
    }
}
