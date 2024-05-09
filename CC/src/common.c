#include "common.h"
#include <ctype.h>
#include <stdio.h>
#include <string.h>
#include <sys/time.h>
#include <x86intrin.h>


/*
 =========================================
 =========================================
                 ARENA
 =========================================
 =========================================
*/

u64 sta_arena_push(Arena* arena, u64 size)
{
  if (arena->ptr + size > arena->maxSize)
  {
    printf("Over max size! %ld %ld %ld\n", size, arena->ptr, arena->maxSize);
    return 0;
  }
  u64 out = arena->memory + arena->ptr;
  arena->ptr += size;
  return out;
}
void sta_arena_pop(Arena* arena, u64 size)
{
  arena->ptr -= size;
}

/*
 =========================================
 =========================================
                PROFILER
 =========================================
 =========================================
*/

Profiler      profiler;
u32           globalProfilerParentIndex = 0;
ProfileAnchor globalProfileAnchors[4096];

void          initProfileBlock(ProfileBlock* block, char const* label_, u32 index_, u64 byteCount)
{
  block->parentIndex         = globalProfilerParentIndex;

  block->index               = index_;
  block->label               = label_;

  ProfileAnchor* profile     = globalProfileAnchors + block->index;
  block->oldElapsedInclusive = profile->elapsedInclusive;
  profile->processedByteCount += byteCount;

  globalProfilerParentIndex = block->index;
  block->startTime          = ReadCPUTimer();
}
void exitProfileBlock(ProfileBlock* block)
{
  u64 elapsed               = ReadCPUTimer() - block->startTime;
  globalProfilerParentIndex = block->parentIndex;

  ProfileAnchor* parent     = globalProfileAnchors + block->parentIndex;
  ProfileAnchor* profile    = globalProfileAnchors + block->index;

  parent->elapsedExclusive -= elapsed;
  profile->elapsedExclusive += elapsed;
  profile->elapsedInclusive = block->oldElapsedInclusive + elapsed;
  ++profile->hitCount;

  profile->label = block->label;
}

static void PrintTimeElapsed(ProfileAnchor* Anchor, u64 timerFreq, u64 TotalTSCElapsed)
{

  f64 Percent = 100.0 * ((f64)Anchor->elapsedExclusive / (f64)TotalTSCElapsed);
  printf("  %s[%lu]: %lu (%.2f%%", Anchor->label, Anchor->hitCount, Anchor->elapsedExclusive, Percent);
  if (Anchor->elapsedInclusive != Anchor->elapsedExclusive)
  {
    f64 PercentWithChildren = 100.0 * ((f64)Anchor->elapsedInclusive / (f64)TotalTSCElapsed);
    printf(", %.2f%% w/children", PercentWithChildren);
  }
  if (Anchor->processedByteCount)
  {
    f64 mb             = 1024.0f * 1024.0f;
    f64 gb             = mb * 1024.0f;

    f64 seconds        = Anchor->elapsedInclusive / (f64)timerFreq;
    f64 bytesPerSecond = Anchor->processedByteCount / seconds;
    f64 mbProcessed    = Anchor->processedByteCount / mb;
    f64 gbProcessed    = bytesPerSecond / gb;

    printf(" %.3fmb at %.2fgb/s", mbProcessed, gbProcessed);
  }
  printf(")\n");
}
static u64 GetOSTimerFreq(void)
{
  return 1000000;
}

static u64 ReadOSTimer(void)
{
  struct timeval Value;
  gettimeofday(&Value, 0);

  u64 Result = GetOSTimerFreq() * (u64)Value.tv_sec + (u64)Value.tv_usec;
  return Result;
}

u64 ReadCPUTimer(void)
{

  return __rdtsc();
}

#define TIME_TO_WAIT 100

