package br.ufc.hotelaria.rpc;

import br.ufc.hotelaria.fila.PublicadorEventos;
import br.ufc.hotelaria.modelo.ComplexoTuristico;
import br.ufc.hotelaria.modelo.Hospede;
import br.ufc.hotelaria.modelo.Hotel;
import br.ufc.hotelaria.modelo.Motel;
import br.ufc.hotelaria.modelo.Pousada;
import br.ufc.hotelaria.modelo.Reserva;
import br.ufc.hotelaria.multicast.MulticastConfig;
import br.ufc.hotelaria.servico.HospedagemService;
import br.ufc.hotelaria.servico.ReservaService;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Servidor de hotelaria: vários clientes TCP compartilham o mesmo catálogo e as reservas. */
public final class RpcServidor {
    private final HospedagemService hospedagens =
            new HospedagemService(new ComplexoTuristico("Complexo Turístico Dunas"));
    private final ReservaService reservas = new ReservaService(hospedagens);
    private final PublicadorEventos publicador;

    public RpcServidor() {
        try {
            String host = System.getenv().getOrDefault("HOTELARIA_FILA_HOST", "127.0.0.1");
            int porta = Integer.parseInt(System.getenv().getOrDefault("HOTELARIA_FILA_PORT", "5050"));
            Path pasta = Path.of(System.getenv().getOrDefault("HOTELARIA_OUTBOX_DIR", "dados_outbox"));
            publicador = new PublicadorEventos(host, porta, pasta);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível inicializar a outbox", e);
        }
        hospedagens.cadastrar(new Hotel(1, "Hotel Praia Azul", "Av. Beira-Mar, 10", 35000, 5, 4));
        hospedagens.cadastrar(new Motel(2, "Motel Sol", "Rua das Flores, 20", 18000, 3, true));
        hospedagens.cadastrar(new Pousada(3, "Pousada do Bosque", "Rua Verde, 30", 22000, 2, true));
    }

    public ProtocoloRpc.Resposta processar(ProtocoloRpc.Requisicao req) {
        if (ProtocoloRpc.REFERENCIA_MULTICAST.equals(req.objectReference()))
            return processarMulticast(req);
        if (!ProtocoloRpc.REFERENCIA_RESERVAS.equals(req.objectReference()))
            return erro("Objeto remoto desconhecido: " + req.objectReference());
        try {
            return switch (req.methodID().toUpperCase(Locale.ROOT)) {
                case "LISTAR" -> ok("Meios de hospedagem do " + hospedagens.nomeComplexo(), -1, 0,
                        hospedagens.listar(), List.of());
                case "CONSULTAR" -> ok("Meio de hospedagem encontrado", -1, 0,
                        List.of(hospedagens.consultar(req.meioId())), List.of());
                case "REGISTRAR" -> {
                    Reserva nova = new Reserva(req.reservaId(), req.meioId(),
                            new Hospede(req.hospedeId(), req.hospedeNome()),
                            LocalDate.ofEpochDay(req.entradaEpoch()), LocalDate.ofEpochDay(req.saidaEpoch()),
                            req.unidades());
                    Reserva salva = reservas.registrarReserva(nova);
                    publicador.publicar("RESERVA_CRIADA", salva);
                    yield ok("Reserva registrada", -1, reservas.valorReservaCentavos(salva.getId()),
                            List.of(), List.of(salva));
                }
                case "CANCELAR" -> {
                    Reserva cancelada = reservas.cancelarReserva(req.reservaId());
                    publicador.publicar("RESERVA_CANCELADA", cancelada);
                    yield ok("Reserva cancelada", -1, 0,
                            List.of(), List.of(cancelada));
                }
                case "EFETIVAR" -> {
                    Reserva efetivada = reservas.efetivarReserva(req.reservaId());
                    publicador.publicar("RESERVA_EFETIVADA", efetivada);
                    yield ok("Reserva efetivada", -1, reservas.valorReservaCentavos(efetivada.getId()),
                            List.of(), List.of(efetivada));
                }
                case "RESERVAS" -> ok("Reservas do complexo", -1, 0, List.of(), reservas.listar());
                case "CONSULTAR_RESERVA" -> ok("Reserva encontrada", -1, 0,
                        List.of(), List.of(reservas.consultar(req.reservaId())));
                case "DISPONIBILIDADE" -> ok("Unidades disponíveis no período",
                        reservas.disponibilidade(req.meioId(), LocalDate.ofEpochDay(req.entradaEpoch()),
                                LocalDate.ofEpochDay(req.saidaEpoch())), 0, List.of(), List.of());
                default -> erro("Método remoto desconhecido: " + req.methodID());
            };
        } catch (IllegalArgumentException | ArithmeticException e) {
            return erro(e.getMessage());
        }
    }

