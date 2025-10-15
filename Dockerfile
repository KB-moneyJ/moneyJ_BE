# Dockerfile
# ----- build stage -----
FROM gradle:8.8-jdk17 AS build
WORKDIR /src
COPY . .
RUN gradle clean bootJar --no-daemon

# ----- run stage -----
FROM eclipse-temurin:17-jre
WORKDIR /
COPY --from=build /src/build/libs/*-SNAPSHOT.jar /app.jar
# JVM 옵션/프로필을 ENV로 받을 수 있게
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app.jar"]
