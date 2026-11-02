package br.ufc.hotelaria.multicast;

/** Configuração do objeto remoto TCP e do grupo UDP multicast. */
public final class MulticastConfig {
    public static final String GRUPO = "230.0.0.1";
    public static final int PORTA_RPC = 5002;
    public static final int PORTA_UDP = 5004;
    public static final String USUARIO_DEMO = "aluno";
    public static final String SENHA_DEMO = "1234";
    private MulticastConfig() {}

    public static String json(String tipo, String mensagem, long timestamp) {
        return "{\"tipo\":\"" + escapar(tipo) + "\",\"mensagem\":\""
                + escapar(mensagem) + "\",\"timestamp\":" + timestamp + "}";
    }

    private static String escapar(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 32) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.toString();
    }
}
