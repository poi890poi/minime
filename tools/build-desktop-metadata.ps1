param([string]$Output='artifacts/native-metadata/probe.exe',[switch]$SourceOnly,[switch]$SourceBaseline)
$ErrorActionPreference='Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    $taskRoot=(Get-Location).Path
    $taskVs=& 'C:/Program Files (x86)/Microsoft Visual Studio/Installer/vswhere.exe' -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
    if(!$taskVs){throw 'MSVC C++ tools required for the pinned MSVC Rime DLL ABI'}
    $taskCompiler=Join-Path $taskVs 'VC/Auxiliary/Build/vcvars64.bat'
    if($SourceOnly -and $SourceBaseline){throw 'Choose one native experiment variant'}
    if($SourceOnly -or $SourceBaseline) {
        $taskCmake=Join-Path $taskRoot '.tools/android-sdk/cmake/3.22.1/bin/cmake.exe'
        $taskNinja=Join-Path $taskRoot '.tools/android-sdk/cmake/3.22.1/bin/ninja.exe'
        $taskClang=Join-Path $taskRoot '.tools/android-sdk/ndk/27.2.12479018/toolchains/llvm/prebuilt/windows-x86_64/bin/clang-cl.exe'
        $taskClang=$taskClang.Replace('\','/')
        $taskAblation=if($SourceOnly){'ON'}else{'OFF'}
        $taskCommand='"{0}" >nul && "{1}" -S tools/desktop-native -B artifacts/native-source-only-clang -G Ninja "-DCMAKE_MAKE_PROGRAM={2}" "-DCMAKE_C_COMPILER={3}" "-DCMAKE_CXX_COMPILER={3}" -DMINIME_SOURCE_ONLY={4} -DCMAKE_BUILD_TYPE=Release && "{1}" --build artifacts/native-source-only-clang -j 6' -f $taskCompiler,$taskCmake,$taskNinja,$taskClang,$taskAblation
        & cmd /d /c $taskCommand
        if($LASTEXITCODE -ne 0){throw 'Pinned source-only native evaluator compilation failed'}
        New-Item -ItemType Directory -Force (Split-Path $Output) | Out-Null
        Copy-Item -LiteralPath artifacts/native-source-only-clang/probe.exe -Destination $Output -Force
        return
    }
    New-Item -ItemType Directory -Force (Split-Path $Output),'artifacts/native-metadata/include/rime' | Out-Null
    # Generate the evaluator-only header from the pinned BSD template. Logging
    # and install paths are unused by this linked metadata reader.
    $taskTemplate=Get-Content '.tools/native-sources/librime-1.16.1/src/rime/build_config.h.in' -Raw
    $taskHeader=[regex]::Replace($taskTemplate,'(?m)^#cmakedefine[^\r\n]*','')
    [IO.File]::WriteAllText((Join-Path $taskRoot 'artifacts/native-metadata/include/rime/build_config.h'),$taskHeader)
    $taskCommand='"{0}" >nul && cl /nologo /std:c++17 /EHsc /O2 /MT /utf-8 /DRIME_IMPORTS /DNOMINMAX /Iartifacts/native-metadata/include /I.tools/native-sources/librime-1.16.1/src /I.tools/native-sources/boost_1_87_0 /I.tools/rime-evaluation/msvc/include tools/desktop_rime_metadata.cpp /Foartifacts/native-metadata/probe.obj /Fe"{1}" /link .tools/rime-evaluation/msvc/dist/lib/rime.lib' -f $taskCompiler,$Output
    & cmd /d /c $taskCommand
    if($LASTEXITCODE -ne 0){throw 'Pinned metadata bridge compilation failed'}
} finally {Pop-Location}
