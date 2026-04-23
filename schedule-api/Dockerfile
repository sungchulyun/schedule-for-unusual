FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon dependencies

COPY src ./src

RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=builder /workspace/build/libs/*.jar ./app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar /app/app.jar"]
