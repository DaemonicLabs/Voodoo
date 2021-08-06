@echo off
setlocal
    set VOODOO_COMMAND=voodoo-dev
    java -jar ..\voodoo\build\libs\voodoo-0.7.0-local-all.jar %*
    if exist wrapper\new.jar move wrapper\new.jar wrapper\wrapper.jar
endlocal