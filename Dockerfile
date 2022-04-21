FROM maven:3.8.5-openjdk-18 as DEPENDENCIES
WORKDIR /app
COPY Client/pom.xml Client/pom.xml
COPY KeyStore/pom.xml KeyStore/pom.xml
COPY Proto/pom.xml Proto/pom.xml
COPY Server/pom.xml Server/pom.xml
COPY pom.xml .

RUN mvn compile

FROM maven:3.8.5-openjdk-18 as BUILDER
WORKDIR /app
COPY --from=dependencies /root/.m2 /root/.m2
COPY --from=dependencies /app /app

COPY KeyStore/src /app/KeyStore/src
COPY Proto/src /app/Proto/src
COPY Server/src /app/Server/src

RUN mvn -B -e clean install

FROM openjdk:18

COPY --from=builder /app/Server/target/server-jar-with-dependencies.jar /app/server.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar","server.jar" , "admin"]
EXPOSE 8080