FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/lumina-server-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
