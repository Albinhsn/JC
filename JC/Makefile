r:
	nasm -f elf64 out.asm && ld  --dynamic-linker=/lib64/ld-linux-x86-64.so.2 out.o -o main -lc -lm  && ./main && echo $?

c:
	java -jar ./out/artifacts/JC/JC.jar 

