FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY ConfigService/pom.xml ./ConfigService/
COPY ScrapperService/pom.xml ./ScrapperService/
COPY VectorService/pom.xml ./VectorService/
RUN mvn dependency:go-offline -B

COPY ConfigService ./ConfigService
COPY ScrapperService ./ScrapperService
COPY VectorService ./VectorService

RUN mvn clean package -DskipTests -pl VectorService -am

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/VectorService/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
