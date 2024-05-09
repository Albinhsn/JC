
#include "scanner.h"
#include "common.h"
#include "stdbool.h"
#include "token.h"
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void error(const char* message)
{
  printf("%s\n", message);
  exit(1);
}

void init_scanner(Scanner* scanner, Arena* arena, String* literal, const char* filename)
{
  scanner->filename = (u8*)filename;
  scanner->index    = 0;
  scanner->line     = 1;
  scanner->input    = literal;
  scanner->arena    = arena;
}

static bool match_substring(String* literal, int index, const char* to_match, int length)
{
  if (literal->len <= index + length)
  {
    return false;
  }
  return strncmp(&literal->buffer[index], to_match, length) == 0;
}

static bool is_out_of_bounds(Scanner* scanner)
{
  return scanner->input->len <= scanner->index;
}

static u8 current_char(Scanner* scanner)
{
  return scanner->input->buffer[scanner->index];
}

static u8 advance(Scanner* scanner)
{
  char out = current_char(scanner);
  scanner->index++;
  return out;
}

static bool match_next(Scanner* scanner, char toMatch)
{
  if (current_char(scanner) == toMatch)
  {
    scanner->index++;
    return true;
  };
  return false;
}

static void skip_whitespace(Scanner* scanner)
{

  while (true)
  {
    if (is_out_of_bounds(scanner))
    {
      return;
    }
    u8 current = current_char(scanner);
    if (isdigit(current) || isalpha(current))
    {
      return;
    }
    switch (current)
    {
    case '/':
    {
      advance(scanner);
      if (match_next(scanner, '/'))
      {
        while (!is_out_of_bounds(scanner) && current_char(scanner) != '\n')
        {
          advance(scanner);
        }
        scanner->line++;
        break;
      }
      scanner->index--;
      return;
    }

    case '\"':
    case '{':
    case '[':
    case ']':
    case '}':
    case '*':
    case ';':
    case '<':
    case '>':
    case '(':
    case ')':
    case '!':
    case '%':
    case '.':
    case '#':
    case '^':
    case ':':
    case ',':
    case '&':
    case '|':
    case '-':
    case '+':
    case '=':
    {
      return;
    }
    case '\n':
    {
      scanner->line++;
    }
    default:
    {
      scanner->index++;
    }
    }
  }
}

static Token* parse_string(Scanner* scanner)
{
  i32 index   = scanner->index;
  u8  current = advance(scanner);

  while (!is_out_of_bounds(scanner) && current != '\"')
  {
    current = advance(scanner);
  }
  if (is_out_of_bounds(scanner))
  {
    error("Unterminated string!");
  }
  String literal = {};
  literal.buffer = (char*)&scanner->input->buffer[index];
  literal.len    = scanner->index - index - 1;

  return create_token(scanner->arena, TOKEN_STRING_LITERAL, literal, scanner->line);
}

static TokenType get_keyword(String literal)
{
  static const char* keywords[]         = {"struct", "string", "if", "else", "for", "while", "return", "short", "int", "long", "double", "float", "byte", "void"};
  static TokenType   tokens[]           = {TOKEN_STRUCT, TOKEN_STRING, TOKEN_IF,   TOKEN_ELSE,   TOKEN_FOR,   TOKEN_WHILE, TOKEN_RETURN,
                                           TOKEN_SHORT,  TOKEN_INT,    TOKEN_LONG, TOKEN_DOUBLE, TOKEN_FLOAT, TOKEN_BYTE,  TOKEN_VOID};
  u32                number_of_keywords = ArrayCount(keywords);
  for (u32 i = 0; i < number_of_keywords; i++)
  {
    if (literal.len == strlen(keywords[i]) && strncmp(keywords[i], literal.buffer, literal.len) == 0)
    {
      return tokens[i];
    }
  }
  return TOKEN_IDENTIFIER;
}

static Token* parse_keyword(Scanner* scanner)
{
  i32 index   = scanner->index - 1;
  u8  current = advance(scanner);
  while (!is_out_of_bounds(scanner) && (isdigit(current) || isalpha(current) || current == '_'))
  {
    current = advance(scanner);
  }

  String literal = {};
  literal.buffer = (char*)&scanner->input->buffer[index];
  scanner->index--;
  literal.len    = scanner->index - index;
  return create_token(scanner->arena, get_keyword(literal), literal, scanner->line);
}

static bool is_valid_hex(char c)
{
  return ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
}

