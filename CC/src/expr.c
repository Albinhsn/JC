#include "expr.h"

static void debug_binary(BinaryExpr* binary)
{
  debug_expr(binary->left);
  printf(" ");
  debug_token(binary->op);
  printf(" ");
  debug_expr(binary->right);
}
static void debug_call(CallExpr* call)
{
  printf("%.*s(", (i32)call->name.len, call->name.buffer);
  for (int i = 0; i < call->argCount; i++)
  {
    debug_expr(call->args[i]);
    if (i < call->argCount - 1)
    {
      printf(", ");
    }
  }
  printf(")");
}
static void debug_cast(CastExpr* cast)
{
  printf("(");
  debug_type(cast->type);
  printf(")");
  debug_expr(cast->target);
}
static void debug_comparison(ComparisonExpr* comparison)
{
  debug_expr(comparison->left);
  printf(" ");
  debug_token(comparison->op);
  printf(" ");
  debug_expr(comparison->right);
}
static void debug_dot(DotExpr* dot)
{
  debug_expr(dot->target);
  printf(".%.*s", (i32)dot->member.len, dot->member.buffer);
}
static void debug_grouped(GroupedExpr* group)
{
  printf("(");
  debug_expr(group->grouped);
  printf(")");
}
static void debug_index(IndexExpr* index)
{
  debug_expr(index->target);
  printf("[");
  debug_expr(index->index);
  printf("]");
}
static void debug_literal(LiteralExpr* literal)
{
  if (literal->literal->type == TOKEN_STRING_LITERAL)
  {
    printf("\"%.*s\"", (i32)literal->literal->literal.len, literal->literal->literal.buffer);
  }else{
    printf("%.*s", (i32)literal->literal->literal.len, literal->literal->literal.buffer);
  }
}
static void debug_logical(LogicalExpr* logical)
{
  debug_expr(logical->left);
  printf(" ");
  debug_token(logical->op);
  printf(" ");
  debug_expr(logical->right);
}
static void debug_postfix(PostfixExpr* postfix)
{
  debug_expr(postfix->target);
  debug_token(postfix->op);
}
static void debug_unary(UnaryExpr* unary)
{
  debug_token(unary->op);
  debug_expr(unary->target);
}
static void debug_variable(VariableExpr* variable)
{
  printf("%.*s", (i32)variable->variable.len, variable->variable.buffer);
}

void debug_expr(Expr* expr)
{
  switch (expr->type)
  {
  case EXPR_BINARY:
  {
    debug_binary(&expr->binary);
    break;
  }
  case EXPR_CALL:
  {
    debug_call(&expr->call);
    break;
  }
  case EXPR_CAST:
  {
    debug_cast(&expr->cast);
    break;
  }
  case EXPR_COMPARISON:
  {
    debug_comparison(&expr->comparison);
    break;
  }
  case EXPR_DOT:
  {
    debug_dot(&expr->dot);
    break;
  }
  case EXPR_GROUPED:
  {
    debug_grouped(&expr->grouped);
    break;
  }
  case EXPR_INDEX:
  {
    debug_index(&expr->index);
    break;
  }
  case EXPR_LITERAL:
  {
    debug_literal(&expr->literal);
    break;
  }
  case EXPR_LOGICAL:
  {
    debug_logical(&expr->logical);
    break;
  }
  case EXPR_POSTFIX:
  {
    debug_postfix(&expr->postfix);
    break;
  }
  case EXPR_UNARY:
  {
    debug_unary(&expr->unary);
    break;
  }
  case EXPR_VARIABLE:
  {
    debug_variable(&expr->variable);
    break;
  }
  }
}
