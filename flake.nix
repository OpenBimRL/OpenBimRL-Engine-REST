{
  description = "OpenBIMRL Engine REST API for NixOS";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs =
    {
      self,
      nixpkgs,
    }:
    let
      supportedSystems = [
        "x86_64-linux"
        "aarch64-linux"
      ];

      forAllSystems = nixpkgs.lib.genAttrs supportedSystems;

      nixpkgsFor = forAllSystems (system: import nixpkgs { inherit system; });
    in
    {
      packages = forAllSystems (system: {
        default = nixpkgsFor.${system}.openbimrl-api;
        openbimrl-api = nixpkgsFor.${system}.openbimrl-api;
      });

      overlays.default = final: prev: {
        openbimrl-api = final.callPackage ./nix/package.nix { };
      };

      nixosModules.default = import ./nix/module.nix;
      nixosModules.openbimrl-api = self.nixosModules.default;
    };
}
