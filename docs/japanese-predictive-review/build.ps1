param([switch]$Indexed)
$ErrorActionPreference='Stop'
Push-Location (Resolve-Path "$PSScriptRoot/../..")
try {
    $benchDir='artifacts/japanese-engine-benchmark'
    $repo=(Get-ChildItem "$benchDir/kazuma" -Directory)[0].FullName
    $lib="$benchDir/build"; $suffix=''
    if($Indexed) {
        $repo=(Resolve-Path "$benchDir/kazuma-indexed").Path; $lib="$benchDir/build-indexed"; $suffix='-indexed'
        & 'E:/Program Files/CMake/bin/cmake.exe' -S $repo -B $lib -G 'MinGW Makefiles' -DCMAKE_CXX_COMPILER=C:/TDM-GCC-64/bin/g++.exe -DCMAKE_MAKE_PROGRAM=C:/TDM-GCC-64/bin/mingw32-make.exe -DENABLE_ZENZ=OFF -DBUILD_MOZC_FETCH=OFF
        if($LASTEXITCODE -ne 0){throw 'Indexed configuration failed'}
        & 'E:/Program Files/CMake/bin/cmake.exe' --build $lib --target path_algorithm token_array louds_utf16 connection_id -j 4
        if($LASTEXITCODE -ne 0){throw 'Indexed library build failed'}
        & C:/TDM-GCC-64/bin/g++.exe -std=c++20 -O3 "-I$repo" "-I$repo/src" "-I$repo/src/dictionary_builder" "-I$repo/src/dictionary_builder/louds_builder" "$PSScriptRoot/../japanese-engine-benchmark/ConverterServer.cpp" "-L$lib" -lpath_algorithm -lgraph_builder -ltoken_array -llouds_utf16 -lconnection_id -pthread -o "$benchDir/build/converter-server-indexed.exe"
        if($LASTEXITCODE -ne 0){throw 'Indexed converter adapter failed'}
        & C:/TDM-GCC-64/bin/g++.exe -std=c++20 -O3 "-I$repo/src/dictionary_builder" "-I$repo/src/dictionary_builder/louds_builder" "$PSScriptRoot/PostingAudit.cpp" "-L$lib" -ltoken_array -llouds_utf16 -o "$benchDir/build/posting-audit.exe"
        if($LASTEXITCODE -ne 0){throw 'Posting audit compile failed'}
    }
    & C:/TDM-GCC-64/bin/g++.exe -std=c++20 -O3 "-I$repo" "-I$repo/src" "-I$repo/src/dictionary_builder" "-I$repo/src/dictionary_builder/louds_builder" "$PSScriptRoot/PredictiveServer.cpp" "-L$lib" -lpath_algorithm -lgraph_builder -ltoken_array -llouds_utf16 -lconnection_id -pthread -o "$benchDir/build/predictive-server$suffix.exe"
    if($LASTEXITCODE -ne 0){throw 'Predictive adapter compilation failed'}
} finally {Pop-Location}
