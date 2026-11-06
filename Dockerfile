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

# Ajustes para a instancia gratuita (512 MB de RAM e 0,1 CPU). O limite vale
# para o processo inteiro, entao heap + metaspace + code cache precisam caber
# juntos com folga. O fuso de Brasilia faz os horarios sairem iguais ao local
# (o container roda em UTC).
ENV JAVA_OPTS="-Xmx256m -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Duser.timezone=America/Sao_Paulo"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
