-- =====================================================================
-- Dados de teste para demonstracao (TCC - Plataforma Dermatologica)
-- =====================================================================
-- Execute SOMENTE depois de ter rodado o backend pelo menos uma vez
-- (para o Hibernate criar as tabelas). Rode no MySQL Workbench, conectado
-- ao banco "dermato_db".
--
-- Antes de rodar, copie as imagens da pasta "seed-images/" (na raiz do
-- projeto) para dentro da pasta "uploads/" do backend (crie a pasta
-- "uploads" ao lado de onde o backend roda, se ela ainda nao existir).
-- Sem isso, os previews de imagem aparecerao quebrados.
--
-- Login de demonstracao criado por este script:
--   E-mail: demo@dermato.com
--   Senha:  Senha123!
-- =====================================================================

-- Selecione o schema correto no Workbench antes de executar:
--   local: dermato_db    |    Aiven (nuvem): defaultdb

-- ----- Medico de demonstracao -----
-- (a senha ja esta em hash BCrypt correspondente a "Senha123!")
INSERT INTO medicos (nome, email, senha, crm, criado_em)
VALUES ('Dra. Ana Beatriz Souza', 'demo@dermato.com',
        '$2a$10$XewYtUDyKXgr3bHixd7WAe.C0ddLqAHzXOUqS/X2gQgq.9AwLmwvu',
        'CRM-SP 123456', NOW());
SET @medico_id = LAST_INSERT_ID();

-- ----- Pacientes de demonstracao -----
INSERT INTO pacientes (nome, cpf, data_nascimento, telefone, email, criado_em) VALUES
('Carlos Eduardo Lima',        '111.222.333-96', '1985-04-12', '(18) 99999-0001', 'carlos.lima@exemplo.com', NOW()),
('Marina Ferreira Costa',      '222.333.444-05', '1992-09-30', '(18) 99999-0002', 'marina.costa@exemplo.com', NOW()),
('Joao Pedro Almeida',         '333.444.555-08', '1978-01-22', '(18) 99999-0003', 'joao.almeida@exemplo.com', NOW()),
('Beatriz Santos Oliveira',    '444.555.666-19', '2001-06-15', '(18) 99999-0004', 'beatriz.oliveira@exemplo.com', NOW()),
('Ricardo Nunes Silva',        '555.666.777-20', '1965-11-03', '(18) 99999-0005', 'ricardo.silva@exemplo.com', NOW()),
('Fernanda Rodrigues Martins', '666.777.888-30', '1990-03-27', '(18) 99999-0006', 'fernanda.martins@exemplo.com', NOW()),
('Paulo Henrique Souza',       '777.888.999-41', '1988-08-19', '(18) 99999-0007', 'paulo.souza@exemplo.com', NOW()),
('Juliana Alves Pereira',      '888.999.000-78', '1995-12-05', '(18) 99999-0008', 'juliana.pereira@exemplo.com', NOW());

-- Banco populado com uma versao ANTIGA deste seed (CPFs sem digito
-- verificador valido): NAO rode o script inteiro, rode so os UPDATEs abaixo
-- (tire os "-- " da frente, selecione e Ctrl+Shift+Enter).
-- UPDATE pacientes SET cpf = '111.222.333-96' WHERE cpf = '111.222.333-44';
-- UPDATE pacientes SET cpf = '222.333.444-05' WHERE cpf = '222.333.444-55';
-- UPDATE pacientes SET cpf = '333.444.555-08' WHERE cpf = '333.444.555-66';
-- UPDATE pacientes SET cpf = '444.555.666-19' WHERE cpf = '444.555.666-77';
-- UPDATE pacientes SET cpf = '555.666.777-20' WHERE cpf = '555.666.777-88';
-- UPDATE pacientes SET cpf = '666.777.888-30' WHERE cpf = '666.777.888-99';
-- UPDATE pacientes SET cpf = '777.888.999-41' WHERE cpf = '777.888.999-00';
-- UPDATE pacientes SET cpf = '888.999.000-78' WHERE cpf = '888.999.000-11';

