#include "parser.h"
#include "common.h"
#include "data_type.h"
#include "expr.h"
#include "scanner.h"
#include "struct.h"
#include "token.h"
#include <stdlib.h>

void init_parser(Parser* parser, Scanner* scanner, Arena* arena)
{
  parser->scanner    = scanner;
  parser->queue      = 0;
  parser->arena      = arena;
  parser->stmt_count = 8;
  parser->stmts      = malloc(sizeof(Stmt) * parser->stmt_count);
}

static void advance(Parser* parser)
{
  parser->previous = parser->current;
  if (parser->queue != 0 && parser->queue->count != 0)
  {
  }
  parser->current = parse_token(parser->scanner);
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

static bool is_declared_struct(Parser* parser, String name)
{
  return true;
}

static DataType parse_type(Parser* parser)
{
  DataType type = get_type_from_token(parser->current);
  if (type.type == DATATYPE_STRUCT && !is_declared_struct(parser, parser->current->literal))
  {
    error("Can't declare struct of unknown type!");
  }
  advance(parser);
  return parse_pointer_type(parser, type);
}
static StructField parse_struct_field(Parser* parser)
{
  DataType type = parse_type(parser);
  if (!match_type(parser, TOKEN_IDENTIFIER))
  {
    error("Expected identifier for struct field!");
  }
  StructField field = {};
  field.type        = type;
  field.name        = parser->previous->literal;
  return field;
}

static Expr* parse_postfix(Parser* parser, Expr* expression, bool canAssign, int line)
{
  Expr* expr           = sta_arena_push_struct(parser->arena, Expr);
  expr->type           = EXPR_POSTFIX;
  expr->postfix.target = expression;
  expr->postfix.op     = parser->previous;
  return expr;
}

static void parse_struct_declaration(Parser* parser)
{
}
static void consume(Parser* parser)
{
}
static void parse_literal(Parser* parser)
{
}
static void parse_expression(Parser* parser)
{
}
static void match_augmented(Parser* parser)
{
}
static void parse_expression_statement(Parser* parser)
{
}
static void parse_return_statement(Parser* parser)
{
}
static void parse_for_statement(Parser* parser)
{
}
static void parse_while_statement(Parser* parser)
{
}
static void parse_body(Parser* parser)
{
}
static void parse_arguments(Parser* parser)
{
}
static void parse_array(Parser* parser)
{
}
static void parse_variable_declaration(Parser* parser)
{
}
static void parse_if_block(Parser* parser)
{
}
static void parse_if_statement(Parser* parser)
{
}
static void parse_statement(Parser* parser)
{
}
static void parse_include(Parser* parser)
{
}
static void parse_define(Parser* parser)
{
}
static void parse_extern(Parser* parser)
{
}
static void parse_function(Parser* parser)
{
}
static void parse_grouped_expression(Parser* parser)
{
}
static void parse_dot_expression(Parser* parser)
{
}
static void parse_right_side_of_binary(Parser* parser)
{
}
static void parse_comparison(Parser* parser)
{
}
static void parse_binary(Parser* parser)
{
}
static void parse_dereference(Parser* parser)
{
}
static void parse_unary(Parser* parser)
{
}
static void parse_list(Parser* parser)
{
}
static void parse_call(Parser* parser)
{
}
static void parse_variable(Parser* parser)
{
}
static void parse_logical(Parser* parser)
{
}
static void parse_array_items(Parser* parser)
{
}
static void parse_index(Parser* parser)
{
}
void parse(Parser* parser)
{
  while (!match_type(parser, TOKEN_EOF))
  {
    advance(parser);
    debug_token(parser->current);
  }
}
