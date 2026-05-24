param(
    [Parameter(Position = 0)]
    [string]$Command = "help",

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$RemainingArgs
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $RootDir "infra/local-oracle/compose.yml"
$BootstrapSql = Join-Path $RootDir "infra/local-oracle/bootstrap.sql"

$Image = if ($env:ORACLE_LOCAL_IMAGE) { $env:ORACLE_LOCAL_IMAGE } else { "container-registry.oracle.com/database/free:23.26.0.0-lite" }
$Container = if ($env:ORACLE_LOCAL_CONTAINER) { $env:ORACLE_LOCAL_CONTAINER } else { "se-issue-tracker-oracle" }
$Volume = if ($env:ORACLE_LOCAL_VOLUME) { $env:ORACLE_LOCAL_VOLUME } else { "se_issue_tracker_oracle_data" }
$Port = if ($env:ORACLE_LOCAL_PORT) { $env:ORACLE_LOCAL_PORT } else { "1521" }
$SysPassword = if ($env:ORACLE_LOCAL_SYS_PASSWORD) { $env:ORACLE_LOCAL_SYS_PASSWORD } else { "OracleLocal2026!" }
$AppUser = if ($env:ORACLE_LOCAL_APP_USER) { $env:ORACLE_LOCAL_APP_USER } else { "ITS_USER" }
$AppPassword = if ($env:ORACLE_LOCAL_APP_PASSWORD) { $env:ORACLE_LOCAL_APP_PASSWORD } else { "ItsLocalDev2026!" }
$TestUser = if ($env:ORACLE_LOCAL_TEST_USER) { $env:ORACLE_LOCAL_TEST_USER } else { "ITS_TEST_USER" }
$TestPassword = if ($env:ORACLE_LOCAL_TEST_PASSWORD) { $env:ORACLE_LOCAL_TEST_PASSWORD } else { "ItsTestLocal2026!" }
$Pdb = if ($env:ORACLE_LOCAL_PDB) { $env:ORACLE_LOCAL_PDB } else { "FREEPDB1" }
$TimeoutSeconds = if ($env:ORACLE_LOCAL_TIMEOUT) { [int]$env:ORACLE_LOCAL_TIMEOUT } else { 900 }
$SqlTimeoutSeconds = if ($env:ORACLE_LOCAL_SQL_TIMEOUT) { [int]$env:ORACLE_LOCAL_SQL_TIMEOUT } else { 30 }
$SettleSeconds = if ($env:ORACLE_LOCAL_SETTLE_SECONDS) { [int]$env:ORACLE_LOCAL_SETTLE_SECONDS } else { 60 }

$env:ORACLE_LOCAL_IMAGE = $Image
$env:ORACLE_LOCAL_CONTAINER = $Container
$env:ORACLE_LOCAL_VOLUME = $Volume
$env:ORACLE_LOCAL_PORT = $Port
$env:ORACLE_LOCAL_SYS_PASSWORD = $SysPassword

function Write-ErrorMessage {
    param([Parameter(Mandatory = $true)][string]$Message)
    [Console]::Error.WriteLine($Message)
}

function Get-JdbcUrl {
    "jdbc:oracle:thin:@//localhost:$Port/$Pdb"
}

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$ComposeArgs)
    & docker compose -f $ComposeFile @ComposeArgs
    $composeExitCode = $LASTEXITCODE
    if ($composeExitCode -ne 0) {
        Write-Error "docker compose failed with exit code $composeExitCode"
    }
}

function Require-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Error "[오류] docker CLI를 찾을 수 없습니다."
    }

    & docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[오류] Docker 데몬에 연결할 수 없습니다."
    }

    & docker compose version *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[오류] docker compose를 사용할 수 없습니다."
    }

    if (-not (Test-Path -LiteralPath $ComposeFile)) {
        Write-Error "[오류] Compose 파일을 찾을 수 없습니다: $ComposeFile"
    }
}

function Require-BootstrapSql {
    if (-not (Test-Path -LiteralPath $BootstrapSql)) {
        Write-Error "[오류] bootstrap SQL을 찾을 수 없습니다: $BootstrapSql"
    }
}

function Invoke-SystemSqlPlus {
    param([Parameter(Mandatory = $true)][string]$SqlText)
    $SqlText | & docker exec -i $Container timeout "${SqlTimeoutSeconds}s" sqlplus -s /nolog
}

function Start-OracleLocal {
    Require-Docker
    Write-Host "[시작] 로컬 Oracle 컨테이너를 기동합니다: $Container"
    Invoke-Compose up -d
    Write-Host "[정보] JDBC URL: $(Get-JdbcUrl)"
}

function Wait-OracleLocal {
    Require-Docker
    Write-Host "[대기] Oracle 준비 상태를 확인합니다. 제한 시간: ${TimeoutSeconds}초"

    $startTime = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $logReady = $false
    $sqlReady = $false

    while ($true) {
        $elapsed = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() - $startTime
        if ($elapsed -gt $TimeoutSeconds) {
            Write-Error "[오류] Oracle 준비 대기 시간이 초과되었습니다.`n로그 확인: ./scripts/oracle-local.ps1 logs"
        }

        $logs = & docker logs $Container 2>&1
        if ($logs -match "DATABASE IS READY TO USE") {
            $logReady = $true
        }

        if ($logReady) {
            $sql = @"
connect system/$SysPassword@//localhost:1521/$Pdb
set heading off feedback off pagesize 0 verify off
select 1 from dual;
exit
"@
            $output = Invoke-SystemSqlPlus -SqlText $sql 2>$null
            if ($LASTEXITCODE -eq 0 -and ($output -match "^\s*1\s*$")) {
                $sqlReady = $true
            }
        }

        if ($logReady -and $sqlReady) {
            Write-Host "[확인] Oracle 로그와 SQL 연결이 모두 준비되었습니다."
            return
        }

        Start-Sleep -Seconds 5
    }
}

