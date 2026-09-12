param([string]$Output='artifacts/native-metadata/probe.exe')
$ErrorActionPreference='Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    $taskRoot=(Get-Location).Path
    $taskVs=& 'C:/Program Files (x86)/Microsoft Visual Studio/Installer/vswhere.exe' -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
    if(!$taskVs){throw 'MSVC C++ tools required for the pinned MSVC Rime DLL ABI'}
    $taskCompiler=Join-Path $taskVs 'VC/Auxiliary/Build/vcvars64.bat'
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
