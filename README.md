# homework

Read or serve records through a cli interface

## Installation

clone https://github.com/hxegon/homework, dependencies will be downloaded as they are needed

    git clone https://github.com/hxegon/homework.git

## Usage

    clj -M:run-m <subcommand> <options>
    
### Subcommands

`read` takes one or more file paths through -f (one delimiter for all files) or -F (one delimiter specified per file) flags, reads them and prints a table of the collected records, and any associated errors

`api` runs a server on port 3000 with the following endpoints:

`/records` - GET an unordered list of records or POST a single record with `delimiter` and `data` keys in the body  
`delimiter` must be one of 'pipe', 'space', or 'comma' and specifies which regular expression (see: hxegon.homework.person/delimiters) will be used to break up the string in `data`

`/records/name` - GET a list of people sorted by first + last name (ascending)  
`/records/gender` - GET a list of people sorted by gender (female then male, then by last name ascending)  
`/records/birthdate` - GET a list of people sorted by date of birth (ascending)

`debug` pretty prints the initial application state map

### Options

Use the `-h` flag to see a detailed description of the options

### Examples

read resources/test-file.txt, silence errors and sort by birthdate

    clj -M:run-m read -f resources/test-file.txt -S -s birthdate
    
start the api server

    clj -M:run-m api

## Development

Run the project directly:

    $ clj -M:run-m -h
    <usage message>

Build an executable jar:

    $ echo "ensure there's a 'classes' folder"
    $ mkdir classes
    $ echo "compile main class"
    $ clj -M -e "(compile 'hxegon.homework)"
    $ uberdeps/package.sh

Run that uberjar:

    $ java -jar target/homework.jar <arguments>

Run all tests:

    $ bin/kaocha

Generate coverage files, show coverage:

    $ bin/kaocha --plugin cloverage

### Bugs

- [ ] `api` currently accepts any options even if they silently do nothing
- [ ] `read` doesn't print errors to STDERR
- [x] When using the compiled JAR, jetty announces logging even if not using the api subcommand

### TODO

Not an exhaustive list, just standouts.

- [x] enable `read` to handle files with different delimiters
## License

Copyright © 2021 Cooperlebrun

Distributed under the Eclipse Public License version 1.0.
