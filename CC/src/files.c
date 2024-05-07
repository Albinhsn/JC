#include "files.h"
#include <assert.h>
#include <ctype.h>
#include <stdlib.h>
#include <string.h>

void sta_draw_rect_to_image(Image* image, u64 x, u64 y, u64 width, u64 height, u8 r, u8 g, u8 b, u8 a)
{
  for (u64 yOffset = 0; yOffset < height; yOffset++)
  {
    for (u64 xOffset = 0; xOffset < width; xOffset++)
    {
      u64 imageOffset              = ((x + xOffset) + (y + yOffset) * image->width) * 4;
      image->data[imageOffset + 0] = r;
      image->data[imageOffset + 1] = g;
      image->data[imageOffset + 2] = b;
      image->data[imageOffset + 3] = a;
    }
  }
}

bool sta_write_ppm(String fileName, Image* image)
{
  char buffer[256] = {};
  strncpy(&buffer[0], &fileName.buffer[0], fileName.len);

  FILE* filePtr = fopen(buffer, "w");
  if (filePtr == 0)
  {
    printf("Failed to open %.*s\n", (i32)fileName.len, fileName.buffer);
    return false;
  }

  int result = fprintf(filePtr, "P3\n%ld %ld\n255\n", image->width, image->height);
  if (result < 0)
  {
    return false;
  }

  u64 imageSize = image->height * image->width * 4;
  u8* imageData = image->data;

  for (u64 i = 0; i < imageSize; i += 4)
  {
    result = fprintf(filePtr, "%d %d %d\n", imageData[i + 0], imageData[i + 1], imageData[i + 2]);
    if (result < 0)
    {
      return false;
    }
  }

  (void)fclose(filePtr);

  return true;
}

bool sta_read_file(Arena* arena, struct String* string, const char* fileName)
{
  FILE* filePtr;
  long  fileSize, count;
  int   error;

  filePtr = fopen(fileName, "r");
  if (!filePtr)
  {
    return NULL;
  }

  fileSize                 = fseek(filePtr, 0, SEEK_END);
  fileSize                 = ftell(filePtr);

  string->len              = fileSize;
  string->buffer           = sta_arena_push_array(arena, char, fileSize + 1);
  string->buffer[fileSize] = '\0';
  fseek(filePtr, 0, SEEK_SET);
  count = fread(string->buffer, 1, fileSize, filePtr);
  if (count != fileSize)
  {
    free(string->buffer);
    return false;
  }

  error = fclose(filePtr);
  if (error != 0)
  {
    sta_arena_pop(arena, fileSize + 1);
    return false;
  }

  return true;
}

bool sta_read_targa_from_file(Arena* arena, Image* image, const char* filename)
{

  struct TargaHeader targaFileHeader;

  FILE*              filePtr;
  unsigned long      count, imageSize;

  filePtr = fopen(filename, "rb");
  if (filePtr == NULL)
  {
    printf("ERROR: file doesn't exist %s\n", filename);
    return false;
  }

  count = fread(&targaFileHeader, sizeof(struct TargaHeader), 1, filePtr);
  if (count != 1)
  {
    printf("ERROR: Failed to read into header\n");
    return false;
  }

  image->width  = targaFileHeader.width;
  image->height = targaFileHeader.height;
  image->bpp    = targaFileHeader.imagePixelSize;

  if (image->bpp == 32)
  {
    imageSize = image->width * image->height * 4;
  }
  else if (image->bpp == 24)
  {
    imageSize = image->width * image->height * 3;
  }else{
    assert(false);
  }

  image->data = sta_arena_push_array(arena, unsigned char, imageSize);

  count       = fread(image->data, 1, imageSize, filePtr);
  if (count != imageSize)
  {
    printf("ERROR: count read doesn't equal imageSize\n");
    return false;
  }

  if (fclose(filePtr) != 0)
  {
    printf("Failed to close file\n");
    return false;
  }

  return true;
}

#define RESIZE_ARRAY(array, type, count, cap)                                                                                                                                                          \
  if (cap >= count)                                                                                                                                                                                    \
  {                                                                                                                                                                                                    \
    cap *= 2;                                                                                                                                                                                          \
    array = (type*)realloc(array, sizeof(type) * cap);                                                                                                                                                 \
  }

static inline bool char_is_empty(char c)
{
  return !isdigit(c) && !isalpha(c);
}

