# syntax=docker/dockerfile:1

# OpenBimRL Engine-REST — standalone Bazel build.
# Depends on the published Engine Maven package (GitHub Packages) and the Engine
# runtime image for IfcOpenShell / OCCT shared libraries.
#
#   docker build -t openbimrl-engine-rest \
#     --build-arg GITHUB_ACTOR=… --build-arg GITHUB_ACCESS_TOKEN=… .

ARG BAZELISK_VERSION=1.29.0
ARG ENGINE_RUNTIME_IMAGE=ghcr.io/openbimrl/openbimrl-engine:latest
ARG GITHUB_ACTOR
ARG GITHUB_ACCESS_TOKEN

FROM eclipse-temurin:21-jdk-noble AS build

ARG BAZELISK_VERSION
ARG GITHUB_ACTOR
ARG GITHUB_ACCESS_TOKEN

ENV DEBIAN_FRONTEND=noninteractive
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH=${JAVA_HOME}/bin:${PATH}

# clang: Bazel rules_cc autoconfigures a local C toolchain even for pure JVM
# targets (transitive via rules_java / protobuf). Without it, analysis fails with
# "Cannot find gcc or CC". Match Engine images: use clang via CC/CXX.
ENV CC=/usr/bin/clang
ENV CXX=/usr/bin/clang++

RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates curl python3 clang \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL "https://github.com/bazelbuild/bazelisk/releases/download/v${BAZELISK_VERSION}/bazelisk-linux-amd64" \
        -o /usr/local/bin/bazel \
    && chmod +x /usr/local/bin/bazel

WORKDIR /app
COPY . .

RUN if [ -n "${GITHUB_ACTOR}" ] && [ -n "${GITHUB_ACCESS_TOKEN}" ]; then \
        printf 'machine maven.pkg.github.com login %s password %s\n' \
            "${GITHUB_ACTOR}" "${GITHUB_ACCESS_TOKEN}" >> /root/.netrc; \
        chmod 600 /root/.netrc; \
    fi

# Alias //:rest_deploy.jar does not emit a file; the deploy jar is rest_app_deploy.jar.
RUN bazel build --config=docker //:rest_deploy.jar \
    && mkdir -p /out \
    && cp -f bazel-bin/rest_app_deploy.jar /out/app.jar

FROM ${ENGINE_RUNTIME_IMAGE}

USER root
RUN rm -rf /app
WORKDIR /app

COPY --from=build /out/app.jar /app/app.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]