-- ----- Pacientes extras (sem consultas) -----
-- Existem para a lista de Pacientes ter mais de uma pagina (10 por pagina) e
-- permitir demonstrar busca, ordenacao e paginacao. CPFs com digito
-- verificador valido, mas ficticios. Datas de cadastro variadas para a
-- ordenacao "Cadastro mais recente/antigo" fazer diferenca.
-- Banco que JA tem o seed: execute so este INSERT (selecione e Ctrl+Shift+Enter).
INSERT INTO pacientes (nome, cpf, data_nascimento, telefone, email, criado_em) VALUES
('Amanda Cristina Rocha',      '114.170.753-50', '1983-02-14', '(18) 99999-0009', 'amanda.rocha@exemplo.com',     DATE_SUB(NOW(), INTERVAL 90 DAY)),
('Bruno Henrique Teixeira',    '158.813.998-03', '1997-07-08', '(18) 99999-0010', 'bruno.teixeira@exemplo.com',   DATE_SUB(NOW(), INTERVAL 82 DAY)),
('Camila Duarte Ribeiro',      '168.976.846-00', '1989-10-21', '(18) 99999-0011', 'camila.ribeiro@exemplo.com',   DATE_SUB(NOW(), INTERVAL 75 DAY)),
('Diego Augusto Moreira',      '557.616.987-57', '1972-05-30', '(18) 99999-0012', 'diego.moreira@exemplo.com',    DATE_SUB(NOW(), INTERVAL 68 DAY)),
('Eduarda Lopes Cardoso',      '645.568.151-77', '2003-01-17', '(18) 99999-0013', 'eduarda.cardoso@exemplo.com',  DATE_SUB(NOW(), INTERVAL 61 DAY)),
('Felipe Augusto Barbosa',     '698.797.309-17', '1980-12-02', '(18) 99999-0014', 'felipe.barbosa@exemplo.com',   DATE_SUB(NOW(), INTERVAL 54 DAY)),
('Gabriela Nogueira Pinto',    '766.344.002-10', '1994-04-25', '(18) 99999-0015', 'gabriela.pinto@exemplo.com',   DATE_SUB(NOW(), INTERVAL 47 DAY)),
('Henrique Mendes Castro',     '778.486.488-42', '1968-09-11', '(18) 99999-0016', 'henrique.castro@exemplo.com',  DATE_SUB(NOW(), INTERVAL 40 DAY)),
('Isabela Fonseca Prado',      '819.672.731-38', '1999-06-03', '(18) 99999-0017', 'isabela.prado@exemplo.com',    DATE_SUB(NOW(), INTERVAL 33 DAY)),
('Lucas Gabriel Monteiro',     '884.472.940-72', '1986-03-19', '(18) 99999-0018', 'lucas.monteiro@exemplo.com',   DATE_SUB(NOW(), INTERVAL 26 DAY)),
('Natalia Freitas Azevedo',    '978.753.477-18', '1991-11-28', '(18) 99999-0019', 'natalia.azevedo@exemplo.com',  DATE_SUB(NOW(), INTERVAL 19 DAY)),
('Otavio Ramos Vieira',        '978.809.320-54', '1975-08-07', '(18) 99999-0020', 'otavio.vieira@exemplo.com',    DATE_SUB(NOW(), INTERVAL 12 DAY));

SET @p1 = (SELECT id FROM pacientes WHERE cpf = '111.222.333-96');
SET @p2 = (SELECT id FROM pacientes WHERE cpf = '222.333.444-05');
SET @p3 = (SELECT id FROM pacientes WHERE cpf = '333.444.555-08');
SET @p4 = (SELECT id FROM pacientes WHERE cpf = '444.555.666-19');
SET @p5 = (SELECT id FROM pacientes WHERE cpf = '555.666.777-20');
SET @p6 = (SELECT id FROM pacientes WHERE cpf = '666.777.888-30');
SET @p7 = (SELECT id FROM pacientes WHERE cpf = '777.888.999-41');
SET @p8 = (SELECT id FROM pacientes WHERE cpf = '888.999.000-78');

-- =====================================================================
-- Consultas + imagens + resultados de IA
-- Cada bloco: cria a consulta, pega o ID, cria a imagem vinculada, pega
-- o ID, cria o resultado da IA vinculado (validado ou pendente).
-- =====================================================================

-- --- Paciente 1: Carlos - 2 consultas, ambas validadas ---
INSERT INTO consultas (medico_id, paciente_id, data_consulta, observacoes)
VALUES (@medico_id, @p1, DATE_SUB(NOW(), INTERVAL 40 DAY), 'Lesao pigmentada no antebraco esquerdo, bordas irregulares.');
SET @c = LAST_INSERT_ID();
INSERT INTO imagens_dermatologicas (consulta_id, caminho_arquivo, data_upload)
VALUES (@c, 'uploads/seed-lesao-1.jpg', DATE_SUB(NOW(), INTERVAL 40 DAY));
SET @img = LAST_INSERT_ID();
INSERT INTO resultados_ia (imagem_id, vetor_probabilidades, diagnostico_validado, validado, data_resultado, data_validacao)
VALUES (@img, '{"Melanoma":0.12,"Nevo":0.71,"Ceratose":0.08,"Carcinoma_Basocelular":0.05,"Dermatite":0.04}',
        'Nevo melanocitico benigno. Sem indicacao de biopsia. Retorno em 12 meses.', 1,
        DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 39 DAY));

