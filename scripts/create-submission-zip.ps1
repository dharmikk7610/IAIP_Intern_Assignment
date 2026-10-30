# Creates EmployeeManagementSystem.zip in the project root for submission.
# Excludes dependencies, build output, Git data, IDE files and local secrets.
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$zipPath = Join-Path $root 'EmployeeManagementSystem.zip'
$staging = Join-Path ([IO.Path]::GetTempPath()) ("ems-submission-" + [Guid]::NewGuid())
$target = Join-Path $staging 'EmployeeManagementSystem'

$excludedDirs = @('node_modules', 'dist', 'target', '.git', '.idea', '.vscode')
$excludedFiles = @('application-local.properties', '.env', '.env.*', '*.log', '*.zip', '*.iml')

try {
    # robocopy exit codes 0-7 mean success
    robocopy $root $target /E /NFL /NDL /NJH /NJS /NP /XD @excludedDirs /XF @excludedFiles | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "robocopy failed with exit code $LASTEXITCODE" }

    $leaked = Get-ChildItem $target -Recurse -File -Filter 'application-local.properties'
    if ($leaked) { throw "Refusing to package local secrets: $($leaked.FullName)" }

    if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
    # Not Compress-Archive: Windows PowerShell 5.1 writes "\" in entry names, which breaks
    # extraction on macOS / Linux. Entries are added one by one with "/" separators instead.
    Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::Open($zipPath, [IO.Compression.ZipArchiveMode]::Create)
    try {
        Get-ChildItem $target -Recurse -File | ForEach-Object {
            $entryName = 'EmployeeManagementSystem/' + $_.FullName.Substring($target.Length + 1).Replace('\', '/')
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $entryName,
                [IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    }
    finally {
        $zip.Dispose()
    }
    $sizeKb = [math]::Round((Get-Item $zipPath).Length / 1KB)
    Write-Host "Created $zipPath ($sizeKb KB)"
}
finally {
    if (Test-Path $staging) { Remove-Item $staging -Recurse -Force }
}
