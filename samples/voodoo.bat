java -jar "%~dp0wrapper\wrapper.jar" %*
IF EXIST %~dp0wrapper\new.jar MOVE %~dp0wrapper\new.jar %~dp0wrapper\wrapper.jar