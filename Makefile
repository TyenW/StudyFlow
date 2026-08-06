ifeq ($(OS),Windows_NT)
    CLEAN_CMD = del /f /q Cursar.class VerificarConsole.class config\*.class 2>nul || exit 0
    RUN_CMD = chcp 65001 >nul && java "-Dfile.encoding=UTF-8" Cursar
    VERIFY_CMD = chcp 65001 >nul && java "-Dfile.encoding=UTF-8" VerificarConsole
else
    CLEAN_CMD = rm -f Cursar.class VerificarConsole.class config/*.class
    RUN_CMD = java Cursar
    VERIFY_CMD = java VerificarConsole
endif

all: compile

compile:
	javac -encoding UTF-8 Cursar.java config/*.java

run: compile
	$(RUN_CMD)

verify: compile
	@if [ -f VerificarConsole.java ]; then $(VERIFY_CMD); else echo "VerificarConsole.java não encontrado. Ignorando verificação."; fi

clean:
	$(CLEAN_CMD)
