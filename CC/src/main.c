
#include "common.h"
#include "files.h"
#include "parser.h"
#include "scanner.h"
#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv)
{
  if (argc == 1)
  {
    printf("Need filename!\n");
    return 1;
  }
  String file = {};
  i32    size = 4096 * 4096;
  Arena  arena;
  arena.ptr     = 0;
  arena.memory  = (u64)malloc(size);
  arena.maxSize = size;
  sta_read_file(&arena, &file, argv[1]);
  Scanner scanner;
  init_scanner(&scanner, &arena, &file, argv[1]);
  Parser parser;

  Arena  arena2;
  arena2.ptr     = 0;
  arena2.memory  = (u64)malloc(size);
  arena2.maxSize = size;

  init_parser(&parser, &scanner, &arena2);
  parse(&parser);

  free_stmts(&parser);

  return 0;
}