INSERT INTO consultas (medico_id, paciente_id, data_consulta, observacoes)
VALUES (@medico_id, @p1, DATE_SUB(NOW(), INTERVAL 5 DAY), 'Retorno de acompanhamento semestral.');
SET @c = LAST_INSERT_ID();
INSERT INTO imagens_dermatologicas (consulta_id, caminho_arquivo, data_upload)
VALUES (@c, 'uploads/seed-lesao-2.jpg', DATE_SUB(NOW(), INTERVAL 5 DAY));
SET @img = LAST_INSERT_ID();
INSERT INTO resultados_ia (imagem_id, vetor_probabilidades, diagnostico_validado, validado, data_resultado, data_validacao)
VALUES (@img, '{"Melanoma":0.09,"Nevo":0.78,"Ceratose":0.06,"Carcinoma_Basocelular":0.04,"Dermatite":0.03}',
        'Sem alteracoes em relacao a consulta anterior.', 1,
        DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));

-- --- Paciente 2: Marina - 1 consulta pendente de validacao ---
INSERT INTO consultas (medico_id, paciente_id, data_consulta, observacoes)
VALUES (@medico_id, @p2, DATE_SUB(NOW(), INTERVAL 2 DAY), 'Mancha avermelhada na regiao do pescoco, paciente relata coceira.');
SET @c = LAST_INSERT_ID();
INSERT INTO imagens_dermatologicas (consulta_id, caminho_arquivo, data_upload)
VALUES (@c, 'uploads/seed-lesao-3.jpg', DATE_SUB(NOW(), INTERVAL 2 DAY));
SET @img = LAST_INSERT_ID();
INSERT INTO resultados_ia (imagem_id, vetor_probabilidades, validado, data_resultado)
VALUES (@img, '{"Melanoma":0.03,"Nevo":0.10,"Ceratose":0.07,"Carcinoma_Basocelular":0.05,"Dermatite":0.75}',
        0, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- --- Paciente 3: Joao Pedro - 1 consulta validada com suspeita relevante ---
INSERT INTO consultas (medico_id, paciente_id, data_consulta, observacoes)
VALUES (@medico_id, @p3, DATE_SUB(NOW(), INTERVAL 15 DAY), 'Lesao de crescimento rapido no dorso, paciente com historico familiar de melanoma.');
SET @c = LAST_INSERT_ID();
INSERT INTO imagens_dermatologicas (consulta_id, caminho_arquivo, data_upload)
VALUES (@c, 'uploads/seed-lesao-4.jpg', DATE_SUB(NOW(), INTERVAL 15 DAY));
SET @img = LAST_INSERT_ID();
INSERT INTO resultados_ia (imagem_id, vetor_probabilidades, diagnostico_validado, validado, data_resultado, data_validacao)
VALUES (@img, '{"Melanoma":0.64,"Nevo":0.18,"Ceratose":0.05,"Carcinoma_Basocelular":0.09,"Dermatite":0.04}',
        'Suspeita compativel com o alerta da IA. Paciente encaminhado para biopsia excisional.', 1,
        DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY));