    private ProtocoloRpc.Resposta processarMulticast(ProtocoloRpc.Requisicao req) {
        List<String> args = req.argumentos();
        return switch (req.methodID().toUpperCase(Locale.ROOT)) {
            case "AUTENTICAR" -> {
                if (args.size() != 2) yield erro("AUTENTICAR requer usuário e senha");
                boolean autorizado = MulticastConfig.USUARIO_DEMO.equals(args.get(0))
                        && MulticastConfig.SENHA_DEMO.equals(args.get(1));
                System.out.println("Autenticação multicast de " + args.get(0) + ": "
                        + (autorizado ? "OK" : "NEGADA"));
                yield autorizado ? ok("Acesso autorizado ao grupo multicast", -1, 0,
                        List.of(), List.of()) : erro("Usuário/senha incorretos");
            }
            case "PUBLICAR" -> {
                if (args.size() != 4) yield erro("PUBLICAR requer usuário, senha, tipo e mensagem");
                if (!MulticastConfig.USUARIO_DEMO.equals(args.get(0))
                        || !MulticastConfig.SENHA_DEMO.equals(args.get(1)))
                    yield erro("Usuário/senha incorretos");
                String tipo = args.get(2).toUpperCase(Locale.ROOT);
                if (!tipo.equals("NOTIFICACAO") && !tipo.equals("ALERTA")
                        && !tipo.equals("ATUALIZACAO"))
                    yield erro("Tipo inválido. Use NOTIFICACAO, ALERTA ou ATUALIZACAO");
                if (args.get(3).isBlank()) yield erro("Mensagem não pode ser vazia");
                try {
                    String datagrama = enviarMulticast(tipo, args.get(3));
                    System.out.println("UDP multicast enviado: " + datagrama);
                    yield ok("Mensagem multicast enviada", -1, 0, List.of(), List.of());
                } catch (IOException e) {
                    System.err.println("Erro ao enviar multicast: " + e.getMessage());
                    yield erro("Falha ao enviar mensagem multicast");
                }
            }
            default -> erro("Método multicast desconhecido: " + req.methodID());
        };
    }

    private static String enviarMulticast(String tipo, String mensagem) throws IOException {
        String datagrama = MulticastConfig.json(tipo, mensagem, System.currentTimeMillis());
        byte[] bytes = datagrama.getBytes(StandardCharsets.UTF_8);
        InetAddress grupo = InetAddress.getByName(MulticastConfig.GRUPO);
        DatagramPacket pacote = new DatagramPacket(bytes, bytes.length, grupo, MulticastConfig.PORTA_UDP);
        try (MulticastSocket emissor = new MulticastSocket()) {
            emissor.setTimeToLive(1);
            emissor.send(pacote);
        }
        return datagrama;
    }
    private static ProtocoloRpc.Resposta ok(String mensagem, int unidades, long centavos,
                                           List<br.ufc.hotelaria.modelo.MeioHospedagem> meios,
                                           List<Reserva> reservas) {
        return new ProtocoloRpc.Resposta(true, mensagem, unidades, centavos, meios, reservas);
    }
    private static ProtocoloRpc.Resposta erro(String mensagem) {
        return new ProtocoloRpc.Resposta(false, mensagem, -1, 0, List.of(), List.of());
    }
    public static void main(String[] args) throws Exception {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 5002;
        RpcServidor app = new RpcServidor();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try (ServerSocket servidor = new ServerSocket(porta)) {
            System.out.println("Servidor RPC ouvindo TCP:" + porta);
            System.out.println("Objeto multicast: " + ProtocoloRpc.REFERENCIA_MULTICAST
                    + " -> " + MulticastConfig.GRUPO + ":" + MulticastConfig.PORTA_UDP);
            while (true) {
                Socket cliente = servidor.accept();
                pool.submit(() -> {
                    try (cliente) {
                        cliente.setSoTimeout(10_000);
                        var req = ProtocoloRpc.lerRequisicao(cliente.getInputStream());
                        ProtocoloRpc.enviarResposta(cliente.getOutputStream(), app.processar(req));
                    } catch (Exception e) {
                        System.err.println("Erro no atendimento TCP: " + e.getMessage());
                    }
                });
            }
        } finally { pool.shutdownNow(); }
    }
}