function Invoke-Bootstrap {
    Require-Docker
    Require-BootstrapSql
    Write-Host "[초기화] 애플리케이션/테스트 계정을 준비합니다."
    & docker cp $BootstrapSql "$Container`:/tmp/bootstrap.sql"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[오류] bootstrap SQL 복사에 실패했습니다."
    }

    $sql = @"
connect system/$SysPassword@//localhost:1521/$Pdb
@/tmp/bootstrap.sql "$AppUser" "$AppPassword" "$TestUser" "$TestPassword"
"@
    Invoke-SystemSqlPlus -SqlText $sql
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[오류] 계정 준비에 실패했습니다."
    }

    Write-Host "[확인] 계정 준비가 완료되었습니다."
}

function Invoke-Settle {
    if ($SettleSeconds -gt 0) {
        Write-Host "[대기] Oracle background 작업 안정화를 위해 ${SettleSeconds}초 대기합니다."
        Start-Sleep -Seconds $SettleSeconds
    } else {
        Write-Host "[대기] Oracle 안정화 대기를 건너뜁니다."
    }
}

function Show-Status {
    Write-Host "[상태] 로컬 Oracle 설정"
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $dockerVersion = & docker --version
        Write-Host "  - Docker CLI: 사용 가능 ($dockerVersion)"
    } else {
        Write-Host "  - Docker CLI: 없음"
    }

    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $composeVersion = & docker compose version --short 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  - Docker Compose: 사용 가능 ($composeVersion)"
        } else {
            Write-Host "  - Docker Compose: 없음"
        }
    } else {
        Write-Host "  - Docker Compose: 없음"
    }

    Write-Host "  - 컨테이너: $Container"
    Write-Host "  - 이미지: $Image"
    Write-Host "  - 볼륨: $Volume"
    Write-Host "  - 포트: localhost:$Port -> 1521"
    Write-Host "  - JDBC URL: $(Get-JdbcUrl)"
    Write-Host "  - 앱 사용자: $AppUser"
    Write-Host "  - 테스트 사용자: $TestUser"

    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $containerNames = & docker ps -a --format "{{.Names}}" 2>$null
        if ($LASTEXITCODE -eq 0 -and ($containerNames -contains $Container)) {
            $containerStatus = & docker inspect -f "{{.State.Status}}" $Container
            Write-Host "  - 컨테이너 상태: $containerStatus"
        } else {
            Write-Host "  - 컨테이너 상태: 생성되지 않음"
        }
    } else {
        Write-Host "  - 컨테이너 상태: 확인 불가"
    }

    Write-Host ""
    Write-Host "다음 명령:"
    Write-Host "  .\gradlew.bat oracleLocalTest --console=plain"
    Write-Host "  .\gradlew.bat oracleLocalInitializeDatabase --console=plain"
    Write-Host "  .\gradlew.bat oracleLocalResetFixedSeed --console=plain"
    Write-Host "  ./scripts/oracle-local.ps1 start"
    Write-Host "  ./scripts/oracle-local.ps1 wait"
    Write-Host "  ./scripts/oracle-local.ps1 bootstrap"
    Write-Host "  ./scripts/oracle-local.ps1 settle"
    Write-Host "  ./scripts/oracle-local.ps1 stop"
    Write-Host "  ./scripts/oracle-local.ps1 reset"
    Write-Host ""
    Write-Host "로그 확인:"
    Write-Host "  ./scripts/oracle-local.ps1 logs"
}

function Reset-OracleLocal {
    Require-Docker
    Require-BootstrapSql
    Write-Host "[초기화] 컨테이너와 로컬 Oracle 볼륨을 삭제합니다: $Volume"
    Invoke-Compose down -v
    Write-Host "[시작] 새 로컬 Oracle 컨테이너를 기동합니다: $Container"
    Invoke-Compose up -d
    Wait-OracleLocal
    Invoke-Bootstrap
    Invoke-Settle
    Write-Host "[확인] 로컬 Oracle 재생성이 완료되었습니다."
}

function Stop-OracleLocal {
    Require-Docker
    Write-Host "[중지] 로컬 Oracle 컨테이너를 중지합니다."
    Invoke-Compose down
}

function Show-Logs {
    Require-Docker
    Invoke-Compose logs -f oracle-local
}

function Show-Usage {
    Write-Host @"
사용법: ./scripts/oracle-local.ps1 <command>

명령:
  start      컨테이너 기동
  wait       Oracle readiness 확인
  bootstrap  실행 중인 컨테이너에 앱/테스트 계정 준비
  settle     bootstrap 후 Oracle background 작업 안정화 대기
  status     현재 설정과 다음 명령 출력
  reset      컨테이너와 볼륨 삭제 후 계정 bootstrap
  stop       컨테이너 중지
  logs       Oracle 컨테이너 로그 팔로우
  help       도움말 출력
"@
}

if ($RemainingArgs.Count -gt 0) {
    Write-ErrorMessage "[오류] 예상하지 못한 추가 인자: $($RemainingArgs -join ' ')"
    Show-Usage
    exit 1
}

switch ($Command) {
    "start" { Start-OracleLocal }
    "wait" { Wait-OracleLocal }
    "bootstrap" { Invoke-Bootstrap }
    "settle" { Invoke-Settle }
    "status" { Show-Status }
    "reset" { Reset-OracleLocal }
    "stop" { Stop-OracleLocal }
    "logs" { Show-Logs }
    { $_ -in @("help", "--help", "-h") } { Show-Usage }
    default {
        Write-ErrorMessage "[오류] 알 수 없는 명령: $Command"
        Show-Usage
        exit 1
    }
}
