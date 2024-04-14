global _start
section .text
_start:
mov rbx, rcx
mov rax, 1
int 0x80