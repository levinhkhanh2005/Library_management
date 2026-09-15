$processedArgs = @()
for ($i = 0; $i -lt $args.Count; $i++) {
    $arg = $args[$i]
    if ($arg.StartsWith(".") -and $processedArgs.Count -gt 0) {
        $processedArgs[-1] = $processedArgs[-1] + $arg
    } else {
        $processedArgs += $arg
    }
}
if (-not (Test-Path "pom.xml") -and (Test-Path "$PSScriptRoot\demo\pom.xml")) {
    Set-Location "$PSScriptRoot\demo"
}
$mvnCmd = "mvn"
if (-not (Get-Command "mvn" -ErrorAction SilentlyContinue)) {
    $fallback = "$env:USERPROFILE\.maven\maven-3.10.0-rc-1\bin\mvn.cmd"
    if (Test-Path $fallback) {
        $mvnCmd = $fallback
    }
}
& $mvnCmd $processedArgs
