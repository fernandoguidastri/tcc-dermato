"""
Microsservico de IA - Plataforma Inteligente para Educacao Dermatologica (TCC)

Recebe a foto de uma lesao (POST /predict, campo multipart "arquivo") e devolve
o vetor de probabilidades das 7 classes do HAM10000 em JSON, conforme a secao
4.1 do documento do TCC.

O modelo e uma EfficientNet-B0 treinada no notebook ml/treino_ham10000.ipynb e
exportada para ONNX, com a calibracao (temperature scaling) e a softmax ja
embutidas: a saida do modelo ja sao as probabilidades. Os parametros de
entrada e os nomes das classes vem do modelo/classes.json, gerado pelo notebook.

IMPORTANTE: o pre-processamento abaixo precisa ser IGUAL ao da secao 5 do
notebook (correcao EXIF, recorte do quadrado central, 224x224 bilinear e
normalizacao do ImageNet). Se mudar la, mude aqui.

A saida e apoio a decisao, nao diagnostico: quem decide e o medico.
"""

import io
import json
from pathlib import Path

import numpy as np
import onnxruntime as ort
from fastapi import FastAPI, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from PIL import Image, ImageOps

PASTA_MODELO = Path(__file__).parent / "modelo"
CONFIG = json.loads((PASTA_MODELO / "classes.json").read_text(encoding="utf-8"))

ENTRADA = CONFIG["entrada"]
TAMANHO = ENTRADA["tamanho"]
MEDIA = np.array(ENTRADA["media"], dtype=np.float32)
DESVIO = np.array(ENTRADA["desvio"], dtype=np.float32)
NOMES = [classe["nome"] for classe in CONFIG["classes"]]

# Uma thread so: o plano gratuito do Render tem pouca CPU, e mais threads
# so disputariam o mesmo processador.
_opcoes = ort.SessionOptions()
_opcoes.intra_op_num_threads = 1
SESSAO = ort.InferenceSession(str(PASTA_MODELO / CONFIG["modelo"]),
                              sess_options=_opcoes, providers=["CPUExecutionProvider"])

app = FastAPI(title="Dermato AI Microservice", version="1.0.0",
              description="Classificacao de lesoes de pele (HAM10000, EfficientNet-B0, ONNX). "
                          "Apoio a decisao clinica: nao e diagnostico.")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


def preprocessar(conteudo_arquivo: bytes) -> np.ndarray:
    """Bytes da imagem -> tensor float32 (1, 3, 224, 224), igual ao notebook."""
    with Image.open(io.BytesIO(conteudo_arquivo)) as img:
        img = ImageOps.exif_transpose(img).convert("RGB")
        largura, altura = img.size
        lado = min(largura, altura)
        esquerda, topo = (largura - lado) // 2, (altura - lado) // 2
        img = img.crop((esquerda, topo, esquerda + lado, topo + lado))
        img = img.resize((TAMANHO, TAMANHO), Image.Resampling.BILINEAR)
    x = np.asarray(img, dtype=np.float32) / 255.0
    x = (x - MEDIA) / DESVIO
    return np.ascontiguousarray(x.transpose(2, 0, 1)[None], dtype=np.float32)


def prever(conteudo_arquivo: bytes) -> dict:
    """Roda o modelo e devolve {classe: probabilidade}, da mais para a menos provavel."""
    probabilidades = SESSAO.run(None, {ENTRADA["nome"]: preprocessar(conteudo_arquivo)})[0][0]
    if not np.all(np.isfinite(probabilidades)):
        # NaN/infinito nao vira JSON valido; o backend receberia um erro 500 sem explicacao
        raise ValueError("o modelo devolveu valores invalidos")
    pares = sorted(zip(NOMES, probabilidades), key=lambda par: par[1], reverse=True)
    return {nome: round(float(p), 4) for nome, p in pares}


@app.get("/", summary="Status do servico")
def status():
    return {"status": "ok", "servico": "Dermato AI Microservice", "modelo": CONFIG["modelo"], "classes": NOMES}


@app.post("/predict", summary="Classificar a imagem de uma lesao",
          description="Recebe a imagem no campo multipart 'arquivo' e devolve o vetor de "
                      "probabilidades das 7 classes (somam ~1), ordenado da maior para a menor.")
async def predict(arquivo: UploadFile = File(...)):
    conteudo = await arquivo.read()
    if not conteudo:
        return JSONResponse(status_code=400, content={"erro": "Arquivo vazio."})
    try:
        return prever(conteudo)
    except (OSError, ValueError, Image.DecompressionBombError):
        return JSONResponse(status_code=400, content={"erro": "O arquivo enviado nao e uma imagem valida."})
