$ErrorActionPreference='Stop'
Push-Location (Resolve-Path "$PSScriptRoot/../..")
try {
    $work='artifacts/japanese-engine-benchmark';$repo=(Resolve-Path "$work/kazuma-bounded").Path;$lib="$work/build-bounded"
    $cmake='E:/Program Files/CMake/bin/cmake.exe'
    & $cmake -S $repo -B $lib -G 'MinGW Makefiles' -DCMAKE_CXX_COMPILER=C:/TDM-GCC-64/bin/g++.exe -DCMAKE_MAKE_PROGRAM=C:/TDM-GCC-64/bin/mingw32-make.exe -DENABLE_ZENZ=OFF -DBUILD_MOZC_FETCH=OFF
    if($LASTEXITCODE -ne 0){throw 'Configure failed'}
    # The patched reader includes the budget header from louds_builder.
    & $cmake --build $lib --target path_algorithm token_array louds_utf16 connection_id -j 4
    if($LASTEXITCODE -ne 0){throw 'Library build failed'}
    & C:/TDM-GCC-64/bin/g++.exe -std=c++20 -O3 "-I$repo" "-I$repo/src" "-I$repo/src/dictionary_builder" "-I$repo/src/dictionary_builder/louds_builder" "$PSScriptRoot/Server.cpp" "-L$lib" -lpath_algorithm -lgraph_builder -ltoken_array -llouds_utf16 -lconnection_id -pthread -o "$work/build/predictive-server-bounded.exe"
    if($LASTEXITCODE -ne 0){throw 'Adapter build failed'}
} finally {Pop-Location}
