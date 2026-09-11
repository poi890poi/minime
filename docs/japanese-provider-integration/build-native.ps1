$ErrorActionPreference='Stop'
Push-Location (Resolve-Path "$PSScriptRoot/../..")
try {
    $native=(Resolve-Path '.tools/android-sdk/ndk/27.2.12479018').Path
    $cmake=(Resolve-Path '.tools/android-sdk/cmake/3.22.1/bin/cmake.exe').Path
    $ninja=(Resolve-Path '.tools/android-sdk/cmake/3.22.1/bin/ninja.exe').Path
    $source=(Resolve-Path 'artifacts/japanese-engine-benchmark/kazuma-portable').Path
    & $cmake -S $PSScriptRoot -B artifacts/japanese-provider-native -G Ninja "-DCMAKE_MAKE_PROGRAM=$ninja" "-DCMAKE_TOOLCHAIN_FILE=$native/build/cmake/android.toolchain.cmake" -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 -DANDROID_STL=c++_static -DCMAKE_BUILD_TYPE=Release "-DUPSTREAM=$source"
    if($LASTEXITCODE -ne 0){throw 'Native evaluation configure failed'}
    & $cmake --build artifacts/japanese-provider-native --target minime_japanese_test -j 4
    if($LASTEXITCODE -ne 0){throw 'Native evaluation build failed'}
    $assets='app/build/generated/japaneseEvaluationAssets/japanese-evaluation'
    New-Item -ItemType Directory -Force $assets | Out-Null
    Copy-Item -LiteralPath artifacts/japanese-provider-native/libminime_japanese_test.so -Destination $assets
    foreach($name in @('yomi_termid.louds','tango.louds','token_array.bin','pos_table.bin','connection_single_column.bin')) {
        Copy-Item -LiteralPath "artifacts/japanese-engine-benchmark/build/$name" -Destination $assets
    }
    foreach($name in @('KAZUMA-LICENSE.txt','MOZC-LICENSE.txt','NOTICE.md')) {
        Copy-Item -LiteralPath "docs/japanese-engine-benchmark/$name" -Destination $assets
    }
} finally {Pop-Location}
