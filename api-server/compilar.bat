@echo off
setlocal
if exist out rmdir /s /q out
mkdir out
for /r src %%f in (*.java) do @echo %%f >> sources.txt
javac -encoding UTF-8 -d out @sources.txt
del sources.txt
if %errorlevel% neq 0 exit /b %errorlevel%
echo Servidor API compilado em out\
