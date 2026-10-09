param(
    [ValidateSet('Play', 'Build')]
    [string]$Mode = 'Play',
    [ValidatePattern('^[A-Za-z0-9_]{1,16}$')]
    [string]$PlayerName
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$projectRoot = Split-Path -Parent $PSScriptRoot
$cacheRoot = Join-Path $projectRoot '.launcher'
$javaRoot = Join-Path $cacheRoot 'java'
$jdkHome = Join-Path $javaRoot 'jdk-25.0.4.1+1'
$readyFile = Join-Path $javaRoot 'jdk-25.ready'
$jdkUrl = 'https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.4.1%2B1/OpenJDK25U-jdk_x64_windows_hotspot_25.0.4.1_1.zip'
$jdkSha256 = '00c847d804f4a78e9f04f2683faf14fed898535b177b7fc704486cb0284e9283'
$lockFile = $null

try {
    if (-not [Environment]::Is64BitOperatingSystem) {
        throw 'This launcher requires 64-bit Windows.'
    }
    if (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'gradlew.bat'))) {
        throw 'Keep the launcher folder beside gradlew.bat in the mod repository.'
    }
    New-Item -ItemType Directory -Path $cacheRoot -Force | Out-Null
    try {
        $lockFile = [IO.File]::Open((Join-Path $cacheRoot 'session.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    } catch {
        throw 'Another launcher or build is already running for this project. Close that session before trying again.'
    }

    $playerNameFile = Join-Path $cacheRoot 'player-name.txt'
    if ([string]::IsNullOrWhiteSpace($PlayerName)) {
        $PlayerName = if (Test-Path -LiteralPath $playerNameFile) {
            (Get-Content -LiteralPath $playerNameFile -Raw).Trim()
        } else { 'Cultivator' }
    }
    if ($PlayerName -notmatch '^[A-Za-z0-9_]{1,16}$') {
        throw 'The saved player name must contain 1-16 letters, numbers, or underscores.'
    }
    Set-Content -LiteralPath $playerNameFile -Value $PlayerName -Encoding ASCII

    if (-not ((Test-Path -LiteralPath $readyFile) -and (Test-Path -LiteralPath (Join-Path $jdkHome 'bin\javac.exe')))) {
        Write-Output 'STATUS:Downloading Java for the first launch (135 MB)...'
        $archive = Join-Path $cacheRoot 'temurin-25.zip'
        $partial = Join-Path $cacheRoot 'temurin-25.zip.partial'
        $validArchive = (Test-Path -LiteralPath $archive) -and ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -eq $jdkSha256)
        if (-not $validArchive) {
            $download = New-Object Net.WebClient
            try { $download.DownloadFile($jdkUrl, $partial) } finally { $download.Dispose() }
            if ((Get-FileHash -LiteralPath $partial -Algorithm SHA256).Hash -ne $jdkSha256) {
                throw 'Java download failed its SHA-256 check. Launch again to retry the download.'
            }
            Move-Item -LiteralPath $partial -Destination $archive -Force
        }
        Write-Output 'STATUS:Unpacking Java...'
        Expand-Archive -LiteralPath $archive -DestinationPath $javaRoot -Force
        if (-not (Test-Path -LiteralPath (Join-Path $jdkHome 'bin\javac.exe'))) {
            throw 'The Java archive did not contain the expected JDK. See launcher/README.md.'
        }
        & (Join-Path $jdkHome 'bin\java.exe') -version
        if ($LASTEXITCODE -ne 0) { throw 'The downloaded Java runtime could not start. Launch again to retry setup.' }
        Set-Content -LiteralPath $readyFile -Value $jdkSha256 -Encoding ASCII
    }

    # These settings apply only to this process and its children.
    $env:JAVA_HOME = $jdkHome
    $env:PATH = (Join-Path $jdkHome 'bin') + ';' + $env:PATH
    $env:GRADLE_USER_HOME = Join-Path $cacheRoot 'gradle'
    Set-Location -LiteralPath $projectRoot
    if ($Mode -eq 'Build') {
        Write-Output 'STATUS:Building and checking the mod...'
        & .\gradlew.bat --console=plain --no-daemon build
    } else {
        Write-Output 'STATUS:Preparing Minecraft. The first launch downloads game files...'
        Write-Output "Development player: $PlayerName"
        # A stable development username also keeps the offline player UUID stable.
        & .\gradlew.bat --console=plain --no-daemon runClient "--args=--username $PlayerName"
    }
    $gradleExitCode = $LASTEXITCODE
    if ($gradleExitCode -ne 0) {
        throw "Minecraft setup or Gradle failed (exit code $gradleExitCode). The details are in the log above."
    }
    Write-Output 'STATUS:Finished.'
    exit 0
} catch {
    Write-Output ('ERROR: ' + $_.Exception.Message)
    exit 1
} finally {
    if ($null -ne $lockFile) { $lockFile.Dispose() }
}
