FROM maven:3.9.8-eclipse-temurin-17

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

RUN mvn exec:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install --with-deps chromium"

EXPOSE 8080

CMD ["java","-jar","target/hotel-scrape-1.0.jar"]