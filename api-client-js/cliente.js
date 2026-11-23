const base = process.env.API_URL || "http://localhost:8080/api";

async function request(caminho, opcoes = {}) {
  const resposta = await fetch(base + caminho, {
    headers: { "Content-Type": "application/json", ...(opcoes.headers || {}) },
    ...opcoes,
  });
  const dados = await resposta.json();
  if (!resposta.ok) throw new Error(dados.mensagem || "Erro HTTP " + resposta.status);
  return dados;
}

async function principal() {
  const usuario = process.argv[2] || "aluno";
  const senha = process.argv[3] || "1234";
  const hospedagens = await request("/hospedagens");
  console.log("Hospedagens:", hospedagens);
  const login = await request("/auth/login", {
    method: "POST",
    body: JSON.stringify({ usuario, senha }),
  });
  console.log("Login:", login);
  const disponibilidade = await request(
    "/hospedagens/1/disponibilidade?entrada=2027-10-10&saida=2027-10-13"
  );
  console.log("Disponibilidade:", disponibilidade);
  const reserva = await request("/reservas", {
    method: "POST",
    body: JSON.stringify({
      id: 901,
      meioHospedagemId: 1,
      hospedeId: 51,
      hospede: "Ana Lima",
      entrada: "2027-10-10",
      saida: "2027-10-13",
      unidades: 2,
    }),
  });
  console.log("Reserva criada:", reserva);
  const efetivada = await request("/reservas/901/efetivar", { method: "PUT" });
  console.log("Reserva efetivada:", efetivada);
  const aviso = await request("/notificacoes", {
    method: "POST",
    body: JSON.stringify({
      usuario,
      senha,
      tipo: "ALERTA",
      mensagem: "Última suíte disponível no Hotel Central",
    }),
  });
  console.log("Notificação publicada:", aviso);
  console.log("Notificações:", await request("/notificacoes"));
}

principal().catch(erro => {
  console.error("Falha no cliente Node.js:", erro.message);
  process.exitCode = 1;
});
