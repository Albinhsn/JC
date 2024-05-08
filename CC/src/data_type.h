
#ifndef DATA_TYPE_H
#define DATA_TYPE_H

#include "common.h"
#include "token.h"

enum DataTypes
{
  DATATYPE_DOUBLE,
  DATATYPE_FLOAT,
  DATATYPE_LONG,
  DATATYPE_INT,
  DATATYPE_SHORT,
  DATATYPE_BYTE,
  DATATYPE_VOID,
  DATATYPE_ARRAY,
  DATATYPE_STRUCT,
  DATATYPE_STRING
};
typedef enum DataTypes DataTypes;

struct DataType
{
  String    name;
  DataTypes type;
  int       depth;
};

typedef struct DataType DataType;

DataType                get_pointer_from_type(DataType type);
DataType                get_type_from_token(Token* token);

void debug_type(DataType type);

#endif
