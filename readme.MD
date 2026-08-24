# OpenBIMRL-Engine-REST

## Introduction

A RESTful API for the [Creator-Tool](https://github.com/OpenBimRL/OpenBimRL-CreatorTool).
It stores IFC files and exposes endpoints to apply Creator-Tool rule graphs to them.

## Dependencies

Spring Boot (Bazel). Engine JVM API is always `@openbimrl_engine//:engine_lib`:

- **Standalone** (this repo as root): GitHub Packages Maven shim (`third_party/engine_maven_shim/`)
- **Workspace dev**: root `local_path_override` → local `OpenBimRL-Engine/`

Published coordinates (shim / CI / Docker):

```xml
<dependency>
  <groupId>de.rub.bi.inf.openbimrl.engine</groupId>
  <artifactId>core</artifactId>
  <version>2026.08.24</version>
</dependency>
```

See [Engine package](https://github.com/OpenBimRL/OpenBimRL-Engine/packages/3211132).

Native IfcOpenShell / OCCT come from the Engine runtime Docker image (or a matching host install).

## Build

GitHub Packages auth: keep credentials in repo `.netrc`, then install into your real home before Bazel resolves Maven (do **not** set `HOME` to this repo):

```bash
install -m 600 .netrc ~/.netrc
```

```bash
bazel build //:rest_deploy.jar
bazel run //:rest_app -- --spring.profiles.active=dev
bazel test //:rest_tests
```

From the OpenBimRL Workspace root (local Engine, no GitHub Packages auth needed):

```bash
bazel build //:rest --config=rest
bazel run //:rest --config=rest -- --spring.profiles.active=dev
```

## Run

The JVM jar alone is not enough: JNA loads `libOpenBimRL-Engine-Native-x86_64.so` from the Engine package, which needs **IfcOpenShell** and a **newer libstdc++** (Engine images use Ubuntu noble). Running `java -jar rest_app_deploy.jar` on Debian bookworm typically fails with:

```
GLIBCXX_3.4.31 not found
```

**Recommended — Engine runtime Docker image** (matches CI / production):

```bash
bazel build //:rest_deploy.jar

docker run --rm -p 8080:8080 \
  -v "$PWD/bazel-bin/rest_app_deploy.jar:/app/app.jar:ro" \
  ghcr.io/openbimrl/openbimrl-engine:latest \
  java -jar /app/app.jar
```

Or build and run the REST image: `docker build …` / `docker run -p 8080:8080 openbimrl-engine-rest` (see below).

**Workspace dev — local Engine** (native rebuilt for this host, no GitHub Packages):

```bash
bazel run //:rest --config=rest -- --spring.profiles.active=dev
```

If you only have the Maven `core` jar, use the Engine image; do not expect bare `java -jar` on an older host glibc/libstdc++.

## Docker

```bash
docker build -t openbimrl-engine-rest \
  --build-arg GITHUB_ACTOR=… \
  --build-arg GITHUB_ACCESS_TOKEN=… \
  --build-arg ENGINE_RUNTIME_IMAGE=ghcr.io/openbimrl/openbimrl-engine:latest \
  .
docker run -p 8080:8080 openbimrl-engine-rest
```

Or:

```bash
docker run -p 8080:8080 ghcr.io/openbimrl/openbimrl-engine-rest:latest
```
