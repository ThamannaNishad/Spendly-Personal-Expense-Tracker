
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/classes ./target/classes
COPY --from=build /app/target/dependency ./target/dependency

EXPOSE 8080

CMD ["java", "-cp", "target/classes:target/dependency/*", "ExpenseServer"]
