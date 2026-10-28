# ===== Build do backend Spring Boot (contexto = raiz do repositorio) =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
RUN mvn -q -B -DskipTests package

# ===== Imagem final, leve =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# Imagens do seed ficam disponiveis em /app/uploads para as telas de demonstracao.
# ATENCAO: no plano gratuito do Render o disco e efemero - uploads feitos
# pelo sistema sao perdidos quando o servico reinicia/dorme/e reimplantado.
COPY seed-images/ /app/uploads/

# Ajustes para a instancia gratuita (512 MB de RAM e 0,1 CPU)
ENV JAVA_OPTS="-Xmx350m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
