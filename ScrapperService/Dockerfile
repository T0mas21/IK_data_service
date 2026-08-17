FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY ConfigService/pom.xml ./ConfigService/
COPY ScrapperService/pom.xml ./ScrapperService/
RUN mvn dependency:go-offline -B

COPY ConfigService ./ConfigService
COPY ScrapperService ./ScrapperService

RUN mvn clean package -DskipTests -pl ScrapperService -am

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/ScrapperService/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]