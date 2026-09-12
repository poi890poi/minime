param(
    [Parameter(Mandatory=$true)][string]$Bundle,
    [Parameter(Mandatory=$true)][string]$Output,
    [string]$SigningDirectory = '.tools/signing',
    [switch]$CreateUploadKey
)
# Local release tooling: no Play upload and no hosted CI. Password protection
# uses Windows DPAPI for the current Windows user on this computer.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    $inputBundle = (Resolve-Path -LiteralPath $Bundle).Path
    $outputBundle = [IO.Path]::GetFullPath((Join-Path $root $Output))
    if (Test-Path -LiteralPath $outputBundle) { throw 'Output already exists; choose a new path.' }
    $signingPath = [IO.Path]::GetFullPath((Join-Path $root $SigningDirectory))
    $store = Join-Path $signingPath 'minime-upload.jks'
    $passwordFile = Join-Path $signingPath 'password.dpapi'
    $certificate = Join-Path $signingPath 'upload-certificate.pem'
    $keytool = Join-Path $env:JAVA_HOME 'bin/keytool.exe'
    $jarsigner = Join-Path $env:JAVA_HOME 'bin/jarsigner.exe'
    if (!(Test-Path -LiteralPath $keytool) -or !(Test-Path -LiteralPath $jarsigner)) {
        throw 'Set JAVA_HOME to the installed JDK before signing.'
    }
    if ($CreateUploadKey) {
        if (Test-Path -LiteralPath $signingPath) {
            throw 'Signing directory already exists; refusing to replace any key material.'
        }
        New-Item -ItemType Directory -Path $signingPath | Out-Null
        # Restrict the directory before writing the key or encrypted password.
        $identity = [Security.Principal.WindowsIdentity]::GetCurrent().User
        $acl = Get-Acl -LiteralPath $signingPath
        $acl.SetAccessRuleProtection($true, $false)
        $rule = [Security.AccessControl.FileSystemAccessRule]::new(
            $identity, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
        $acl.SetAccessRule($rule)
        Set-Acl -LiteralPath $signingPath -AclObject $acl
        $random = New-Object byte[] 48
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($random) } finally { $rng.Dispose() }
        $securePassword = ConvertTo-SecureString ([Convert]::ToBase64String($random)) -AsPlainText -Force
        ConvertFrom-SecureString $securePassword | Set-Content -LiteralPath $passwordFile
        [Array]::Clear($random, 0, $random.Length)
    } else {
        if (!(Test-Path -LiteralPath $store) -or !(Test-Path -LiteralPath $passwordFile)) {
            throw 'Upload key or DPAPI password is missing. Use -CreateUploadKey only for initial setup.'
        }
        $securePassword = (Get-Content -LiteralPath $passwordFile -Raw).Trim() | ConvertTo-SecureString
    }
    $previousPassword = [Environment]::GetEnvironmentVariable('MINIME_LOCAL_SIGNING_PASSWORD', 'Process')
    try {
        $env:MINIME_LOCAL_SIGNING_PASSWORD = [Net.NetworkCredential]::new('', $securePassword).Password
        if ($CreateUploadKey) {
            & $keytool -genkeypair -keystore $store -storetype JKS -alias minime-upload `
                -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=MinIME Upload' `
                -storepass:env MINIME_LOCAL_SIGNING_PASSWORD -keypass:env MINIME_LOCAL_SIGNING_PASSWORD
            if ($LASTEXITCODE -ne 0) { throw 'Upload key generation failed; preserve the directory for inspection.' }
        }
        & $keytool -exportcert -rfc -keystore $store -alias minime-upload `
            -storepass:env MINIME_LOCAL_SIGNING_PASSWORD -file $certificate
        if ($LASTEXITCODE -ne 0) { throw 'Certificate export failed.' }
        New-Item -ItemType Directory -Force (Split-Path $outputBundle -Parent) | Out-Null
        & $jarsigner -keystore $store -storepass:env MINIME_LOCAL_SIGNING_PASSWORD `
            -keypass:env MINIME_LOCAL_SIGNING_PASSWORD -sigalg SHA256withRSA -digestalg SHA-256 `
            -signedjar $outputBundle $inputBundle minime-upload
        if ($LASTEXITCODE -ne 0) { throw 'Bundle signing failed.' }
    } finally {
        [Environment]::SetEnvironmentVariable('MINIME_LOCAL_SIGNING_PASSWORD', $previousPassword, 'Process')
        if ($securePassword) { $securePassword.Dispose() }
    }
    & $jarsigner -verify $outputBundle
    if ($LASTEXITCODE -ne 0) { throw 'Signed bundle verification failed.' }
    & (Join-Path $env:JAVA_HOME 'bin/java.exe') (Join-Path $PSScriptRoot 'VerifySignedBundle.java') $outputBundle $certificate
    if ($LASTEXITCODE -ne 0) { throw 'Bundle payload or upload-certificate verification failed.' }
    Write-Output "Signed AAB: $outputBundle"
    Write-Output "Public upload certificate: $certificate"
} finally { Pop-Location }
