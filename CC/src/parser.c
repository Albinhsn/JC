#include "parser.h"
#include "common.h"
#include "data_type.h"
#include "expr.h"
#include "files.h"
#include "scanner.h"
#include "stmt.h"
#include "struct.h"
#include "token.h"
#include <stdlib.h>
#include <string.h>

static Stmt*     parse_statement(Parser* parser);
static Expr*     parse_dereference(Parser* parser, Expr* expr, bool canAssign, int line);
static Expr*     parse_dot_expression(Parser* parser, Expr* expr, bool canAssign, int line);
static Expr*     parse_comparison(Parser* parser, Expr* expr, bool canAssign, int line);
static Expr*     parse_postfix(Parser* parser, Expr* expr, bool canAssign, int line);
static Expr*     parse_variable(Parser* parser, Expr* expr, bool canAssign, int line);
static Expr*     parse_unary(Parser* parser, Expr* expr, bool canAssign, int line);
static Expr*     parse_index(Parser* parser, Expr* expr, bool canAssign, int line);
static Expr*     parse_binary(Parser* parser, Expr* expr, bool canAssign, int line);
static Expr*     parse_logical(Parser* parser, Expr* expr, bool canAssign, int line);
static Expr*     parse_literal(Parser* parser, Expr* expr, bool canAssign, int line);
static Expr*     parse_grouped_expression(Parser* parser, Expr* expr, bool canAssign, int line);

static ParseRule rules[] = {

    [TOKEN_VOID]            = {                       0,                    0,       PREC_NONE},
    [TOKEN_STRING]          = {                       0,                    0,       PREC_NONE},
    [TOKEN_STRING_LITERAL]  = {           parse_literal,                    0,       PREC_NONE},
    [TOKEN_INT_LITERAL]     = {           parse_literal,                    0,       PREC_NONE},
    [TOKEN_FLOAT_LITERAL]   = {           parse_literal,                    0,       PREC_NONE},
    [TOKEN_BYTE]            = {                       0,                    0,       PREC_NONE},
    [TOKEN_INT]             = {                       0,                    0,       PREC_NONE},
    [TOKEN_FLOAT]           = {                       0,                    0,       PREC_NONE},
    [TOKEN_STRUCT]          = {                       0,                    0,       PREC_NONE},
    [TOKEN_RETURN]          = {                       0,                    0,       PREC_NONE},
    [TOKEN_DOUBLE]          = {                       0,                    0,       PREC_NONE},
    [TOKEN_LONG]            = {                       0,                    0,       PREC_NONE},
    [TOKEN_SHORT]           = {                       0,                    0,       PREC_NONE},
    [TOKEN_LEFT_PAREN]      = {parse_grouped_expression,                    0,       PREC_CALL},
    [TOKEN_RIGHT_PAREN]     = {                       0,                    0,       PREC_NONE},
    [TOKEN_LEFT_BRACE]      = {                       0,                    0,       PREC_NONE},
    [TOKEN_RIGHT_BRACE]     = {                       0,                    0,       PREC_NONE},
    [TOKEN_LEFT_BRACKET]    = {                       0,          parse_index,       PREC_CALL},
    [TOKEN_RIGHT_BRACKET]   = {                       0,                    0,       PREC_NONE},
    [TOKEN_INCLUDE]         = {                       0,                    0,       PREC_NONE},
    [TOKEN_EXTERN]          = {                       0,                    0,       PREC_NONE},
    [TOKEN_DEFINE]          = {                       0,                    0,       PREC_NONE},
    [TOKEN_ELLIPSIS]        = {                       0,                    0,       PREC_NONE},
    [TOKEN_MINUS]           = {             parse_unary,         parse_binary,       PREC_TERM},
    [TOKEN_PLUS]            = {                       0,         parse_binary,       PREC_TERM},
    [TOKEN_SLASH]           = {                       0,         parse_binary,     PREC_FACTOR},
    [TOKEN_STAR]            = {       parse_dereference,         parse_binary,     PREC_FACTOR},
    [TOKEN_MOD]             = {                       0,         parse_binary,       PREC_TERM},
    [TOKEN_SHIFT_RIGHT]     = {                       0,         parse_binary,    PREC_BITWISE},
    [TOKEN_SHIFT_LEFT]      = {                       0,         parse_binary,    PREC_BITWISE},
    [TOKEN_SEMICOLON]       = {                       0,                    0,       PREC_NONE},
    [TOKEN_COMMA]           = {                       0,                    0,       PREC_NONE},
    [TOKEN_DOT]             = {                       0, parse_dot_expression,       PREC_CALL},
    [TOKEN_BANG]            = {             parse_unary,                    0,       PREC_NONE},
    [TOKEN_BANG_EQUAL]      = {                       0,     parse_comparison, PREC_COMPARISON},
    [TOKEN_EQUAL]           = {                       0,                    0,       PREC_NONE},
    [TOKEN_EQUAL_EQUAL]     = {                       0,     parse_comparison, PREC_COMPARISON},
    [TOKEN_GREATER]         = {                       0,     parse_comparison, PREC_COMPARISON},
    [TOKEN_GREATER_EQUAL]   = {                       0,     parse_comparison, PREC_COMPARISON},
    [TOKEN_LESS]            = {                       0,     parse_comparison, PREC_COMPARISON},
    [TOKEN_LESS_EQUAL]      = {                       0,     parse_comparison, PREC_COMPARISON},
    [TOKEN_INCREMENT]       = {             parse_unary,        parse_postfix,       PREC_TERM},
    [TOKEN_DECREMENT]       = {             parse_unary,        parse_postfix,       PREC_TERM},
    [TOKEN_IF]              = {                       0,                    0,       PREC_NONE},
    [TOKEN_ELSE]            = {                       0,                    0,       PREC_NONE},
    [TOKEN_FOR]             = {                       0,                    0,       PREC_NONE},
    [TOKEN_WHILE]           = {                       0,                    0,       PREC_NONE},
    [TOKEN_AUGMENTED_MINUS] = {                       0,                    0,       PREC_NONE},
    [TOKEN_AUGMENTED_PLUS]  = {                       0,                    0,       PREC_NONE},
    [TOKEN_AUGMENTED_SLASH] = {                       0,                    0,       PREC_NONE},
    [TOKEN_AUGMENTED_STAR]  = {                       0,                    0,       PREC_NONE},
    [TOKEN_AUGMENTED_AND]   = {                       0,                    0,       PREC_NONE},
    [TOKEN_AUGMENTED_OR]    = {                       0,                    0,       PREC_NONE},
    [TOKEN_AUGMENTED_XOR]   = {                       0,                    0,       PREC_NONE},
    [TOKEN_IDENTIFIER]      = {          parse_variable,                    0,       PREC_NONE},
    [TOKEN_AND_LOGICAL]     = {                       0,        parse_logical,        PREC_AND},
    [TOKEN_OR_LOGICAL]      = {                       0,        parse_logical,         PREC_OR},
    [TOKEN_AND_BIT]         = {             parse_unary,         parse_binary,    PREC_BITWISE},
    [TOKEN_OR_BIT]          = {             parse_unary,         parse_binary,    PREC_BITWISE},
    [TOKEN_XOR]             = {                       0,         parse_binary,    PREC_BITWISE},
    [TOKEN_EOF]             = {                       0,                    0,       PREC_NONE}
};

