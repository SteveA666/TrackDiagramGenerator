param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$WixDirectory,
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $JdkHome) {
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) { $JdkHome = Split-Path -Parent (Split-Path -Parent $compiler.Source) }
}
if (-not $JdkHome -or -not (Test-Path -LiteralPath (Join-Path $JdkHome 'bin/jpackage.exe'))) {
    throw 'Pass -JdkHome with a JDK 26+ containing bin/jpackage.exe.'
}
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:PATH
Push-Location $projectRoot
try {
    $env:JAVA_HOME = $JdkHome
    if (-not $SkipBuild) {
        & ./mvnw.cmd verify
        if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed.' }
    }
    if (-not $WixDirectory) { $WixDirectory = Join-Path $projectRoot 'work/tools/wix314' }
    if (-not (Test-Path -LiteralPath (Join-Path $WixDirectory 'candle.exe'))) {
        New-Item -ItemType Directory -Path "$projectRoot/work/tools" -Force | Out-Null
        $download = "$projectRoot/work/tools/wix314-binaries.zip"
        Invoke-WebRequest -Uri 'https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip' -OutFile $download -UseBasicParsing
        Expand-Archive -LiteralPath $download -DestinationPath $WixDirectory -Force
    }
    foreach ($tool in @('candle.exe', 'light.exe')) {
        if (-not (Test-Path -LiteralPath (Join-Path $WixDirectory $tool))) { throw "WiX tool missing: $tool" }
    }
    $env:PATH = "$WixDirectory;$env:PATH"
    [xml]$pom = Get-Content -LiteralPath pom.xml
    $version = [string]$pom.project.version
    if ($version -notmatch '^\d+\.\d+\.\d+$') { throw 'MSI releases require a numeric x.y.z version.' }
    $jar = Join-Path $projectRoot "target/track-diagram-generator-$version.jar"
    if (-not (Test-Path -LiteralPath $jar)) { throw "Build first: missing $jar" }
    $architecture = (Select-String -LiteralPath (Join-Path $JdkHome 'release') -Pattern '^OS_ARCH="([^"]+)"').Matches.Groups[1].Value
    $architecture = switch ($architecture) {
        'amd64' { 'x64' } 'x86_64' { 'x64' } 'aarch64' { 'arm64' } 'arm64' { 'arm64' }
        default { throw "Unsupported JDK architecture: $architecture" }
    }
    $stage = Join-Path $projectRoot ('target/msi-build/' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path "$stage/input", "$stage/output", "$projectRoot/target/releases" -Force | Out-Null
    Copy-Item -LiteralPath $jar -Destination "$stage/input"
    $imageOptions = @('--type', 'app-image', '--name', 'TrackDiagramGenerator', '--app-version', $version,
        '--input', "$stage/input", '--main-jar', (Split-Path -Leaf $jar), '--main-class', 'Main',
        '--java-options', '-Dtrackdiagram.installed=true',
        '--add-modules', 'java.desktop,java.logging,jdk.unsupported',
        '--jlink-options', '--strip-debug --no-header-files --no-man-pages', '--dest', "$stage/image")
    & (Join-Path $JdkHome 'bin/jpackage.exe') @imageOptions
    if ($LASTEXITCODE -ne 0) { throw 'Installer application image failed.' }
    $image = "$stage/image/TrackDiagramGenerator"
    # Test with an isolated override so the build does not write the builder's installed user data.
    & (Join-Path $JdkHome 'bin/javac.exe') -cp $jar -d "$stage/smoke" "$PSScriptRoot/PortableSmoke.java"
    if ($LASTEXITCODE -ne 0) { throw 'Smoke-test compilation failed.' }
    $smokeHome = New-Item -ItemType Directory -Path "$stage/test-data"
    & "$image/runtime/bin/java.exe" '-Djava.awt.headless=true' '-Dtrackdiagram.installed=true' "-Dtrackdiagram.home=$($smokeHome.FullName)" -cp "$image/app/$(Split-Path -Leaf $jar);$stage/smoke" PortableSmoke $smokeHome.FullName
    if ($LASTEXITCODE -ne 0) { throw 'Installer runtime check failed.' }
    $msiOptions = @('--type', 'msi', '--name', 'TrackDiagramGenerator', '--app-version', $version,
        '--app-image', $image, '--dest', "$stage/output", '--vendor', 'Track Diagram Generator',
        '--description', 'Railway track diagram editor', '--win-per-user-install',
        '--install-dir', 'TrackDiagramGeneratorApp', '--win-dir-chooser', '--win-menu',
        '--win-menu-group', 'Track Diagram Generator', '--win-shortcut', '--win-shortcut-prompt',
        '--win-upgrade-uuid', 'b4fc283d-7e42-44eb-8f6f-a147ee11635a')
    & (Join-Path $JdkHome 'bin/jpackage.exe') @msiOptions
    if ($LASTEXITCODE -ne 0) { throw 'MSI packaging failed.' }
    $built = @(Get-ChildItem -LiteralPath "$stage/output" -Filter '*.msi')
    if ($built.Count -ne 1) { throw 'Expected exactly one MSI output.' }
    $destination = Join-Path $projectRoot "target/releases/TrackDiagramGenerator-$version-windows-$architecture.msi"
    Copy-Item -LiteralPath $built[0].FullName -Destination $destination -Force
    $hash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
    Set-Content -LiteralPath "$destination.sha256" -Encoding ascii -Value "$hash  $(Split-Path -Leaf $destination)"
    Write-Host "Created $destination"
    Write-Host "Installer application image: $image"
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:PATH = $previousPath
    Pop-Location
}
