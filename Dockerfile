FROM maven:3.9.8-eclipse-temurin-17

WORKDIR /app

# Copy Maven configuration and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the project
COPY . .

# Build the Spring Boot application
RUN mvn clean package -DskipTests

# Playwright browser location
ENV PLAYWRIGHT_BROWSERS_PATH=/root/.cache/ms-playwright

# Install Chromium for Playwright
RUN mvn exec:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install chromium"

# Give proper permissions
RUN chmod -R 755 /root/.cache/ms-playwright

EXPOSE 9090

CMD ["java","-jar","target/hotel-scrape-1.0.jar"]