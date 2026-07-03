# Stage 1: Build the React frontend
FROM node:20-slim AS frontend-build

WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci --silent
COPY frontend/ ./
RUN npm run build
# Output is now at /frontend/dist/


# Stage 2: Build and run the Spring Boot backend
FROM mcr.microsoft.com/playwright/java:v1.45.0-jammy

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy the built frontend into Spring Boot's static resources directory
# so it gets packaged INSIDE the JAR — no filesystem dependency at runtime
COPY --from=frontend-build /frontend/dist/ src/main/resources/static/

# Copy Java source and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests -q

# Playwright browser path (already installed in base image)
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

# JVM memory limits for Render free tier (512MB RAM)
ENV JAVA_OPTS="-Xmx400m -Xms128m -XX:+UseContainerSupport"

EXPOSE 9090

CMD ["sh", "-c", "java $JAVA_OPTS -jar target/hotel-scrape-1.0.jar"]
