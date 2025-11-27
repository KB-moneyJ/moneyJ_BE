# 1. 자바 17 기반의 이미지 사용
FROM openjdk:17-jdk-slim

# 2. JAR 파일을 컨테이너에 복사
# build/libs/your-app.jar는 빌드된 파일의 경로와 이름에 맞게 수정
ARG JAR_FILE=moneyj/build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 3. 컨테이너가 실행될 때 JAR 파일 실행
ENTRYPOINT ["java","-jar","/app.jar"]
