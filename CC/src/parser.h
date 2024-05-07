#ifndef PARSER_H
#define PARSER_H

#include "common.h"
#include "scanner.h"
#include "stmt.h"

enum Precedence
{
  NONE,
  ASSIGNMENT,
  OR,
  AND,
  EQUALITY,
  COMPARISON,
  BITWISE,
  TERM,
  FACTOR,
  UNARY,
  CALL,
  PRIMARY
};
typedef enum Precedence Precedence;

struct TokenQueue
{
  Token* queue;
  int    count;
  int    capacity;
};
typedef struct TokenQueue TokenQueue;

void                      push_token(TokenQueue* queue, Token* token);
void                      pop_token(TokenQueue* queue, Token* token);

struct Parser
{
  Arena*      arena;
  TokenQueue* queue;
  Scanner*    scanner;
  Stmt*       stmts;
  int         stmt_count;
  Token*      current;
  Token*      previous;
};
typedef struct Parser Parser;

void                  init_parser(Parser* parser, Scanner* scanner, Arena* arena);
void                  parse(Parser* parser);

#endif
