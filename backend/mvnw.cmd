@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)

@SET MVN_CMD=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4ttckcbc\apache-maven-3.9.11\bin\mvn.cmd

@IF EXIST "%MVN_CMD%" (
  @CMD /C "%MVN_CMD%" %*
  @GOTO :EOF
)

@echo Maven bulunamadı: %MVN_CMD%
@exit /B 1
