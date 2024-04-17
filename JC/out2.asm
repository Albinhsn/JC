global _start
extern malloc
extern free
extern printf


section .data
const0 db "Hello World!", 10,  0


section .text
_start:
call main
mov rbx, rax
mov rax, 1
int 0x80


main:
push rbp
mov rbp, rsp
lea rdi, [const0]
call printf
mov rsp, rbp
pop rbp
ret
