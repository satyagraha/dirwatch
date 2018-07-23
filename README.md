# dirwatch
Scala directory watch utility
## Usage
Command-line options:
- `-d` | `--dir` : directory to watch
- `-g` | `--glob` : glob wildcard pattern for files in directory
- `--` separates options from command string to execute

Command string substitutions:
- `%d` : directory
- `%f` : filename
- `%p` : directory/filename path

An example of command-line usage to monitor a directory for GraphViz _.dot_ file changes:

`-d D:\myDir -g "*.dot" -- D:\software\graphviz-2.38\bin\dot.exe %p -Tpdf -O`

## License
Apache 2.0
