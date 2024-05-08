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
  STMT_MACRO
};
typedef enum StmtType StmtType;

typedef struct Stmt   Stmt;

struct StmtBlock
{
  Stmt** block;
  int    block_count;
  int    block_capacity;
};
typedef struct StmtBlock StmtBlock;

struct ArrayStmt
{
  Expr**   items;
  String   name;
  DataType item_type;
  int      item_count;
  int      item_size;
};
typedef struct ArrayStmt ArrayStmt;

struct AssignStmt
{
  Expr* target;
  Expr* value;
};
typedef struct AssignStmt AssignStmt;

struct ExpressionStmt
{
  Expr* expr;
};
typedef struct ExpressionStmt ExpressionStmt;

struct ForStmt
{
  Stmt*     init;
  Stmt*     condition;
  Stmt*     update;
  StmtBlock block;
};
typedef struct ForStmt ForStmt;

struct WhileStmt
{
  Expr*     condition;
  StmtBlock block;
};
typedef struct WhileStmt WhileStmt;

struct IfBlock
{
  Expr*     condition;
  StmtBlock block;
};
typedef struct IfBlock IfBlock;

struct IfStmt
{
  IfBlock** blocks;
  int       block_count;
  int       block_capacity;
  StmtBlock else_block;
  bool      else_;
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
  StmtBlock    block;
  int          argument_count;
  DataType     return_type;
  String       name;
};
typedef struct FunctionStmt FunctionStmt;

struct StructStmt
{
  String       name;
  StructField* fields;
  int          field_count;
  int          field_capacity;
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
  Token* content;
  String name;
  int    count;
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

void                debug_stmt(Stmt* stmt);

#endif