void init_parser(Parser* parser, Scanner* scanner, Arena* arena, Symbol* symbols, int symbol_count, int symbol_cap)
{
  parser->scanner    = scanner;
  parser->arena      = arena;
  parser->stmt_count = 0;
  parser->stmt_cap   = 8;
  parser->stmts      = malloc(sizeof(Stmt) * parser->stmt_cap);

  if (symbols == 0)
  {
    parser->symbol_count = 0;
    parser->symbol_cap   = 8;
    parser->symbols      = malloc(sizeof(Symbol) * parser->symbol_cap);
  }
  else
  {
    parser->symbol_count = symbol_count;
    parser->symbol_cap   = symbol_cap;
    parser->symbols      = symbols;
  }
}

static void add_symbol(Parser* parser, String name, SymbolType type)
{
  if (parser->symbol_count >= parser->symbol_cap)
  {
    parser->symbol_cap *= 2;
    parser->symbols = (Symbol*)realloc(parser->symbols, sizeof(Symbol) * parser->symbol_cap);
  }
  parser->symbols[parser->symbol_count].type  = type;
  parser->symbols[parser->symbol_count++].key = name;
}

static bool is_declared_symbol(Parser* parser, String* symbol)
{
  for (int i = 0; i < parser->symbol_count; i++)
  {
    if (sta_strcmp(&parser->symbols[i].key, symbol))
    {
      return true;
    }
  }
  return false;
}

static bool is_declared_struct(Parser* parser, String name)
{
  Symbol* symbols = parser->symbols;
  for (int i = 0; i < parser->symbol_count; i++)
  {
    if (symbols[i].type == SYMBOL_STRUCT && sta_strcmp(&symbols[i].key, &name))
    {
      return true;
    }
  }
  return false;
}

static void advance(Parser* parser)
{
  parser->previous = parser->current;
  parser->current  = parse_token(parser->scanner);
  printf("New current %.*s\n", (i32)parser->current->literal.len, parser->current->literal.buffer);
}
static bool match_type(Parser* parser, TokenType type)
{
  if (parser->current->type != type)
  {
    return false;
  }
  advance(parser);
  return true;
}
static bool is_variable_type(TokenType current)
{
  return current == TOKEN_INT || current == TOKEN_FLOAT || current == TOKEN_BYTE || current == TOKEN_STRING || current == TOKEN_LONG || current == TOKEN_SHORT || current == TOKEN_DOUBLE ||
         current == TOKEN_VOID;
}
static DataType parse_pointer_type(Parser* parser, DataType type)
{
  while (match_type(parser, TOKEN_STAR))
  {
    type = get_pointer_from_type(type);
  }
  return type;
}

