#include "data_type.h"
#include "common.h"
#include "scanner.h"
#include "token.h"
#include <stdlib.h>

DataType get_pointer_from_type(DataType type)
{
  DataType out = {};
  out.type     = type.type;
  out.name     = type.name;
  out.depth    = type.depth + 1;
  return out;
}

DataType get_int()
{
  DataType out = {};
  out.type     = DATATYPE_INT;
  out.depth    = 0;
  sta_initString(&out.name, "int");
  return out;
}

DataType get_float()
{
  DataType out = {};
  out.type     = DATATYPE_FLOAT;
  out.depth    = 0;
  sta_initString(&out.name, "float");
  return out;
}

DataType get_string()
{
  DataType out = {};
  out.type     = DATATYPE_STRING;
  out.depth    = 0;
  sta_initString(&out.name, "string");
  return out;
}
DataType get_struct(String literal)
{
  DataType out = {};
  out.type     = DATATYPE_STRUCT;
  out.depth    = 0;
  out.name     = literal;
  return out;
}
DataType get_byte()
{
  DataType out = {};
  out.type     = DATATYPE_BYTE;
  out.depth    = 0;
  sta_initString(&out.name, "byte");
  return out;
}
DataType get_void()
{
  DataType out = {};
  out.type     = DATATYPE_VOID;
  out.depth    = 0;
  sta_initString(&out.name, "void");
  return out;
}
DataType get_short()
{
  DataType out = {};
  out.type     = DATATYPE_SHORT;
  out.depth    = 0;
  sta_initString(&out.name, "short");
  return out;
}
DataType get_double()
{
  DataType out = {};
  out.type     = DATATYPE_DOUBLE;
  out.depth    = 0;
  sta_initString(&out.name, "double");
  return out;
}
DataType get_long()
{
  DataType out = {};
  out.type     = DATATYPE_LONG;
  out.depth    = 0;
  sta_initString(&out.name, "long");
  return out;
}

DataType get_type_from_token(Token* token)
{
  switch (token->type)
  {
  case TOKEN_INT:
  case TOKEN_INT_LITERAL:
  {
    return get_int();
  }
  case TOKEN_FLOAT:
  case TOKEN_FLOAT_LITERAL:
  {
    return get_float();
  }
  case TOKEN_STRING:
  case TOKEN_STRING_LITERAL:
  {
    return get_string();
  }
  case TOKEN_IDENTIFIER:
  {
    return get_struct(token->literal);
  }
  case TOKEN_VOID:
  {
    return get_void();
  }
  case TOKEN_BYTE:
  {
    return get_byte();
  }
  case TOKEN_DOUBLE:
  {
    return get_double();
  }
  case TOKEN_SHORT:
  {
    return get_short();
  }
  case TOKEN_LONG:
  {
    return get_long();
  }
  default:
  {
  }
  }
  printf("%.*s\n", (i32)token->literal.len, token->literal.buffer);
  error("Can't convert this token to data type?");
  exit(1);
}

void debug_type(DataType type)
{
  printf("%.*s ", (i32)type.name.len, type.name.buffer);
  for (int i = 0; i < type.depth; i++)
  {
    printf("*");
  }
}
