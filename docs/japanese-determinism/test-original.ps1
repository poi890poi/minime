$ErrorActionPreference='Stop'
Push-Location (Resolve-Path "$PSScriptRoot/../..")
try {
    $work='artifacts/japanese-engine-benchmark';$repo=(Get-ChildItem "$work/kazuma" -Directory)[0].FullName;$lib="$work/build"
    & C:/TDM-GCC-64/bin/g++.exe -std=c++20 -O3 -DORIGINAL "-I$repo/src" "-I$repo/src/dictionary_builder" "-I$repo/src/dictionary_builder/louds_builder" "$PSScriptRoot/TieRegression.cpp" "-L$lib" -lgraph_builder -ltoken_array -llouds_utf16 -lconnection_id -pthread -o "$lib/tie-regression-original.exe"
    if($LASTEXITCODE -ne 0){throw 'Regression compilation failed'}
    & "$lib/tie-regression-original.exe"
    if($LASTEXITCODE -ne 1){throw 'Expected the original comparator to fail'}
    Write-Output 'PASS original comparator fails the regression as expected'
} finally {Pop-Location}
