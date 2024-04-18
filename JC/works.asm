global _start
extern printf
extern malloc
extern free
extern printf


section .data


section .text
_start:
call main
mov rbx, rax
mov rax, 1
int 0x80


rec_struct:
push rbp
mov rbp, rsp
lea rax, [rbp + 16]
lea rax, [rax + 8]
mov rax, [rax]
mov rsp, rbp
pop rbp
ret


main:
push rbp
mov rbp, rsp
sub rsp, 32
mov [rbp -24], rax
lea rax, [rbp -24]
lea rax, [rax + 8]
push rax
mov rax, 5
mov rcx, rax
pop rax
mov [rax], rcx
lea rax, [rbp -24]
sub rsp, 24
mov rcx, [rax]
mov [rsp], rcx
mov rcx, [rax + 8]
mov [rsp + 8], rcx
mov rcx, [rax + 16]
mov [rsp + 16], rcx
call rec_struct
add rsp, 24
mov rsp, rbp
pop rbp
ret
