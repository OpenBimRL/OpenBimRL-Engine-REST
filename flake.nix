{
  description = "OpenBIMRL Engine REST API for NixOS";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    # Monorepo root (Bazel MODULE.bazel). Override locally:
    #   nix build --override-input openbimrl-workspace path:../..
    # when this flake is used from OpenBimRL-Engine-REST/.
    openbimrl-workspace = {
      url = "github:OpenBimRL/Workspace";
      flake = false;
    };
  };

  outputs =
    {
      self,
      nixpkgs,
      openbimrl-workspace,
    }:
    let
      supportedSystems = [
        "x86_64-linux"
      ];

      forAllSystems = nixpkgs.lib.genAttrs supportedSystems;

      overlay = final: prev: {
        openbimrl-api = final.callPackage ./nix/package.nix {
          workspaceSrc = openbimrl-workspace;
        };
      };

      pkgsFor = forAllSystems (
        system:
        import nixpkgs {
          inherit system;
          overlays = [ overlay ];
        }
      );
    in
    {
      packages = forAllSystems (system: {
        default = pkgsFor.${system}.openbimrl-api;
        openbimrl-api = pkgsFor.${system}.openbimrl-api;
      });

      overlays.default = overlay;

      nixosModules.default = import ./nix/module.nix;
      nixosModules.openbimrl-api = self.nixosModules.default;
    };
}
