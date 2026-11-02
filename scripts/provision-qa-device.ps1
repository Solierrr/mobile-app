param(
    [Parameter(Mandatory = $true)]
    [SecureString]$DeviceToken,
    [string]$Serial,
    [string]$Adb = "adb"
)

$ErrorActionPreference = "Stop"
$package = "com.project.solaria_mobile.qa"
$targetArgs = if ($Serial) { @("-s", $Serial) } else { @() }
$buffer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($DeviceToken)
try {
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($buffer)
    if ($plain -cnotmatch '^[A-Za-z0-9_-]{43,128}$') { throw "Credencial de aparelho inválida." }
    & $Adb @targetArgs shell am force-stop $package
    if ($LASTEXITCODE -ne 0) { throw "Não foi possível parar o aplicativo QA." }
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $Adb
    $start.UseShellExecute = $false
    $start.RedirectStandardInput = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    foreach ($argument in $targetArgs) { $start.ArgumentList.Add($argument) }
    $start.ArgumentList.Add("shell")
    $start.ArgumentList.Add("run-as $package sh -c 'umask 077; mkdir -p no_backup; cat > no_backup/qa-provisioning-token'")
    $process = [Diagnostics.Process]::Start($start)
    $process.StandardInput.Write($plain)
    $process.StandardInput.Close()
    $process.StandardOutput.ReadToEnd() | Out-Null
    $process.StandardError.ReadToEnd() | Out-Null
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "Falha ao provisionar. Instale o APK QA debug e autorize o aparelho no ADB." }
    & $Adb @targetArgs shell am start -n "$package/com.project.solaria_mobile.MainActivity" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "A configuração foi entregue, mas o app não abriu." }
    Write-Output "Aparelho QA configurado. A credencial será cifrada no armazenamento privado do app."
} finally {
    $plain = $null
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($buffer)
}
