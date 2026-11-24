# ---------- Build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -DskipTests package && cp target/*.jar app.jar

# ---------- Runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home chamados
COPY --from=build /app/app.jar app.jar
USER chamados
ENV SERVER_PORT=8081 JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8081
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]
