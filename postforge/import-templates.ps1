<#
.SYNOPSIS
  ScrumTools e-posta şablonlarını PostForge'a yükler (idempotent: varsa günceller).

.EXAMPLE
  $env:PF_EMAIL="you@example.com"; $env:PF_PASSWORD="..."
  .\postforge\import-templates.ps1

.EXAMPLE
  .\postforge\import-templates.ps1 -Token <jwt> -BaseUrl http://localhost:8080
#>
param(
    [string]$BaseUrl  = $(if ($env:PF_BASE_URL) { $env:PF_BASE_URL } else { "https://postforge.kocak.net.tr" }),
    [string]$Token    = $env:PF_TOKEN,
    [string]$Email    = $env:PF_EMAIL,
    [string]$Password = $env:PF_PASSWORD
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$BaseUrl = $BaseUrl.TrimEnd('/')
$manifestPath = Join-Path $PSScriptRoot 'templates.json'
if (-not (Test-Path $manifestPath)) { throw "templates.json bulunamadi: $manifestPath" }

$manifest = Get-Content $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json

# UTF-8 gövde: PS 5.1 Invoke-RestMethod string body'yi Latin-1'e çevirebilir, byte[] gönderiyoruz
function Send-Json {
    param([string]$Method, [string]$Uri, $Body, $Headers)
    $bytes = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 10))
    Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers `
        -ContentType 'application/json; charset=utf-8' -Body $bytes
}

if (-not $Token) {
    if (-not $Email -or -not $Password) { throw "PF_EMAIL + PF_PASSWORD veya PF_TOKEN tanimlayin" }
    Write-Host "-> PostForge'a giris yapiliyor ($BaseUrl)..."
    $auth = Send-Json -Method Post -Uri "$BaseUrl/api/auth/login" `
        -Body ([ordered]@{ email = $Email; password = $Password }) -Headers @{}
    $Token = $auth.token
    if (-not $Token) { throw "Giris basarisiz (e-posta/sifre?)" }
}

$headers = @{ Authorization = "Bearer $Token" }

Write-Host "-> Mevcut sablonlar okunuyor..."
$existing = @(Invoke-RestMethod -Uri "$BaseUrl/api/templates" -Headers $headers)

foreach ($t in $manifest) {
    $file = Join-Path $PSScriptRoot $t.file
    if (-not (Test-Path $file)) { throw "Sablon dosyasi bulunamadi: $file" }

    $payload = [ordered]@{
        code            = $t.code
        name            = $t.name
        subjectTemplate = $t.subjectTemplate
        htmlBody        = (Get-Content $file -Raw -Encoding UTF8)
        paramsSchema    = ($t.paramsSchema | ConvertTo-Json -Depth 10 -Compress)
        active          = $true
    }

    $match = $existing | Where-Object { $_.code -eq $t.code } | Select-Object -First 1
    if ($match) {
        Send-Json -Method Put -Uri "$BaseUrl/api/templates/$($match.id)" -Body $payload -Headers $headers | Out-Null
        Write-Host "   guncellendi: $($t.code) (id=$($match.id))"
    } else {
        Send-Json -Method Post -Uri "$BaseUrl/api/templates" -Body $payload -Headers $headers | Out-Null
        Write-Host "   olusturuldu: $($t.code)"
    }
}

Write-Host "Tamam - $($manifest.Count) sablon senkronize edildi."