static Token* parse_hex(Scanner* scanner, i32 index)
{
  char current = advance(scanner);
  while (!is_out_of_bounds(scanner) && (isdigit(current) || is_valid_hex(current)))
  {
    current = advance(scanner);
  }
  scanner->index--;
  String literal = {};
  literal.buffer = (char*)&scanner->input->buffer[index];
  literal.len    = scanner->index - index;
  return create_token(scanner->arena, TOKEN_INT_LITERAL, literal, scanner->line);
}

static Token* parse_binary(Scanner* scanner, i32 index)
{
  char current = advance(scanner);
  while (!is_out_of_bounds(scanner) && (current == '0' || current == '1'))
  {
    current = advance(scanner);
  }
  scanner->index--;
  String literal = {};
  literal.buffer = (char*)&scanner->input->buffer[index];
  literal.len    = scanner->index - index;
  return create_token(scanner->arena, TOKEN_INT_LITERAL, literal, scanner->line);
}

static Token* parse_number(Scanner* scanner)
{
  i32       index   = scanner->index - 1;
  TokenType type    = TOKEN_INT_LITERAL;

  char      current = advance(scanner);

  if (scanner->input->buffer[index] == '0')
  {
    if (current == 'x')
    {
      return parse_hex(scanner, index);
    }
    else if (current == 'b')
    {
      return parse_binary(scanner, index);
    }
  }

  while (!is_out_of_bounds(scanner) && isdigit(current))
  {
    current = advance(scanner);
  }

  if (current == '.')
  {
    current = current_char(scanner);
    type    = TOKEN_FLOAT_LITERAL;

    while (!is_out_of_bounds(scanner) && isdigit(current))
    {
      current = advance(scanner);
    }
  }
  scanner->index--;
  String literal = {};
  literal.buffer = (char*)&scanner->input->buffer[index];
  literal.len    = scanner->index - index;
  return create_token(scanner->arena, type, literal, scanner->line);
}

