#include "parser.h"
#include "common.h"
#include "data_type.h"
#include "expr.h"
#include "scanner.h"
#include "stmt.h"
#include "struct.h"
#include "token.h"
#include <stdlib.h>

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
    [TOKEN_LEFT_BRACKET]    = {             parse_index,                    0,       PREC_CALL},
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
    [TOKEN_INCREMENT]       = {             parse_unary,        parse_postfix, PREC_ASSIGNMENT},
    [TOKEN_DECREMENT]       = {             parse_unary,        parse_postfix, PREC_ASSIGNMENT},
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
  stmt->struct_.fields      = (StructField*)parser->arena->ptr;
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

  return expr;
}

static Expr* parse_literal(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Expr* literal            = sta_arena_push_struct(parser->arena, Expr);
  literal->type            = EXPR_LITERAL;
  literal->literal.literal = parser->previous->literal;

  return literal;
}
static Expr* parse_grouped_expression(Parser* parser, Expr* expr, bool canAssign, int line)
{
  return expr;
}
static Expr* parse_dot_expression(Parser* parser, Expr* expr, bool canAssign, int line)
{
  consume(parser, TOKEN_IDENTIFIER, "Expected identifier after dot");
  Expr* dot_expr       = sta_arena_push_struct(parser->arena, Expr);
  dot_expr->type       = EXPR_DOT;
  dot_expr->dot.member = parser->previous->literal;

  return dot_expr;
}
static Expr* parse_right_side_of_binary(Parser* parser, Precedence precedence)
{
  return parse_expression(parser, 0, precedence + 1);
}
static Expr* parse_comparison(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Token* op                        = parser->previous;
  Expr*  comparison_expr           = sta_arena_push_struct(parser->arena, Expr);
  comparison_expr->comparison.left = expr;
  comparison_expr->type            = EXPR_COMPARISON;
  Expr* right_side                 = parse_right_side_of_binary(parser, rules[op->type].precedence);

  return comparison_expr;
}
static Expr* parse_binary(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Token* op                = parser->previous;
  Expr*  binary_expr       = sta_arena_push_struct(parser->arena, Expr);
  binary_expr->binary.left = expr;
  binary_expr->type        = EXPR_BINARY;
  Expr* right_side         = parse_right_side_of_binary(parser, rules[op->type].precedence);
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
static void parse_list(Parser* parser)
{
}
static Expr* parse_call(Parser* parser, Token* variable)
{
  Expr* call_expr          = sta_arena_push_struct(parser->arena, Expr);
  call_expr->type          = EXPR_CALL;
  call_expr->call.name     = variable->literal;
  call_expr->call.argCount = 0;
  call_expr->call.args     = (Expr**)&parser->arena->ptr;

  if (parser->current->type != TOKEN_RIGHT_PAREN)
  {
    do
    {
      sta_arena_push_struct(parser->arena, Expr*);
      call_expr->call.args[call_expr->call.argCount++] = parse_expression(parser, 0, PREC_ASSIGNMENT);
    } while (!match_type(parser, TOKEN_RIGHT_PAREN));
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
static void parse_array_items(Parser* parser)
{
}
static Expr* parse_index(Parser* parser, Expr* expr, bool canAssign, int line)
{
  Expr* index        = sta_arena_push_struct(parser->arena, Expr);
  expr->type         = EXPR_INDEX;
  expr->index.target = expr;
  expr->index.index  = parse_expression(parser, 0, PREC_ASSIGNMENT);
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
  int   line = parser->scanner->line;
  Stmt* stmt = sta_arena_push_struct(parser->arena, Stmt);
  Expr* expr = parse_expression(parser, 0, PREC_OR);

  if (match_type(parser, TOKEN_EQUAL))
  {
    Expr* equals        = parse_expression(parser, 0, PREC_ASSIGNMENT);
    stmt->type          = STMT_ASSIGN;
    stmt->assign.target = expr;
    stmt->assign.value  = equals;
  }
  else if (match_augmented(parser))
  {
    Token* op           = parser->previous;
    Expr*  right_side   = parse_expression(parser, 0, PREC_ASSIGNMENT);
    stmt->type          = STMT_ASSIGN;
    stmt->assign.target = expr;
    stmt->assign.value  = right_side;
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
static int parse_body(Parser* parser)
{
  // THIS DOESN*T WORK
  int count = 0;
  while (!match_type(parser, TOKEN_RIGHT_BRACE))
  {
    if (!match_type(parser, TOKEN_SEMICOLON))
    {
      parse_statement(parser);
      count++;
    }
  }

  return count;
}
static Stmt* parse_for_statement(Parser* parser)
{
  advance(parser);
  Stmt* out = sta_arena_push_struct(parser->arena, Stmt);
  out->type = STMT_FOR;
  int line  = parser->previous->line;
  consume(parser, TOKEN_LEFT_PAREN, "Expect ( after for");

  out->for_.init      = parse_statement(parser);
  out->for_.condition = parse_expression_statement(parser);
  consume(parser, TOKEN_SEMICOLON, "Expect ';' after condition stmt");

  out->for_.update = parse_expression_statement(parser);
  consume(parser, TOKEN_RIGHT_PAREN, "Expect ')' after update");
  consume(parser, TOKEN_LEFT_BRACE, "Expect '{' after for loop (init, condition, update)");

  out->for_.body       = (Stmt*)parser->arena->ptr;
  out->for_.body_count = parse_body(parser);

  return out;
}
static Stmt* parse_while_statement(Parser* parser)
{
  Stmt* out = sta_arena_push_struct(parser->arena, Stmt);
  out->type = STMT_WHILE;
  advance(parser);
  int line = parser->previous->line;

  consume(parser, TOKEN_LEFT_PAREN, "expect '(' after while");
  out->while_.condition = parse_expression(parser, 0, PREC_ASSIGNMENT);
  consume(parser, TOKEN_RIGHT_PAREN, "expected ')' after while");
  consume(parser, TOKEN_LEFT_BRACE, "Expected '{' after while condition");

  out->while_.body       = (Stmt*)&parser->arena->ptr;
  out->while_.body_count = parse_body(parser);

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
}
static Stmt* parse_variable_declaration(Parser* parser, DataType type)
{
}
static IfBlock* parse_if_block(Parser* parser)
{
}
static Stmt* parse_if_statement(Parser* parser)
{
}
static Stmt* parse_statement(Parser* parser)
{
}
static void parse_include(Parser* parser)
{
}
static Stmt* parse_define(Parser* parser)
{
}
static Stmt* parse_extern(Parser* parser)
{
}
static Stmt* parse_function(Parser* parser)
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
