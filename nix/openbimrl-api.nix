# OpenBIMRL Engine REST API — NixOS integration
#
# Adds `services.openbimrl-api.enable` and builds the stack from cloned GitHub
# repositories (no Docker):
#   - Maven-Bounding-Volume-Hierarchy
#   - OpenBimRL (API)
#   - OpenBimRL-Engine (+ native submodule)
#   - OpenBimRL-Engine-REST
#
# Usage in configuration.nix:
#
#   imports = [ ./OpenBimRL-Engine-REST/nix/openbimrl-api.nix ];
#
#   services.openbimrl-api.enable = true;
#   services.openbimrl-api.port = 8080;
#   services.openbimrl-api.openFirewall = true;
#   services.openbimrl-api.accessToken = "your-secret-token";
#
# Build the package manually (first build needs network for Maven; uses the
# workspace Maven cache at ../../.m2/repository when present):
#
#   nix-build ./OpenBimRL-Engine-REST/nix/openbimrl-api.nix -A openbimrl-api \
#     --option sandbox false
#
# Or with flakes (after adding nix files to git):
#
#   nix build ./OpenBimRL-Engine-REST#openbimrl-api --option sandbox false
#
{ pkgs ? import <nixpkgs> { } }:

{
  nixpkgs.overlays = [
    (final: prev: {
      openbimrl-api = final.callPackage ./package.nix { };
    })
  ];

  imports = [ ./module.nix ];
}
