# Defcode recognizer with RosSvyaz import tool

This tool is implemented to recognize mobile numbers with defcode base (in CSV-file).

## Defcode database file

Defcode database file is a simple CSV-file (with ; as delimeters) with fields listed below.

```
<Range start value>;<Range finish value>;<Operator name>;<Region>
```

## Numbers file

Numbers file is a CSV-file with numbers to recognize. File format:

```
<number>;<payload1>;<payload2>;....;<payloadn>
```

## Usage

You can compile tool and use it with such parameters:

```
dist/defcode_recognizer.jar <Path to csv-file with defcodes> <number of threads> <path to csv-file with numbers>
```

For example:

```
$ java -jar dist/defcode_recognizer.jar tmp/def3_final.csv 8 tmp/numbers2.csv > out2.csv
```

File out2.csv will containt recognized values in format:

```
<number>;<Range start value>;<Range finish value>;<Operator name>;<Region>;<payload1>;<payload2>;....;<payloadn>
```

or 

```
<number>;;;;;<payload1>;<payload2>;....;<payloadn>
```

if no range found

## Creating CSV-file with defcodes for Russian mobile operators

There is a web-page with all ranges for Russian mobile cellular operators: http://www.rossvyaz.ru/docs/articles/DEF-9x.html

So, you can convert this page to a defcode csv-file.

```
$ ./import_rossvyaz_defcodes.sh
```

After script completion you can find file ```def3.csv``` in current directory.

Note: this script uses bash, wget, sed, grep, awk. So, you've to install these programs.
