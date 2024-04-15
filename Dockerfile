# syntax=docker/dockerfile:1
FROM ghcr.io/openbimrl/openbimrl-engine:latest

RUN rm -rf /app
WORKDIR /app

COPY . .

RUN mvn install

CMD ["mvn", "spring-boot:run"]
EXPOSE 8080