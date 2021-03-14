@echo off
setlocal
    set VOODOO_COMMAND=voodoo-release
    java -jar wrapper\wrapper.jar %*
endlocal