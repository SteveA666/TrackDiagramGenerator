$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$buildPath = Join-Path $projectRoot 'work/model-tests'
New-Item -ItemType Directory -Force -Path $buildPath | Out-Null
$sourceFiles = @((Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Recurse -Filter '*.java').FullName)
$testFiles = @((Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.java').FullName)
& javac -d $buildPath @sourceFiles @testFiles
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
foreach ($testClass in @('ModelTests', 'EditorTests', 'CanvasTests', 'NavigationTests')) {
    & java '-Djava.awt.headless=true' -cp $buildPath $testClass
    if ($LASTEXITCODE -ne 0) { throw "$testClass failed" }
}
