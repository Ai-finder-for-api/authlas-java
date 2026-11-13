param(
    [Parameter(Mandatory = $true)][string]$KeyId,
    [string]$Gpg = 'C:\Program Files\GnuPG\bin\gpg.exe',
    [switch]$DryRun
)

# Signs and publishes authlas to Maven Central through the Central Portal.
# Dry run (signs and verifies locally, uploads nothing):  .\publish.ps1 -KeyId <KEYID> -DryRun
# Publish (uploads and releases):                          .\publish.ps1 -KeyId <KEYID>

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$goals = if ($DryRun) { @('-DskipTests', 'verify') } else { @('clean', 'deploy') }

& mvn -B -ntp -P release "-Dgpg.executable=$Gpg" "-Dgpg.keyname=$KeyId" @goals
if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }

if ($DryRun) { Write-Output 'Dry run OK: artifacts built and signed, nothing uploaded.' }
else { Write-Output 'Published. Check https://central.sonatype.com/publishing/deployments' }
