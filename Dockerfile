FROM openjdk:23-slim AS builder

ENV LEIN_VERSION=2.11.2 \
    LEIN_HOME=/usr/local/bin

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    git \
    && curl -L https://raw.githubusercontent.com/technomancy/leiningen/$LEIN_VERSION/bin/lein -o $LEIN_HOME/lein \
    && chmod +x $LEIN_HOME/lein \
    && lein self-install \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY resources/logback.xml /app/resources/logback.xml
COPY project.clj /app/
COPY src /app/src

RUN lein uberjar

FROM openjdk:17-slim

WORKDIR /app

COPY --from=builder /app/target/uberjar/*-standalone.jar app.jar
COPY --from=builder /app/resources /app/resources

CMD ["java", "-Dlogback.configurationFile=/app/resources/logback.xml", "-jar", "app.jar"]
