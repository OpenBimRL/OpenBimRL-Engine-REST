# FIXME: Update to `bazel build //:rest_deploy.jar` in this repo (Maven dep:
# de.rub.bi.inf.openbimrl.engine:core via GitHub Packages ~/.netrc).
{
  lib,
  stdenv,
  fetchFromGitHub,
  fetchgit,
  buildBazelPackage,
  bazel_9,
  jdk21,
  cmake,
  ninja,
  llvmPackages,
  gnumake,
  hdf5,
  gmp,
  libxml2,
  mpfr,
  opencascade-occt_7_6,
  makeWrapper,
  patchelf,
  boost179,
  eigen,
  fontconfig,
  libGL,
  libx11,
  freetype,
  tcl,
  tk,
  python3,
  git,
  which,
  cacert,
  # Monorepo root (MODULE.bazel). Override for flakes / local checkouts.
  workspaceSrc ? null,
}:

let
  clang = llvmPackages.clang;
  openmp = llvmPackages.openmp;

  # buildBazelPackage does `bazel.override { enableNixHacks = true; }`, which
  # bazel_8+/bazel_9 reject (intentional in nixpkgs). Ignore the flag.
  bazel = bazel_9 // {
    override = { enableNixHacks ? false, ... }: bazel_9;
  };

  resolvedWorkspaceSrc =
    if workspaceSrc != null then
      workspaceSrc
    else if builtins.pathExists ../../MODULE.bazel then
      lib.cleanSourceWith {
        src = ../..;
        name = "openbimrl-workspace-src";
        filter =
          path: _type:
          let
            base = baseNameOf path;
          in
          !(builtins.elem base [
            ".git"
            "node_modules"
            "bazel-bin"
            "bazel-out"
            "bazel-testlogs"
            "bazel-openbimrl"
            "bazel-OpenBimRL-Engine"
            "target"
            ".m2"
            ".cache"
            "result"
            "CMakeFiles"
          ]);
      }
    else
      fetchFromGitHub {
        owner = "OpenBimRL";
        repo = "Workspace";
        rev = "96a88b41d009ab08a1572d2fe33fac2c0c54617f";
        # Bump after changing rev (must include Engine + Native submodules).
        hash = "sha256-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        fetchSubmodules = true;
      };

  ifcopenshellSrc = fetchgit {
    url = "https://github.com/IfcOpenShell/IfcOpenShell.git";
    rev = "eafa158ca0cd5ba2ca22b5e588b0375cab2efbce";
    hash = "sha256-Uic2GWI49Bo/MPmm6o8JYEBqii3SU5Zk9tuWF0Sj8CM=";
    fetchSubmodules = true;
  };

  ifcopenshell = stdenv.mkDerivation {
    pname = "openbimrl-ifcopenshell";
    version = "0.8.0-eafa158";
    src = ifcopenshellSrc;

    nativeBuildInputs = [
      cmake
      ninja
      clang
      gnumake
    ];
    buildInputs = [
      opencascade-occt_7_6
      boost179
      eigen
      hdf5
      gmp
      mpfr
      libxml2
    ];

    dontUseCmakeConfigure = true;

    buildPhase = ''
      runHook preBuild
      substituteInPlace cmake/CMakeLists.txt \
        --replace 'add_subdirectory(../src/svgfill svgfill)' '# nix: svgfill disabled'
      cmake -G Ninja -S cmake -B build \
        -DCMAKE_C_COMPILER=${clang}/bin/clang \
        -DCMAKE_CXX_COMPILER=${clang}/bin/clang++ \
        -DCMAKE_INSTALL_PREFIX=$out \
        -DCMAKE_BUILD_TYPE=Release \
        -DOCC_INCLUDE_DIR=${opencascade-occt_7_6}/include/opencascade \
        -DOCC_LIBRARY_DIR=${opencascade-occt_7_6}/lib \
        -DBUILD_SHARED_LIBS=ON \
        -DSCHEMA_VERSIONS="2x3;4;4x3_add2" \
        -DBUILD_CONVERT=OFF \
        -DBUILD_IFCPYTHON=OFF \
        -DBUILD_GEOMSERVER=OFF \
        -DBUILD_EXAMPLES=OFF \
        -DBUILD_DOCUMENTATION=OFF \
        -DWITH_CGAL=OFF \
        -DCOLLADA_SUPPORT=OFF \
        -DHDF5_SUPPORT=OFF \
        -DGLTF_SUPPORT=OFF \
        -DIFCXML_SUPPORT=OFF \
        -DUSD_SUPPORT=OFF
      cmake --build build -j"$NIX_BUILD_CORES"
      runHook postBuild
    '';

    installPhase = ''
      runHook preInstall
      cmake --install build
      runHook postInstall
    '';
  };

  runtimeLibs = [
    opencascade-occt_7_6
    boost179
    hdf5
    gmp
    libxml2
    mpfr
    openmp
    fontconfig
    freetype
    libGL
    libx11
    tcl
    tk
    stdenv.cc.cc.lib
  ];

  # Host layout expected by Engine .bazelrc + native cmake cache_entries.
  setupHostTooling = ''
    mkdir -p /opt /usr/bin
    ln -sfn ${clang}/bin/clang /usr/bin/clang
    ln -sfn ${clang}/bin/clang++ /usr/bin/clang++
    ln -sfn ${ifcopenshell} /opt/ifcopenshell
    export CC=${clang}/bin/clang
    export CXX=${clang}/bin/clang++
    export JAVA_HOME=${jdk21}
    export OPENBIMRL_USE_PREBUILT_IFCOPENSHELL=ON
    export OPENBIMRL_IFCOPENSHELL_PREFIX=/opt/ifcopenshell
    export LD_LIBRARY_PATH=/opt/ifcopenshell/lib''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
    export CMAKE_PREFIX_PATH=${
      lib.makeSearchPathOutput "dev" "lib/cmake" [
        boost179
        hdf5
        opencascade-occt_7_6
      ]
    }
    export PKG_CONFIG_PATH=${
      lib.makeSearchPathOutput "dev" "lib/pkgconfig" [
        hdf5
        libxml2
      ]
    }
    export NIX_CFLAGS_COMPILE="-I${opencascade-occt_7_6}/include/opencascade ''${NIX_CFLAGS_COMPILE:-}"
  '';

