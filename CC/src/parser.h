#ifndef PARSER_H
#define PARSER_H

#include "common.h"
#include "scanner.h"
#include "stmt.h"

enum SymbolType
{
  SYMBOL_FUNCTION,
  SYMBOL_STRUCT,
  SYMBOL_MACRO,
  SYMBOL_VARIABLE
};
typedef enum SymbolType SymbolType;

typedef struct
{
  SymbolType type;
  String     key;
} Symbol;

enum Precedence
{
  PREC_NONE,
  PREC_LITERAL,
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

struct Parser
{
  Arena*      arena;
  Scanner*    scanner;
  Symbol*     symbols;
  int         symbol_count;
  int         symbol_cap;
  Stmt**      stmts;
  int         stmt_count;
  int         stmt_cap;
  Token*      current;
  Token*      previous;
};
typedef struct Parser Parser;

typedef Expr*         (*ParseFn)(Parser* parser, Expr* expr, bool canAssign, int line);

typedef struct
{
  ParseFn    prefix;
  ParseFn    infix;
  Precedence precedence;
} ParseRule;

void init_parser(Parser* parser, Scanner* scanner, Arena* arena, Symbol* symbols, int symbol_count, int symbol_cap);
void parse(Parser* parser);
void free_stmts(Parser* parser);
void free_parser(Parser* parser);

#endif
