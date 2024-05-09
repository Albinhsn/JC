#include "stmt.h"

void debug_array(ArrayStmt* array)
{
  debug_type(array->item_type);
  printf(" %.*s[%d] = [", (i32)array->name.len, array->name.buffer, array->item_size);
  for (int i = 0; i < array->item_count; i++)
  {
    debug_expr(array->items[i]);
    if (i < array->item_count - 1)
    {
      printf(", ");
    }
  }
  printf("]");
}

void debug_assign(AssignStmt* assign)
{
  debug_expr(assign->target);
  printf(" = ");
  debug_expr(assign->value);
  printf(";");
}
void debug_expr_stmt(ExpressionStmt* expr)
{
  debug_expr(expr->expr);
  printf(";");
}

void debug_block(StmtBlock* block)
{
  printf("{\n");
  for (int i = 0; i < block->block_count; i++)
  {
    printf("\t");
    debug_stmt(block->block[i]);
    printf("\n");
  }
  printf("}\n");
}

void debug_for(ForStmt* for_)
{
  printf("for(");
  debug_stmt(for_->init);
  printf("; ");
  debug_stmt(for_->condition);
  printf("; ");
  debug_stmt(for_->update);
  printf("(");
  debug_block(&for_->block);
}
void debug_while(WhileStmt* while_)
{
  printf("while(");
  debug_expr(while_->condition);
  printf(")");
  debug_block(&while_->block);
}

void debug_if_block(IfBlock* if_block)
{
  printf("(");
  debug_expr(if_block->condition);
  printf(")");
  debug_block(&if_block->block);
}

void debug_if(IfStmt* if_)
{
  printf("if");
  debug_if_block(if_->blocks[0]);
  for (int i = 1; i < if_->block_count; i++)
  {
    printf("else if");
    debug_if_block(if_->blocks[i]);
  }
  if (if_->else_)
  {
    printf("else");
    debug_block(&if_->else_block);
  }
}
void debug_return(ReturnStmt* return_)
{
  printf("return ");
  debug_expr(return_->value);
  printf(";\n");
}
void debug_variable(VariableStmt* variable)
{
  debug_type(variable->type);
  printf(" %.*s = ", (i32)variable->name.len, variable->name.buffer);
  debug_expr(variable->value);
  printf(";\n");
}
void debug_function(FunctionStmt* function)
{
  debug_type(function->return_type);
  printf(" %.*s(", (i32)function->name.len, function->name.buffer);
  for (int i = 0; i < function->argument_count; i++)
  {
    debug_field(&function->arguments[i]);
    if (i < function->argument_count - 1)
    {
      printf(", ");
    }
  }
  printf(")");
  debug_block(&function->block);
}
void debug_struct(StructStmt* strukt)
{
  printf("struct %.*s{\n\t", (i32)strukt->name.len, strukt->name.buffer);
  for (int i = 0; i < strukt->field_count; i++)
  {
    debug_field(&strukt->fields[i]);
    printf(";\n");
    if(i < strukt->field_count - 1){
      printf("\t");
    }
  }
  printf("}\n");
}
void debug_macro(MacroStmt* macro)
{
  printf("#define %.*s ", (i32)macro->name.len, macro->name.buffer);
  for (int i = 0; i < macro->count; i++)
  {
    debug_token(&macro->content[i]);
  }
  printf("\n");
}
void debug_extern(ExternStmt* extern_)
{
  printf("#extern ");
  debug_type(extern_->return_type);
  printf(" %.*s(", (i32)extern_->name.len, extern_->name.buffer);
  if (extern_->var_args)
  {
    printf("...)\n");
    return;
  }
  for (int i = 0; i < extern_->argument_count; i++)
  {
    debug_field(&extern_->arguments[i]);
    if (i < extern_->argument_count - 1)
    {
      printf(", ");
    }
  }
  printf(")\n");
}

void debug_stmt(Stmt* stmt)
{
  switch (stmt->type)
  {
  case STMT_ARRAY:
  {
    debug_array(&stmt->array);
    break;
  }
  case STMT_ASSIGN:
  {
    debug_assign(&stmt->assign);
    break;
  }
  case STMT_EXPRESSION:
  {
    debug_expr_stmt(&stmt->expr);
    break;
  }
  case STMT_FOR:
  {
    debug_for(&stmt->for_);
    break;
  }
  case STMT_WHILE:
  {
    debug_while(&stmt->while_);
    break;
  }
  case STMT_IF:
  {
    debug_if(&stmt->if_);
    break;
  }
  case STMT_RETURN:
  {
    debug_return(&stmt->return_);
    break;
  }
  case STMT_VARIABLE:
  {
    debug_variable(&stmt->variable);
    break;
  }
  case STMT_FUNCTION:
  {
    debug_function(&stmt->function);
    break;
  }
  case STMT_STRUCT:
  {
    debug_struct(&stmt->struct_);
    break;
  }
  case STMT_EXTERN:
  {
    debug_extern(&stmt->extern_);
    break;
  }
  case STMT_MACRO:
  {
    debug_macro(&stmt->macro);
    break;
  }
  }
}

