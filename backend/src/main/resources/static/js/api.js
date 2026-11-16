// ===== Camada de comunicacao com a API REST (Spring Boot) =====
const API_BASE = "/api";

function getToken() {
  return localStorage.getItem("dermato_token");
}

function setSessao(token, nome, email) {
  localStorage.setItem("dermato_token", token);
  localStorage.setItem("dermato_nome", nome);
  localStorage.setItem("dermato_email", email);
}

function logout() {
  localStorage.removeItem("dermato_token");
  localStorage.removeItem("dermato_nome");
  localStorage.removeItem("dermato_email");
  window.location.href = "login.html";
}

function exigirLogin() {
  if (!getToken()) {
    window.location.href = "login.html";
    return;
  }
  mostrarUsuarioLogado();
}

// ===== Utilitarios de interface (usados por todas as telas) =====

// Escapa texto vindo do banco antes de colocar em innerHTML (ex.: nome com "<" nao quebra a tela)
function escaparHtml(texto) {
  return String(texto ?? "").replace(/[&<>"']/g, c =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

// "Dra. Ana Beatriz Souza" -> "Ana"
function primeiroNome(nome) {
  const partes = (nome || "").trim().split(/\s+/).filter(p => !/^dra?\.?$/i.test(p));
  return partes[0] || "";
}

function iniciais(nome) {
  const partes = (nome || "").trim().split(/\s+/).filter(p => !/^dra?\.?$/i.test(p));
  if (partes.length === 0) return "?";
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : "";
  return (partes[0][0] + ultima).toUpperCase();
}

// Mostra o medico logado no rodape da barra lateral (nome/e-mail ficam salvos no login)
function mostrarUsuarioLogado() {
  const rodape = document.querySelector(".sidebar-footer");
  if (!rodape || rodape.querySelector(".usuario-logado")) return;
  const nome = localStorage.getItem("dermato_nome") || "";
  const email = localStorage.getItem("dermato_email") || "";
  const bloco = document.createElement("div");
  bloco.className = "usuario-logado";
  bloco.title = nome;
  bloco.innerHTML = `
    <span class="avatar">${escaparHtml(iniciais(nome))}</span>
    <span class="usuario-dados">
      <span class="usuario-nome">${escaparHtml(nome)}</span>
      <span class="usuario-email">${escaparHtml(email)}</span>
    </span>`;
  rodape.prepend(bloco);
}

// "2026-11-16T14:30:00" -> "16/11/2026 às 14:30"
function formatarDataHora(iso) {
  if (!iso) return "-";
  const d = new Date(iso);
  if (isNaN(d)) return "-";
  const data = d.toLocaleDateString("pt-BR");
  const hora = d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
  return `${data} às ${hora}`;
}

// "1980-03-05" -> "05/03/1980" (sem passar por Date, para nao mudar o dia por causa do fuso)
function formatarData(dataIso) {
  if (!dataIso) return "-";
  const [ano, mes, dia] = dataIso.split("-");
  return `${dia}/${mes}/${ano}`;
}

function calcularIdade(dataIso) {
  if (!dataIso) return null;
  const [ano, mes, dia] = dataIso.split("-").map(Number);
  const hoje = new Date();
  let idade = hoje.getFullYear() - ano;
  if (hoje.getMonth() + 1 < mes || (hoje.getMonth() + 1 === mes && hoje.getDate() < dia)) idade--;
  return idade;
}

// Mascara enquanto digita: 18999990001 -> (18) 99999-0001 ; 1833334444 -> (18) 3333-4444
function formatarTelefone(texto) {
  const d = somenteDigitos(texto).slice(0, 11);
  if (d.length <= 2) return d.length ? `(${d}` : "";
  const ddd = d.slice(0, 2);
  const resto = d.slice(2);
  const corte = d.length === 11 ? 5 : 4;
  if (resto.length <= corte) return `(${ddd}) ${resto}`;
  return `(${ddd}) ${resto.slice(0, corte)}-${resto.slice(corte)}`;
}

// Desabilita o botao e mostra um texto de espera enquanto a acao roda (evita clique duplo)
async function comCarregamento(botao, textoCarregando, acao) {
  const conteudoOriginal = botao.innerHTML;
  botao.disabled = true;
  botao.classList.add("carregando");
  botao.textContent = textoCarregando;
  try {
    return await acao();
  } finally {
    botao.disabled = false;
    botao.classList.remove("carregando");
    botao.innerHTML = conteudoOriginal;
  }
}

// Botao "Mostrar/Ocultar" dos campos de senha
function alternarSenha(idCampo, botao) {
  const campo = document.getElementById(idCampo);
  const mostrar = campo.type === "password";
  campo.type = mostrar ? "text" : "password";
  botao.textContent = mostrar ? "Ocultar" : "Mostrar";
  botao.setAttribute("aria-label", mostrar ? "Ocultar senha" : "Mostrar senha");
}

// Janela de confirmacao com o visual do sistema (substitui o confirm() do navegador).
// Uso: if (await confirmar({ titulo, mensagem, textoConfirmar, perigo: true })) { ... }
function confirmar({ titulo = "Confirmar", mensagem = "", textoConfirmar = "Confirmar", perigo = false } = {}) {
  return new Promise(resolve => {
    const dialogo = document.createElement("dialog");
    dialogo.className = "modal";
    dialogo.innerHTML = `
      <form method="dialog">
        <h2>${escaparHtml(titulo)}</h2>
        <p>${escaparHtml(mensagem)}</p>
        <div class="modal-acoes">
          <button value="cancelar" class="secondary">Cancelar</button>
          <button value="ok" class="primary ${perigo ? "perigo" : ""}">${escaparHtml(textoConfirmar)}</button>
        </div>
      </form>`;
    document.body.appendChild(dialogo);
    dialogo.addEventListener("close", () => {
      resolve(dialogo.returnValue === "ok");
      dialogo.remove();
    });
    // clique fora da caixa = cancelar
    dialogo.addEventListener("click", (e) => { if (e.target === dialogo) dialogo.close("cancelar"); });
    dialogo.showModal();
    dialogo.querySelector('button[value="cancelar"]').focus();
  });
}

// Abre a foto em tamanho grande; fecha com clique, toque ou Esc
function ampliarImagem(src, legenda) {
  const dialogo = document.createElement("dialog");
  dialogo.className = "modal-imagem";
  dialogo.innerHTML = `
    <img src="${escaparHtml(src)}" alt="${escaparHtml(legenda || "Imagem da lesão")}">
    ${legenda ? `<p>${escaparHtml(legenda)}</p>` : ""}
    <button type="button" class="modal-fechar" aria-label="Fechar">×</button>`;
  document.body.appendChild(dialogo);
  dialogo.addEventListener("click", () => dialogo.close());
  dialogo.addEventListener("close", () => dialogo.remove());
  dialogo.showModal();
}

// ===== Resultado da IA =====

// Le o JSON salvo no banco. Retorna { erro } ou { entradas: [[classe, prob], ...] } da maior para a menor
function lerProbabilidades(vetorJson) {
  let probs;
  try { probs = JSON.parse(vetorJson); } catch (e) { return { erro: "resultado ilegível" }; }
  if (!probs || probs.erro) return { erro: (probs && probs.erro) || "resultado vazio" };
  const entradas = Object.entries(probs).sort((a, b) => b[1] - a[1]);
  return { entradas };
}

function formatarPercentual(valor) {
  const pct = valor * 100;
  if (pct > 0 && pct < 1) return "< 1%";
  return Math.round(pct) + "%";
}

// Lista de barras com a classe mais provavel em destaque.
// "limite" mostra so as N primeiras e esconde o resto em "Ver todas as classes".
function htmlProbabilidades(entradas, limite = 0) {
  const linha = ([categoria, valor], i) => `
    <div class="prob-row ${i === 0 ? "prob-top" : ""}">
      <div class="prob-linha">
        <span class="prob-nome">${escaparHtml(categoria)}${i === 0 ? ' <span class="prob-tag">maior probabilidade</span>' : ""}</span>
        <span class="prob-pct">${formatarPercentual(valor)}</span>
      </div>
      <div class="prob-bar-bg"><div class="prob-bar" style="width:${Math.round(valor * 100)}%"></div></div>
    </div>`;

  const visiveis = limite > 0 ? entradas.slice(0, limite) : entradas;
  const ocultas = limite > 0 ? entradas.slice(limite) : [];
  let html = `<div class="prob-lista">${visiveis.map(linha).join("")}`;
  if (ocultas.length) {
    html += `<details class="prob-mais"><summary>Ver todas as classes (${entradas.length})</summary>
               ${ocultas.map((e, i) => linha(e, i + limite)).join("")}
             </details>`;
  }
  return html + "</div>";
}

async function apiFetch(path, options = {}) {
  const headers = options.headers || {};
  const token = getToken();
  if (token) headers["Authorization"] = "Bearer " + token;

  const resp = await fetch(API_BASE + path, { ...options, headers });

  if (resp.status === 401) {
    logout();
    return null;
  }
  return resp;
}

// Extrai uma mensagem de erro legivel da resposta. O backend agora devolve
// {"erro": "..."} nos casos previstos; se nao for esse formato, cai para texto puro.
async function extrairMensagemErro(resp) {
  const texto = await resp.text();
  try {
    const json = JSON.parse(texto);
    if (json.erro) return json.erro;
    if (json.message) return json.message;
  } catch (e) {
    // resposta nao era JSON, usa o texto puro mesmo
  }
  return texto || "Ocorreu um erro inesperado. Verifique se o backend e o microservico de IA estao rodando.";
}

// ----- Validacoes (as mesmas regras do backend, em ValidadorDados.java) -----
const SENHA_TAMANHO_MINIMO = 8;

function somenteDigitos(texto) {
  return (texto || "").replace(/\D/g, "");
}

// 11 digitos, nao todos iguais e com os 2 digitos verificadores corretos
function cpfValido(cpf) {
  const d = somenteDigitos(cpf);
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) return false;
  const digito = (tamanho) => {
    let soma = 0;
    for (let i = 0; i < tamanho; i++) soma += Number(d[i]) * (tamanho + 1 - i);
    const resto = (soma * 10) % 11;
    return resto === 10 ? 0 : resto;
  };
  return digito(9) === Number(d[9]) && digito(10) === Number(d[10]);
}

// Mascara enquanto digita: 12345678901 -> 123.456.789-01
function formatarCpf(texto) {
  const d = somenteDigitos(texto).slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d{1,2})$/, ".$1-$2");
}