in
buildBazelPackage {
  pname = "openbimrl-api";
  version = "0.7.0-beta";

  src = resolvedWorkspaceSrc;
  inherit bazel;

  # Fat jar for the Spring Boot REST service (pulls @openbimrl_engine native .so).
  bazelTargets = [ "//OpenBimRL-Engine-REST:rest_deploy.jar" ];

  bazelBuildFlags = [
    "--ignore_dev_dependency" # skip CreatorTool npm / aspect_rules_js
    "--verbose_failures"
    "--action_env=CMAKE_PREFIX_PATH"
    "--action_env=PKG_CONFIG_PATH"
    "--action_env=NIX_CFLAGS_COMPILE"
    "--action_env=LD_LIBRARY_PATH"
    "--action_env=OPENBIMRL_IFCOPENSHELL_PREFIX"
    "--action_env=OPENBIMRL_USE_PREBUILT_IFCOPENSHELL"
    "--action_env=JAVA_HOME"
  ];

  bazelFetchFlags = [
    "--ignore_dev_dependency"
  ];

  # Keep local_* repos if analysis materializes them; remotejdk is fine either way.
  removeLocal = false;

  fetchAttrs = {
    # Fixed-output: first successful prefetch prints the real hash — bump this.
    hash = "sha256-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    __noChroot = true;
    nativeBuildInputs = [
      cmake
      ninja
      gnumake
      clang
      jdk21
      git
      which
      python3
      cacert
    ];
    buildInputs = runtimeLibs ++ [
      hdf5.bin
      boost179.dev
      eigen
    ];
    preBuild = setupHostTooling + ''
      # Match nixpkgs bazel version so the wrapper does not try to download
      # another release from .bazelversion (project pins 9.2.0).
      echo "${bazel_9.version}" > .bazelversion
      echo "${bazel_9.version}" > OpenBimRL-Engine/.bazelversion
      export SSL_CERT_FILE=${cacert}/etc/ssl/certs/ca-bundle.crt
      export GIT_SSL_CAINFO=$SSL_CERT_FILE
    '';
  };

  buildAttrs = {
    __noChroot = true;
    nativeBuildInputs = [
      cmake
      ninja
      gnumake
      clang
      jdk21
      makeWrapper
      patchelf
      git
      which
      python3
    ];
    buildInputs = runtimeLibs ++ [
      hdf5.bin
      boost179.dev
      eigen
      fontconfig
      libGL
      libx11
    ];

    preConfigure = setupHostTooling + ''
      echo "${bazel_9.version}" > .bazelversion
      echo "${bazel_9.version}" > OpenBimRL-Engine/.bazelversion
    '';

    installPhase = ''
      runHook preInstall

      jarFile=
      for candidate in \
        bazel-bin/OpenBimRL-Engine-REST/rest_deploy.jar \
        "$bazelOut"/execroot/*/bazel-out/*/bin/OpenBimRL-Engine-REST/rest_deploy.jar
      do
        if [ -f "$candidate" ]; then
          jarFile=$candidate
          break
        fi
      done
      if [ -z "$jarFile" ]; then
        echo "Could not find rest_deploy.jar" >&2
        find bazel-bin "$bazelOut" -name 'rest_deploy.jar' 2>/dev/null | head || true
        exit 1
      fi

      mkdir -p $out/lib $out/bin
      cp -L "$jarFile" $out/lib/openbimrl-api.jar

      # IfcOpenShell runtime shared libs (OpenBIMRL native .so is inside the jar).
      find ${ifcopenshell}/lib \( -name '*.so' -o -name '*.so.*' \) -type f -exec cp -L {} $out/lib/ \;
      for target in $out/lib/libIfcGeom.so.0.8.0 $out/lib/libIfcParse.so.0.8.0; do
        [ -f "$target" ] || continue
        soname="''${target%.0}"
        if [ ! -e "$soname" ]; then
          ln -s "$(basename "$target")" "$soname"
        fi
      done

      for lib in $out/lib/*.so*; do
        [ -e "$lib" ] || continue
        patchelf --set-rpath "${lib.makeLibraryPath runtimeLibs}:$out/lib" "$lib" || true
      done

      makeWrapper ${jdk21}/bin/java $out/bin/openbimrl-api \
        --add-flags "-jar $out/lib/openbimrl-api.jar" \
        --prefix LD_LIBRARY_PATH : "${lib.makeLibraryPath runtimeLibs}:$out/lib"

      runHook postInstall
    '';
  };

  meta = with lib; {
    description = "OpenBIMRL Engine REST API (Spring Boot, Bazel)";
    homepage = "https://github.com/OpenBimRL/OpenBimRL-Engine-REST";
    license = licenses.mit;
    platforms = platforms.linux;
    mainProgram = "openbimrl-api";
  };
}
