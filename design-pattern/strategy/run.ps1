# Compile and run the Strategy pattern demo (needs JDK 17+ on PATH).
$ErrorActionPreference = 'Stop'
$out = Join-Path $PSScriptRoot 'out'
$sources = (Get-ChildItem -Recurse -Filter *.java (Join-Path $PSScriptRoot 'src')).FullName
javac -encoding UTF-8 -d $out $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java -cp $out com.swd392.strategy.Main