-- --- Paciente 4: Beatriz - 1 consulta pendente ---
INSERT INTO consultas (medico_id, paciente_id, data_consulta, observacoes)
VALUES (@medico_id, @p4, DATE_SUB(NOW(), INTERVAL 1 DAY), 'Lesao aspera na testa, paciente relata exposicao solar frequente sem protecao.');
SET @c = LAST_INSERT_ID();
INSERT INTO imagens_dermatologicas (consulta_id, caminho_arquivo, data_upload)
VALUES (@c, 'uploads/seed-lesao-5.jpg', DATE_SUB(NOW(), INTERVAL 1 DAY));
SET @img = LAST_INSERT_ID();
INSERT INTO resultados_ia (imagem_id, vetor_probabilidades, validado, data_resultado)
VALUES (@img, '{"Melanoma":0.05,"Nevo":0.09,"Ceratose":0.68,"Carcinoma_Basocelular":0.13,"Dermatite":0.05}',
        0, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- --- Paciente 5: Ricardo - 2 consultas, uma validada e uma pendente ---
INSERT INTO consultas (medico_id, paciente_id, data_consulta, observacoes)
VALUES (@medico_id, @p5, DATE_SUB(NOW(), INTERVAL 60 DAY), 'Lesao perolada na aba do nariz, sangramento ocasional.');
SET @c = LAST_INSERT_ID();
INSERT INTO imagens_dermatologicas (consulta_id, caminho_arquivo, data_upload)
VALUES (@c, 'uploads/seed-lesao-6.jpg', DATE_SUB(NOW(), INTERVAL 60 DAY));
SET @img = LAST_INSERT_ID();
INSERT INTO resultados_ia (imagem_id, vetor_probabilidades, diagnostico_validado, validado, data_resultado, data_validacao)
VALUES (@img, '{"Melanoma":0.07,"Nevo":0.05,"Ceratose":0.11,"Carcinoma_Basocelular":0.70,"Dermatite":0.07}',
        'Carcinoma basocelular confirmado por biopsia. Paciente encaminhado para exerese cirurgica.', 1,
        DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 55 DAY));

INSERT INTO consultas (medico_id, paciente_id, data_consulta, observacoes)
VALUES (@medico_id, @p5, DATE_SUB(NOW(), INTERVAL 3 DAY), 'Retorno pos-operatorio, avaliacao de nova area suspeita na regiao das costas.');
SET @c = LAST_INSERT_ID();
INSERT INTO imagens_dermatologicas (consulta_id, caminho_arquivo, data_upload)
VALUES (@c, 'uploads/seed-lesao-7.jpg', DATE_SUB(NOW(), INTERVAL 3 DAY));
SET @img = LAST_INSERT_ID();
INSERT INTO resultados_ia (imagem_id, vetor_probabilidades, validado, data_resultado)
VALUES (@img, '{"Melanoma":0.14,"Nevo":0.62,"Ceratose":0.10,"Carcinoma_Basocelular":0.08,"Dermatite":0.06}',
        0, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- --- Paciente 6: Fernanda - 1 consulta validada ---
INSERT INTO consultas (medico_id, paciente_id, data_consulta, observacoes)
VALUES (@medico_id, @p6, DATE_SUB(NOW(), INTERVAL 20 DAY), 'Rotina de check-up anual de pele.');
SET @c = LAST_INSERT_ID();
INSERT INTO imagens_dermatologicas (consulta_id, caminho_arquivo, data_upload)
VALUES (@c, 'uploads/seed-lesao-8.jpg', DATE_SUB(NOW(), INTERVAL 20 DAY));
SET @img = LAST_INSERT_ID();
INSERT INTO resultados_ia (imagem_id, vetor_probabilidades, diagnostico_validado, validado, data_resultado, data_validacao)
VALUES (@img, '{"Melanoma":0.06,"Nevo":0.73,"Ceratose":0.09,"Carcinoma_Basocelular":0.05,"Dermatite":0.07}',
        'Nevo estavel, sem alteracoes suspeitas. Manter rotina anual.', 1,
        DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY));

-- --- Paciente 7: Paulo - consulta sem imagem ainda (so a anamnese) ---
INSERT INTO consultas (medico_id, paciente_id, data_consulta, observacoes)
VALUES (@medico_id, @p7, DATE_SUB(NOW(), INTERVAL 7 DAY), 'Primeira consulta. Paciente relata coceira generalizada, sem lesao especifica ainda fotografada.');

-- --- Paciente 8: Juliana - consulta sem imagem ainda ---
INSERT INTO consultas (medico_id, paciente_id, data_consulta, observacoes)
VALUES (@medico_id, @p8, DATE_SUB(NOW(), INTERVAL 10 DAY), 'Avaliacao de rotina, aguardando agendamento para fotografia da lesao.');

-- =====================================================================
-- Resultado esperado no Dashboard apos rodar este script:
--   - 20 pacientes (8 com consultas + 12 extras), 10 consultas, 8 imagens analisadas
--   - Tela de Pacientes: 2 paginas de 10
--   - 5 diagnosticos validados, 3 pendentes de validacao
--   - Distribuicao de suspeitas: Nevo (4), Melanoma (1), Ceratose (1),
--     Carcinoma_Basocelular (1), Dermatite (1)
-- =====================================================================
