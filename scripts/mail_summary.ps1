param(
  # Path to an EXISTING generated Allure report folder (must contain widgets and data\test-cases)
  [string] $AllureReportDir = (Join-Path (Get-Location) "allure-report"),

  # Email fields
  [string] $To        = "nihilesh.karthickmani@kpn.com",
  [string] $Cc        = "",
  [string] $Subject   = "Prepaid Regression - Allure Summary",

  # Optional: link to Confluence page where you publish the ZIP
  [string] $ConfluenceLink = "",

  # Optional: attach the Allure ZIP
  [switch] $AttachZip,
  [string] $ZipPath   = (Join-Path (Get-Location) "target\AllureReport.zip"),

  # Grouping key for Suites table: suite|parentSuite|subSuite|feature|story|package
  [string] $GroupBy   = "suite"
)

$ErrorActionPreference = "Stop"

function Percent($num,$denom){
  if($denom -le 0){return 0}
  return [math]::Round(($num*100.0)/$denom,1)
}

function Format-Duration([int64]$ms) {
  $ts = [TimeSpan]::FromMilliseconds($ms)
  if ($ts.TotalHours -ge 1)   { return "{0:00}h {1:00}m {2:00}s" -f $ts.Hours,$ts.Minutes,$ts.Seconds }
  elseif ($ts.TotalMinutes -ge 1) { return "{0:00}m {1:00}s" -f $ts.Minutes,$ts.Seconds }
  else { return "{0}s" -f [int]$ts.TotalSeconds }
}

# --- 1) Load overall summary (totals, timing) ---
$summaryJsonPath = Join-Path $AllureReportDir "widgets\summary.json"
if (!(Test-Path $summaryJsonPath)) { throw "summary.json not found: $summaryJsonPath" }
$summary = Get-Content $summaryJsonPath -Raw | ConvertFrom-Json

$total   = [int]$summary.statistic.total
$passed  = [int]$summary.statistic.passed
$failed  = [int]$summary.statistic.failed
$broken  = [int]$summary.statistic.broken
$skipped = [int]$summary.statistic.skipped
$unknown = [int]$summary.statistic.unknown

$startTs    = [int64]$summary.time.start
$stopTs     = [int64]$summary.time.stop
$durationMs = [int64]$summary.time.duration
$durationStr = Format-Duration $durationMs
$start = (Get-Date "1970-01-01").AddMilliseconds($startTs)
$stop  = (Get-Date "1970-01-01").AddMilliseconds($stopTs)

$pp = Percent $passed $total
$pf = Percent ($failed+$broken) $total
$ps = Percent $skipped $total
$pu = Percent $unknown $total

# --- 2) Build suite table from data\test-cases ---
$casesDir = Join-Path $AllureReportDir "data\test-cases"
if (!(Test-Path $casesDir)) {
  Write-Warning "No test-cases directory found at $casesDir. Suites table may be empty."
}

# Helper: extract a label value by name from a test-case's labels array
function Get-LabelValue([object]$labels, [string]$name) {
  if ($labels -eq $null) { return $null }
  foreach($l in $labels){
    if ($l.name -eq $name -and $l.value) { return [string]$l.value }
  }
  return $null
}

# Grouping map: name => counts
$bySuite = @{}

if (Test-Path $casesDir) {
  Get-ChildItem $casesDir -Filter *.json | ForEach-Object {
    $tc = Get-Content $_.FullName -Raw | ConvertFrom-Json

    # pick group key
    $key = Get-LabelValue $tc.labels $GroupBy
    if ([string]::IsNullOrWhiteSpace($key)) {
      # fallback chain for robustness
      $key = Get-LabelValue $tc.labels "suite"
      if ([string]::IsNullOrWhiteSpace($key)) { $key = Get-LabelValue $tc.labels "parentSuite" }
      if ([string]::IsNullOrWhiteSpace($key)) { $key = Get-LabelValue $tc.labels "feature" }
      if ([string]::IsNullOrWhiteSpace($key)) { $key = Get-LabelValue $tc.labels "package" }
      if ([string]::IsNullOrWhiteSpace($key)) { $key = "Uncategorized" }
    }

    if (-not $bySuite.ContainsKey($key)) {
      $bySuite[$key] = [ordered]@{ total=0; passed=0; failed=0; broken=0; skipped=0; unknown=0 }
    }

    $bySuite[$key].total++

    switch -Exact ($tc.status) {
      "passed"  { $bySuite[$key].passed++ }
      "failed"  { $bySuite[$key].failed++ }
      "broken"  { $bySuite[$key].broken++ }
      "skipped" { $bySuite[$key].skipped++ }
      default   { $bySuite[$key].unknown++ }
    }
  }
}

