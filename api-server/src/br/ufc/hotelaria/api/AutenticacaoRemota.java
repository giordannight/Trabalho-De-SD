package br.ufc.hotelaria.api;

import java.time.Instant;
import java.util.*;

public final class AutenticacaoRemota {
    public record Notificacao(int id, String tipo, String mensagem, String timestamp) {}

    private final Map<String, String> usuarios = Map.of("aluno", "1234");
    private final List<Notificacao> notificacoes = new ArrayList<>();
    private int proximoId = 1;

    public synchronized boolean autenticar(String usuario, String senha) {
        return usuarios.containsKey(usuario) && Objects.equals(usuarios.get(usuario), senha);
    }

    public synchronized Notificacao publicar(String usuario, String senha, String tipo, String mensagem) {
        if (!autenticar(usuario, senha))
            throw new ApiException(401, "Credenciais inválidas");
        if (!Set.of("NOTIFICACAO", "ALERTA", "ATUALIZACAO").contains(tipo))
            throw new ApiException(400, "Tipo de notificação inválido");
        if (mensagem == null || mensagem.isBlank())
            throw new ApiException(400, "Mensagem obrigatória");
        Notificacao notificacao = new Notificacao(proximoId++, tipo,
            mensagem.trim(), Instant.now().toString());
        notificacoes.add(0, notificacao);
        return notificacao;
    }

    public synchronized List<Notificacao> listar() {
        return new ArrayList<>(notificacoes);
    }
}
