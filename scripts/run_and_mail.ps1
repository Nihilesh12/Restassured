param(
  [string] $AllureCliBat = "C:\Users\karth501\OneDrive - KPN BV\Documents\Tools\allure-2.36.0\bin\allure.bat",
  [string] $ManagerEmail = "nihilesh.karthickmani@kpn.com",
  [string] $Cc = "",
  [string] $Subject = "Prepaid Regression - Allure Report (Auto)"
)

$ErrorActionPreference = "Stop"


# Paths
$project = Get-Location
$results = Join-Path $project "target\allure-results"
$report  = Join-Path $project "allure-report"

if (!(Test-Path $results)) {
  throw "Expected 'allure-results' under $project, but not found. Run your tests or check Allure listener."
}

# Rebuild report cleanly
if (Test-Path $report) { Remove-Item $report -Recurse -Force }

Write-Host "=== 2) Generating Allure report with CLI ==="
& "$AllureCliBat" generate "$results" --clean
if ($LASTEXITCODE -ne 0) { throw "Allure CLI generation failed ($LASTEXITCODE)" }

# Zip the report
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = Join-Path $project "target\AllureReport.zip"
$zipDir = Split-Path $zip -Parent
if (!(Test-Path $zipDir)) { New-Item -ItemType Directory -Path $zipDir | Out-Null }
if (Test-Path $zip) { Remove-Item $zip -Force }

Write-Host "=== 3) Zipping allure-report to $zip ==="
[System.IO.Compression.ZipFile]::CreateFromDirectory($report, $zip)

# Send via Outlook COM (AUTO-SEND, falls back to draft if blocked)
Write-Host "=== 4) Sending email via Outlook (auto) ==="
$Outlook = New-Object -ComObject Outlook.Application

# Ensure Outlook session opens a profile (touch Inbox)
$ns = $Outlook.GetNamespace("MAPI")
$inbox = $ns.GetDefaultFolder(6) | Out-Null

$mail = $Outlook.CreateItem(0)
$mail.To = $ManagerEmail
if ($Cc) { $mail.CC = $Cc }
$mail.Subject = $Subject

# Use single-quoted here-string to keep body literal/plain ASCII
$mail.Body = @'
Hi,

The automated Prepaid regression run has completed.
Attached is the Allure report ZIP.

To view properly (avoid "Loading..."):
1) Extract the ZIP
2) Open using Allure CLI:
   "<ALLURE_CLI_PATH>" open <extracted-allure-report-folder>

Regards,
Karthick
'@

# Replace placeholder with your configured path so it displays correctly
$mail.Body = $mail.Body -replace "<ALLURE_CLI_PATH>", [Regex]::Escape($AllureCliBat)

$mail.Attachments.Add($zip) | Out-Null

try {
  $mail.Send()
  Write-Host "=== Email sent (placed in Outbox). ==="
} catch {
  Write-Error "Outlook blocked auto-send or failed: $_"
  Write-Host "Falling back to draft window..."
  $mail.Display()
}

Write-Host "=== Completed. ==="