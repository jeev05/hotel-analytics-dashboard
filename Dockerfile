FROM maven:3.9.8-eclipse-temurin-17

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY . .

RUN mvn clean package -DskipTests

RUN mvn exec:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install --with-deps chromium"

ENV PLAYWRIGHT_BROWSERS_PATH=/root/.cache/ms-playwright

EXPOSE 9090

CMD ["java","-jar","target/hotel-scrape-1.0.jar","--server.port=9090"]