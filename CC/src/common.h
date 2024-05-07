#ifndef COMMON_H
#define COMMON_H

#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#define ArrayCount(Array)  (sizeof(Array) / sizeof((Array)[0]))

#define MAX(a, b)          ((a) < (b) ? (b) : (a))
#define MIN(a, b)          ((a) > (b) ? (b) : (a))

#define ANSI_COLOR_RED     "\x1b[31m"
#define ANSI_COLOR_GREEN   "\x1b[32m"
#define ANSI_COLOR_YELLOW  "\x1b[33m"
#define ANSI_COLOR_BLUE    "\x1b[34m"
#define ANSI_COLOR_MAGENTA "\x1b[35m"
#define ANSI_COLOR_CYAN    "\x1b[36m"
#define ANSI_COLOR_RESET   "\x1b[0m"

typedef uint8_t  u8;
typedef uint16_t u16;
typedef uint32_t u32;
typedef uint64_t u64;

typedef int8_t   i8;
typedef int16_t  i16;
typedef int32_t  i32;
typedef int64_t  i64;

typedef float    f32;
typedef double   f64;

#define PI 3.14159265358979

struct Arena
{
  u64 memory;
  u64 ptr;
  u64 maxSize;
};
typedef struct Arena Arena;
u64                  sta_arena_push(Arena* arena, u64 size);
void                 sta_arena_pop(Arena* arena, u64 size);
#define sta_arena_push_array(arena, type, count) (type*)sta_arena_push((arena), sizeof(type) * (count))
#define sta_arena_push_struct(arena, type)       sta_arena_push_array((arena), type, 1)

struct String
{
  u64   len;
  char* buffer;
};
typedef struct String String;

struct StringArray
{
  String* str;
  u64     len;
  u64     capacity;
};
typedef struct StringArray StringArray;

void                       sta_initString(String* s, const char* msg);
bool                       sta_strncmp(String* s1, String* s2, u64 len);
void                       sta_strncpy(Arena* arena, String* res, String* source, i32 n);
void                       sta_strcpy(Arena* arena, String* res, String* source);
void                       sta_strsplit(Arena* arena, StringArray* res, String* s1, char c);
void                       sta_strrchr(Arena* arena, String* res, String* s1, char c);
void                       sta_strstr(Arena* arena, String* res, String* haystack, String* needle);
void                       sta_strchr(Arena* arena, String* res, String* s1, char c);
void                       sta_strncat(Arena* arena, String* s1, String* s2, u64 n);
void                       sta_strcat(Arena* arena, String* s1, String* s2);
i32                        sta_strcmpi32(String* s1, String* s2);
bool                       sta_strcmp(String* s1, String* s2);
i32                        sta_strncmpi32(String* s1, String* s2, u64 len);

struct Profiler
{
  u64 StartTSC;
  u64 EndTSC;
};
typedef struct Profiler Profiler;

extern Profiler         profiler;
u64                     ReadCPUTimer(void);
u64                     EstimateCPUTimerFreq(void);

void                    initProfiler();
void                    displayProfilingResult();

#define PROFILER 1
#if PROFILER

struct ProfileAnchor
{
  u64         elapsedExclusive;
  u64         elapsedInclusive;
  u64         hitCount;
  u64         processedByteCount;
  char const* label;
};
typedef struct ProfileAnchor ProfileAnchor;

extern ProfileAnchor         globalProfileAnchors[4096];
extern u32                   globalProfilerParentIndex;

struct ProfileBlock
{
  char const* label;
  u64         oldElapsedInclusive;
  u64         startTime;
  u32         parentIndex;
  u32         index;
};
typedef struct ProfileBlock ProfileBlock;
void                        initProfileBlock(ProfileBlock* block, char const* label_, u32 index_, u64 byteCount);
void                        exitProfileBlock(ProfileBlock* block);

#define NameConcat2(A, B) A##B
#define NameConcat(A, B)  NameConcat2(A, B)
#define TimeBandwidth(Name, ByteCount)                                                                                                                                                                 \
  ProfileBlock Name;                                                                                                                                                                                   \
  initProfileBlock(&Name, "Name", __COUNTER__ + 1, ByteCount);
#define ExitBlock(Name)              exitProfileBlock(&Name)
#define TimeBlock(Name)              TimeBandwidth(Name, 0)
#define ProfilerEndOfCompilationUnit static_assert(__COUNTER__ < ArrayCount(GlobalProfilerAnchors), "Number of profile points exceeds size of profiler::Anchors array")
#define TimeFunction                 TimeBlock(__func__)

#else

#define TimeBlock(blockName)
#define TimeFunction
#endif

void sta_parse_float_from_string(float* dest, char* source, u8* length);
void sta_parse_int_from_string(int* dest, char* source, u8* length);

struct Logger
{
  FILE* filePtr;
};
typedef struct Logger Logger;

enum LoggingLevel
{
  LOGGING_LEVEL_INFO,
  LOGGING_LEVEL_WARNING,
  LOGGING_LEVEL_ERROR,
};
typedef enum LoggingLevel LoggingLevel;

void                      sta_log(Logger* logger, LoggingLevel level, String msg);

bool                      sta_initLogger(Logger* logger, String fileName);
bool                      sta_destroyLogger(Logger* logger);

#endif