static DataType parse_type(Parser* parser)
{
  DataType type = get_type_from_token(parser->current);
  if (type.type == DATATYPE_STRUCT && !is_declared_struct(parser, type.name))
  {
    error("Can't declare struct of unknown type!");
  }
  advance(parser);
  return parse_pointer_type(parser, type);
}

static void parse_struct_field(Parser* parser)
{
  StructField* field = sta_arena_push_struct(parser->arena, StructField);
  DataType     type  = parse_type(parser);
  if (!match_type(parser, TOKEN_IDENTIFIER))
  {
    error("Expected identifier for struct field!");
  }
  field->type = type;
  field->name = parser->previous->literal;
}

static Expr* parse_postfix(Parser* parser, Expr* expression, bool canAssign, int line)
{
  Expr* expr           = sta_arena_push_struct(parser->arena, Expr);
  expr->type           = EXPR_POSTFIX;
  expr->postfix.target = expression;
  expr->postfix.op     = parser->previous;
  return expr;
}

static Stmt* parse_struct_declaration(Parser* parser)
{
  if (!match_type(parser, TOKEN_IDENTIFIER))
  {
    error("Expected identifier after struct!");
  }
  Token* name = parser->previous;

  if (is_declared_struct(parser, name->literal))
  {
    error("Trying to redeclare struct!");
  }

  if (!match_type(parser, TOKEN_LEFT_BRACE))
  {
    error("Need '{' after struct name");
  }

  Stmt* stmt                = sta_arena_push_struct(parser->arena, Stmt);
  stmt->type                = STMT_STRUCT;
  stmt->struct_.name        = name->literal;
  stmt->struct_.field_count = 0;
  stmt->struct_.fields      = (StructField*)(parser->arena->memory + parser->arena->ptr);

  add_symbol(parser, name->literal, SYMBOL_STRUCT);

  while (!match_type(parser, TOKEN_RIGHT_BRACE))
  {
    parse_struct_field(parser);
    if (!match_type(parser, TOKEN_SEMICOLON))
    {
      error("Expect ';' after struct field");
    }
    stmt->struct_.field_count++;
  }

  return stmt;
}
static void consume(Parser* parser, TokenType type, const char* message)
{
  if (!match_type(parser, type))
  {
    error(message);
  }
}

static Expr* parse_expression(Parser* parser, Expr* expr, Precedence precedence)
{
  advance(parser);

  ParseRule prefix = rules[parser->previous->type];
  if (prefix.prefix == 0)
  {
    error("Expected expression!");
  }

  bool canAssign    = precedence <= PREC_ASSIGNMENT;

  expr              = prefix.prefix(parser, expr, canAssign, parser->previous->line);
  ParseRule current = rules[parser->current->type];

  printf("Parsing %s %d\n", parser->scanner->filename, parser->scanner->line);
  while (precedence <= current.precedence)
  {
    advance(parser);
    expr    = current.infix(parser, expr, canAssign, parser->previous->line);
    current = rules[parser->current->type];
  }

  if (canAssign && match_type(parser, TOKEN_EQUAL))
  {
    error("Can't assign to this");
  }

  return expr;
}

