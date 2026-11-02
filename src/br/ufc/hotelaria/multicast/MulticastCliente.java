package br.ufc.hotelaria.multicast;

import br.ufc.hotelaria.rpc.ProtocoloRpc;
import br.ufc.hotelaria.rpc.RpcCliente;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Cliente do objeto remoto multicast, com recepção UDP em thread separada. */
public class MulticastCliente {
    @SuppressWarnings("deprecation") // joinGroup/leaveGroup sem interface: mais simples no laboratório local
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        String usuario = args.length > 1 ? args[1] : "aluno";
        String senha = args.length > 2 ? args[2] : "1234";
        int porta = args.length > 3 ? Integer.parseInt(args[3]) : MulticastConfig.PORTA_RPC;
        ProtocoloRpc.Resposta login = RpcCliente.invocar(host, porta,
                new ProtocoloRpc.Requisicao(ProtocoloRpc.REFERENCIA_MULTICAST,
                        "AUTENTICAR", List.of(usuario, senha)));
        System.out.println("Resposta TCP: " + login.mensagem());
        if (!login.sucesso()) return;

        InetAddress grupo = InetAddress.getByName(MulticastConfig.GRUPO);
        AtomicBoolean ativo = new AtomicBoolean(true);
        try (MulticastSocket recepcao = new MulticastSocket(MulticastConfig.PORTA_UDP)) {
            recepcao.setSoTimeout(1000);
            recepcao.joinGroup(grupo);
            Thread escuta = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (ativo.get()) {
                    try {
                        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                        recepcao.receive(pacote);
                        System.out.println("[MULTICAST] " + new String(pacote.getData(),
                                pacote.getOffset(), pacote.getLength(), StandardCharsets.UTF_8));
                    } catch (SocketTimeoutException ignored) {
                        // timeout curto permite observar o encerramento da thread
                    } catch (Exception e) {
                        if (ativo.get()) System.err.println("Recepção: " + e.getMessage());
                        break;
                    }
                }
            }, "escuta-multicast");
            escuta.start();
            Thread interacao = new Thread(() -> {
                try {
                    BufferedReader teclado = new BufferedReader(new InputStreamReader(
                            System.in, StandardCharsets.UTF_8));
                    System.out.println("Cliente ouvindo o grupo. Digite TIPO|mensagem para publicar "
                            + "(NOTIFICACAO, ALERTA ou ATUALIZACAO), ou 'sair':");
                    String linha;
                    while ((linha = teclado.readLine()) != null) {
                        if (linha.equalsIgnoreCase("sair")) break;
                        String[] partes = linha.split("\\|", 2);
                        if (partes.length != 2 || partes[1].isBlank()) {
                            System.out.println("Formato esperado: ALERTA|Última suíte disponível");
                            continue;
                        }
                        ProtocoloRpc.Resposta resposta = RpcCliente.invocar(host,
                                porta,
                                new ProtocoloRpc.Requisicao(ProtocoloRpc.REFERENCIA_MULTICAST,
                                        "PUBLICAR", List.of(usuario, senha,
                                                partes[0].trim(), partes[1])));
                        System.out.println("Resposta TCP: " + resposta.mensagem());
                    }
                } catch (Exception e) {
                    if (ativo.get()) System.err.println("Interação: " + e.getMessage());
                } finally { ativo.set(false); }
            }, "interacao-usuario");
            interacao.start();
            interacao.join();
            ativo.set(false);
            escuta.join(2000);
            recepcao.leaveGroup(grupo);
            System.out.println("Cliente saiu do grupo multicast (leaveGroup).");
        }
    }
}