static inline bool parse_end_of_csv_field(String* s, u64* curr)
{
  bool quoted = false;
  while (*curr < s->len && ((s->buffer[*curr] != ',' && s->buffer[*curr] != '\n') && !quoted))
  {
    if (s->buffer[*curr] == '\"')
    {
      quoted = !quoted;
    }
    (*curr)++;
  }
  return s->buffer[*curr] == '\n' && !quoted;
}

static inline u64 skip_whitespace_string(String* s, u64 curr)
{
  while (curr < s->len && (s->buffer[curr] == ' ' || s->buffer[curr] == '\n'))
  {
    curr++;
  }
  return curr;
}

static void free_csv(CSV* csv)
{

  for (u64 i = 0; i < csv->recordCount; i++)
  {
    CSVRecord* record = &csv->records[i];
    free(record->data);
  }
  free(csv->records);
}

bool sta_read_csv_from_string(CSV* csv, String csvData)
{
  csv->recordCap           = 1;
  csv->recordCount         = 1;
  csv->records             = (CSVRecord*)malloc(sizeof(CSVRecord));
  CSVRecord* currentRecord = &csv->records[0];
  currentRecord->dataCount = 0;
  currentRecord->dataCap   = 1;
  currentRecord->data      = (String*)malloc(sizeof(String));
  u64 columnsPerRecord     = -1;
  u64 fileDataIndex        = 0;

  while (fileDataIndex < csvData.len)
  {
    u64  previousDataIndex = fileDataIndex;
    bool foundEnd          = parse_end_of_csv_field(&csvData, &fileDataIndex);
    RESIZE_ARRAY(currentRecord->data, String, currentRecord->dataCount, currentRecord->dataCap);
    currentRecord->data[currentRecord->dataCount].buffer = &csvData.buffer[previousDataIndex];
    currentRecord->data[currentRecord->dataCount].len    = fileDataIndex - previousDataIndex;
    currentRecord->dataCount++;
    if (foundEnd)
    {
      if (columnsPerRecord == -1)
      {
        columnsPerRecord = currentRecord->dataCount;
      }
      else if (columnsPerRecord != currentRecord->dataCount)
      {
        free_csv(csv);
        return false;
      }
      RESIZE_ARRAY(csv->records, CSVRecord, csv->recordCount, csv->recordCap);
      currentRecord            = &csv->records[csv->recordCount];
      currentRecord->dataCount = 0;
      currentRecord->dataCap   = columnsPerRecord;
      currentRecord->data      = (String*)malloc(sizeof(String) * columnsPerRecord);
      csv->recordCount++;
    }
    fileDataIndex++;
  }

  return true;
}

bool sta_read_csv_from_file(Arena* arena, CSV* csv, String fileLocation)
{
  String fileData;

  bool   result = sta_read_file(arena, &fileData, fileLocation.buffer);
  if (!result)
  {
    return false;
  }

  return sta_read_csv_from_string(csv, fileData);
}
void sta_debug_csv(CSV* csv)
{
  for (u64 i = 0; i < csv->recordCount; i++)
  {
    CSVRecord currentRecord = csv->records[i];
    for (u64 j = 0; j < currentRecord.dataCount; j++)
    {
      printf("%.*s", (i32)currentRecord.data[j].len, currentRecord.data[j].buffer);
      printf("%c", j == currentRecord.dataCount - 1 ? '\n' : ',');
    }
  }
}

bool sta_write_csv_to_file(CSV* csv, String fileLocation)
{
  String tempStr;
  tempStr.buffer = (char*)malloc(sizeof(char) * (fileLocation.len + 1));
  tempStr.len    = fileLocation.len + 1;
  strncpy(tempStr.buffer, fileLocation.buffer, fileLocation.len);
  tempStr.buffer[tempStr.len - 1] = '\0';
  FILE* filePtr                   = fopen(tempStr.buffer, "w");
  if (!filePtr)
  {
    return false;
  }
  for (u64 i = 0; i < csv->recordCount; i++)
  {
    CSVRecord currentRecord = csv->records[i];
    for (u64 j = 0; j < currentRecord.dataCount; j++)
    {
      i32 res = fprintf(filePtr, "%.*s", (i32)currentRecord.data[j].len, currentRecord.data[j].buffer);
      if (res < 0)
      {
        fclose(filePtr);
        return false;
      }
      res = fprintf(filePtr, "%c", j == currentRecord.dataCount - 1 ? '\n' : ',');
      if (res < 0)
      {
        fclose(filePtr);
        return false;
      }
    }
  }

  return fclose(filePtr) == 0;
}