static Expr* parse_literal(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Expr* literal            = sta_arena_push_struct(parser->arena, Expr);
  literal->type            = EXPR_LITERAL;
  literal->literal.literal = parser->previous;

  return literal;
}
static Expr* parse_grouped_expression(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Expr* out;
  if (is_variable_type(parser->current->type) || is_declared_struct(parser, parser->current->literal))
  {
    out            = sta_arena_push_struct(parser->arena, Expr);
    out->type      = EXPR_CAST;
    out->cast.type = parse_type(parser);
    consume(parser, TOKEN_RIGHT_PAREN, "Expected ')' after grouped expression");
    out->cast.target = parse_expression(parser, 0, PREC_ASSIGNMENT);
  }
  else
  {
    out = parse_expression(parser, 0, PREC_ASSIGNMENT);
    consume(parser, TOKEN_RIGHT_PAREN, "Expected ')' after grouped expression");
  }
  return out;
}
static Expr* parse_dot_expression(Parser* parser, Expr* expr, bool canAssign, int line)
{
  consume(parser, TOKEN_IDENTIFIER, "Expected identifier after dot");
  Expr* dot_expr       = sta_arena_push_struct(parser->arena, Expr);
  dot_expr->type       = EXPR_DOT;
  dot_expr->dot.member = parser->previous->literal;
  dot_expr->dot.target = expr;
  debug_expr(dot_expr);

  return dot_expr;
}
static Expr* parse_right_side_of_binary(Parser* parser, Precedence precedence)
{
  return parse_expression(parser, 0, precedence + 1);
}
static Expr* parse_comparison(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Expr* comparison_expr             = sta_arena_push_struct(parser->arena, Expr);
  comparison_expr->comparison.left  = expr;
  comparison_expr->comparison.op    = parser->previous;
  comparison_expr->type             = EXPR_COMPARISON;
  comparison_expr->comparison.right = parse_right_side_of_binary(parser, rules[parser->previous->type].precedence);

  return comparison_expr;
}
static Expr* parse_binary(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Expr* binary_expr         = sta_arena_push_struct(parser->arena, Expr);
  binary_expr->type         = EXPR_BINARY;
  binary_expr->binary.left  = expr;
  binary_expr->binary.op    = parser->previous;
  binary_expr->binary.right = parse_right_side_of_binary(parser, rules[parser->previous->type].precedence);
  debug_expr(binary_expr);
  return binary_expr;
}
static Expr* parse_dereference(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Expr* unary_expr         = sta_arena_push_struct(parser->arena, Expr);
  unary_expr->type         = EXPR_UNARY;
  unary_expr->unary.op     = parser->previous;
  unary_expr->unary.target = parse_expression(parser, 0, canAssign ? PREC_ASSIGNMENT : PREC_OR);

  return unary_expr;
}
static Expr* parse_unary(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Expr* unary_expr         = sta_arena_push_struct(parser->arena, Expr);
  unary_expr->type         = EXPR_UNARY;
  unary_expr->unary.op     = parser->previous;
  unary_expr->unary.target = parse_expression(parser, 0, PREC_UNARY);
  return unary_expr;
}
static Expr* parse_call(Parser* parser, Token* variable)
{
  Expr* call_expr              = sta_arena_push_struct(parser->arena, Expr);
  call_expr->type              = EXPR_CALL;
  call_expr->call.name         = variable->literal;
  call_expr->call.arg_count    = 0;
  call_expr->call.args         = (Expr**)malloc(sizeof(Expr*));
  call_expr->call.arg_capacity = 1;

  if (parser->current->type != TOKEN_RIGHT_PAREN)
  {
    do
    {
      if (call_expr->call.arg_capacity <= call_expr->call.arg_count)
      {
        call_expr->call.arg_capacity *= 2;
        call_expr->call.args = (Expr**)realloc(call_expr->call.args, sizeof(Expr*) * call_expr->call.arg_capacity);
      }
      call_expr->call.args[call_expr->call.arg_count] = parse_expression(parser, 0, PREC_ASSIGNMENT);
      call_expr->call.arg_count++;
    } while (match_type(parser, TOKEN_COMMA));
  }
  consume(parser, TOKEN_RIGHT_PAREN, "Expect ')' after call args");

  return call_expr;
}
static Expr* parse_variable(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Token* variable = parser->previous;
  if (match_type(parser, TOKEN_LEFT_PAREN))
  {
    return parse_call(parser, variable);
  }

  Expr* var_expr              = sta_arena_push_struct(parser->arena, Expr);
  var_expr->type              = EXPR_VARIABLE;
  var_expr->variable.variable = variable->literal;

  return var_expr;
}
static Expr* parse_logical(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Expr* logical          = sta_arena_push_struct(parser->arena, Expr);
  logical->type          = EXPR_LOGICAL;
  logical->logical.op    = parser->previous;
  logical->logical.left  = expr;
  logical->logical.right = parse_expression(parser, 0, PREC_AND);

  return logical;
}
static Expr* parse_index(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Expr* index         = sta_arena_push_struct(parser->arena, Expr);
  index->type         = EXPR_INDEX;
  index->index.target = expr;
  index->index.index  = parse_expression(parser, 0, PREC_ASSIGNMENT);
  consume(parser, TOKEN_RIGHT_BRACKET, "Expect ']' after index");
  return index;
}

