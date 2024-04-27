## ToDo

## Features remaining
### IMPROVEMENTS

* unify functions regarding struct fields
* remove setle things
* actively disallow globals and things outside of function declaration
* the parser can just parse functions and structs directly rather then a list of stmt

* movsb rather then some wierd with another register
* Do something with instructions that are just randomly spread out strings
* Do some cleanup with what state that gets sent around to each parser/scanner
* redo the symboltable static getSize struct
* Do lea rax, [0 + rax * structSize] for struct pointer thingy
* Do proper cleanup of error messages
* figure if which rules that don't exist
* sizeof macro :)