#define CURRENT_CHAR(buffer, curr) (buffer[*curr])
#define ADVANCE(curr)              ((*curr)++)

static void serializeJsonValue(JsonValue* value, FILE* filePtr);
static void serializeJsonObject(JsonObject* object, FILE* filePtr);

static void serializeJsonArray(JsonArray* arr, FILE* filePtr)
{
  fwrite("[", 1, 1, filePtr);
  for (i32 i = 0; i < arr->arraySize; i++)
  {
    serializeJsonValue(&arr->values[i], filePtr);
    if (i != arr->arraySize - 1)
    {
      fwrite(",", 1, 1, filePtr);
    }
  }
  fwrite("]", 1, 1, filePtr);
}

static void serializeJsonObject(JsonObject* object, FILE* filePtr)
{
  fwrite("{", 1, 1, filePtr);
  for (i32 i = 0; i < object->size; i++)
  {
    fprintf(filePtr, "\"%s\":", object->keys[i]);
    serializeJsonValue(&object->values[i], filePtr);
    if (i != object->size - 1)
    {
      fwrite(",", 1, 1, filePtr);
    }
  }
  fwrite("}", 1, 1, filePtr);
}

static void serializeJsonValue(JsonValue* value, FILE* filePtr)
{
  switch (value->type)
  {
  case JSON_OBJECT:
  {
    serializeJsonObject(value->obj, filePtr);
    break;
  }
  case JSON_BOOL:
  {
    if (value->b)
    {
      fwrite("true", 1, 4, filePtr);
    }
    else
    {
      fwrite("false", 1, 5, filePtr);
    }
    break;
  }
  case JSON_NULL:
  {
    fwrite("null", 1, 4, filePtr);
    break;
  }
  case JSON_NUMBER:
  {
    fprintf(filePtr, "%lf", value->number);
    break;
  }
  case JSON_ARRAY:
  {
    serializeJsonArray(value->arr, filePtr);
    break;
  }
  case JSON_STRING:
  {
    fprintf(filePtr, "\"%s\"", value->string);
    break;
  }
  default:
  {
    break;
  }
  }
}

bool sta_serialize_json_to_file(Json* json, const char* filename)
{
  FILE* filePtr;

  filePtr = fopen(filename, "w");
  if (!filePtr)
  {
    printf("Failed to open '%s'\n", filename);
    return false;
  }
  switch (json->headType)
  {
  case JSON_OBJECT:
  {
    serializeJsonObject(&json->obj, filePtr);
    break;
  }
  case JSON_ARRAY:
  {
    serializeJsonArray(&json->array, filePtr);
    break;
  }
  case JSON_VALUE:
  {
    serializeJsonValue(&json->value, filePtr);
    break;
  }
  default:
  {
    printf("HOW IS THIS THE HEAD TYPE? %d\n", json->headType);
    break;
  }
  }
  fclose(filePtr);
  return true;
}
static void debugJsonObject(JsonObject* object);
static void debugJsonArray(JsonArray* arr);

static void debugJsonValue(JsonValue* value)
{
  switch (value->type)
  {
  case JSON_OBJECT:
  {
    debugJsonObject(value->obj);
    break;
  }
  case JSON_BOOL:
  {
    if (value->b)
    {
      printf("true");
    }
    else
    {
      printf("false");
    }
    break;
  }
  case JSON_NULL:
  {
    printf("null");
    break;
  }
  case JSON_NUMBER:
  {
    printf("%lf", value->number);
    break;
  }
  case JSON_ARRAY:
  {
    debugJsonArray(value->arr);
    break;
  }
  case JSON_STRING:
  {
    printf("\"%s\"", value->string);
    break;
  }
  default:
  {
    break;
  }
  }
}

static void debugJsonObject(JsonObject* object)
{
  printf("{\n");
  for (i32 i = 0; i < object->size; i++)
  {
    printf("\"%s\":", object->keys[i]);
    debugJsonValue(&object->values[i]);
    if (i != object->size - 1)
    {
      printf(",\n");
    }
  }
  printf("\n}n");
}

static void debugJsonArray(JsonArray* arr)
{
  printf("[");
  for (i32 i = 0; i < arr->arraySize; i++)
  {
    debugJsonValue(&arr->values[i]);
    if (i != arr->arraySize - 1)
    {
      printf(", ");
    }
  }
  printf("]");
}

