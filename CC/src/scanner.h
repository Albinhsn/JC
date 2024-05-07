

#ifndef SCANNER_H
#define SCANNER_H
#include "common.h"

#include "token.h"

struct Scanner
{
  Arena*  arena;
  String* input;
  i32     index;
  i32     line;
  u8*     filename;
};

typedef struct Scanner Scanner;

void init_scanner(Scanner* scanner, Arena* arena, String* literal, const char* filename);
void error(const char * msg);
Token*                 parse_token(Scanner* scanner);

#endif