# Build rows sorted by name (or by total desc if you prefer)
$suiteRows = @()
$keys = $bySuite.Keys | Sort-Object
foreach ($name in $keys) {
  $st = $bySuite[$name]
  $t = [int]$st.total
  $p = [int]$st.passed
  $fb = $st.failed + $st.broken
  $sk = [int]$st.skipped

  $pw  = Percent $p $t
  $fbw = Percent $fb $t
  $skw = Percent $sk $t

  $suiteRows += @"
<tr>
  <td style='padding:6px 8px;border-bottom:1px solid #eee;'>$name</td>
  <td style='padding:6px 8px;border-bottom:1px solid #eee;text-align:right;'>$t</td>
  <td style='padding:6px 8px;border-bottom:1px solid #eee;text-align:right;color:#2e7d32;'>$p</td>
  <td style='padding:6px 8px;border-bottom:1px solid #eee;text-align:right;color:#c62828;'>$fb</td>
  <td style='padding:6px 8px;border-bottom:1px solid #eee;text-align:right;color:#6a5acd;'>$sk</td>
  <td style='padding:6px 8px;border-bottom:1px solid #eee;'>
    <div style='width:200px;height:8px;background:#f2f2f2;border-radius:4px;overflow:hidden;'>
      <div style='width:${pw}%;height:8px;background:#4caf50;float:left;'></div>
      <div style='width:${fbw}%;height:8px;background:#e53935;float:left;'></div>
      <div style='width:${skw}%;height:8px;background:#7e57c2;float:left;'></div>
    </div>
  </td>
</tr>
"@
}

# If still empty, show a helpful note
$emptyNote = ""
if ($suiteRows.Count -eq 0) {
  $emptyNote = "<tr><td colspan='6' style='padding:8px;color:#888;'>No suite-level rows could be computed from data\test-cases. Verify that the Allure report was generated and that test-cases exist.</td></tr>"
}

# --- 3) Optional Confluence link ---
$confLinkHtml = ""
if ($ConfluenceLink -and $ConfluenceLink.Trim().Length -gt 0) {
  $enc = [System.Web.HttpUtility]::HtmlEncode($ConfluenceLink)
  $confLinkHtml = "<p style='margin:8px 0;'>Confluence page: $enc</p>"
}

$reportDate = (Get-Date -Format "yyyy-MM-dd HH:mm")

