#ifndef PARSER_H
#define PARSER_H

#include "common.h"
#include "scanner.h"
#include "stmt.h"

enum Precedence
{
  PREC_NONE,
  PREC_ASSIGNMENT,
  PREC_OR,
  PREC_AND,
  PREC_EQUALITY,
  PREC_COMPARISON,
  PREC_BITWISE,
  PREC_TERM,
  PREC_FACTOR,
  PREC_UNARY,
  PREC_CALL,
  PREC_PRIMARY
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
  Stmt**       stmts;
  int         stmt_count;
  int stmt_cap;
  Token*      current;
  Token*      previous;
};
typedef struct Parser Parser;

typedef Expr * (*ParseFn)(Parser* parser, Expr* expr, bool canAssign, int line);

typedef struct
{
  ParseFn    prefix;
  ParseFn    infix;
  Precedence precedence;
} ParseRule;

void init_parser(Parser* parser, Scanner* scanner, Arena* arena);
void parse(Parser* parser);
void                free_stmts(Parser* parser);

#endif
