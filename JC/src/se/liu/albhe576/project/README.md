## ToDo

## Parser
* do actual proper error handling while parsing, and figure out what to do with exceptions
* includes

## Compiler
* just rework how we manage structs and type, functions and signatures 
  * symbols especially
* float and byte :)
* pointers
* array
* index

### Bugs
* support recursive structures
* when doing a binary check if both are loads
  * that means we can optimize away some push/pop
* can't reassign struct int etc
* fix a check whether a function has returned or not?
* get signatures prior to compiling functions?
* we don't catch mismatched arg size?
* which registers do we need to grab after a condition check
* can redeclare variable if it's arg?
* actually test every instruction
* insert ret if not there?




