"""
Microservico de IA - Plataforma de Educacao Dermatologica (TCC)

Este servico simula a inferencia de uma rede neural convolucional (CNN)
para classificacao de imagens dermatologicas. Recebe uma imagem via POST,
"processa" e retorna um vetor de probabilidades no formato JSON, conforme
definido na secao 4.1 do documento do TCC.

Para o protótipo (entrega ~70%), o modelo real ainda nao esta treinado/integrado,
entao este endpoint gera probabilidades de forma controlada (pseudo-aleatoria,
mas deterministica por imagem) para permitir a demonstracao completa do fluxo
Java <-> Python <-> Medico (human-in-the-loop). Basta substituir a funcao
`prever` pelo carregamento real do modelo (ex: TensorFlow/PyTorch) quando estiver pronto.
"""

import hashlib
import random

from fastapi import FastAPI, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="Dermato AI Microservice", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

CATEGORIAS = ["Melanoma", "Nevo", "Ceratose", "Carcinoma_Basocelular", "Dermatite"]


def prever(conteudo_arquivo: bytes) -> dict:
    """
    Simula a inferencia do modelo. Usa o hash do arquivo como seed para que
    a mesma imagem sempre retorne o mesmo resultado (comportamento previsivel
    para fins de demonstracao e teste).

    TODO (pos-70%): substituir por carregamento real do modelo treinado,
    pre-processamento da imagem (resize, normalizacao) e inferencia real.
    """
    seed = int(hashlib.md5(conteudo_arquivo).hexdigest(), 16) % (2**32)
    rng = random.Random(seed)

    valores = [rng.random() for _ in CATEGORIAS]
    soma = sum(valores)
    probabilidades = {cat: round(v / soma, 4) for cat, v in zip(CATEGORIAS, valores)}

    return probabilidades


@app.get("/")
def status():
    return {"status": "ok", "servico": "Dermato AI Microservice"}


@app.post("/predict")
async def predict(arquivo: UploadFile = File(...)):
    conteudo = await arquivo.read()
    probabilidades = prever(conteudo)
    return probabilidades
