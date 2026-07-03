# Use the official Playwright Java image as the base — it comes with all required
# system dependencies for headless Chromium pre-installed (libnss3, libatk, libgbm,
# libxcomposite, etc.). Using a plain JDK image and trying to install those manually
# is fragile and the #1 reason Playwright fails on cloud deployments.
FROM mcr.microsoft.com/playwright/java:v1.45.0-jammy

WORKDIR /app

# Install Maven (not included in the Playwright image)
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy Maven config first for layer caching — dependencies only re-download
# when pom.xml changes, not on every code change.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
COPY frontend ./frontend

RUN mvn clean package -DskipTests -q

# The Playwright image already has Chromium installed at the correct path.
# We just need to tell Playwright where to find it.
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

# Render free tier has 512MB RAM — these JVM flags keep memory usage reasonable
# and prevent OOM kills during scraping.
ENV JAVA_OPTS="-Xmx400m -Xms128m -XX:+UseContainerSupport"

EXPOSE 9090

CMD ["sh", "-c", "java $JAVA_OPTS -jar target/hotel-scrape-1.0.jar"]
