#include "struct.h"
#include "data_type.h"

void debug_field(StructField* field)
{
  debug_type(field->type);
  printf(" %.*s", (i32)field->name.len, field->name.buffer);
}
