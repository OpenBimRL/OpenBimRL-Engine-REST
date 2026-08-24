# OpenBIMRL Engine REST API — NixOS integration
#
# Builds with pkgs.buildBazelPackage (no Docker / no Maven for the app):
#   - Monorepo Bazel target //OpenBimRL-Engine-REST:rest_deploy.jar
#   - Engine (+ native) via @openbimrl_engine local_path_override
#   - IfcOpenShell (pinned) for native runtime libs
#
# OpenBimRL schema and BVH are compiled inside Engine's Bazel graph (http_archive).
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
# Build (needs network for the Bazel deps FOD; host layout uses __noChroot):
#
#   nix-build ./OpenBimRL-Engine-REST/nix/openbimrl-api.nix -A openbimrl-api \
#     --option sandbox false
#
# Or with flakes:
#
#   nix build ./OpenBimRL-Engine-REST#openbimrl-api --option sandbox false
#
# Monorepo override for flakes (from Workspace root):
#
#   nix build ./OpenBimRL-Engine-REST#openbimrl-api --option sandbox false \
#     --override-input openbimrl-workspace path:.
#
{ pkgs ? import <nixpkgs> { } }:

{
  nixpkgs.overlays = [
    (final: prev: {
      openbimrl-api = final.callPackage ./package.nix {
        # Workspace root that contains MODULE.bazel + OpenBimRL-Engine (+ Native).
        workspaceSrc = final.lib.cleanSource ../..;
      };
    })
  ];

  imports = [ ./module.nix ];
}