// static void debugJson(Json* json)
// {
//   switch (json->headType)
//   {
//   case JSON_OBJECT:
//   {
//     debugJsonObject(&json->obj);
//     break;
//   }
//   case JSON_ARRAY:
//   {
//     debugJsonArray(&json->array);
//     break;
//   }
//   case JSON_VALUE:
//   {
//     debugJsonValue(&json->value);
//     break;
//   }
//   default:
//   {
//     printf("HOW IS THIS THE HEAD TYPE? %d\n", json->headType);
//     break;
//   }
//   }
//   printf("\n");
// }

static inline void resizeObject(JsonObject* obj)
{
  if (obj->cap == 0)
  {
    obj->cap    = 4;
    obj->values = (JsonValue*)malloc(sizeof(JsonValue) * obj->cap);
    obj->keys   = (char**)malloc(sizeof(char*) * obj->cap);
  }
  else if (obj->size >= obj->cap)
  {
    obj->cap *= 2;
    obj->values = realloc(obj->values, obj->cap * sizeof(JsonValue));
    obj->keys   = realloc(obj->keys, obj->cap * sizeof(char*));
  }
}

static bool parseNumber(f32* number, char* buffer, u64* curr)
{
  u64 start = *curr;
  ADVANCE(curr);
  char line[32];
  while (isdigit(CURRENT_CHAR(buffer, curr)))
  {
    ADVANCE(curr);
  }
  if (CURRENT_CHAR(buffer, curr) == '.')
  {
    ADVANCE(curr);
    while (isdigit(CURRENT_CHAR(buffer, curr)))
    {
      ADVANCE(curr);
    }
  }
  u64 size = (*curr) - start;
  strncpy(&line[0], &buffer[start], size);
  line[size] = '\0';
  *number    = strtof(line, NULL);

  return true;
}
static bool parseString(char** key, char* buffer, u64* curr)
{
  ADVANCE(curr);
  u64 start = *curr;
  while (buffer[*curr] != '"')
  {
    (*curr)++;
  }
  u64 len     = ((*curr) - start);
  *key        = (char*)malloc(sizeof(char) * (1 + len));
  (*key)[len] = '\0';
  strncpy(*key, &buffer[start], len);
  (*curr)++;
  return true;
}

static u64 skipWhitespaceJson(char* buffer)
{
  u64 res = 0;
  while (buffer[res] == ' ' || buffer[res] == '\n')
  {
    res++;
  }
  return res;
}

static bool consumeToken(char got, char expected, u64* curr)
{
  if (expected != got)
  {
    printf("Expected '%c' but got '%c'\n", expected, got);
    return false;
  }
  (*curr)++;
  return true;
}

static bool parseJsonValue(JsonValue* value, char* buffer, u64* curr);
static bool parseJsonArray(JsonArray* arr, char* buffer, u64* curr);

static bool parseKeyValuePair(JsonObject* obj, char* buffer, u64* curr)
{
  resizeObject(obj);

  parseString(&obj->keys[obj->size], buffer, curr);
  *curr += skipWhitespaceJson(&buffer[*curr]);

  if (!consumeToken(buffer[*curr], ':', curr))
  {
    return false;
  }

  bool res = parseJsonValue(&obj->values[obj->size], buffer, curr);
  if (!res)
  {
    return false;
  }
  obj->size++;

  *curr += skipWhitespaceJson(&buffer[*curr]);
  return true;
}

static bool parseJsonObject(JsonObject* obj, char* buffer, u64* curr)
{
  ADVANCE(curr);
  (*curr) = *curr + skipWhitespaceJson(&buffer[*curr]);
  // end or string
  while (buffer[*curr] != '}')
  {
    bool res = parseKeyValuePair(obj, buffer, curr);
    if (!res)
    {
      return false;
    }

    (*curr) += skipWhitespaceJson(&buffer[*curr]);
    if (buffer[*curr] == ',')
    {
      // It's illegal to have a ',' and then end it
      ADVANCE(curr);
    }
    (*curr) += skipWhitespaceJson(&buffer[*curr]);
  }
  ADVANCE(curr);
  return true;
}