u64 EstimateCPUTimerFreq(void)
{
  u64 OSFreq     = GetOSTimerFreq();

  u64 CPUStart   = ReadCPUTimer();
  u64 OSStart    = ReadOSTimer();
  u64 OSElapsed  = 0;
  u64 OSEnd      = 0;
  u64 OSWaitTime = OSFreq * TIME_TO_WAIT / 1000;
  while (OSElapsed < OSWaitTime)
  {
    OSEnd     = ReadOSTimer();
    OSElapsed = OSEnd - OSStart;
  }

  u64 CPUEnd     = ReadCPUTimer();
  u64 CPUElapsed = CPUEnd - CPUStart;

  return OSFreq * CPUElapsed / OSElapsed;
}
#undef TIME_TO_WAIT

void initProfiler()
{
  profiler.StartTSC = ReadCPUTimer();
}

void displayProfilingResult()
{
  u64 endTime      = ReadCPUTimer();
  u64 totalElapsed = endTime - profiler.StartTSC;
  u64 cpuFreq      = EstimateCPUTimerFreq();

  printf("\nTotal time: %0.4fms (CPU freq %lu)\n", 1000.0 * (f64)totalElapsed / (f64)cpuFreq, cpuFreq);
  for (u32 i = 0; i < ArrayCount(globalProfileAnchors); i++)
  {
    ProfileAnchor* profile = globalProfileAnchors + i;

    if (profile->elapsedInclusive)
    {
      PrintTimeElapsed(profile, cpuFreq, totalElapsed);
    }
  }
}

/*
 =========================================
 =========================================
                COMMON
 =========================================
 =========================================
*/

void sta_parse_int_from_string(int* dest, char* source, u8* length)
{
  char number[32];
  memset(number, 0, 32);

  for (int i = 0; i < 32; i++)
  {
    number[i] = 0;
  }
  u8 pos = 0;
  while (isdigit(source[pos]))
  {
    pos++;
  }
  memcpy(number, source, pos);
  *dest   = atoi(number);
  *length = pos;
}

void sta_parse_float_from_string(float* dest, char* source, u8* length)
{
  char number[32];
  u8   pos = 0;
  while (source[pos] != ' ')
  {
    pos++;
  }
  memcpy(number, source, pos);
  *dest   = atof(number);
  *length = pos;
}


/*
 =========================================
 =========================================
                LOGGING
 =========================================
 =========================================
*/

static inline void sta_writeToLogFile(Logger* logger, String msg)
{
  if (logger && logger->filePtr)
  {
    fprintf(logger->filePtr, "%.*s\n", (i32)msg.len, msg.buffer);
  }
}

static inline void sta_sendLogMessage(Logger* logger, String msg, char* color)
{
  fprintf(stderr, "%s%.*s\n", color, (i32)msg.len, msg.buffer);
  sta_writeToLogFile(logger, msg);
}

void sta_log(Logger* logger, LoggingLevel level, String msg)
{
  switch (level)
  {
  case LOGGING_LEVEL_INFO:
  {
    sta_sendLogMessage(logger, msg, ANSI_COLOR_GREEN);
    break;
  }
  case LOGGING_LEVEL_WARNING:
  {
    sta_sendLogMessage(logger, msg, ANSI_COLOR_YELLOW);
    break;
  }
  case LOGGING_LEVEL_ERROR:
  {
    sta_sendLogMessage(logger, msg, ANSI_COLOR_RED);
    break;
  }
  }
  printf(ANSI_COLOR_RESET);
}

bool sta_initLogger(Logger* logger, String fileName)
{
  char file[256];
  strncpy(file, fileName.buffer, fileName.len);
  logger->filePtr = fopen(file, "a");
  return logger->filePtr != 0;
}

bool sta_destroyLogger(Logger* logger)
{
  return fclose(logger->filePtr);
}

/*
 =========================================
 =========================================
                STRING 
 =========================================
 =========================================
*/

void sta_initString(String* s, const char* msg)
{
  s->buffer = (char*)msg;
  s->len    = strlen(msg);
}
void sta_strInit(Arena* arena, String* s1, const char* str)
{
  u64    len = strlen(str);
  String tmp = (String){.len = len, .buffer = (char*)str};
  sta_strcpy(arena, s1, &tmp);
}

bool sta_strncmp(String* s1, String* s2, u64 len)
{
  u64 minLen = s1->len < s2->len ? s1->len : s2->len;
  return minLen >= len && strncmp(s1->buffer, s2->buffer, len) == 0;
}

