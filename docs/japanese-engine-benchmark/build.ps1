$ErrorActionPreference = 'Stop'
Push-Location (Resolve-Path "$PSScriptRoot/../..")
try {
    $benchDir='artifacts/japanese-engine-benchmark'
    $repo=(Get-ChildItem "$benchDir/kazuma" -Directory)[0].FullName
    $cmake='E:/Program Files/CMake/bin/cmake.exe'
    & $cmake -S $repo -B "$benchDir/build" -G 'MinGW Makefiles' -DCMAKE_CXX_COMPILER=C:/TDM-GCC-64/bin/g++.exe -DCMAKE_MAKE_PROGRAM=C:/TDM-GCC-64/bin/mingw32-make.exe -DENABLE_ZENZ=OFF -DBUILD_MOZC_FETCH=OFF
    if ($LASTEXITCODE -ne 0) {throw 'Converter configure failed'}
    & $cmake --build "$benchDir/build" --target astar_bunsetsu_cli dictionary_builder tries_token_builder -j 4
    if ($LASTEXITCODE -ne 0) {throw 'Converter build failed'}
    & "$benchDir/build/dictionary_builder.exe" --in "$benchDir/mozc-data" --out "$benchDir/build/mozc_reading.louds" --conn-out "$benchDir/build/connection_single_column.bin"
    if ($LASTEXITCODE -ne 0) {throw 'Connection data build failed'}
    & "$benchDir/build/tries_token_builder.exe" --in_dir "$benchDir/mozc-data" --out_dir "$benchDir/build"
    if ($LASTEXITCODE -ne 0) {throw 'Lexicon build failed'}
    $jdk='C:/Program Files/Microsoft/jdk-17.0.11.9-hotspot/bin'
    $sources=@(Get-ChildItem core/src/main/java -Recurse -Filter *.java | ForEach-Object FullName)
    & "$jdk/javac.exe" -encoding UTF-8 -d core/build/manual @sources "$PSScriptRoot/MinimeServer.java"
    if ($LASTEXITCODE -ne 0) {throw 'Java adapter compile failed'}
    & C:/TDM-GCC-64/bin/g++.exe -std=c++20 -O3 "-I$repo" "-I$repo/src" "-I$repo/src/dictionary_builder" "-I$repo/src/dictionary_builder/louds_builder" "$PSScriptRoot/ConverterServer.cpp" "-L$benchDir/build" -lpath_algorithm -lgraph_builder -ltoken_array -llouds_utf16 -lconnection_id -pthread -o "$benchDir/build/converter-server.exe"
    if ($LASTEXITCODE -ne 0) {throw 'C++ adapter compile failed'}
} finally {Pop-Location}
