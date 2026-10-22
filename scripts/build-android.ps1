param(
    [ValidateSet('qa', 'prod')][string]$Environment = 'qa',
    [string[]]$Tasks = @()
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$cli = Get-Command infisical -ErrorAction SilentlyContinue
$winget = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\infisical.infisical_*\infisical.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($winget) { $binary = $winget.FullName }
elseif ($cli) { $binary = $cli.Source }
else { throw 'Instale o Infisical CLI e autentique antes do build.' }
$variable = 'MAPS_ANDROID_KEY_' + $Environment.ToUpperInvariant()
$previous = [Environment]::GetEnvironmentVariable($variable, 'Process')
try {
    $raw = & $binary secrets --projectId=2296d19c-5f3b-41e1-afa3-fcde39966a71 "--env=$Environment" --path=/google --output=json --silent 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível ler a configuração de mapas no Infisical.' }
    $key = @((($raw -join "`n") | ConvertFrom-Json) | Where-Object secretKey -eq 'GOOGLE_MAPS_ANDROID_KEY')
    if ($key.Count -ne 1 -or [string]::IsNullOrWhiteSpace($key[0].secretValue)) { throw 'Chave Android de mapas não configurada para o ambiente.' }
    [Environment]::SetEnvironmentVariable($variable, $key[0].secretValue, 'Process')
    if ($Tasks.Count -eq 0) { $Tasks = @(':app:assemble' + (Get-Culture).TextInfo.ToTitleCase($Environment) + 'Debug') }
    & "$repo\gradlew.bat" -p $repo @Tasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'O build Android falhou. Consulte os diagnósticos acima.' }
} finally {
    [Environment]::SetEnvironmentVariable($variable, $previous, 'Process')
    $raw = $null
    $key = $null
}
