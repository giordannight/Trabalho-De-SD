package br.ufc.hotelaria.rpc;

import br.ufc.hotelaria.modelo.Hotel;
import br.ufc.hotelaria.modelo.MeioHospedagem;
import br.ufc.hotelaria.modelo.Motel;
import br.ufc.hotelaria.modelo.Pousada;
import br.ufc.hotelaria.modelo.Reserva;
import br.ufc.hotelaria.streams.ReservaCodec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Ex. 3: protocolo binário explícito de request/reply; não depende de serialização Java nativa. */
public final class ProtocoloRpc {
    private static final int REQ = 0x48525133; // HRQ3
    private static final int REP = 0x48525031; // HRP1
    private static final int LIMITE = 10_000;
    private static final int LIMITE_ARGUMENTOS = 16;
    public static final String REFERENCIA_RESERVAS = "hotelaria.reservas";
    public static final String REFERENCIA_MULTICAST = "hotelaria.multicast";
    private ProtocoloRpc() {}

    public record Requisicao(String objectReference, String methodID,
                             int reservaId, int meioId, int hospedeId,
                             String hospedeNome, long entradaEpoch, long saidaEpoch, int unidades,
                             List<String> argumentos) {
        public Requisicao {
            if (objectReference == null || objectReference.isBlank())
                throw new IllegalArgumentException("Referência do objeto remoto obrigatória");
            if (methodID == null || methodID.isBlank())
                throw new IllegalArgumentException("Identificador do método obrigatório");
            if (hospedeNome == null) hospedeNome = "";
            if (argumentos == null || argumentos.size() > LIMITE_ARGUMENTOS)
                throw new IllegalArgumentException("Quantidade de argumentos inválida");
            argumentos = List.copyOf(argumentos);
        }
        public Requisicao(String objectReference, String methodID, int reservaId, int meioId,
                          int hospedeId, String hospedeNome, long entradaEpoch,
                          long saidaEpoch, int unidades) {
            this(objectReference, methodID, reservaId, meioId, hospedeId, hospedeNome,
                    entradaEpoch, saidaEpoch, unidades, List.of());
        }
        public Requisicao(String objectReference, String methodID, List<String> argumentos) {
            this(objectReference, methodID, 0, 0, 0, "", 0, 0, 0, argumentos);
        }
    }
    public record Resposta(boolean sucesso, String mensagem, int disponibilidade,
                           long valorCentavos, List<MeioHospedagem> meios, List<Reserva> reservas) {}

    public static void enviarRequisicao(OutputStream destino, Requisicao req) throws IOException {
        DataOutputStream out = new DataOutputStream(destino);
        out.writeInt(REQ);
        out.writeUTF(req.objectReference());
        out.writeUTF(req.methodID());
        out.writeInt(req.reservaId());
        out.writeInt(req.meioId());
        out.writeInt(req.hospedeId());
        out.writeUTF(req.hospedeNome());
        out.writeLong(req.entradaEpoch());
        out.writeLong(req.saidaEpoch());
        out.writeInt(req.unidades());
        out.writeInt(req.argumentos().size());
        for (String argumento : req.argumentos()) out.writeUTF(argumento);
        out.flush();
    }
    public static Requisicao lerRequisicao(InputStream origem) throws IOException {
        DataInputStream in = new DataInputStream(origem);
        if (in.readInt() != REQ) throw new IOException("Cabeçalho HRQ3 de request inválido");
        String objectReference = in.readUTF();
        String methodID = in.readUTF();
        int reservaId = in.readInt();
        int meioId = in.readInt();
        int hospedeId = in.readInt();
        String hospedeNome = in.readUTF();
        long entradaEpoch = in.readLong();
        long saidaEpoch = in.readLong();
        int unidades = in.readInt();
        int qtdArgumentos = in.readInt();
        if (qtdArgumentos < 0 || qtdArgumentos > LIMITE_ARGUMENTOS)
            throw new IOException("Quantidade de argumentos fora do limite do protocolo");
        List<String> argumentos = new ArrayList<>();
        for (int i = 0; i < qtdArgumentos; i++) argumentos.add(in.readUTF());
        return new Requisicao(objectReference, methodID, reservaId, meioId, hospedeId,
                hospedeNome, entradaEpoch, saidaEpoch, unidades, argumentos);
    }
    public static void enviarResposta(OutputStream destino, Resposta rep) throws IOException {
        DataOutputStream out = new DataOutputStream(destino);
        out.writeInt(REP);
        out.writeBoolean(rep.sucesso());
        out.writeUTF(rep.mensagem());
        out.writeInt(rep.disponibilidade());
        out.writeLong(rep.valorCentavos());
        out.writeInt(rep.meios().size());
        for (MeioHospedagem meio : rep.meios()) escreverMeio(out, meio);
        out.writeInt(rep.reservas().size());
        for (Reserva reserva : rep.reservas()) ReservaCodec.escrever(out, reserva);
        out.flush();
    }
    public static Resposta lerResposta(InputStream origem) throws IOException {
        DataInputStream in = new DataInputStream(origem);
        if (in.readInt() != REP) throw new IOException("Cabeçalho HRP1 de reply inválido");
        boolean sucesso = in.readBoolean();
        String mensagem = in.readUTF();
        int disponibilidade = in.readInt();
        long valor = in.readLong();
        int qtdMeios = quantidade(in.readInt());
        List<MeioHospedagem> meios = new ArrayList<>();
        for (int i = 0; i < qtdMeios; i++) meios.add(lerMeio(in));
        int qtdReservas = quantidade(in.readInt());
        List<Reserva> reservas = new ArrayList<>();
        for (int i = 0; i < qtdReservas; i++) reservas.add(ReservaCodec.ler(in));
        return new Resposta(sucesso, mensagem, disponibilidade, valor, meios, reservas);
    }
    private static int quantidade(int valor) throws IOException {
        if (valor < 0 || valor > LIMITE) throw new IOException("Quantidade fora do limite do protocolo");
        return valor;
    }
    private static void escreverMeio(DataOutputStream out, MeioHospedagem m) throws IOException {
        out.writeUTF(m.getTipo());
        out.writeInt(m.getId());
        out.writeUTF(m.getNome());
        out.writeUTF(m.getEndereco());
        out.writeLong(m.getDiariaCentavos());
        out.writeInt(m.getTotalUnidades());
        if (m instanceof Hotel h) out.writeInt(h.getEstrelas());
        else if (m instanceof Motel m2) out.writeBoolean(m2.isGaragemPrivativa());
        else if (m instanceof Pousada p) out.writeBoolean(p.isCafeDaManhaIncluso());
        else throw new IOException("Tipo de hospedagem não reconhecido: " + m.getClass().getName());
    }
    private static MeioHospedagem lerMeio(DataInputStream in) throws IOException {
        String tipo = in.readUTF();
        int id = in.readInt();
        String nome = in.readUTF();
        String endereco = in.readUTF();
        long diaria = in.readLong();
        int unidades = in.readInt();
        try {
            return switch (tipo) {
                case "HOTEL" -> new Hotel(id, nome, endereco, diaria, unidades, in.readInt());
                case "MOTEL" -> new Motel(id, nome, endereco, diaria, unidades, in.readBoolean());
                case "POUSADA" -> new Pousada(id, nome, endereco, diaria, unidades, in.readBoolean());
                default -> throw new IOException("Tipo recebido desconhecido: " + tipo);
            };
        } catch (IllegalArgumentException e) {
            throw new IOException("Dados de hospedagem inválidos", e);
        }
    }
}
