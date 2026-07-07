{
  lib,
  stdenv,
  fetchgit,
  fetchFromGitHub,
  fetchurl,
  jdk21,
  maven,
  cmake,
  llvmPackages,
  gnumake,
  hdf5,
  gmp,
  libxml2,
  mpfr,
  opencascade-occt_7_6,
  makeWrapper,
  autoPatchelfHook,
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
}:

let
  openbimrlApiVersion = "2023.07.1";

  bvhSrc = fetchFromGitHub {
    owner = "RUB-Informatik-im-Bauwesen";
    repo = "Maven-Bounding-Volume-Hierarchy";
    rev = "d92129c5af88743e19b9ab801f69e3fb72baf46d";
    hash = "sha256-tAnnLvv1oNBilgQ52VdRyNI+ug/JUfzeYAiXVDCRzaY=";
  };

  apiSrc = fetchFromGitHub {
    owner = "RUB-Informatik-im-Bauwesen";
    repo = "OpenBimRL";
    rev = "83bd65f52803d7e86a464b592899c6709888c47a";
    hash = "sha256-y/4UzD28PkQBYXMbcm8BPUQYlcJ50pAr44H/TowF0tg=";
  };

  engineBaseSrc = fetchFromGitHub {
    owner = "OpenBimRL";
    repo = "OpenBimRL-Engine";
    rev = "f0267f5144ae1bbf115069e5984f424561446565";
    hash = "sha256-9YIz+qCxycBvcPopsNqH05ZA3XZaGkU/FYq6yYFFGQE=";
  };

  engineNativeSrc = fetchFromGitHub {
    owner = "OpenBimRL";
    repo = "OpenBimRL-Engine-Native";
    rev = "ab8f90e6817ece6cfab2b8123e850ff17afbd6c3";
    hash = "sha256-XNpH3BeoP/oYcH30WVQc/mjWJf0WyxbLbNcavbVrnTw=";
  };

  engineSrc = stdenv.mkDerivation {
    pname = "openbimrl-engine-src";
    version = "2026.07.05";
    dontUnpack = true;
    installPhase = ''
      mkdir -p "$out"
      cp -r ${engineBaseSrc}/. "$out/"
      chmod -R u+w "$out"
      rm -rf "$out/src/main/cpp"
      cp -r ${engineNativeSrc}/. "$out/src/main/cpp/"
    '';
  };

  # REST sources come from the enclosing repo checkout (submodule in infrastructure).
  restSrc = lib.cleanSource ../.;

  jsonSrc = fetchurl {
    url = "https://github.com/nlohmann/json/releases/download/v3.11.3/json.tar.xz";
    hash = "sha256-1sZaymse1o56GC9HVyV7EHrkAwMnYO1u8SHJ1V6BdX0=";
  };

  ifcopenshellSrc = fetchgit {
    url = "https://github.com/IfcOpenShell/IfcOpenShell.git";
    rev = "eafa158ca0cd5ba2ca22b5e588b0375cab2efbce";
    hash = "sha256-Uic2GWI49Bo/MPmm6o8JYEBqii3SU5Zk9tuWF0Sj8CM=";
    fetchSubmodules = true;
  };

  googletestSrc = fetchFromGitHub {
    owner = "google";
    repo = "googletest";
    rev = "f8d7d77c06936315286eb55f8de22cd23c188571";
    hash = "sha256-t0RchAHTJbuI5YW4uyBPykTvcjy90JW9AOPNjIhwh6U=";
  };

  jsonUnpacked = stdenv.mkDerivation {
    pname = "nlohmann-json-src";
    version = "3.11.3";
    src = jsonSrc;
    unpackPhase = "runHook preUnpack; tar -xJf $src; runHook postUnpack";
    installPhase = "mkdir -p $out; cp -r json/* $out/";
  };

  clang = llvmPackages.clang;
  openmp = llvmPackages.openmp;

  # Sandbox-safe Maven cache: fetched once with network, then pinned by outputHash.
  mavenRepository = stdenv.mkDerivation {
    pname = "openbimrl-maven-repository";
    version = builtins.hashString "sha256" (
      (builtins.readFile "${restSrc}/pom.xml")
      + (builtins.readFile "${bvhSrc}/pom.xml")
      + (builtins.readFile "${apiSrc}/pom.xml")
      + (builtins.readFile "${engineSrc}/pom.xml")
    );

    nativeBuildInputs = [ maven jdk21 ];
    dontUnpack = true;
    outputHashMode = "recursive";
    outputHashAlgo = "sha256";
    outputHash = "sha256-BgAF4cqK8iAYDwI9G9EQOooHDTemZoyHYxoBrgCbhFQ=";

    buildPhase = ''
      mavenRepo="$NIX_BUILD_TOP/maven-repo"
      mkdir -p "$mavenRepo"
      work="$NIX_BUILD_TOP/work"
      mkdir -p "$work"
      cp -r ${bvhSrc} "$work/bvh"
      cp -r ${apiSrc} "$work/api"
      cp -r ${engineSrc} "$work/engine"
      cp -r ${restSrc} "$work/rest"
      chmod -R u+w "$work"

      mvnLocal() {
        mvn --batch-mode -Dmaven.repo.local="$mavenRepo" "$@"
      }

      fetchPlugins() {
        mvnLocal -f "$1" dependency:go-offline dependency:resolve-plugins -DskipTests
      }

      fetchPlugins "$work/bvh/pom.xml"
      mvnLocal -f "$work/bvh/pom.xml" compiler:compile jar:jar install:install

      fetchPlugins "$work/api/pom.xml"
      pushd "$work/api"
      mvnLocal -Dproject.build.sourceEncoding=ISO-8859-1 \
        compiler:compile jar:jar install:install \
        -DgroupId=inf.bi.rub.de \
        -DartifactId=OpenBIMRL-API \
        -Dversion=${openbimrlApiVersion} \
        -Dpackaging=jar
      popd

      fetchPlugins "$work/engine/pom.xml"

      # REST depends on the locally built engine artifact; install a stub so
      # dependency:go-offline can prefetch Spring/Kotlin deps without native build.
      stubJar="$work/engine-stub.jar"
      ${jdk21}/bin/jar cf "$stubJar" -C ${restSrc} pom.xml
      mvnLocal install:install-file \
        -Dfile="$stubJar" \
        -DgroupId=inf.bi.rub.de \
        -DartifactId=openbimrl-engine \
        -Dversion=2026.07.05 \
        -Dpackaging=jar \
        -DgeneratePom=true

      fetchPlugins "$work/rest/pom.xml"
      mvnLocal -f "$work/rest/pom.xml" dependency:resolve -DskipTests

      for artifact in \
        "org.glassfish.jaxb:jaxb-runtime:4.0.5" \
        "com.google.code.gson:gson:2.10.1" \
        "org.hamcrest:hamcrest-core:2.2" \
        "org.jetbrains.kotlin:kotlin-maven-plugin:2.3.10" \
        "org.jetbrains.kotlin:kotlin-maven-allopen:2.3.10" \
        "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.3"
      do
        mvnLocal dependency:get -Dartifact="$artifact" -Dtransitive=true
      done
    '';

    installPhase = ''
      mkdir -p "$out"
      cp -r "$mavenRepo"/. "$out/"
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

in
stdenv.mkDerivation rec {
  pname = "openbimrl-api";
  version = "0.6.0-beta";

  src = restSrc;

  dontUseCmakeConfigure = true;
  dontConfigure = true;

  nativeBuildInputs = [
    cmake
    gnumake
    maven
    jdk21
    clang
    makeWrapper
    patchelf
    python3
  ];

  dontAutoPatchELF = true;

  buildInputs = runtimeLibs ++ [
    hdf5.bin
    boost179.dev
    eigen
    fontconfig
    libGL
    libx11
  ];

  env = {
    NIX_CFLAGS_COMPILE = "-I${opencascade-occt_7_6}/include/opencascade";
    CC = "${clang}/bin/clang";
    CXX = "${clang}/bin/clang++";
    OPENBIMRL_ENABLE_ROCM_OFFLOAD = "OFF";
    CMAKE_PREFIX_PATH = lib.makeSearchPathOutput "dev" "lib/cmake" [
      boost179
      hdf5
      opencascade-occt_7_6
    ];
    PKG_CONFIG_PATH = lib.makeSearchPathOutput "dev" "lib/pkgconfig" [
      hdf5
      libxml2
    ];
  };


  buildPhase = ''
    runHook preBuild

    mavenRepo="$NIX_BUILD_TOP/maven-repo"
    work="$NIX_BUILD_TOP/openbimrl-work"

    mkdir -p "$mavenRepo" "$work"
    cp -r ${mavenRepository}/. "$mavenRepo/"
    chmod -R u+w "$mavenRepo"
    cp -r ${bvhSrc} "$work/bvh"
    cp -r ${apiSrc} "$work/api"
    cp -r ${engineSrc} "$work/engine"
    cp -r ${ifcopenshellSrc} "$work/ifcopenshell"
    cp -r $src "$work/rest"
    chmod -R u+w "$work"

    substituteInPlace "$work/ifcopenshell/cmake/CMakeLists.txt" \
      --replace 'add_subdirectory(../src/svgfill svgfill)' '# nix: svgfill disabled'

    ifcSrc="$work/ifcopenshell"

    mvnLocal() {
      mvn --batch-mode -Dmaven.repo.local="$mavenRepo" "$@"
    }

    substituteInPlace "$work/engine/src/main/cpp/CMakeLists.txt" \
      --replace '"/usr/include/opencascade"' '"${opencascade-occt_7_6}/include/opencascade"' \
      --replace '"/usr/include/oce"' '"${opencascade-occt_7_6}/include/opencascade"'

    substituteInPlace "$work/engine/Makefile" \
      --replace '-DOPENBIMRL_IFCOPENSHELL_PREFIX=$(OPENBIMRL_IFCOPENSHELL_PREFIX);' \
        '-DOPENBIMRL_IFCOPENSHELL_PREFIX=$(OPENBIMRL_IFCOPENSHELL_PREFIX) \
          -DOpenCASCADE_DIR=${opencascade-occt_7_6}/lib/cmake/opencascade \
          -DCMAKE_PREFIX_PATH=${eigen}/share/eigen3/cmake:${boost179.dev}/lib/cmake:${hdf5.dev}/lib/cmake \
          -DFETCHCONTENT_SOURCE_DIR_IFCOPENSHELL=IFCOPENSHELL_SRC_PLACEHOLDER \
          -DFETCHCONTENT_SOURCE_DIR_JSON=${jsonUnpacked};'

    substituteInPlace "$work/engine/Makefile" \
      --replace 'IFCOPENSHELL_SRC_PLACEHOLDER' "$ifcSrc"

    echo "Building Maven dependency: BVH ..."
    mvnLocal -f "$work/bvh/pom.xml" compiler:compile jar:jar install:install

    echo "Building Maven dependency: OpenBIMRL-API ..."
    pushd "$work/api"
    mvnLocal -Dproject.build.sourceEncoding=ISO-8859-1 \
      compiler:compile jar:jar install:install \
      -DgroupId=inf.bi.rub.de \
      -DartifactId=OpenBIMRL-API \
      -Dversion=${openbimrlApiVersion} \
      -Dpackaging=jar
    popd

    echo "Building OpenBIMRL Engine (native + Maven) ..."
    pushd "$work/engine"
    OPENBIMRL_NATIVE_CACHE_DIR="$NIX_BUILD_TOP/openbimrl-native-cache" \
      mvnLocal install -DskipTests
    popd

    echo "Building OpenBIMRL Engine REST ..."
    mvnLocal -f "$work/rest/pom.xml" package -Dmaven.test.skip=true

    runHook postBuild
  '';

  installPhase = ''
    runHook preInstall

    mavenRepo="$NIX_BUILD_TOP/maven-repo"
    work="$NIX_BUILD_TOP/openbimrl-work"

    jarFile=$(find "$work/rest/target" -maxdepth 1 -name 'OpenBimRL-Engine-REST-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -print -quit)
    if [ -z "$jarFile" ]; then
      echo "Could not find REST application jar in $work/rest/target"
      ls -la "$work/rest/target" || true
      exit 1
    fi

    mkdir -p $out/lib $out/bin
    cp "$jarFile" $out/lib/openbimrl-api.jar

    nativeCache="$NIX_BUILD_TOP/openbimrl-native-cache/cmake"
    ifcLibDir="$nativeCache/_deps/ifcopenshell-build"

    if [ -d "$ifcLibDir" ]; then
      find "$ifcLibDir" \( -name '*.so' -o -name '*.so.*' \) -type f -exec cp -L {} $out/lib/ \;
      for target in $out/lib/libIfcGeom.so.0.8.0 $out/lib/libIfcParse.so.0.8.0; do
        [ -f "$target" ] || continue
        soname="''${target%.0}"
        if [ ! -e "$soname" ]; then
          ln -s "$(basename "$target")" "$soname"
        fi
      done
    fi

    if [ -f "$nativeCache/libOpenBIMRL_Native.so" ]; then
      cp -L "$nativeCache/libOpenBIMRL_Native.so" $out/lib/
    fi

    for lib in $out/lib/*.so*; do
      [ -e "$lib" ] || continue
      patchelf --set-rpath "${lib.makeLibraryPath runtimeLibs}:$out/lib" "$lib"
    done

    makeWrapper ${jdk21}/bin/java $out/bin/openbimrl-api \
      --add-flags "-jar $out/lib/openbimrl-api.jar" \
      --prefix LD_LIBRARY_PATH : "${lib.makeLibraryPath runtimeLibs}:$out/lib"

    runHook postInstall
  '';

  meta = with lib; {
    description = "OpenBIMRL Engine REST API (Spring Boot)";
    homepage = "https://github.com/OpenBimRL/OpenBimRL-Engine-REST";
    license = licenses.mit;
    platforms = platforms.linux;
    mainProgram = "openbimrl-api";
  };
}
