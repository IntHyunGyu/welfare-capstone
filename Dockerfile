FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/welfare-capstone-0.0.1-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
# 무료 플랜의 작은 메모리 한도 안에서 힙이 컨테이너 메모리를 넘지 않도록 비율로 잡는다.
# 서버(Railway)는 UTC라, 저장 시각과 "오늘" 판정이 한국 날짜와 어긋나지 않도록 JVM 시간대를 서울로 둔다.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