# --- 4) Build email HTML ---
$bodyHtml = @"
<html>
<head><meta http-equiv='Content-Type' content='text/html; charset=utf-8' /></head>
<body style='font-family:Segoe UI,Arial,sans-serif;color:#333;'>
  <h2 style='margin:0 0 8px;'>Prepaid Regression - Allure Summary</h2>
  <div style='font-size:12px;color:#666;margin-bottom:12px;'>Generated: $reportDate</div>

  <table cellpadding='0' cellspacing='0' style='border-collapse:collapse;margin:0 0 14px;'>
    <tr>
      <td style='padding:6px 8px;background:#f7f7f7;border:1px solid #e0e0e0;'><b>Total</b></td>
      <td style='padding:6px 12px;border:1px solid #e0e0e0;'>$total</td>
      <td style='padding:6px 8px;background:#f7f7f7;border:1px solid #e0e0e0;'><b>Passed</b></td>
      <td style='padding:6px 12px;border:1px solid #e0e0e0;color:#2e7d32;'>$passed</td>
      <td style='padding:6px 8px;background:#f7f7f7;border:1px solid #e0e0e0;'><b>Failed+Broken</b></td>
      <td style='padding:6px 12px;border:1px solid #e0e0e0;color:#c62828;'>$($failed+$broken)</td>
      <td style='padding:6px 8px;background:#f7f7f7;border:1px solid #e0e0e0;'><b>Skipped</b></td>
      <td style='padding:6px 12px;border:1px solid #e0e0e0;color:#6a5acd;'>$skipped</td>
    </tr>
    <tr>
      <td style='padding:6px 8px;background:#f7f7f7;border:1px solid #e0e0e0;'><b>Unknown</b></td>
      <td style='padding:6px 12px;border:1px solid #e0e0e0;'>$unknown</td>
      <td style='padding:6px 8px;background:#f7f7f7;border:1px solid #e0e0e0;'><b>Duration</b></td>
      <td style='padding:6px 12px;border:1px solid #e0e0e0;' colspan='3'>$durationStr</td>
      <td style='padding:6px 8px;background:#f7f7f7;border:1px solid #e0e0e0;'><b>Window</b></td>
      <td style='padding:6px 12px;border:1px solid #e0e0e0;' colspan='1'>$(Get-Date $start -Format "HH:mm:ss") - $(Get-Date $stop -Format "HH:mm:ss")</td>
    </tr>
  </table>

  <div style='margin:6px 0 10px;'>
    <div style='width:360px;height:10px;background:#f2f2f2;border-radius:5px;overflow:hidden;'>
      <div title='Passed $pp%' style='height:10px;width:${pp}%;background:#4caf50;float:left;'></div>
      <div title='Failed+Broken $pf%' style='height:10px;width:${pf}%;background:#e53935;float:left;'></div>
      <div title='Skipped $ps%' style='height:10px;width:${ps}%;background:#7e57c2;float:left;'></div>
      <div title='Unknown $pu%' style='height:10px;width:${pu}%;background:#9e9e9e;float:left;'></div>
    </div>
    <div style='font-size:12px;color:#666;margin-top:4px;'>
      Passed $pp% &nbsp;|&nbsp; Failed+Broken $pf% &nbsp;|&nbsp; Skipped $ps% &nbsp;|&nbsp; Unknown $pu%
    </div>
  </div>

  <h3 style='margin:14px 0 6px;'>Suites (grouped by: $GroupBy)</h3>
  <table cellpadding='0' cellspacing='0' style='border-collapse:collapse;min-width:600px;'>
    <thead>
      <tr style='background:#f7f7f7;'>
        <th align='left'  style='padding:6px 8px;border-bottom:1px solid #ddd;'>Suite</th>
        <th align='right' style='padding:6px 8px;border-bottom:1px solid #ddd;'>Total</th>
        <th align='right' style='padding:6px 8px;border-bottom:1px solid #ddd;color:#2e7d32;'>Passed</th>
        <th align='right' style='padding:6px 8px;border-bottom:1px solid #ddd;color:#c62828;'>Failed+Broken</th>
        <th align='right' style='padding:6px 8px;border-bottom:1px solid #ddd;color:#6a5acd;'>Skipped</th>
        <th align='left'  style='padding:6px 8px;border-bottom:1px solid #ddd;'>Bar</th>
      </tr>
    </thead>
    <tbody>
      $(
        if ($suiteRows.Count -gt 0) { $suiteRows -join "`n" } else { $emptyNote }
      )
    </tbody>
  </table>

  $confLinkHtml

  <p style='margin-top:12px;font-size:12px;color:#666;'>
    Static email summary (Outlook-safe). For interactive graphs, open the Allure report.
  </p>
</body>
</html>
"@

# --- 6) Send via Outlook (HTML) ---
$Outlook = New-Object -ComObject Outlook.Application
$ns = $Outlook.GetNamespace("MAPI")
$inbox = $ns.GetDefaultFolder(6) | Out-Null

$mail = $Outlook.CreateItem(0)
$mail.To = $To
if ($Cc) { $mail.CC = $Cc }
$mail.Subject = $Subject
$mail.HTMLBody = $bodyHtml

if ($AttachZip) {
  if (!(Test-Path $ZipPath)) { Write-Warning "Zip attachment requested but not found: $ZipPath" }
  else { $mail.Attachments.Add($ZipPath) | Out-Null }
}

try {
  $mail.Send()
  Write-Host "[Ok] Summary email sent."
} catch {
  Write-Error "Auto-send blocked or failed: $_"
  Write-Host "Opening draft..."
  $mail.Display()
}