## ToDo

## Parser
* make scope an actual stack
* do actual proper error handling while parsing, and figure out what to do with exceptions
* includes

## Tree walker 
* Left side of assignment can be both a Token representing the name and an expression that will evaluate to a memory address
  * this can be solved if we always just refer to an address rather then the name for it. 
  * Can we walk through and preallocate everything on the stack then?
* figure out the symbol situation