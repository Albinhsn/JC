## ToDo

## Features remaining

* macros
  * just #define and then process them during parsing?
* array
  * stack based 
* string and byte
* support Foo** foo;

## Bugs
### Compiler
* support recursive structures

### Parser
* allow int foo; Foo foo;
  * this is simplified if parser also knows about struct declarations
* allow Foo * foo and a * b
* do actual proper error handling while parsing, and figure out what to do with exceptions

### Optimizations
* when doing a binary check if both are loads
  * that means we can optimize away some push/pop
