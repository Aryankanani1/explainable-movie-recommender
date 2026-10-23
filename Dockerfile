# Build stage: the full JDK and Maven (through the wrapper). Tests run in CI, not here.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Dependencies in their own layer, so a source-only change doesn't download them again.
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q -Dmaven.test.skip=true package && cp target/movie-recommender-*.jar app.jar

# Runtime stage: JRE only, non-root.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home recsys
WORKDIR /app
# The first boot imports MovieLens from here (recsys.data-dir is relative to /app).
COPY data/ml-latest-small data/ml-latest-small
COPY --from=build /src/app.jar app.jar
USER recsys
# Heap as a share of the container's memory limit; hosts can override the whole variable.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
