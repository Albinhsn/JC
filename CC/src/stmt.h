#ifndef STMT_H
#define STMT_H

#include "data_type.h"
#include "expr.h"
#include "struct.h"

enum StmtType
{
  STMT_ARRAY,
  STMT_ASSIGN,
  STMT_EXPRESSION,
  STMT_FOR,
  STMT_WHILE,
  STMT_IF,
  STMT_RETURN,
  STMT_VARIABLE,
  STMT_FUNCTION,
  STMT_STRUCT,
  STMT_EXTERN,
};
typedef enum StmtType StmtType;

typedef struct Stmt   Stmt;

struct ArrayStmt
{
  Expr*    items;
  DataType item_type;
  Token    target;
  int      item_count;
  int      size;
};
typedef struct ArrayStmt ArrayStmt;

struct AssignStmt
{
  Expr target;
  Expr value;
};
typedef struct AssignStmt AssignStmt;

struct ExpressionStmt
{
  Expr expr;
};
typedef struct ExpressionStmt ExpressionStmt;

struct ForStmt
{
  Stmt* init;
  Stmt* condition;
  Stmt* update;
  Stmt* body;
  int   body_count;
};
typedef struct ForStmt ForStmt;

struct WhileStmt
{
  Expr  condition;
  Stmt* body;
  int   body_count;
};
typedef struct WhileStmt WhileStmt;

struct IfBlock
{
  Expr  condition;
  Stmt* body;
  int   body_count;
};
typedef struct IfBlock IfBlock;

struct IfStmt
{
  IfBlock* blocks;
  Stmt*    else_body;
  int      else_body_count;
};
typedef struct IfStmt IfStmt;

struct ReturnStmt
{
  Expr* value;
};
typedef struct ReturnStmt ReturnStmt;

struct VariableStmt
{
  Expr*    value;
  DataType type;
  String   name;
};
typedef struct VariableStmt VariableStmt;

struct FunctionStmt
{
  StructField* arguments;
  Stmt*        body;
  int          argument_count;
  int          body_count;
  DataType     return_type;
  String       name;
};
typedef struct FunctionStmt FunctionStmt;

struct StructStmt
{
  String       name;
  StructField* fields;
  int          field_count;
};
typedef struct StructStmt StructStmt;

struct ExternStmt
{
  StructField* arguments;
  DataType     return_type;
  String       name;
  bool         var_args;
  int          argument_count;
};
typedef struct ExternStmt ExternStmt;

struct MacroStmt
{
  Token* arguments;
  Stmt*  content;
  String name;
  int    argument_count;
  int    content_size;
};
typedef struct MacroStmt MacroStmt;

struct Stmt
{
  union
  {
    ArrayStmt      array;
    AssignStmt     assign;
    ExpressionStmt expr;
    ForStmt        for_;
    WhileStmt      while_;
    IfStmt         if_;
    ReturnStmt     return_;
    VariableStmt   variable;
    FunctionStmt   function;
    StructStmt     struct_;
    ExternStmt     extern_;
    MacroStmt      macro;
  };
  StmtType type;
};
typedef struct Stmt Stmt;

#endif
