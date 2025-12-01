# 자바 17 기반의 이미지 사용
FROM amazoncorretto:17

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# JAR 파일을 컨테이너에 복사
# build/libs/your-app.jar는 빌드된 파일의 경로와 이름에 맞게 수정
ARG JAR_FILE=/build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 컨테이너가 실행될 때 JAR 파일 실행
ENTRYPOINT ["java","-jar","/app.jar"]
