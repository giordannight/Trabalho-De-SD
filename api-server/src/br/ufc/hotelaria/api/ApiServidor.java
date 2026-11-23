package br.ufc.hotelaria.api;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public final class ApiServidor {
    private final HospedagemRemota hospedagens = new HospedagemRemota();
    private final ReservaRemota reservas = new ReservaRemota(hospedagens);
    private final AutenticacaoRemota autenticacao = new AutenticacaoRemota();

    public static void main(String[] args) throws IOException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new ApiServidor().iniciar(porta);
    }

    private void iniciar(int porta) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.createContext("/api", this::tratar);
        servidor.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        servidor.start();
        System.out.println("API hotelaria iniciada em http://localhost:" + porta + "/api");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> servidor.stop(0)));
    }

    private void tratar(HttpExchange exchange) {
        try {
            Headers cabecalhos = exchange.getResponseHeaders();
            cabecalhos.set("Content-Type", "application/json; charset=utf-8");
            cabecalhos.set("Access-Control-Allow-Origin", "*");
            cabecalhos.set("Access-Control-Allow-Headers", "Content-Type");
            cabecalhos.set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                responder(exchange, 204, "{}");
                return;
            }
            String caminho = exchange.getRequestURI().getPath();
            String metodo = exchange.getRequestMethod();
            Map<String, String> corpo = metodo.equals("GET") || metodo.equals("DELETE")
                ? Collections.emptyMap() : lerJson(exchange);
            Object resposta;
            int status = 200;

            if (metodo.equals("POST") && caminho.equals("/api/auth/login")) {
                boolean autorizado = autenticacao.autenticar(texto(corpo, "usuario"),
                    texto(corpo, "senha"));
                if (!autorizado) throw new ApiException(401, "Credenciais inválidas");
                resposta = objeto("autorizado", "true", "usuario", texto(corpo, "usuario"));
            } else if (metodo.equals("GET") && caminho.equals("/api/hospedagens")) {
                resposta = "[" + hospedagens.listar().stream()
                    .map(this::hospedagemJson).reduce((a, b) -> a + "," + b).orElse("") + "]";
            } else if (metodo.equals("GET") && caminho.matches("/api/hospedagens/\\d+")) {
                int id = Integer.parseInt(caminho.substring(caminho.lastIndexOf('/') + 1));
                resposta = hospedagemJson(hospedagens.consultar(id));
            } else if (metodo.equals("GET") && caminho.matches("/api/hospedagens/\\d+/disponibilidade")) {
                int id = Integer.parseInt(caminho.split("/")[3]);
                Map<String, String> q = query(exchange);
                int livre = reservas.disponibilidade(id, LocalDate.parse(q.get("entrada")),
                    LocalDate.parse(q.get("saida")));
                resposta = objeto("meioHospedagemId", String.valueOf(id),
                    "disponiveis", String.valueOf(livre));
            } else if (metodo.equals("POST") && caminho.equals("/api/reservas")) {
                ReservaRemota.Reserva reserva = reservas.registrar(
                    inteiro(corpo, "id"), inteiro(corpo, "meioHospedagemId"),
                    inteiro(corpo, "hospedeId"), texto(corpo, "hospede"),
                    LocalDate.parse(texto(corpo, "entrada")),
                    LocalDate.parse(texto(corpo, "saida")),
                    inteiro(corpo, "unidades"));
                status = 201;
                resposta = reservaJson(reserva);
            } else if (metodo.equals("GET") && caminho.equals("/api/reservas")) {
                resposta = "[" + reservas.listar().stream()
                    .map(this::reservaJson).reduce((a, b) -> a + "," + b).orElse("") + "]";
            } else if (metodo.equals("GET") && caminho.matches("/api/reservas/\\d+$")) {
                int id = Integer.parseInt(caminho.substring(caminho.lastIndexOf('/') + 1));
                resposta = reservaJson(reservas.consultar(id));
            } else if (metodo.equals("PUT") && caminho.matches("/api/reservas/\\d+/efetivar")) {
                int id = Integer.parseInt(caminho.split("/")[3]);
                ReservaRemota.Reserva reserva = reservas.efetivar(id);
                resposta = reservaJson(reserva);
            } else if (metodo.equals("DELETE") && caminho.matches("/api/reservas/\\d+$")) {
                int id = Integer.parseInt(caminho.substring(caminho.lastIndexOf('/') + 1));
                ReservaRemota.Reserva reserva = reservas.cancelar(id);
                resposta = reservaJson(reserva);
            } else if (metodo.equals("POST") && caminho.equals("/api/notificacoes")) {
                AutenticacaoRemota.Notificacao notificacao = autenticacao.publicar(
                    texto(corpo, "usuario"), texto(corpo, "senha"),
                    texto(corpo, "tipo"), texto(corpo, "mensagem"));
                status = 201;
                resposta = notificacaoJson(notificacao);
            } else if (metodo.equals("GET") && caminho.equals("/api/notificacoes")) {
                resposta = "[" + autenticacao.listar().stream()
                    .map(this::notificacaoJson).reduce((a, b) -> a + "," + b).orElse("") + "]";
            } else {
                throw new ApiException(404, "Endpoint não encontrado");
            }
            responder(exchange, status, resposta.toString());
        } catch (ApiException erro) {
            responder(exchange, erro.getStatus(),
                objeto("erro", true, "mensagem", erro.getMessage()));
        } catch (Exception erro) {
            String mensagem = erro.getMessage() == null ? "Requisição inválida" : erro.getMessage();
            responder(exchange, 400, objeto("erro", true, "mensagem", mensagem));
        } finally {
            exchange.close();
        }
    }

    private String hospedagemJson(HospedagemRemota.Hospedagem h) {
        return "{\"id\":" + h.id() + ",\"tipo\":\"" + esc(h.tipo()) +
            "\",\"nome\":\"" + esc(h.nome()) + "\",\"endereco\":\"" + esc(h.endereco()) +
            "\",\"diariaCentavos\":" + h.diariaCentavos() +
            ",\"totalUnidades\":" + h.totalUnidades() +
            ",\"detalhe\":\"" + esc(h.detalhe()) + "\"}";
    }

    private String reservaJson(ReservaRemota.Reserva r) {
        return "{\"id\":" + r.id() + ",\"meioHospedagemId\":" + r.meioHospedagemId() +
            ",\"hospedeId\":" + r.hospedeId() + ",\"hospede\":\"" + esc(r.hospede()) +
            "\",\"entrada\":\"" + r.entrada() + "\",\"saida\":\"" + r.saida() +
            "\",\"unidades\":" + r.unidades() + ",\"status\":\"" + r.status() +
            "\",\"valorCentavos\":" + reservas.valorCentavos(r.id()) + "}";
    }

    private String notificacaoJson(AutenticacaoRemota.Notificacao n) {
        return "{\"id\":" + n.id() + ",\"tipo\":\"" + esc(n.tipo()) +
            "\",\"mensagem\":\"" + esc(n.mensagem()) +
            "\",\"timestamp\":\"" + esc(n.timestamp()) + "\"}";
    }

    private String objeto(String chave1, String valor1, String chave2, String valor2) {
        boolean booleano = "true".equals(valor1) || "false".equals(valor1);
        return "{\"" + chave1 + "\":" + (booleano ? valor1 : "\"" + esc(valor1) + "\"") +
            ",\"" + chave2 + "\":\"" + esc(valor2) + "\"}";
    }

    private String objeto(String chave1, boolean valor1, String chave2, String valor2) {
        return "{\"" + chave1 + "\":" + valor1 + ",\"" + chave2 + "\":\"" + esc(valor2) + "\"}";
    }

    private Map<String, String> lerJson(HttpExchange exchange) throws IOException {
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> mapa = new HashMap<>();
        if (json.isBlank()) return mapa;
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\"([^\"]+)\"\\s*:\\s*(\"(?:\\\\.|[^\"\\\\])*\"|[^,}\\]]+)")
            .matcher(json);
        while (m.find()) {
            String valor = m.group(2).trim();
            if (valor.startsWith("\"")) valor = desesc(valor.substring(1, valor.length() - 1));
            mapa.put(m.group(1), valor);
        }
        return mapa;
    }

    private Map<String, String> query(HttpExchange exchange) {
        Map<String, String> mapa = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) return mapa;
        for (String parte : query.split("&")) {
            String[] par = parte.split("=", 2);
            if (par.length == 2) {
                try {
                    mapa.put(par[0], java.net.URLDecoder.decode(par[1], StandardCharsets.UTF_8));
                } catch (IllegalArgumentException ignorar) {}
            }
        }
        return mapa;
    }

    private String texto(Map<String, String> mapa, String chave) {
        String valor = mapa.get(chave);
        if (valor == null || valor.isBlank()) throw new ApiException(400, "Campo obrigatório: " + chave);
        return valor;
    }

    private int inteiro(Map<String, String> mapa, String chave) {
        try { return Integer.parseInt(texto(mapa, chave)); }
        catch (NumberFormatException erro) { throw new ApiException(400, "Campo inválido: " + chave); }
    }

    private String esc(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String desesc(String texto) {
        return texto.replace("\\\"", "\"").replace("\\n", "\n")
            .replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\");
    }

    private void responder(HttpExchange exchange, int status, String json) {
        try {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream saida = exchange.getResponseBody()) {
                saida.write(bytes);
            }
        } catch (IOException ignorar) {}
    }
}
