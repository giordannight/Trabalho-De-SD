package br.ufc.hotelaria.rpc;

import br.ufc.hotelaria.modelo.MeioHospedagem;
import br.ufc.hotelaria.modelo.Reserva;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDate;
import java.util.Locale;

/** Cliente de linha de comando: empacota request, recebe reply e reconstitui POJOs. */
public final class RpcCliente {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) { ajuda(); return; }
        String host = args[0];
        int porta = Integer.parseInt(args[1]);
        String op = args[2].toUpperCase(Locale.ROOT);
        int reservaId = 0, meioId = 0, hospedeId = 0, unidades = 0;
        String nome = "";
        long entrada = 0, saida = 0;
        switch (op) {
            case "LISTAR", "RESERVAS" -> { if (args.length != 3) { ajuda(); return; } }
            case "CONSULTAR", "DISPONIBILIDADE" -> {
                int esperado = op.equals("CONSULTAR") ? 4 : 6;
                if (args.length != esperado) { ajuda(); return; }
                meioId = Integer.parseInt(args[3]);
                if (op.equals("DISPONIBILIDADE")) {
                    entrada = LocalDate.parse(args[4]).toEpochDay();
                    saida = LocalDate.parse(args[5]).toEpochDay();
                }
            }
            case "CONSULTAR_RESERVA", "CANCELAR", "EFETIVAR" -> {
                if (args.length != 4) { ajuda(); return; }
                reservaId = Integer.parseInt(args[3]);
            }
            case "REGISTRAR" -> {
                if (args.length != 10) { ajuda(); return; }
                reservaId = Integer.parseInt(args[3]);
                meioId = Integer.parseInt(args[4]);
                hospedeId = Integer.parseInt(args[5]);
                nome = args[6];
                entrada = LocalDate.parse(args[7]).toEpochDay();
                saida = LocalDate.parse(args[8]).toEpochDay();
                unidades = Integer.parseInt(args[9]);
            }
            default -> { ajuda(); return; }
        }
        ProtocoloRpc.Requisicao requisicao = new ProtocoloRpc.Requisicao(
                ProtocoloRpc.REFERENCIA_RESERVAS, op, reservaId,
                meioId, hospedeId, nome, entrada, saida, unidades);
        ProtocoloRpc.Resposta resposta = invocar(host, porta, requisicao);
        System.out.println("Sucesso: " + resposta.sucesso() + " | " + resposta.mensagem());
        for (MeioHospedagem meio : resposta.meios()) System.out.println(meio);
        for (Reserva reserva : resposta.reservas()) System.out.println(reserva);
        if (resposta.disponibilidade() >= 0)
            System.out.println("Unidades disponíveis: " + resposta.disponibilidade());
        if (resposta.valorCentavos() > 0)
            System.out.printf("Valor da reserva: R$ %d,%02d%n",
                    resposta.valorCentavos() / 100, resposta.valorCentavos() % 100);
    }

    public static ProtocoloRpc.Resposta invocar(String host, int porta,
                                                ProtocoloRpc.Requisicao requisicao)
            throws IOException {
        try (Socket socket = new Socket(host, porta)) {
            socket.setSoTimeout(10_000);
            ProtocoloRpc.enviarRequisicao(socket.getOutputStream(), requisicao);
            return ProtocoloRpc.lerResposta(socket.getInputStream());
        }
    }
    private static void ajuda() {
        System.out.println("Uso: RpcCliente HOST PORTA OPERAÇÃO [PARÂMETROS]\n"
                + "LISTAR | RESERVAS | CONSULTAR MEIO_ID | CONSULTAR_RESERVA RESERVA_ID\n"
                + "REGISTRAR RESERVA_ID MEIO_ID HOSPEDE_ID NOME AAAA-MM-DD AAAA-MM-DD UNIDADES\n"
                + "EFETIVAR RESERVA_ID | CANCELAR RESERVA_ID\n"
                + "DISPONIBILIDADE MEIO_ID AAAA-MM-DD AAAA-MM-DD");
    }
}
