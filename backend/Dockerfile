# Maven'in resmi imajı: Maven kurulu geliyor.
#
# <b>Neden değişti:</b> önceden temel imaj `eclipse-temurin:25-jdk` idi ve Maven
# build sırasında `archive.apache.org`'dan tarball olarak indiriliyordu. O adres
# arşiv sunucusudur (CDN değil) ve 21 Ağustos 2026'da yanıt vermedi: `curl`
# zaman aşımı olmadığı için adım ~10 dakika asılı kaldı, sonra
# "curl: (56) Recv failure" ile pipeline düştü. Yani her deploy, kod ile hiç
# ilgisi olmayan tek bir dış adresin ayakta olmasına bağlıydı.
#
# Resmi imaj bu bağımlılığı da, `apt-get install curl` adımını da ortadan
# kaldırıyor; Docker Hub zaten çekilmek zorunda olan bir kayıt defteri.
FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

# -B (batch): Jenkins konsolunda ilerleme çubuğu yerine düz satır üretir.
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/scrumtools-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
