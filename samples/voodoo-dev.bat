@echo off
setlocal
    set VOODOO_COMMAND=voodoo-dev
    java -jar "%~dp0..\voodoo\build\libs\voodoo-0.6.0-local-all.jar" %*
    IF EXIST %~dp0wrapper\new.jar MOVE %~dp0wrapper\new.jar %~dp0wrapper\wrapper.jar
endlocal