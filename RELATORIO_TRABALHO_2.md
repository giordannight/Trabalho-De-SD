# Trabalho 2 — Invocação remota da questão 4

**Disciplina:** Sistemas Distribuídos  
**Curso:** UFC — Campus de Quixadá  
**Serviço:** notificações de hotelaria por multicast, disponibilizadas como objeto remoto

**URL do repositório:** preencher com o endereço do repositório entregue no Moodle.

## 1. Objetivo

O Trabalho 2 solicita reimplementar a questão 4 do Trabalho 1 por meio de comunicação cliente-servidor e organizar o serviço como um objeto remoto. A aplicação mantém a autenticação e as solicitações de serviço em chamadas remotas TCP, e usa UDP multicast para distribuir as notificações aos clientes inscritos no grupo.

O sistema preserva o domínio de hotelaria do Trabalho 1. As chamadas remotas podem consultar hospedagens e operar reservas por `hotelaria.reservas`, ou autenticar clientes e publicar notificações por `hotelaria.multicast`.

## 2. Arquitetura e fluxo

1. O servidor TCP recebe conexões de clientes em um pool de oito threads.
2. Cada chamada contém a referência textual do objeto remoto, o identificador do método e seus argumentos.
3. O servidor resolve a referência e executa o método correspondente no objeto local. Para o serviço multicast, valida as credenciais e aceita os tipos `NOTIFICACAO`, `ALERTA` e `ATUALIZACAO`.
4. O servidor envia uma resposta TCP ao cliente. Para `PUBLICAR`, também envia a notificação em JSON por UDP multicast ao grupo `230.0.0.1:5004`.
5. Após autenticar-se por TCP, cada cliente entra no grupo UDP com `joinGroup`, mantém uma thread de recepção e outra de interação e sai do grupo com `leaveGroup`.

```text
Cliente -- request TCP (AUTENTICAR) --> Objeto remoto hotelaria.multicast
Cliente <-- reply TCP ----------------- Servidor
Cliente -- joinGroup ----------------> 230.0.0.1:5004
Cliente -- request TCP (PUBLICAR) ----> Objeto remoto hotelaria.multicast
Cliente <-- reply TCP ----------------- Servidor
Clientes <-- notificação JSON via UDP multicast -- Servidor
```

## 3. Invocação remota e representação de dados

O protocolo TCP request/reply é implementado em `ProtocoloRpc`. O request contém o cabeçalho `HRQ3`, `objectReference`, `methodID`, campos numéricos de reserva e uma lista de argumentos textuais. A resposta `HRP1` informa sucesso ou falha e pode transportar dados de hospedagens e reservas.

O campo `objectReference` é uma referência lógica ao serviço remoto, não uma cópia do objeto: `hotelaria.multicast` identifica o objeto servidor que fornece autenticação e publicação. Os argumentos são serializados por valor no protocolo binário; o servidor os desempacota e os usa para executar o método local. As notificações são representadas externamente como JSON UTF-8, com tipo, mensagem e timestamp.

Métodos remotos multicast:

| Referência | Método | Argumentos | Resultado |
|---|---|---|---|
| `hotelaria.multicast` | `AUTENTICAR` | usuário, senha | autorização ou recusa |
| `hotelaria.multicast` | `PUBLICAR` | usuário, senha, tipo, mensagem | resposta TCP e envio multicast UDP |

O mesmo protocolo disponibiliza mais de quatro operações remotas em `hotelaria.reservas`, incluindo `LISTAR`, `CONSULTAR`, `REGISTRAR`, `CANCELAR`, `EFETIVAR`, `RESERVAS`, `CONSULTAR_RESERVA` e `DISPONIBILIDADE`.

## 4. Requisitos atendidos

- **Cliente-servidor e protocolo de invocação remota:** sockets TCP, request/reply, referência do objeto, método e argumentos.
- **Multicast:** grupo IPv4 classe D `230.0.0.1`, porta fixa UDP `5004`, envio concorrente e JSON com timestamp.
- **Autenticação:** método remoto TCP `AUTENTICAR`; conta demonstrativa `aluno / 1234`.
- **Múltiplos clientes:** o servidor atende chamadas em pool de threads e os clientes inscritos recebem a mesma notificação.
- **Threads do cliente:** uma thread recebe UDP em tempo real e outra lê os comandos do usuário.
- **Entrada e saída do grupo:** `joinGroup` após login válido e `leaveGroup` ao digitar `sair`.
- **Modelo de entidades:** o domínio de hotelaria mantém as entidades `Hotel`, `Motel`, `Pousada`, `Hospede` e `Reserva`; as agregações são `ComplexoTuristico`–`MeioHospedagem` e `Reserva`–`Hospede`, e `Hotel`, `Motel` e `Pousada` estendem `MeioHospedagem`.
- **Representação externa:** protocolo binário explícito para chamadas remotas e JSON UTF-8 para as notificações multicast.

## 5. Execução e demonstração

Compile e execute os testes:

```bat
compilar.bat
java -cp out br.ufc.hotelaria.testes.Testes
```

Inicie o servidor:

```bat
java -cp out br.ufc.hotelaria.rpc.RpcServidor 5002
```

Em dois terminais, inicie clientes:

```bat
java -cp out br.ufc.hotelaria.multicast.MulticastCliente localhost aluno 1234
```

Em um cliente, publique mensagens digitando:

```text
ALERTA|Última suíte disponível
NOTIFICACAO|Nova reserva registrada
ATUALIZACAO|Horários de entrada atualizados
```

Os clientes exibem os datagramas recebidos como JSON. Digite `sair` em cada cliente para deixar o grupo. Para testar uma recusa de autenticação, use uma senha diferente de `1234`.

O servidor RPC também pode receber chamadas de reserva, por exemplo:

```bat
java -cp out br.ufc.hotelaria.rpc.RpcCliente localhost 5002 LISTAR
```

O teste de integração automatiza a compilação, chamadas TCP, publicação de multicast com dois clientes e a recusa de login. Para executar em Linux/macOS ou Git Bash:

```bash
./testar_integracao.sh
```

## 6. Limitações

As credenciais e os dados de hotel são demonstrativos e mantidos em memória. TCP não usa TLS, e UDP multicast não oferece criptografia nem controle de associação ao grupo; TTL 1 limita a propagação à rede local, mas não substitui controles de segurança de produção. Redes ou VPNs que bloqueiam multicast podem exigir que cliente e servidor sejam executados na mesma máquina ou em uma rede compatível.