Token* parse_token(Scanner* scanner)
{
  skip_whitespace(scanner);
  if (is_out_of_bounds(scanner))
  {
    String literal = {};
    literal.buffer = "EOF";
    literal.len    = 3;
    return create_token(scanner->arena, TOKEN_EOF, literal, scanner->line);
  }

  char current = advance(scanner);

  if (isalpha(current))
  {
    return parse_keyword(scanner);
  }

  if (isdigit(current))
  {
    return parse_number(scanner);
  }

  switch (current)
  {
  case '{':
  {
    String literal = {};
    literal.buffer = "{";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_LEFT_BRACE, literal, scanner->line);
  }
  case '}':
  {
    String literal = {};
    literal.buffer = "}";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_RIGHT_BRACE, literal, scanner->line);
  }
  case '[':
  {
    String literal = {};
    literal.buffer = "[";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_LEFT_BRACKET, literal, scanner->line);
  }
  case ']':
  {
    String literal = {};
    literal.buffer = "]";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_RIGHT_BRACKET, literal, scanner->line);
  }
  case ';':
  {
    String literal = {};
    literal.buffer = ";";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_SEMICOLON, literal, scanner->line);
  }
  case '%':
  {
    String literal = {};
    literal.buffer = "%";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_MOD, literal, scanner->line);
  }
  case ',':
  {
    String literal = {};
    literal.buffer = ",";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_COMMA, literal, scanner->line);
  }
  case '(':
  {
    String literal = {};
    literal.buffer = "(";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_LEFT_PAREN, literal, scanner->line);
  }
  case ')':
  {
    String literal = {};
    literal.buffer = ")";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_RIGHT_PAREN, literal, scanner->line);
  }
  case '/':
  {
    String literal = {};
    if (match_next(scanner, '='))
    {
      literal.buffer = "/=";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_AUGMENTED_SLASH, literal, scanner->line);
    }
    literal.buffer = "/";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_SLASH, literal, scanner->line);
  }
  case '*':
  {
    String literal = {};
    if (match_next(scanner, '='))
    {
      literal.buffer = "*=";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_AUGMENTED_STAR, literal, scanner->line);
    }
    literal.buffer = "*";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_STAR, literal, scanner->line);
  }
  case '&':
  {
    String literal = {};
    if (match_next(scanner, '='))
    {
      literal.buffer = "&=";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_AUGMENTED_AND, literal, scanner->line);
    }
    else if (match_next(scanner, '&'))
    {
      literal.buffer = "&&";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_AND_LOGICAL, literal, scanner->line);
    }

    literal.buffer = "&";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_AND_BIT, literal, scanner->line);
  }
  case '|':
  {
    String literal = {};
    if (match_next(scanner, '='))
    {
      literal.buffer = "|=";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_AUGMENTED_OR, literal, scanner->line);
    }
    else if (match_next(scanner, '|'))
    {
      literal.buffer = "||";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_OR_LOGICAL, literal, scanner->line);
    }

    literal.buffer = "|";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_OR_BIT, literal, scanner->line);
  }
  case '!':
  {
    String literal = {};
    if (match_next(scanner, '='))
    {
      literal.buffer = "!=";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_BANG_EQUAL, literal, scanner->line);
    }
    literal.buffer = "!";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_BANG, literal, scanner->line);
  }
  case '>':
  {
    String literal = {};
    if (match_next(scanner, '='))
    {
      literal.buffer = ">=";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_GREATER_EQUAL, literal, scanner->line);
    }
    else if (match_next(scanner, '>'))
    {
      literal.buffer = ">>";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_SHIFT_RIGHT, literal, scanner->line);
    }
    literal.buffer = ">";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_GREATER, literal, scanner->line);
  }
  case '-':
  {
    String literal = {};
    if (match_next(scanner, '-'))
    {
      literal.buffer = "--";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_DECREMENT, literal, scanner->line);
    }
    else if (match_next(scanner, '='))
    {
      literal.buffer = "-=";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_AUGMENTED_MINUS, literal, scanner->line);
    }
    literal.buffer = "-";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_MINUS, literal, scanner->line);
  }
  case '+':
  {
    String literal = {};
    if (match_next(scanner, '+'))
    {
      literal.buffer = "++";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_INCREMENT, literal, scanner->line);
    }
    else if (match_next(scanner, '='))
    {
      literal.buffer = "-=";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_AUGMENTED_PLUS, literal, scanner->line);
    }
    literal.buffer = "+";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_PLUS, literal, scanner->line);
  }
  case '=':
  {
    String literal = {};
    if (match_next(scanner, '='))
    {
      literal.buffer = "==";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_EQUAL_EQUAL, literal, scanner->line);
    }
    literal.buffer = "=";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_EQUAL, literal, scanner->line);
  }
  case '<':
  {
    String literal = {};
    if (match_next(scanner, '='))
    {
      literal.buffer = "<=";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_LESS_EQUAL, literal, scanner->line);
    }
    else if (match_next(scanner, '<'))
    {
      literal.buffer = "<<";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_SHIFT_LEFT, literal, scanner->line);
    }
    literal.buffer = "<";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_LESS, literal, scanner->line);
  }
  case '^':
  {
    String literal = {};
    if (match_next(scanner, '='))
    {
      literal.buffer = "^=";
      literal.len    = 2;
      return create_token(scanner->arena, TOKEN_AUGMENTED_XOR, literal, scanner->line);
    }
    literal.buffer = "^";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_XOR, literal, scanner->line);
  }
  case '.':
  {
    String literal      = {};
    int    match_length = 2;
    if (match_substring(scanner->input, scanner->index, "..", match_length))
    {
      scanner->index += match_length;
      literal.buffer = "...";
      literal.len    = 3;
      return create_token(scanner->arena, TOKEN_ELLIPSIS, literal, scanner->line);
    }
    literal.buffer = ".";
    literal.len    = 1;
    return create_token(scanner->arena, TOKEN_DOT, literal, scanner->line);
  }
  case '\"':
  {
    return parse_string(scanner);
  }
  case '#':
  {
    String literal = {};
    if (match_substring(scanner->input, scanner->index, "extern", 6))
    {
      scanner->index += 6;
      literal.buffer = "#extern";
      literal.len    = 7;
      return create_token(scanner->arena, TOKEN_EXTERN, literal, scanner->line);
    }
    else if (match_substring(scanner->input, scanner->index, "include", 7))
    {
      scanner->index += 7;
      literal.buffer = "#include";
      literal.len    = 8;
      return create_token(scanner->arena, TOKEN_INCLUDE, literal, scanner->line);
    }
    else if (match_substring(scanner->input, scanner->index, "define", 6))
    {
      scanner->index += 6;
      literal.buffer = "#define";
      literal.len    = 7;
      return create_token(scanner->arena, TOKEN_DEFINE, literal, scanner->line);
    }
  }
  default:
  {
    printf("%c %d\n", scanner->input->buffer[scanner->index], scanner->input->buffer[scanner->index]);
    error("Illegal character!");
  }
  }
  return 0;
}
