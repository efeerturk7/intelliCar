# ---------- 1. Build aşaması ----------
# DÜZELTME: pom.xml java.version=21 olduğu için Java 17 imajıyla derleme hata verirdi. 17 -> 21.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Önce sadece pom.xml: bağımlılıklar cache'lenir, kod değişince tekrar inmez
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B


# ---------- 2. Çalışma aşaması ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Root olmayan kullanıcı ile çalıştır (güvenlik)
RUN addgroup -S spring && adduser -S spring -G spring
# Yüklenen ilan görselleri için yazılabilir klasör (docker-compose'ta kalıcı volume'a bağlanır)
RUN mkdir -p /app/uploads && chown spring:spring /app/uploads
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Container'ın verdiği bellek sınırına göre heap ayarla
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
