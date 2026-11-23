import json
import sys
import urllib.parse
import urllib.request
import os

BASE = os.getenv("API_URL", "http://localhost:8080/api")


def requisicao(caminho, metodo="GET", dados=None):
    corpo = None
    cabecalhos = {}
    if dados is not None:
        corpo = json.dumps(dados, ensure_ascii=False).encode("utf-8")
        cabecalhos["Content-Type"] = "application/json"
    requisicao_http = urllib.request.Request(
        BASE + caminho, data=corpo, headers=cabecalhos, method=metodo
    )
    try:
        with urllib.request.urlopen(requisicao_http, timeout=10) as resposta:
            return json.load(resposta)
    except urllib.error.HTTPError as erro:
        texto = erro.read().decode("utf-8")
        raise RuntimeError(texto)


def principal():
    usuario = sys.argv[1] if len(sys.argv) > 1 else "aluno"
    senha = sys.argv[2] if len(sys.argv) > 2 else "1234"

    hospedagens = requisicao("/hospedagens")
    print("Hospedagens:", json.dumps(hospedagens, ensure_ascii=False))

    login = requisicao("/auth/login", "POST", {"usuario": usuario, "senha": senha})
    print("Login:", login)

    disponibilidade = requisicao(
        "/hospedagens/2/disponibilidade"
        "?entrada=2027-11-01&saida=2027-11-05"
    )
    print("Disponibilidade:", disponibilidade)

    reserva = requisicao(
        "/reservas",
        "POST",
        {
            "id": 902,
            "meioHospedagemId": 2,
            "hospedeId": 52,
            "hospede": "João Silva",
            "entrada": "2027-11-01",
            "saida": "2027-11-05",
            "unidades": 1,
        },
    )
    print("Reserva criada:", reserva)

    cancelada = requisicao("/reservas/902", "DELETE")
    print("Reserva cancelada:", cancelada)

    aviso = requisicao(
        "/notificacoes",
        "POST",
        {
            "usuario": usuario,
            "senha": senha,
            "tipo": "NOTIFICACAO",
            "mensagem": "Nova reserva registrada pelo cliente Python",
        },
    )
    print("Notificação publicada:", aviso)
    print("Notificações:", requisicao("/notificacoes"))


if __name__ == "__main__":
    try:
        principal()
    except Exception as erro:
        print("Falha no cliente Python:", erro)
        raise SystemExit(1)
