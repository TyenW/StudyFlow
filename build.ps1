param (
    [string]$Action = "run"
)

function Get-JavaCommand {
    param ([string]$Command)
    
    # 1. Check Android Studio JBR (Java 17/21)
    $asPath = "C:\Program Files\Android\Android Studio\jbr\bin\$Command.exe"
    if (Test-Path $asPath) { return $asPath }
    
    # 2. Check VSCode / Antigravity RedHat Java Extension (Java 21)
    $extPaths = Get-ChildItem "$env:USERPROFILE\.antigravity-ide\extensions\", "$env:USERPROFILE\.vscode\extensions\" -Filter "$Command.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
    foreach ($p in $extPaths) {
        if (Test-Path $p) { return $p }
    }

    # 3. Check JAVA_HOME if set
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\$Command.exe")) {
        return "$env:JAVA_HOME\bin\$Command.exe"
    }

    # 4. Fallback to system PATH
    return $Command
}

$JavacExec = Get-JavaCommand "javac"
$JavaExec  = Get-JavaCommand "java"

if ($Action -eq "clean") {
    Write-Host "Limpando arquivos .class..."
    Remove-Item -Path "*.class", "config/*.class" -ErrorAction SilentlyContinue
    Write-Host "Limpeza concluida!"
}
elseif ($Action -eq "compile") {
    Write-Host "Compilando StudyFlow..."
    & $JavacExec -encoding UTF-8 Cursar.java config/*.java
    if ($?) {
        Write-Host "Compilacao concluida com sucesso!"
    }
}
else {
    Write-Host "Compilando StudyFlow..."
    & $JavacExec -encoding UTF-8 Cursar.java config/*.java
    if ($?) {
        chcp 65001 | Out-Null
        & $JavaExec "-Dfile.encoding=UTF-8" Cursar
    }
}
