FROM ghcr.io/openbimrl/openbimrl-engine:2024.11.03

RUN rm -rf /app
WORKDIR /app

COPY . .

RUN mvn install

CMD ["mvn", "spring-boot:run"]
EXPOSE 8080
