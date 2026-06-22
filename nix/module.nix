{
  config,
  lib,
  pkgs,
  ...
}:

let
  cfg = config.services.openbimrl-api;
in
{
  options.services.openbimrl-api = {
    enable = lib.mkEnableOption "OpenBIMRL Engine REST API";

    package = lib.mkOption {
      type = lib.types.package;
      default = pkgs.openbimrl-api;
      defaultText = lib.literalExpression "pkgs.openbimrl-api";
      description = "The OpenBIMRL API package to run.";
    };

    port = lib.mkOption {
      type = lib.types.port;
      default = 8080;
      description = "TCP port for the REST API.";
    };

    listenAddress = lib.mkOption {
      type = lib.types.str;
      default = "0.0.0.0";
      description = "Address the REST API binds to.";
    };

    openFirewall = lib.mkOption {
      type = lib.types.bool;
      default = false;
      description = "Open the configured TCP port in the firewall.";
    };

    extraJavaOpts = lib.mkOption {
      type = lib.types.listOf lib.types.str;
      default = [ ];
      description = "Extra arguments passed to the Java runtime.";
    };

    extraEnvironment = lib.mkOption {
      type = lib.types.attrsOf lib.types.str;
      default = { };
      description = "Extra environment variables for the service.";
    };
  };

  config = lib.mkIf cfg.enable {
    systemd.services.openbimrl-api = {
      description = "OpenBIMRL Engine REST API";
      wantedBy = [ "multi-user.target" ];
      after = [ "network.target" ];
      wants = [ "network.target" ];

      environment = cfg.extraEnvironment;

      serviceConfig = {
        Type = "simple";
        DynamicUser = true;
        StateDirectory = "openbimrl-api";
        ExecStart = lib.escapeShellArgs (
          [
            "${cfg.package}/bin/openbimrl-api"
            "--server.address=${cfg.listenAddress}"
            "--server.port=${toString cfg.port}"
          ]
          ++ cfg.extraJavaOpts
        );
        Restart = "on-failure";
        RestartSec = "5s";
      };
    };

    networking.firewall.allowedTCPPorts = lib.mkIf cfg.openFirewall [ cfg.port ];
  };
}
