@echo off
setlocal enabledelayedexpansion

REM =====================================
REM Variables
REM =====================================
set PROJECT_DIR=%cd%
set WEB_CONTENT=%PROJECT_DIR%\WebContent
set WEB_INF=%WEB_CONTENT%\WEB-INF
set LIB=%WEB_INF%\lib

set TOMCAT_HOME=C:\apache-tomcat-10.1.28
set WEBAPPS=%TOMCAT_HOME%\webapps
set WAR_NAME=BackOffice.war

REM =====================================
REM Vérifier que WEB-INF\lib existe
REM =====================================
if not exist "%LIB%" mkdir "%LIB%"



REM =====================================
REM Créer WEB-INF\classes
REM =====================================
if not exist "%WEB_INF%\classes" mkdir "%WEB_INF%\classes"

REM =====================================
REM Compiler les classes Java
REM =====================================
set JAVA_FILES=

for /R src %%f in (*.java) do (
    set JAVA_FILES=!JAVA_FILES! %%f
)

javac -parameters -cp "%WEB_INF%\lib\*" -d "%WEB_INF%\classes" %JAVA_FILES%

if errorlevel 1 (
    echo ❌ Erreur lors de la compilation !
    exit /b 1
)

echo ✅ Classes compilées avec succès

REM =====================================
REM Copier auth.properties
REM =====================================
if exist "auth.properties" (
    copy /Y "auth.properties" "%WEB_INF%\classes\"
    echo ✅ auth.properties copié
)

REM =====================================
REM Créer le WAR
REM =====================================
if exist "%WAR_NAME%" del "%WAR_NAME%"

jar cvf "%WAR_NAME%" ^
    -C "%WEB_CONTENT%" . ^
    -C "%WEB_INF%" .

if errorlevel 1 (
    echo ❌ Erreur création WAR
    exit /b 1
)

echo ✅ WAR créé avec succès

REM =====================================
REM Déployer dans Tomcat
REM =====================================
if exist "%WEBAPPS%\%WAR_NAME%" del "%WEBAPPS%\%WAR_NAME%"

copy /Y "%WAR_NAME%" "%WEBAPPS%"

echo ✅ Application déployée

REM =====================================
REM Redémarrer Tomcat
REM =====================================
echo 🔄 Redémarrage Tomcat...

call "%TOMCAT_HOME%\bin\shutdown.bat"
timeout /t 3 >nul
call "%TOMCAT_HOME%\bin\startup.bat"

echo =====================================
echo 🚀 Déploiement terminé !
echo URL: http://localhost:8080/BackOffice
echo =====================================

pause
