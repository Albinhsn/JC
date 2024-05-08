#ifndef EXPR_H
#define EXPR_H

#include "common.h"
#include "data_type.h"
#include "token.h"

enum ExprType
{
  EXPR_BINARY,
  EXPR_CALL,
  EXPR_CAST,
  EXPR_COMPARISON,
  EXPR_DOT,
  EXPR_GROUPED,
  EXPR_INDEX,
  EXPR_LITERAL,
  EXPR_LOGICAL,
  EXPR_POSTFIX,
  EXPR_UNARY,
  EXPR_VARIABLE
};
typedef enum ExprType ExprType;

typedef struct Expr   Expr;

struct BinaryExpr
{
  Expr*  left;
  Expr*  right;
  Token* op;
};
typedef struct BinaryExpr BinaryExpr;

struct CallExpr
{
  String name;
  Expr** args;
  int    arg_count;
  int    arg_capacity;
};
typedef struct CallExpr CallExpr;

struct CastExpr
{
  Expr*    target;
  DataType type;
};
typedef struct CastExpr CastExpr;

struct ComparisonExpr
{
  Expr*  left;
  Expr*  right;
  Token* op;
};
typedef struct ComparisonExpr ComparisonExpr;

struct DotExpr
{
  String member;
  Expr*  target;
};
typedef struct DotExpr DotExpr;

struct GroupedExpr
{
  Expr* grouped;
};
typedef struct GroupedExpr GroupedExpr;

struct IndexExpr
{
  Expr* index;
  Expr* target;
};
typedef struct IndexExpr IndexExpr;

struct LiteralExpr
{
  Token* literal;
};
typedef struct LiteralExpr LiteralExpr;

struct LogicalExpr
{
  Expr*  left;
  Expr*  right;
  Token* op;
};
typedef struct LogicalExpr LogicalExpr;

struct PostfixExpr
{
  Token* op;
  Expr*  target;
};
typedef struct PostfixExpr PostfixExpr;

struct UnaryExpr
{
  Token* op;
  Expr*  target;
};
typedef struct UnaryExpr UnaryExpr;

struct VariableExpr
{
  String variable;
};
typedef struct VariableExpr VariableExpr;

struct Expr
{
  ExprType type;
  union
  {
    BinaryExpr     binary;
    CallExpr       call;
    CastExpr       cast;
    ComparisonExpr comparison;
    DotExpr        dot;
    GroupedExpr    grouped;
    IndexExpr      index;
    LiteralExpr    literal;
    LogicalExpr    logical;
    PostfixExpr    postfix;
    UnaryExpr      unary;
    VariableExpr   variable;
  };
  int    line;
  String file;
};

typedef struct Expr Expr;

void                debug_expr(Expr* expr);

#endif
