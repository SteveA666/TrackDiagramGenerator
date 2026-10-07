param(
    [string]$JdkHome = $env:JAVA_HOME,
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $JdkHome) {
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) { $JdkHome = Split-Path -Parent (Split-Path -Parent $compiler.Source) }
}
if (-not $JdkHome -or -not (Test-Path -LiteralPath (Join-Path $JdkHome 'bin/jpackage.exe'))) {
    throw 'Pass -JdkHome with the path to a JDK 26 or newer (containing bin/jpackage.exe).'
}
$previousJavaHome = $env:JAVA_HOME
Push-Location $projectRoot
try {
    $env:JAVA_HOME = $JdkHome
    if (-not $SkipBuild) {
        & ./mvnw.cmd clean verify
        if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
    }
    [xml]$pom = Get-Content -LiteralPath 'pom.xml'
    $version = [string]$pom.project.version
    if ($version -notmatch '^\d+\.\d+\.\d+$') { throw 'Portable releases require a numeric x.y.z version in pom.xml.' }
    $jar = Join-Path $projectRoot "target/track-diagram-generator-$version.jar"
    if (-not (Test-Path -LiteralPath $jar)) { throw "Build first: missing $jar" }
    $architecture = (Select-String -LiteralPath (Join-Path $JdkHome 'release') -Pattern '^OS_ARCH="([^"]+)"').Matches.Groups[1].Value
    $architecture = switch ($architecture) {
        'amd64' { 'x64' } 'x86_64' { 'x64' } 'aarch64' { 'arm64' } 'arm64' { 'arm64' }
        default { throw "Unsupported JDK architecture: $architecture" }
    }
    $stage = Join-Path $projectRoot ('target/portable-build/' + [guid]::NewGuid().ToString('N'))
    $inputDirectory = New-Item -ItemType Directory -Path "$stage/input" -Force
    Copy-Item -LiteralPath $jar -Destination $inputDirectory.FullName
    $jpackageOptions = @('--type', 'app-image', '--name', 'TrackDiagramGenerator',
        '--app-version', $version, '--input', $inputDirectory.FullName,
        '--main-jar', (Split-Path -Leaf $jar), '--main-class', 'Main',
        '--add-modules', 'java.desktop,java.logging,jdk.unsupported',
        '--jlink-options', '--strip-debug --no-header-files --no-man-pages',
        '--dest', "$stage/native", '--description', 'Railway track diagram editor')
    & (Join-Path $JdkHome 'bin/jpackage.exe') @jpackageOptions
    if ($LASTEXITCODE -ne 0) { throw 'jpackage failed.' }
    $nativeRoot = "$stage/native/TrackDiagramGenerator"
    $universalRoot = "$stage/universal/TrackDiagramGenerator"
    New-Item -ItemType Directory -Path $universalRoot -Force | Out-Null
    Copy-Item -LiteralPath $jar -Destination "$universalRoot/TrackDiagramGenerator.jar"
    Copy-Item -LiteralPath "$PSScriptRoot/launch.cmd", "$PSScriptRoot/launch.sh", "$PSScriptRoot/Launch.command" -Destination $universalRoot
    foreach ($portableRoot in @($nativeRoot, $universalRoot)) {
        New-Item -ItemType File -Path "$portableRoot/.portable" -Force | Out-Null
        New-Item -ItemType Directory -Path "$portableRoot/saved_diagrams" -Force | Out-Null
        Set-Content -LiteralPath "$portableRoot/saved_diagrams/.gitkeep" -Value ''
        Copy-Item -LiteralPath "$projectRoot/default.json", "$PSScriptRoot/PORTABLE.txt" -Destination $portableRoot
    }
    & (Join-Path $JdkHome 'bin/javac.exe') -cp $jar -d "$stage/smoke" "$PSScriptRoot/PortableSmoke.java"
    if ($LASTEXITCODE -ne 0) { throw 'Smoke-test compilation failed.' }
    & "$nativeRoot/runtime/bin/java.exe" '-Djava.awt.headless=true' -cp "$nativeRoot/app/$(Split-Path -Leaf $jar);$stage/smoke" PortableSmoke $nativeRoot
    if ($LASTEXITCODE -ne 0) { throw 'Bundled runtime smoke test failed.' }
    $outputDirectory = New-Item -ItemType Directory -Path "$projectRoot/target/releases" -Force
    # tar includes the hidden .portable marker, unlike Compress-Archive on some hosts.
    $nativeZip = Join-Path $outputDirectory "TrackDiagramGenerator-$version-windows-$architecture.zip"
    & tar.exe -a -cf $nativeZip -C "$stage/native" TrackDiagramGenerator
    if ($LASTEXITCODE -ne 0) { throw 'Windows archive creation failed.' }
    $universalZip = Join-Path $outputDirectory "TrackDiagramGenerator-$version-universal-jar.zip"
    & tar.exe -a -cf $universalZip -C "$stage/universal" TrackDiagramGenerator
    if ($LASTEXITCODE -ne 0) { throw 'Universal archive creation failed.' }
    foreach ($archive in @($nativeZip, $universalZip)) {
        $hash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
        Set-Content -LiteralPath "$archive.sha256" -Encoding ascii -Value "$hash  $(Split-Path -Leaf $archive)"
        Write-Host "Created $archive"
    }
    Write-Host "Uncompressed Windows app: $nativeRoot"
} finally {
    $env:JAVA_HOME = $previousJavaHome
    Pop-Location
}
