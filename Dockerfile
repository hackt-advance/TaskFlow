FROM eclipse-temurin:22-jdk

WORKDIR /app

COPY src ./src

RUN mkdir -p out && javac -d out $(find src/main/java -name "*.java")

EXPOSE 8080

CMD ["java", "-cp", "out", "com.example.tasktracker.TaskTrackerApplication"]

