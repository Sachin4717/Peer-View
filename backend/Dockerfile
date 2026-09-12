# ---------- Stage 1: Build the jar with Maven + JDK 21 ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy only the pom first so Docker can cache the dependency download layer
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Now copy the rest of the source and build
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---------- Stage 2: Slim runtime image ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy only the built jar from the build stage (name-agnostic)
COPY --from=build /app/target/*.jar app.jar

# Render injects PORT at runtime; application.properties reads ${PORT:5001}
ENV PORT=5001
EXPOSE 5001

ENTRYPOINT ["java", "-jar", "app.jar"]
