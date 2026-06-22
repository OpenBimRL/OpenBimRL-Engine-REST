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

  engineSrc = fetchgit {
    url = "https://github.com/OpenBimRL/OpenBimRL-Engine.git";
    rev = "3783cae8ab08d7037a5b1cc0c1ace265d06b70ea";
    hash = "sha256-Nvzyr+hDK3hE1afQ8TiWELSYSu21ClcRjb9Q6hjjj8A=";
    fetchSubmodules = true;
  };

  restSrc = fetchFromGitHub {
    owner = "OpenBimRL";
    repo = "OpenBimRL-Engine-REST";
    rev = "fa4ce76a6851e58a73477fa5e1f87b428eab4ad6";
    hash = "sha256-/6mqo1SXzIa4Gm+ZpDqkRRI125+FoqMu0pz5Sx1PiT8=";
  };

  jsonSrc = fetchurl {
    url = "https://github.com/nlohmann/json/releases/download/v3.11.3/json.tar.xz";
    hash = "sha256-1sZaymse1o56GC9HVyV7EHrkAwMnYO1u8SHJ1V6BdX0=";
  };

  ifcopenshellSrc = fetchgit {
    url = "https://github.com/IfcOpenShell/IfcOpenShell.git";
    rev = "60a70f5236528eaad63a1bb9109044d04a108679";
    hash = "sha256-r7oYouJGMrnCP0NkAw7L03/Lfh0ak/CCPbLyQBm7MKo=";
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

  # Populated by local dev builds under ../../.m2/repository.
  # Regenerate with: ./scripts/dev-start.sh (or any successful mvn build in this workspace).
  prefetchedMavenRepo = builtins.path {
    path = ../../.m2/repository;
    name = "openbimrl-maven-repo";
  };

  runtimeLibs = [
    opencascade-occt_7_6
    boost179
    hdf5
    gmp
    libxml2
    mpfr
    openmp
    stdenv.cc.cc.lib
  ];

in
stdenv.mkDerivation rec {
  pname = "openbimrl-api";
  version = "0.5.4-alpha";

  src = restSrc;

  dontUseCmakeConfigure = true;
  dontConfigure = true;

  nativeBuildInputs = [
    cmake
    gnumake
    maven
    jdk21
    clang
    autoPatchelfHook
    makeWrapper
    patchelf
    python3
  ];

  buildInputs = runtimeLibs ++ [ hdf5.bin boost179.dev ];

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
    cp -r ${prefetchedMavenRepo}/. "$mavenRepo/"
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
      --replace '"/usr/include/oce"' '"${opencascade-occt_7_6}/include/opencascade"' \
      --replace 'find_package(HDF5 REQUIRED)' 'set(HDF5_INCLUDE_DIRS "${hdf5.dev}/include")
set(HDF5_INCLUDE_DIR "${hdf5.dev}/include")
set(HDF5_LIBRARIES "${hdf5}/lib/libhdf5.so")
set(HDF5_FOUND TRUE)'

    substituteInPlace "$work/engine/Makefile" \
      --replace 'C_COMPILER="$(CC)";' 'C_COMPILER="${clang}/bin/clang";' \
      --replace 'CXX_COMPILER="$(CXX)";' 'CXX_COMPILER="${clang}/bin/clang++";' \
      --replace '-DOPENBIMRL_ROCM_OFFLOAD_ARCH=$$ROCM_ARCH;' '-DOPENBIMRL_ROCM_OFFLOAD_ARCH=$$ROCM_ARCH \
			-DBoost_NO_BOOST_CMAKE=ON \
			-DBOOST_ROOT=${boost179.dev} \
			-DFETCHCONTENT_SOURCE_DIR_IFCOPENSHELL=IFCOPENSHELL_SRC_PLACEHOLDER \
			-DFETCHCONTENT_SOURCE_DIR_JSON=${jsonUnpacked} \
			-DFETCHCONTENT_SOURCE_DIR_GOOGLETEST=${googletestSrc};'

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
    mvnLocal install -DskipTests
    popd

    echo "Building OpenBIMRL Engine REST ..."
    mvnLocal -f "$work/rest/pom.xml" package -DskipTests

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

    ifcLibDir="$work/engine/build/cmake/_deps/ifcopenshell-build"
    if [ -d "$ifcLibDir" ]; then
      cp -L "$ifcLibDir"/libIfcGeom.so* "$ifcLibDir"/libIfcParse.so* $out/lib/ 2>/dev/null || true
      for lib in $out/lib/libIfc*.so*; do
        patchelf --set-rpath "${lib.makeLibraryPath runtimeLibs}" "$lib"
      done
    fi

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