static bool match_augmented(Parser* parser)
{
  return match_type(parser, TOKEN_AUGMENTED_XOR) || match_type(parser, TOKEN_AUGMENTED_AND) || match_type(parser, TOKEN_AUGMENTED_OR) || match_type(parser, TOKEN_AUGMENTED_MINUS) ||
         match_type(parser, TOKEN_AUGMENTED_STAR) || match_type(parser, TOKEN_AUGMENTED_SLASH) || match_type(parser, TOKEN_AUGMENTED_PLUS);
}
static Stmt* parse_expression_statement(Parser* parser)
{
  Stmt* stmt = sta_arena_push_struct(parser->arena, Stmt);
  Expr* expr = parse_expression(parser, 0, PREC_OR);

  if (match_type(parser, TOKEN_EQUAL))
  {
    Expr* equals        = parse_expression(parser, 0, PREC_ASSIGNMENT);
    stmt->type          = STMT_ASSIGN;
    stmt->assign.target = expr;
    stmt->assign.value  = equals;
    return stmt;
  }
  else if (match_augmented(parser))
  {
    Expr* binary         = sta_arena_push_struct(parser->arena, Expr);
    binary->type         = EXPR_BINARY;
    binary->binary.op    = parser->previous;
    binary->binary.left  = expr;
    binary->binary.right = parse_expression(parser, 0, PREC_ASSIGNMENT);
    stmt->type           = STMT_ASSIGN;
    stmt->assign.target  = expr;
    stmt->assign.value   = binary;
    return stmt;
  }

  stmt->expr.expr = expr;
  stmt->type      = STMT_EXPRESSION;
  return stmt;
}
static Stmt* parse_return_statement(Parser* parser)
{
  advance(parser);

  Stmt* out          = sta_arena_push_struct(parser->arena, Stmt);
  out->type          = STMT_RETURN;
  out->return_.value = 0;
  if (match_type(parser, TOKEN_SEMICOLON))
  {
    return out;
  }
  out->return_.value = parse_expression(parser, 0, PREC_ASSIGNMENT);
  return out;
}
static void parse_body(Parser* parser, StmtBlock* block)
{
  block->block_count    = 0;
  block->block_capacity = 4;
  block->block          = (Stmt**)malloc(sizeof(Stmt*) * block->block_capacity);
  while (!match_type(parser, TOKEN_RIGHT_BRACE))
  {
    if (!match_type(parser, TOKEN_SEMICOLON))
    {
      if (block->block_count >= block->block_capacity)
      {
        block->block_capacity *= 2;
        block->block = (Stmt**)realloc(block->block, block->block_capacity * sizeof(Stmt*));
      }
      block->block[block->block_count++] = parse_statement(parser);
    }
  }
}
static Stmt* parse_for_statement(Parser* parser)
{
  advance(parser);
  Stmt* out = sta_arena_push_struct(parser->arena, Stmt);
  out->type = STMT_FOR;
  consume(parser, TOKEN_LEFT_PAREN, "Expect ( after for");

  out->for_.init = parse_statement(parser);
  consume(parser, TOKEN_SEMICOLON, "Expect ';' after condition stmt");
  out->for_.condition = parse_expression_statement(parser);
  consume(parser, TOKEN_SEMICOLON, "Expect ';' after condition stmt");

  out->for_.update = parse_expression_statement(parser);
  debug_stmt(out->for_.update);
  consume(parser, TOKEN_RIGHT_PAREN, "Expect ')' after update");
  consume(parser, TOKEN_LEFT_BRACE, "Expect '{' after for loop (init, condition, update)");

  parse_body(parser, &out->for_.block);

  return out;
}
static Stmt* parse_while_statement(Parser* parser)
{
  Stmt* out = sta_arena_push_struct(parser->arena, Stmt);
  out->type = STMT_WHILE;
  advance(parser);

  consume(parser, TOKEN_LEFT_PAREN, "expect '(' after while");
  out->while_.condition = parse_expression(parser, 0, PREC_ASSIGNMENT);
  consume(parser, TOKEN_RIGHT_PAREN, "expected ')' after while");
  consume(parser, TOKEN_LEFT_BRACE, "Expected '{' after while condition");

  parse_body(parser, &out->while_.block);

  return out;
}
static int parse_arguments(Parser* parser)
{
  int count = 0;
  if (parser->current->type != TOKEN_RIGHT_PAREN)
  {
    do
    {
      parse_struct_field(parser);
      count++;
    } while (match_type(parser, TOKEN_COMMA));
  }
  consume(parser, TOKEN_RIGHT_PAREN, "Expected right paren after arguments");
  return count;
}
static Stmt* parse_array(Parser* parser, String name, DataType type, int line)
{
  consume(parser, TOKEN_INT_LITERAL, "Expected array size?");

  Stmt* stmt                                   = sta_arena_push_struct(parser->arena, Stmt);
  stmt->type                                   = STMT_ARRAY;
  stmt->array.name                             = name;

  char* item_size_str                          = malloc(parser->previous->literal.len + 1);
  item_size_str[parser->previous->literal.len] = 0;
  strncpy(item_size_str, parser->previous->literal.buffer, parser->previous->literal.len);
  stmt->array.item_size = atoi(item_size_str);

  free(item_size_str);
  consume(parser, TOKEN_RIGHT_BRACKET, "expect ] after array size");

  stmt->array.item_count = 0;
  stmt->array.items      = sta_arena_push_array(parser->arena, Expr*, stmt->array.item_size);

  if (match_type(parser, TOKEN_EQUAL))
  {
    consume(parser, TOKEN_LEFT_BRACKET, "Expected [ after equals in array stmt");
    if (parser->current->type != TOKEN_RIGHT_BRACKET)
    {
      do
      {
        stmt->array.items[stmt->array.item_count++] = parse_expression(parser, 0, PREC_ASSIGNMENT);
      } while (match_type(parser, TOKEN_COMMA));
    }
    if (stmt->array.item_size < stmt->array.item_count)
    {
      error("Can't have more items in array then declared!");
    }
    consume(parser, TOKEN_RIGHT_BRACKET, "expect ] after array stmt");
  }

  consume(parser, TOKEN_SEMICOLON, "Expected ';' after array stmt");

  return stmt;
}
static Stmt* parse_variable_declaration(Parser* parser, DataType type)
{
  int line = parser->current->line;
  consume(parser, TOKEN_IDENTIFIER, "Expected identifier after variable type");

  String name = parser->previous->literal;

  advance(parser);
  switch (parser->previous->type)
  {
  case TOKEN_EQUAL:
  {
    Stmt* stmt           = sta_arena_push_struct(parser->arena, Stmt);
    stmt->type           = STMT_VARIABLE;
    stmt->variable.type  = type;
    stmt->variable.name  = name;
    stmt->variable.value = parse_expression(parser, 0, PREC_ASSIGNMENT);
    return stmt;
  }
  case TOKEN_SEMICOLON:
  {
    Stmt* stmt           = sta_arena_push_struct(parser->arena, Stmt);
    stmt->type           = STMT_VARIABLE;
    stmt->variable.type  = type;
    stmt->variable.name  = name;
    stmt->variable.value = 0;
    return stmt;
  }
  default:
  {
    return parse_array(parser, name, type, line);
  }
  }
}
static IfBlock* parse_if_block(Parser* parser)
{
  IfBlock* if_block = sta_arena_push_struct(parser->arena, IfBlock);
  consume(parser, TOKEN_LEFT_PAREN, "Expect ( after if");
  if_block->condition = parse_expression(parser, 0, PREC_ASSIGNMENT);
  consume(parser, TOKEN_RIGHT_PAREN, "Expect ) after if condition");
  consume(parser, TOKEN_LEFT_BRACE, "Expect { after if condition");

  parse_body(parser, &if_block->block);
  return if_block;
}
static Stmt* parse_if_statement(Parser* parser)
{
  advance(parser);
  Stmt* if_stmt               = sta_arena_push_struct(parser->arena, Stmt);
  if_stmt->type               = STMT_IF;

  if_stmt->if_.block_count    = 1;
  if_stmt->if_.block_capacity = 1;
  if_stmt->if_.blocks         = (IfBlock**)malloc(sizeof(IfBlock*) * if_stmt->if_.block_capacity);
  if_stmt->if_.blocks[0]      = parse_if_block(parser);
  while (match_type(parser, TOKEN_ELSE))
  {
    if (match_type(parser, TOKEN_IF))
    {
      if (if_stmt->if_.block_count >= if_stmt->if_.block_capacity)
      {
        if_stmt->if_.block_capacity *= 2;
        if_stmt->if_.blocks = (IfBlock**)realloc(if_stmt->if_.blocks, if_stmt->if_.block_capacity * sizeof(IfBlock*));
      }
      if_stmt->if_.blocks[if_stmt->if_.block_count++] = parse_if_block(parser);
    }
    else
    {
      consume(parser, TOKEN_LEFT_BRACE, "Expected '{' after if condition");
      if_stmt->if_.else_ = true;
      parse_body(parser, &if_stmt->if_.else_block);
    }
  }

  return if_stmt;
}
static Stmt* parse_statement(Parser* parser)
{
  switch (parser->current->type)
  {
  case TOKEN_FOR:
  {
    return parse_for_statement(parser);
  }
  case TOKEN_WHILE:
  {
    return parse_while_statement(parser);
  }
  case TOKEN_IF:
  {
    return parse_if_statement(parser);
  }
  case TOKEN_RETURN:
  {
    return parse_return_statement(parser);
  }
  default:
  {

    if (is_variable_type(parser->current->type) || is_declared_struct(parser, parser->current->literal))
    {
      DataType type = parse_type(parser);
      return parse_variable_declaration(parser, type);
    }
    else if (match_type(parser, TOKEN_SEMICOLON))
    {
      return 0;
    }

    printf("Parsing expression_statement\n");
    Stmt*  out          = parse_expression_statement(parser);
    String prev_literal = parser->previous->literal;
    String curr_literal = parser->current->literal;
    consume(parser, TOKEN_SEMICOLON, "Expected ';' after expression statement");
    return out;
  }
  }
}
static void parse_include(Parser* parser)
{
  consume(parser, TOKEN_STRING_LITERAL, "Expected string after include");
  String filename     = parser->previous->literal;
  String file_content = {};

  char   file[32];
  memset(file, 0, 32);
  strncpy(file, filename.buffer, filename.len);
  sta_read_file(parser->scanner->arena, &file_content, file);

  Scanner scanner;
  init_scanner(&scanner, parser->scanner->arena, &file_content, file);
  Parser include_parser;

  init_parser(&include_parser, &scanner, parser->arena, parser->symbols, parser->symbol_count, parser->symbol_cap);
  parse(&include_parser);

  parser->symbol_cap   = include_parser.symbol_cap;
  parser->symbol_count = include_parser.symbol_count;
  parser->symbols      = include_parser.symbols;

  int prev_count       = parser->stmt_count;
  parser->stmt_count += include_parser.stmt_count;
  while (parser->stmt_cap <= parser->stmt_count)
  {
    parser->stmt_cap *= 2;
  }

  parser->stmts = (Stmt**)realloc(parser->stmts, sizeof(Stmt*) * parser->stmt_cap);
  for (int i = prev_count, j = 0; j < include_parser.stmt_count; j++, i++)
  {
    parser->stmts[i] = include_parser.stmts[j];
  }
  free(include_parser.stmts);
}
static Stmt* parse_define(Parser* parser)
{
  consume(parser, TOKEN_IDENTIFIER, "Expect name of macro!");
  Stmt* macro          = sta_arena_push_struct(parser->arena, Stmt);
  macro->type          = STMT_MACRO;
  macro->macro.name    = parser->previous->literal;
  macro->macro.content = (Token*)(parser->arena->ptr + parser->arena->memory);

  add_symbol(parser, macro->macro.name, SYMBOL_MACRO);
  int line = parser->scanner->line;
  while (line == parser->scanner->line)
  {
    (void)sta_arena_push_struct(parser->arena, Token);
    advance(parser);
  }

  return macro;
}
static Stmt* parse_extern(Parser* parser)
{
  if (!(is_variable_type(parser->current->type)))
  {
  }
  Stmt* external                = sta_arena_push_struct(parser->arena, Stmt);
  external->type                = STMT_EXTERN;
  external->extern_.return_type = parse_type(parser);

  consume(parser, TOKEN_IDENTIFIER, "Expected identifier for function name after #extern");
  external->extern_.name = parser->previous->literal;
  add_symbol(parser, external->extern_.name, SYMBOL_FUNCTION);

  consume(parser, TOKEN_LEFT_PAREN, "Expected ( for function arguments after function name in extern");
  if (match_type(parser, TOKEN_ELLIPSIS))
  {
    consume(parser, TOKEN_RIGHT_PAREN, "Expected ) after function args in extern");
    external->extern_.argument_count = 0;
    external->extern_.var_args       = true;
    external->extern_.arguments      = 0;
  }
  else
  {
    external->extern_.arguments      = (StructField*)(parser->arena->ptr + parser->arena->memory);
    external->extern_.argument_count = parse_arguments(parser);
  }
  return external;
}
static Stmt* parse_function(Parser* parser)
{
  if (!(is_variable_type(parser->current->type) || is_declared_struct(parser, parser->current->literal)))
  {
    error("Can't declare something other then extern, include, struct or function");
  }

  Stmt* stmt                 = sta_arena_push_struct(parser->arena, Stmt);
  stmt->type                 = STMT_FUNCTION;
  stmt->function.return_type = parse_type(parser);

  consume(parser, TOKEN_IDENTIFIER, "Expected function name after return type");
  stmt->function.name = parser->previous->literal;
  consume(parser, TOKEN_LEFT_PAREN, "Expected '(' after function name");
  stmt->function.arguments      = (StructField*)(parser->arena->ptr + parser->arena->memory);
  stmt->function.argument_count = parse_arguments(parser);
  consume(parser, TOKEN_LEFT_BRACE, "Expect '{' after function arguments");

  add_symbol(parser, stmt->function.name, SYMBOL_FUNCTION);

  parse_body(parser, &stmt->function.block);

  return stmt;
}
static void add_stmt(Parser* parser, Stmt* stmt)
{
  if (parser->stmt_count >= parser->stmt_cap)
  {
    parser->stmt_cap *= 2;
    parser->stmts = (Stmt**)realloc(parser->stmts, sizeof(Stmt*) * parser->stmt_cap);
  }
  parser->stmts[parser->stmt_count++] = stmt;
}

