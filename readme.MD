# OpenBIMRL-Engine-REST

## Introduction 
A RESTful API to be used with the [Creator-Tool](https://github.com/OpenBimRL/OpenBimRL-CreatorTool). It provides a storage for IFC files and endpoints for applying rule graphs from the Creator-Tool directly to those files. 

## Dependencies
This is a Spring Application. All dependencies are managed via Maven. 
Be sure to have the matching version of the [Engine](https://github.com/OpenBimRL/OpenBimRL-Engine) installed in your local Maven repository (since it's not yet published on public repositories)

## Usage
it's easiest to use the Docker container provided. Simply run:
```bash
docker run -p 8080:8080 ghcr.io/openbimrl/openbimrl-engine-rest:latest
```