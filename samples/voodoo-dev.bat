@echo off
setlocal
    set VOODOO_COMMAND=voodoo-dev
    java -jar "%~dp0..\voodoo\build\libs\voodoo-0.6.0-local-all.jar" %*
endlocal