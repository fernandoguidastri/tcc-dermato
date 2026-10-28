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
  }
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