function emailValido(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test((email || "").trim());
}

// ----- Auth -----
async function login(email, senha) {
  const resp = await fetch(API_BASE + "/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, senha }),
  });
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

async function cadastrarMedico(dados) {
  const resp = await fetch(API_BASE + "/auth/cadastro", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

// ----- Pacientes -----
async function listarPacientes() {
  const resp = await apiFetch("/pacientes");
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

// Lista paginada, com busca por nome/CPF e ordenacao (feitas no servidor).
// Retorna { conteudo, pagina, tamanho, totalElementos, totalPaginas }; "pagina" comeca em 0.
async function buscarPacientes({ busca = "", pagina = 0, tamanho = 10, ordenar = "nome", direcao = "asc" } = {}) {
  const params = new URLSearchParams({ pagina, tamanho, ordenar, direcao });
  if (busca.trim()) params.set("busca", busca.trim());
  const resp = await apiFetch(`/pacientes?${params}`);
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

async function criarPaciente(dados) {
  const resp = await apiFetch("/pacientes", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

async function atualizarPaciente(id, dados) {
  const resp = await apiFetch(`/pacientes/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

async function removerPaciente(id) {
  const resp = await apiFetch(`/pacientes/${id}`, { method: "DELETE" });
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
}

// ----- Consultas -----
async function criarConsulta(pacienteId, observacoes) {
  const resp = await apiFetch("/consultas", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ pacienteId, observacoes }),
  });
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

// Retorna as consultas do paciente ja com as imagens + resultado da IA embutidos
async function listarConsultasDetalhado(pacienteId) {
  const resp = await apiFetch(`/consultas/paciente/${pacienteId}/detalhado`);
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

// Prontuario = dados do paciente + consultas detalhadas, numa chamada so
async function buscarProntuario(pacienteId) {
  const resp = await apiFetch(`/prontuarios/${pacienteId}`);
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

// ----- Imagens / IA -----
async function uploadImagem(consultaId, arquivo) {
  const formData = new FormData();
  formData.append("arquivo", arquivo);

  const token = getToken();
  const resp = await fetch(`${API_BASE}/imagens/upload/${consultaId}`, {
    method: "POST",
    headers: { Authorization: "Bearer " + token },
    body: formData,
  });
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

// Pede uma nova analise da IA para uma imagem ja enviada (ex.: a IA estava fora do ar)
async function reanalisarImagem(imagemId) {
  const resp = await apiFetch(`/imagens/analisar/${imagemId}`, { method: "POST" });
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

// ----- Validacao (human-in-the-loop) -----
async function validarResultado(resultadoId, diagnosticoValidado) {
  const resp = await apiFetch(`/resultados/${resultadoId}/validar`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ diagnosticoValidado }),
  });
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}

// ----- Dashboard (KPIs) -----
async function buscarResumoDashboard() {
  const resp = await apiFetch("/dashboard/resumo");
  if (!resp.ok) throw new Error(await extrairMensagemErro(resp));
  return resp.json();
}
