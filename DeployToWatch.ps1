param (
    [Parameter(Mandatory = $false)]
    [string]$WatchIP,

    [Parameter(Mandatory = $false)]
    [int]$ConnectPort = 5555,

    [Parameter(Mandatory = $false)]
    [int]$PairingPort,

    [Parameter(Mandatory = $false)]
    [string]$PairingCode
)

$apkPath = "$PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $apkPath)) {
    Write-Error "APK not found at $apkPath. Please build first using: .\gradlew assembleDebug"
    exit 1
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Galaxy Watch 8 - Deployment Tool" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# Check if a device is already connected via adb
$connectedDevices = (adb devices | Select-String -Pattern "device$")
if ($connectedDevices) {
    Write-Host "Found connected device(s):" -ForegroundColor Green
    $connectedDevices | ForEach-Object { Write-Host "  $_" -ForegroundColor Green }
} elseif (-not $WatchIP) {
    Write-Host "No connected ADB devices detected." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To connect your Galaxy Watch wirelessly:" -ForegroundColor White
    Write-Host "1. On Watch: Settings > About watch > Software info > Tap 'Software version' 7 times" -ForegroundColor Gray
    Write-Host "2. On Watch: Settings > Developer options > Enable 'ADB debugging' & 'Wireless debugging'" -ForegroundColor Gray
    Write-Host "3. Note the IP & port shown under 'Wireless debugging'" -ForegroundColor Gray
    Write-Host "4. If pairing for the first time, tap 'Pair new device' and run:" -ForegroundColor White
    Write-Host "   .\DeployToWatch.ps1 -WatchIP <IP> -PairingPort <PairPort> -PairingCode <Code> -ConnectPort <Port>" -ForegroundColor Yellow
    Write-Host "   Otherwise, simply run:" -ForegroundColor White
    Write-Host "   .\DeployToWatch.ps1 -WatchIP <IP> -ConnectPort <Port>" -ForegroundColor Yellow
    exit 0
}

# Perform wireless pairing if pairing code and port are provided
if ($PairingPort -and $PairingCode) {
    Write-Host "Pairing with $WatchIP:$PairingPort..." -ForegroundColor Cyan
    adb pair "$WatchIP`:$PairingPort" $PairingCode
}

# Connect via ADB
if ($WatchIP) {
    Write-Host "Connecting to $WatchIP:$ConnectPort..." -ForegroundColor Cyan
    adb connect "$WatchIP`:$ConnectPort"
}

# Install APK
Write-Host "Installing $apkPath onto Galaxy Watch..." -ForegroundColor Cyan
adb install -r -d $apkPath

if ($LASTEXITCODE -eq 0) {
    Write-Host "=========================================" -ForegroundColor Green
    Write-Host " Successfully installed Galaxy Games! " -ForegroundColor Green
    Write-Host " Launching application..." -ForegroundColor Green
    Write-Host "=========================================" -ForegroundColor Green
    adb shell am start -n com.gaurav.galaxyapp.debug/com.gaurav.galaxyapp.MainActivity
} else {
    Write-Error "Installation failed. Check ADB connection."
}
