@echo off
REM Minimal Gradle wrapper stub. Run `gradle wrapper` after installing Gradle
REM to generate the full wrapper jar and scripts.

if not defined JAVA_HOME (
    echo JAVA_HOME is not set. Please set it to a JDK 21 installation.
    exit /b 1
)

set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if not exist "%JAVA_EXE%" (
    echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
    exit /b 1
)

set APP_HOME=%~dp0
set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
    echo ERROR: %WRAPPER_JAR% is missing. Run 'gradle wrapper' to generate it.
    exit /b 1
)

"%JAVA_EXE%" -Dorg.gradle.appname=gradlew -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