void parse(Parser* parser)
{
  advance(parser);
  while (!match_type(parser, TOKEN_EOF))
  {
    if (match_type(parser, TOKEN_INCLUDE))
    {
      parse_include(parser);
    }
    else if (match_type(parser, TOKEN_STRUCT))
    {
      add_stmt(parser, parse_struct_declaration(parser));
    }
    else if (match_type(parser, TOKEN_DEFINE))
    {
      add_stmt(parser, parse_define(parser));
    }
    else if (match_type(parser, TOKEN_EXTERN))
    {
      add_stmt(parser, parse_extern(parser));
    }
    else if (!match_type(parser, TOKEN_SEMICOLON))
    {
      add_stmt(parser, parse_function(parser));
    }
  }
  for (int i = 0; i < parser->stmt_count; i++)
  {
    debug_stmt(parser->stmts[i]);
  }
}

void free_expr(Expr* expr)
{
  switch (expr->type)
  {
  case EXPR_BINARY:
  {
    BinaryExpr* binary = &expr->binary;
    free_expr(binary->left);
    free_expr(binary->right);
    break;
  }
  case EXPR_CALL:
  {
    CallExpr* call = &expr->call;
    for (int i = 0; i < call->arg_count; i++)
    {
      free_expr(call->args[i]);
    }
    free(call->args);
    break;
  }
  case EXPR_CAST:
  {
    free_expr(expr->cast.target);
    break;
  }
  case EXPR_COMPARISON:
  {
    ComparisonExpr* comparison = &expr->comparison;
    free_expr(comparison->left);
    free_expr(comparison->right);
    break;
  }
  case EXPR_DOT:
  {
    DotExpr* dot = &expr->dot;
    free_expr(dot->target);
    break;
  }
  case EXPR_GROUPED:
  {
    GroupedExpr* grouped = &expr->grouped;
    free_expr(grouped->grouped);
    break;
  }
  case EXPR_INDEX:
  {
    IndexExpr* index = &expr->index;
    free_expr(index->target);
    free_expr(index->index);
    break;
  }
  case EXPR_LOGICAL:
  {
    LogicalExpr* logical = &expr->logical;
    free_expr(logical->left);
    free_expr(logical->right);
    break;
  }
  case EXPR_POSTFIX:
  {
    PostfixExpr* postfix = &expr->postfix;
    free_expr(postfix->target);
    break;
  }
  case EXPR_UNARY:
  {
    UnaryExpr* unary = &expr->unary;
    free_expr(unary->target);
    break;
  }
  case EXPR_VARIABLE:
  case EXPR_LITERAL:
  {
    break;
  }
  }
}

