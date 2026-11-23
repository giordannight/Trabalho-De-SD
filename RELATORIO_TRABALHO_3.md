# Trabalho 3 - API REST de Hotelaria

**Disciplina:** Sistemas Distribuídos  
**Curso:** UFC - Campus de Quixadá
**Repositório:** https://github.com/giordannight/Trabalho-De-SD

## 1. Arquitetura

O projeto utiliza uma API REST com HTTP e JSON. Não usa sockets TCP/UDP nem RMI. O servidor está em `api-server/` e os clientes estão em projetos separados: `api-client-js/` e `api-client-python/`.

O servidor publica três objetos distribuídos:

| Objeto remoto | Responsabilidade |
|---|---|
| `HospedagemRemota` | Catálogo Hotel, Motel e Pousada |
| `ReservaRemota` | Disponibilidade, registro, consulta, efetivação e cancelamento |
| `AutenticacaoRemota` | Login e publicação/consulta de notificações |

## 2. Endpoints

| Método | Endpoint | Recurso |
|---|---|---|
| POST | `/api/auth/login` | Autenticação |
| GET | `/api/hospedagens` | Lista hospedagens |
| GET | `/api/hospedagens/{id}` | Consulta hospedagem |
| GET | `/api/hospedagens/{id}/disponibilidade?entrada=...&saida=...` | Verifica disponibilidade |
| POST | `/api/reservas` | Registra reserva |
| GET | `/api/reservas` | Lista reservas |
| GET | `/api/reservas/{id}` | Consulta reserva |
| PUT | `/api/reservas/{id}/efetivar` | Efetiva reserva |
| DELETE | `/api/reservas/{id}` | Cancela reserva |
| POST | `/api/notificacoes` | Publica notificação autenticada |
| GET | `/api/notificacoes` | Lista notificações |

Credenciais de demonstração: `aluno / 1234`.

## 3. Execução

Terminal 1:

```bat
cd api-server
compilar.bat
executar.bat
```

Terminal 2:

```bat
cd api-client-js
node cliente.js aluno 1234
```

Terminal 3:

```bat
cd api-client-python
python cliente.py aluno 1234
```

Os clientes demonstram autenticação, consulta de hospedagens e disponibilidade, operações de reserva e notificações em duas linguagens diferentes da linguagem Java do servidor.

## 4. Representação e concorrência

HTTP usa JSON UTF-8. Datas usam ISO `AAAA-MM-DD`. Reservas controlam as ocupações por intervalo, no qual a saída é exclusiva. O servidor usa pool de oito threads e os mapas de domínio são sincronizados para evitar registros concorrentes inválidos.
