FROM node:22 AS frontend
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build/backend
COPY backend/pom.xml ./
RUN mvn -B dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /build/frontend/dist ./src/main/resources/static
RUN mvn -B clean verify
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /build/backend/target/brain-bucket-backend-0.0.1-SNAPSHOT.jar /app/app.jar
ENV UPLOAD_DIR=/app/uploads
VOLUME /app/uploads
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
