FROM ghcr.io/openbimrl/openbimrl-engine:2024.07.26

RUN rm -rf /app
WORKDIR /app

COPY . .

RUN mvn install

CMD ["mvn", "spring-boot:run"]
EXPOSE 8080