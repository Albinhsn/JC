## ToDo

## Features remaining

* array
* index
* includes
* string and byte

### Bugs
* line number of every expr/stmt
* some sort of features to track back through quads to find the actual name of the thing we operate on
* do actual proper error handling while parsing, and figure out what to do with exceptions
* support recursive structures
* can assign a float to and int even though it will be treated as a float afterwards
* figure out if we store every floating point value inside 
* a lot of buggyness and awkwardsness with float vs int conversions, figure out format/schema for it

### Optimizations
* when doing a binary check if both are loads
  * that means we can optimize away some push/pop
