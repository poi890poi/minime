$ErrorActionPreference='Stop'
Push-Location (Resolve-Path "$PSScriptRoot/../..")
try {
    $cmake='E:/Program Files/CMake/bin/cmake.exe';$source=(Resolve-Path artifacts/japanese-engine-benchmark/kazuma-portable).Path
    & $cmake -S $PSScriptRoot -B artifacts/japanese-provider-desktop -G 'MinGW Makefiles' -DCMAKE_CXX_COMPILER=C:/TDM-GCC-64/bin/g++.exe -DCMAKE_MAKE_PROGRAM=C:/TDM-GCC-64/bin/mingw32-make.exe "-DUPSTREAM=$source" -DCMAKE_BUILD_TYPE=Release
    if($LASTEXITCODE -ne 0){throw 'Configure failed'}
    & $cmake --build artifacts/japanese-provider-desktop --target converter-server-portable beam-regression -j 4
    if($LASTEXITCODE -ne 0){throw 'Build failed'}
    & artifacts/japanese-provider-desktop/beam-regression.exe
    if($LASTEXITCODE -ne 0){throw 'Beam regression failed'}
    Copy-Item artifacts/japanese-provider-desktop/converter-server-portable.exe artifacts/japanese-engine-benchmark/build/
} finally {Pop-Location}
