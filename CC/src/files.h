#ifndef STA_FILES_H
#define STA_FILES_H
#include "common.h"
#include "string.h"
#include <stdbool.h>
#include <stdio.h>

struct Image
{
  u64            width, height;
  i32            bpp;
  unsigned char* data;
};
typedef struct Image Image;

struct Framebuffer
{
  u64            width, height;
  unsigned char* data;
};
typedef struct Framebuffer Framebuffer;

struct TargaHeader
{
  union
  {
    u8 header[18];
    struct
    {
      u8  charactersInIdentificationField;
      u8  colorMapType;
      u8  imageType;
      u8  colorMapSpec[5];
      u16 xOrigin;
      u16 yOrigin;
      u16 width;
      u16 height;
      u8  imagePixelSize;
      u8  imageDescriptor;
    };
  };
};
typedef struct TargaHeader TargaHeader;

struct CSVRecord
{
  String* data;
  u64     dataCount;
  u64     dataCap;
};
typedef struct CSVRecord CSVRecord;

struct CSV
{
  CSVRecord* records;
  u64        recordCount;
  u64        recordCap;
};
typedef struct CSV CSV;

enum JsonType
{
  JSON_VALUE,
  JSON_OBJECT,
  JSON_ARRAY,
  JSON_STRING,
  JSON_NUMBER,
  JSON_BOOL,
  JSON_NULL
};
typedef enum JsonType JsonType;

struct JsonValue;
struct JsonObject;
struct JsonArray;

struct JsonValue
{
  JsonType type;
  union
  {
    struct JsonObject* obj;
    struct JsonArray*  arr;
    bool               b;
    char*              string;
    float              number;
  };
};
typedef struct JsonValue JsonValue;

struct JsonObject
{
  char**     keys;
  JsonValue* values;
  u64        size;
  u64        cap;
};
typedef struct JsonObject JsonObject;

struct JsonArray
{
  uint32_t   arraySize;
  uint32_t   arrayCap;
  JsonValue* values;
};
typedef struct JsonArray JsonArray;

struct Json
{
  JsonType headType;
  union
  {
    JsonValue  value;
    JsonObject obj;
    JsonArray  array;
  };
};
typedef struct Json Json;

bool                sta_deserialize_json_from_file(Arena* arena, Json* json, const char* filename);
bool                sta_serialize_json_to_file(Json* json, const char* filename);
void                sta_debug_json(Json* json);

bool                sta_read_csv_from_file(Arena* arena, CSV* csv, String fileLocation);
bool                sta_write_csv_to_file(CSV* csv, String fileLocation);
void                sta_debug_csv(CSV* csv);
bool                sta_read_csv_from_string(CSV* csv, String csvData);

void                sta_draw_rect_to_image(Image* image, u64 x, u64 y, u64 width, u64 height, u8 r, u8 g, u8 b, u8 a);
bool                sta_read_targa_from_file(Arena* arena, struct Image* image, const char* filename);
bool                sta_read_file(Arena* arena, struct String* string, const char* fileName);
bool                sta_append_to_file(String fileName, String message);
bool                sta_write_ppm(String fileName, Image* image);

#endif
