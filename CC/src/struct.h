#ifndef STRUCT_H
#define STRUCT_H

#include "data_type.h"
struct StructField
{
  DataType type;
  String   name;
};
typedef struct StructField StructField;

#endif
