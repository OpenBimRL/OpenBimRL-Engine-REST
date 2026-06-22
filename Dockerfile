FROM ghcr.io/openbimrl/openbimrl-engine:2026.06.22

RUN rm -rf /app
WORKDIR /app

COPY . .

RUN mvn install

CMD ["mvn", "spring-boot:run"]
EXPOSE 8080
