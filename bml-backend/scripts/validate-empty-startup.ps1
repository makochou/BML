[CmdletBinding()]
param(
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$DbName = "bml_system_empty_check",
    [string]$DbUser = "root",
    [string]$DbPassword = "",
    [string]$RedisHost = "127.0.0.1",
    [int]$RedisPort = 6379,
    [string]$RedisPassword = "",
    [int]$RedisDatabase = 0,
    [int]$ServerPort = 18081,
    [int]$ManagementPort = 18082,
    [int]$StartupTimeoutSeconds = 180,
    [string]$MysqlCli = "mysql",
    [string]$MavenCli = "mvn",
    [string]$JavaCmd = "java"
)

$ErrorActionPreference = "Stop"

function Get-RequiredCommand {
    param([Parameter(Mandatory = $true)][string]$Name)

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "Required command not found: $Name"
    }
    return $command.Source
}

function Test-TcpPort {
    param(
        [Parameter(Mandatory = $true)][string]$Host,
        [Parameter(Mandatory = $true)][int]$Port
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect($Host, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne(3000, $false)) {
            return $false
        }
        $client.EndConnect($async)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Invoke-MySql {
    param(
        [Parameter(Mandatory = $true)][string]$Executable,
        [Parameter(Mandatory = $true)][string]$Sql,
        [string]$Database = ""
    )

    $arguments = @(
        "--protocol=tcp",
        "-h", $DbHost,
        "-P", "$DbPort",
        "-u", $DbUser,
        "--default-character-set=utf8mb4",
        "-N",
        "-B"
    )

    if ($Database) {
        $arguments += @("-D", $Database)
    }

    $arguments += @("-e", $Sql)

    try {
        if ($DbPassword) {
            $env:MYSQL_PWD = $DbPassword
        }

        $output = & $Executable @arguments 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw ($output | Out-String).Trim()
        }

        return @($output)
    } finally {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Wait-ForHealth {
    param(
        [Parameter(Mandatory = $true)][int]$Port,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds,
        [Parameter(Mandatory = $true)][System.Diagnostics.Process]$Process,
        [Parameter(Mandatory = $true)][string]$StdoutPath,
        [Parameter(Mandatory = $true)][string]$StderrPath
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $healthUrl = "http://127.0.0.1:$Port/actuator/health"

    while ((Get-Date) -lt $deadline) {
        if ($Process.HasExited) {
            $stdout = if (Test-Path $StdoutPath) { Get-Content $StdoutPath -Raw } else { "" }
            $stderr = if (Test-Path $StderrPath) { Get-Content $StderrPath -Raw } else { "" }
            throw "Application exited early.`nSTDOUT:`n$stdout`nSTDERR:`n$stderr"
        }

        try {
            $response = Invoke-RestMethod -Method Get -Uri $healthUrl -TimeoutSec 5
            if ($response.status -eq "UP") {
                return
            }
        } catch {
        }

        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for actuator health on $healthUrl"
}

if ($DbName -notmatch "^[A-Za-z0-9_]+$") {
    throw "DbName must contain only letters, numbers, and underscores."
}

$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$targetDir = Join-Path $backendRoot "target\\empty-db-validation"
$stdoutPath = Join-Path $targetDir "app.stdout.log"
$stderrPath = Join-Path $targetDir "app.stderr.log"

New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
Remove-Item $stdoutPath, $stderrPath -Force -ErrorAction SilentlyContinue

$mysqlExe = Get-RequiredCommand -Name $MysqlCli
$mavenExe = Get-RequiredCommand -Name $MavenCli
$javaExe = Get-RequiredCommand -Name $JavaCmd

if (-not (Test-TcpPort -Host $RedisHost -Port $RedisPort)) {
    throw "Redis is not reachable at $RedisHost`:$RedisPort"
}

$envBackup = @{
    BML_DB_URL = $env:BML_DB_URL
    BML_DB_USERNAME = $env:BML_DB_USERNAME
    BML_DB_PASSWORD = $env:BML_DB_PASSWORD
    BML_REDIS_HOST = $env:BML_REDIS_HOST
    BML_REDIS_PORT = $env:BML_REDIS_PORT
    BML_REDIS_PASSWORD = $env:BML_REDIS_PASSWORD
    BML_REDIS_DATABASE = $env:BML_REDIS_DATABASE
    BML_SERVER_PORT = $env:BML_SERVER_PORT
    BML_MANAGEMENT_PORT = $env:BML_MANAGEMENT_PORT
    BML_MANAGEMENT_ADDRESS = $env:BML_MANAGEMENT_ADDRESS
    BML_JWT_SECRET = $env:BML_JWT_SECRET
    BML_OPENAPI_SECRET_ENCRYPTION_KEY = $env:BML_OPENAPI_SECRET_ENCRYPTION_KEY
    BML_MONITOR_PUBLIC_IP_ENABLED = $env:BML_MONITOR_PUBLIC_IP_ENABLED
    BML_SWAGGER_ENABLED = $env:BML_SWAGGER_ENABLED
}

$appProcess = $null

try {
    Write-Host "[1/5] Packaging backend application..."
    & $mavenExe "-q" "-pl" "bml-app" "-am" "-DskipTests" "package"
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed."
    }

    $jar = Get-ChildItem -Path (Join-Path $backendRoot "bml-app\\target") -Filter "bml-app-*.jar" |
        Where-Object { $_.Name -notlike "*.jar.original" -and $_.Name -notlike "*sources.jar" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $jar) {
        throw "Built jar not found under bml-app\\target."
    }

    Write-Host "[2/5] Recreating empty database $DbName..."
    Invoke-MySql -Executable $mysqlExe -Sql "DROP DATABASE IF EXISTS $DbName; CREATE DATABASE $DbName CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" | Out-Null

    $env:BML_DB_URL = "jdbc:mariadb://$DbHost`:$DbPort/$DbName?useUnicode=true&characterEncoding=utf-8&useSSL=false"
    $env:BML_DB_USERNAME = $DbUser
    $env:BML_DB_PASSWORD = $DbPassword
    $env:BML_REDIS_HOST = $RedisHost
    $env:BML_REDIS_PORT = "$RedisPort"
    $env:BML_REDIS_PASSWORD = $RedisPassword
    $env:BML_REDIS_DATABASE = "$RedisDatabase"
    $env:BML_SERVER_PORT = "$ServerPort"
    $env:BML_MANAGEMENT_PORT = "$ManagementPort"
    $env:BML_MANAGEMENT_ADDRESS = "127.0.0.1"
    $env:BML_JWT_SECRET = "empty-db-validation-jwt-secret-please-change"
    $env:BML_OPENAPI_SECRET_ENCRYPTION_KEY = "empty-db-validation-openapi-secret"
    $env:BML_MONITOR_PUBLIC_IP_ENABLED = "false"
    $env:BML_SWAGGER_ENABLED = "false"

    Write-Host "[3/5] Starting application on ports $ServerPort / $ManagementPort..."
    $appProcess = Start-Process -FilePath $javaExe `
        -ArgumentList @("-jar", $jar.FullName) `
        -WorkingDirectory $backendRoot `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -PassThru

    Write-Host "[4/5] Waiting for actuator health..."
    Wait-ForHealth -Port $ManagementPort -TimeoutSeconds $StartupTimeoutSeconds -Process $appProcess -StdoutPath $stdoutPath -StderrPath $stderrPath

    Write-Host "[5/5] Verifying migrated tables..."
    $tableNames = Invoke-MySql -Executable $mysqlExe -Database $DbName -Sql "SELECT table_name FROM information_schema.tables WHERE table_schema = '$DbName' ORDER BY table_name;"
    $tableSet = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($tableName in $tableNames) {
        if ($tableName) {
            [void]$tableSet.Add($tableName.Trim())
        }
    }

    $requiredTables = @(
        "flyway_schema_history",
        "sys_user",
        "sys_role",
        "sys_menu",
        "sys_api_account",
        "sys_api_registry",
        "sys_api_permission",
        "sys_alert"
    )
    $legacyTables = @(
        "bml_api_group",
        "bml_api_info",
        "bml_api_app",
        "bml_api_access"
    )

    $missingTables = @($requiredTables | Where-Object { -not $tableSet.Contains($_) })
    $legacyTablesStillPresent = @($legacyTables | Where-Object { $tableSet.Contains($_) })

    if ($missingTables.Count -gt 0) {
        throw "Missing required tables: $($missingTables -join ', ')"
    }

    if ($legacyTablesStillPresent.Count -gt 0) {
        throw "Legacy bml_api_* tables should not remain after migration: $($legacyTablesStillPresent -join ', ')"
    }

    $successfulFlywayMigrations = Invoke-MySql -Executable $mysqlExe -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1;"
    $migrationCount = [int]($successfulFlywayMigrations | Select-Object -First 1)

    Write-Host ""
    Write-Host "Empty database validation succeeded."
    Write-Host "Database: $DbName"
    Write-Host "Successful Flyway migrations: $migrationCount"
    Write-Host "Application log: $stdoutPath"
    Write-Host "Error log: $stderrPath"
} finally {
    if ($appProcess -and -not $appProcess.HasExited) {
        Stop-Process -Id $appProcess.Id -Force
        $appProcess.WaitForExit()
    }

    foreach ($item in $envBackup.GetEnumerator()) {
        if ($null -eq $item.Value) {
            Remove-Item "Env:$($item.Key)" -ErrorAction SilentlyContinue
        } else {
            Set-Item "Env:$($item.Key)" $item.Value
        }
    }
}
