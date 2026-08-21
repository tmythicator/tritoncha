{
  description = "Tritoncha: Live-coding music and 3D WebGL visuals studio";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    flake-utils.lib.eachSystem
      [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ]
      (
        system:
        let
          pkgs = import nixpkgs {
            inherit system;
          };
        in
        {
          devShells.default = pkgs.mkShell {
            buildInputs = with pkgs; [
              nodejs
              pnpm
              clj-kondo
              git
            ];

            shellHook = ''
              if [ ! -d "node_modules" ]; then
                pnpm install --ignore-scripts
              fi
            '';
          };
        }
      );
}
