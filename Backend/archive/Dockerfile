FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# LibreOffice + Tesseract + langues
RUN apt-get update && apt-get install -y \
    libreoffice \
    libreoffice-writer \
    libreoffice-calc \
    libreoffice-impress \
    tesseract-ocr \
    tesseract-ocr-fra \
    tesseract-ocr-eng \
    tesseract-ocr-ara \
    fonts-liberation \
    fonts-dejavu \
    && rm -rf /var/lib/apt/lists/*

# Répertoire temporaire pour les conversions LibreOffice
RUN mkdir -p /tmp/libreoffice-work && chmod 777 /tmp/libreoffice-work

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]