void        free_stmt(Stmt* stmt);

static void free_stmt_block(StmtBlock* block)
{
  for (int i = 0; i < block->block_count; i++)
  {
    free_stmt(block->block[i]);
  }
  free(block->block);
}

void free_stmt(Stmt* stmt)
{
  switch (stmt->type)
  {
  case STMT_IF:
  {
    IfStmt* if_stmt = &stmt->if_;
    for (int i = 0; i < if_stmt->block_count; i++)
    {
      free_stmt_block(&if_stmt->blocks[i]->block);
    }
    free(if_stmt->blocks);

    if (if_stmt->else_)
    {
      free_stmt_block(&if_stmt->else_block);
    }
    break;
  }
  case STMT_FOR:
  {
    ForStmt* for_stmt = &stmt->for_;

    free_stmt(for_stmt->condition);
    free_stmt(for_stmt->update);
    free_stmt(for_stmt->init);
    free_stmt_block(&for_stmt->block);
    break;
  }
  case STMT_WHILE:
  {
    WhileStmt* while_stmt = &stmt->while_;
    free_expr(while_stmt->condition);
    free_stmt_block(&while_stmt->block);
    break;
  }
  case STMT_FUNCTION:
  {
    FunctionStmt* function = &stmt->function;
    free_stmt_block(&function->block);
    break;
  }
  case STMT_ARRAY:
  {
    ArrayStmt* array = &stmt->array;
    for (int i = 0; i < array->item_count; i++)
    {
      free_expr(array->items[i]);
    }
    free(array->items);
    break;
  }
  case STMT_ASSIGN:
  {
    AssignStmt* assign = &stmt->assign;
    free_expr(assign->value);
    free_expr(assign->target);
    break;
  }
  case STMT_EXPRESSION:
  {
    ExpressionStmt* expr = &stmt->expr;
    free_expr(expr->expr);
    break;
  }
  case STMT_RETURN:
  {
    ReturnStmt* return_stmt = &stmt->return_;
    if (return_stmt->value)
    {
      free_expr(return_stmt->value);
    }
    break;
  }
  case STMT_VARIABLE:
  {
    VariableStmt* variable_stmt = &stmt->variable;
    if (variable_stmt->value)
    {

      free_expr(variable_stmt->value);
    }
    break;
  }
  case STMT_STRUCT:
  case STMT_EXTERN:
  case STMT_MACRO:
  {
    break;
  }
  }
}
void free_stmts(Parser* parser)
{
  for (int i = 0; i < parser->stmt_count; i++)
  {
    free_stmt(parser->stmts[i]);
  }
  free(parser->stmts);
  free((void*)parser->arena->memory);
}
void free_parser(Parser* parser)
{
  free_stmts(parser);
  free(parser->symbols);
  free((void*)parser->arena->memory);
  free((void*)parser->scanner->arena->memory);
}