void sta_strncpy(Arena* arena, String* res, String* source, i32 n)
{
  res->len    = n;
  res->buffer = sta_arena_push_array(arena, char, n);
  memset(res->buffer, 0, n);
  memcpy(res->buffer, source->buffer, n);
}

void sta_strcpy(Arena* arena, String* res, String* source)
{
  res->len    = source->len;
  res->buffer = sta_arena_push_array(arena, char, source->len);
  memcpy(res->buffer, source->buffer, source->len);
}

void sta_strsplit(Arena* arena, StringArray* res, String* s1, char c)
{
  res->str      = sta_arena_push_struct(arena, String);
  res->len      = 0;
  res->capacity = 1;

  u64 prevIdx   = 0;
  for (u64 idx = 0; idx < s1->len; idx++)
  {
    if (s1->buffer[idx] == c)
    {
      if (prevIdx == idx)
      {
        prevIdx = idx;
        continue;
      }
      (void)sta_arena_push_struct(arena, String);
      res->str[res->len] = (String){.len = idx - prevIdx, .buffer = &s1->buffer[prevIdx]};
      res->len++;
      res->capacity++;

      prevIdx = idx + 1;
    }
  }
  String tmp = (String){.len = s1->len - prevIdx, .buffer = &s1->buffer[prevIdx]};
  sta_strncpy(arena, &res->str[res->len], &tmp, s1->len - prevIdx);
  res->len++;
  res->capacity++;
}

void sta_strrchr(Arena* arena, String* res, String* s1, char c)
{
  for (u64 idx = s1->len - 1; idx >= 0; idx--)
  {
    if (s1->buffer[idx] == c)
    {
      res->len    = s1->len - idx;
      res->buffer = sta_arena_push_array(arena, char, res->len);
      return;
    }
  }
  res->buffer = 0;
}

void sta_strstr(Arena* arena, String* res, String* haystack, String* needle)
{
  String tmp;
  tmp.len    = haystack->len;
  tmp.buffer = haystack->buffer;
  for (u64 idx = 0; idx < haystack->len - needle->len; idx++)
  {
    if (sta_strncmp(&tmp, needle, needle->len))
    {
      sta_strcpy(arena, res, &tmp);
      return;
    }
    tmp.len--;
    tmp.buffer++;
  }
  res->buffer = 0;
}

void sta_strchr(Arena* arena, String* res, String* s1, char c)
{
  for (u64 idx = 0; idx < s1->len; idx++)
  {
    if (s1->buffer[idx] == c)
    {
      res->len    = idx + 1;
      res->buffer = sta_arena_push_array(arena, char, res->len);
      return;
    }
  }
  res->buffer = 0;
}
void sta_strncat(Arena* arena, String* s1, String* s2, u64 n)
{
  u64 prevLen = s1->len;
  s1->len += n;
  char* prevBuffer = s1->buffer;
  s1->buffer       = sta_arena_push_array(arena, char, s1->len);
  memcpy(s1->buffer, prevBuffer, prevLen);
}

void sta_strcat(Arena* arena, String* s1, String* s2)
{
  u64 prevLen = s1->len;
  s1->len += s2->len;
  char* prevBuffer = s1->buffer;
  s1->buffer       = sta_arena_push_array(arena, char, s1->len);
  memcpy(s1->buffer, prevBuffer, prevLen);
}

i32 sta_strcmpi32(String* s1, String* s2)
{
  u64 minLen = s1->len < s2->len ? s1->len : s2->len;
  i32 cmp    = strncmp(s1->buffer, s2->buffer, minLen);
  return cmp != 0 ? cmp : (s1->len > s2->len ? 1 : -1);
}

bool sta_strcmp(String* s1, String* s2)
{
  u64 minLen = s1->len < s2->len ? s1->len : s2->len;
  return s1->len == s2->len && strncmp(s1->buffer, s2->buffer, minLen) == 0;
}

i32 sta_strncmpi32(String* s1, String* s2, u64 len)
{
  return strncmp(s1->buffer, s2->buffer, len);
}
