param([ValidateSet('stable','indexed-stable')][string]$Variant='stable')
$ErrorActionPreference='Stop'
Push-Location (Resolve-Path "$PSScriptRoot/../..")
try {
    $work='artifacts/japanese-engine-benchmark';$repo=(Resolve-Path "$work/kazuma-$Variant").Path;$lib="$work/build-$Variant"
    $cmake='E:/Program Files/CMake/bin/cmake.exe'
    & $cmake -S $repo -B $lib -G 'MinGW Makefiles' -DCMAKE_CXX_COMPILER=C:/TDM-GCC-64/bin/g++.exe -DCMAKE_MAKE_PROGRAM=C:/TDM-GCC-64/bin/mingw32-make.exe -DENABLE_ZENZ=OFF -DBUILD_MOZC_FETCH=OFF
    if($LASTEXITCODE -ne 0){throw 'Configure failed'}
    & $cmake --build $lib --target path_algorithm token_array louds_utf16 connection_id -j 4
    if($LASTEXITCODE -ne 0){throw 'Library build failed'}
    $includes=@("-I$repo","-I$repo/src","-I$repo/src/dictionary_builder","-I$repo/src/dictionary_builder/louds_builder")
    & C:/TDM-GCC-64/bin/g++.exe -std=c++20 -O3 @includes "$PSScriptRoot/../japanese-engine-benchmark/ConverterServer.cpp" "-L$lib" -lpath_algorithm -lgraph_builder -ltoken_array -llouds_utf16 -lconnection_id -pthread -o "$work/build/converter-server-$Variant.exe"
    if($LASTEXITCODE -ne 0){throw 'Adapter build failed'}
    & C:/TDM-GCC-64/bin/g++.exe -std=c++20 -O3 @includes "$PSScriptRoot/TieRegression.cpp" "-L$lib" -lgraph_builder -ltoken_array -llouds_utf16 -lconnection_id -pthread -o "$work/build/tie-regression-$Variant.exe"
    if($LASTEXITCODE -ne 0){throw 'Regression build failed'}
    & "$work/build/tie-regression-$Variant.exe"
    if($LASTEXITCODE -ne 0){throw 'Tie regression failed'}
} finally {Pop-Location}
