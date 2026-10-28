# Plataforma Inteligente para Educação Dermatológica

Protótipo desenvolvido como Trabalho de Conclusão de Curso de Engenharia de
Computação (UNILINS, 2026).

O sistema apoia a análise de lesões de pele. O médico cadastra o paciente,
registra a consulta e envia uma foto da lesão, que pode ser tirada pela câmera
do celular. Um microsserviço de IA devolve as probabilidades de cada classe de
lesão, e o médico registra o diagnóstico final. A IA serve apenas como apoio:
quem decide é sempre o médico (*human-in-the-loop*).

> Protótipo acadêmico. Não é um dispositivo médico e usa somente dados fictícios.

## Funcionalidades

- Cadastro e login de médicos (JWT, senha com BCrypt).
- Cadastro, edição, exclusão e busca de pacientes, com paginação e ordenação.
- Registro de consultas e envio de imagens, com opção de tirar a foto pelo celular.
- Análise da imagem pelo microsserviço de IA e validação do diagnóstico pelo médico.
- Histórico de consultas por paciente, com o médico responsável por cada uma.
- Dashboard com totais de pacientes, consultas, imagens analisadas e casos pendentes de validação.

Os pacientes são compartilhados entre os médicos cadastrados, como em uma
clínica: o prontuário pertence à instituição e cada consulta registra quem
atendeu.

## Tecnologias

- **Back-end:** Java 17, Spring Boot 3.2 (Web, Data JPA, Security, Validation), JWT, Swagger (springdoc).
- **Banco de dados:** MySQL 8.
- **Front-end:** HTML, CSS e JavaScript, sem frameworks, servido pelo próprio Spring Boot.
- **IA:** Python com FastAPI. Por enquanto é um simulador; o modelo real está em desenvolvimento.

## Estrutura

```
tcc-dermato/
├── backend/        API Spring Boot e páginas do front-end (src/main/resources/static)
├── ai-service/     microsserviço de IA (FastAPI)
├── seed.sql        dados fictícios para demonstração
├── seed-images/    imagens usadas pelo seed
├── Dockerfile      imagem do back-end para o deploy
└── render.yaml     configuração dos serviços no Render
```

## Como rodar localmente

Pré-requisitos: JDK 17, MySQL 8 e Python 3.12.

### 1. Banco de dados

O banco `dermato_db` é criado automaticamente na primeira execução, e as
tabelas são geradas pelo Hibernate.

Em `backend/src/main/resources/`, copie `application-local.properties.example`
para `application-local.properties` e preencha a senha do seu MySQL. Esse
arquivo é ignorado pelo Git.

### 2. Microsserviço de IA

```bash
cd ai-service
py -3.12 -m venv venv
venv\Scripts\activate.bat        # Linux/macOS: source venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

No Windows, use o **cmd**, porque o PowerShell costuma bloquear o script de
ativação do venv. O serviço fica em `http://localhost:8000`.

O simulador atual devolve probabilidades fixas para cada imagem (calculadas a
partir do conteúdo do arquivo), apenas para testar o fluxo completo.

### 3. Back-end

Abra a pasta `backend/` no IntelliJ IDEA e rode a classe `DermatoApplication`,
ou, com o Maven instalado:

```bash
cd backend
mvn spring-boot:run
```

Acesse `http://localhost:8080`.

## Dados de demonstração

O script `seed.sql` cria um médico de teste e 20 pacientes fictícios, sendo 8
deles com consultas, imagens e resultados da IA (validados e pendentes).

1. Rode o back-end uma vez para as tabelas serem criadas.
2. Copie as imagens de `seed-images/` para a pasta `uploads/` do back-end
   (geralmente `backend/uploads/`; crie a pasta se ela não existir).
3. No MySQL Workbench, selecione o banco `dermato_db` e execute o `seed.sql` inteiro.
4. Entre com `demo@dermato.com` / `Senha123!`.

As imagens do seed são ilustrações genéricas, não fotos médicas reais.

## Uso no celular

O sistema é responsivo e usa a câmera do celular pelo navegador (botão
"Tirar foto" na tela de consulta). Antes do envio, a foto é reduzida no próprio
aparelho.

Para testar na rede local, abra `http://IP-DO-PC:8080` no celular (o IP aparece
no `ipconfig`). Se a página não abrir, libere a porta 8080 no Firewall do
Windows.

## Deploy

O deploy usa dois serviços no Render, descritos no `render.yaml`:
`dermato-app` (back-end e front-end, via `Dockerfile`) e `dermato-ai`
(microsserviço de IA). O banco é um MySQL gratuito da Aiven.

Variáveis de ambiente do `dermato-app`:

| Variável | Valor |
|---|---|
| `DB_URL` | `jdbc:mysql://HOST:PORTA/defaultdb?sslMode=REQUIRED&serverTimezone=UTC` |
| `DB_USERNAME` | usuário da Aiven (`avnadmin`) |
| `DB_PASSWORD` | senha da Aiven |
| `AI_SERVICE_URL` | URL do `dermato-ai` terminando em `/predict` |
| `JWT_SECRET` | gerada pelo Render |

No plano gratuito, os serviços entram em repouso depois de 15 minutos sem uso
e levam cerca de 1 minuto para voltar. As imagens enviadas também se perdem a
cada reinício; os dados do banco são mantidos.

## API

Com o back-end rodando, a documentação interativa fica em
`http://localhost:8080/swagger-ui.html`. Para testar as rotas protegidas, faça
login em `POST /api/auth/login`, clique em **Authorize** e cole o token.

Todas as rotas, exceto cadastro e login, exigem o cabeçalho
`Authorization: Bearer <token>`. O token vale por 8 horas. Erros são devolvidos
no formato `{"erro": "mensagem"}`.

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/medicos` ou `/api/auth/cadastro` | Cadastrar médico |
| POST | `/api/auth/login` | Login (retorna o token) |
| GET | `/api/medicos/{id}` | Buscar médico |
| GET | `/api/pacientes` | Listar pacientes (aceita `pagina`, `tamanho`, `busca`, `ordenar` e `direcao`) |
| POST | `/api/pacientes` | Cadastrar paciente |
| GET / PUT / DELETE | `/api/pacientes/{id}` | Buscar, editar ou excluir paciente |
| POST | `/api/consultas/{pacienteId}` | Registrar consulta |
| GET | `/api/consultas?pacienteId={id}` | Consultas de um paciente |
| GET | `/api/prontuarios/{pacienteId}` | Prontuário completo do paciente |
| POST | `/api/imagens/upload` | Enviar imagem (`consultaId` e `arquivo`) e analisar |
| POST | `/api/imagens/analisar/{id}` | Reanalisar uma imagem ainda não validada |
| PUT | `/api/resultados/{id}/validar` | Registrar o diagnóstico do médico |
| GET | `/api/dashboard/resumo` | Indicadores do dashboard |

## Próximos passos

- Substituir o simulador por um modelo treinado com a base HAM10000
  (Tschandl et al., 2018), servido com ONNX Runtime.
- Testes automatizados (JUnit no back-end e pytest na IA) e avaliação de
  usabilidade com o questionário SUS.
- Testes no celular pelo ambiente publicado no Render.
