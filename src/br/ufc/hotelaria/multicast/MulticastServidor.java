package br.ufc.hotelaria.multicast;

import br.ufc.hotelaria.rpc.RpcServidor;

/** Compatibilidade: inicia o servidor RPC que também publica avisos multicast. */
public class MulticastServidor {
    public static void main(String[] args) throws Exception {
        RpcServidor.main(args);
    }
}