static inline void resizeArray(JsonArray* arr)
{
  if (arr->arraySize == 0)
  {
    arr->arrayCap = 4;
    arr->values   = (JsonValue*)malloc(sizeof(JsonValue) * arr->arrayCap);
  }
  else if (arr->arraySize >= arr->arrayCap)
  {
    arr->arrayCap *= 2;
    arr->values = realloc(arr->values, arr->arrayCap * sizeof(JsonValue));
  }
}
static bool parseJsonArray(JsonArray* arr, char* buffer, u64* curr)
{
  (*curr)++;
  *curr += skipWhitespaceJson(&buffer[*curr]);
  bool res;
  while (buffer[*curr] != ']')
  {
    resizeArray(arr);
    res = parseJsonValue(&arr->values[arr->arraySize], buffer, curr);
    if (!res)
    {
      return false;
    }
    arr->arraySize++;
    *curr += skipWhitespaceJson(&buffer[*curr]);
    if (buffer[*curr] == ',')
    {
      ADVANCE(curr);
    }
  }
  ADVANCE(curr);

  return true;
}
static bool parseJsonValue(JsonValue* value, char* buffer, u64* curr)
{
  *curr += skipWhitespaceJson(&buffer[*curr]);
  if (isdigit(buffer[*curr]) || buffer[*curr] == '-')
  {
    value->type = JSON_NUMBER;
    return parseNumber(&value->number, buffer, curr);
  }
  switch (buffer[*curr])
  {
  case '\"':
  {
    value->type = JSON_STRING;
    return parseString(&value->string, buffer, curr);
  }
  case '{':
  {
    value->type      = JSON_OBJECT;
    value->obj->cap  = 0;
    value->obj->size = 0;
    return parseJsonObject(value->obj, buffer, curr);
  }
  case '[':
  {
    value->type           = JSON_ARRAY;
    value->arr            = (JsonArray*)malloc(sizeof(JsonArray));
    value->arr->arrayCap  = 0;
    value->arr->arraySize = 0;
    return parseJsonArray(value->arr, buffer, curr);
  }
  case 't':
  {
    if (buffer[*curr + 1] == 'r' && buffer[*curr + 2] == 'u' && buffer[*curr + 3] == 'e')
    {
      value->type = JSON_BOOL;
      value->b    = true;
      (*curr) += 4;
      return true;
    }
    printf("Got 't' but wasn't true?\n");
    return false;
  }
  case 'f':
  {
    if (buffer[*curr + 1] == 'a' && buffer[*curr + 2] == 'l' && buffer[*curr + 3] == 's' && buffer[*curr + 4] == 'e')
    {
      value->type = JSON_BOOL;
      value->b    = false;
      (*curr) += 5;
      return true;
    }
    printf("Got 'f' but wasn't false?\n");
    return false;
  }
  case 'n':
  {
    if (buffer[*curr + 1] == 'u' && buffer[*curr + 2] == 'l' && buffer[*curr + 3] == 'l')
    {
      value->type = JSON_NULL;
      (*curr) += 4;
      return true;
    }
    printf("Got 'n' but wasn't null?\n");
    return false;
  }
  default:
  {
    printf("Unknown value token '%c'\n", buffer[*curr]);
    return false;
  }
  }
}

bool sta_deserialize_json_from_file(Arena* arena, Json* json, const char* filename)
{
  String fileContent;
  bool   result;

  result = sta_read_file(arena, &fileContent, filename);
  if (!result)
  {
    return false;
  }
  u64  curr = 0;
  bool res;
  bool first = false;
  while (!first)
  {
    switch (fileContent.buffer[curr])
    {
    case '{':
    {
      json->headType = JSON_OBJECT;
      json->obj.cap  = 0;
      json->obj.size = 0;
      res            = parseJsonObject(&json->obj, fileContent.buffer, &curr);
      first          = true;
      break;
    }
    case '[':
    {
      json->headType        = JSON_ARRAY;
      json->array.arrayCap  = 0;
      json->array.arraySize = 0;
      res                   = parseJsonArray(&json->array, fileContent.buffer, &curr);
      first                 = true;
      break;
    }
    case ' ':
    {
    }
    case '\n':
    {
    }
    case '\t':
    {
      curr++;
      break;
    }
    default:
    {
      printf("Default: %c\n", fileContent.buffer[curr]);
      json->headType = JSON_VALUE;
      res            = parseJsonValue(&json->value, fileContent.buffer, &curr);
      first          = true;
      break;
    }
    }
  }
  if (!res)
  {
    return false;
  }
  if (curr != fileContent.len)
  {
    printf("Didn't reach eof after parsing first value? %ld %ld\n", curr, fileContent.len);
    return false;
  }
  return true;
}
