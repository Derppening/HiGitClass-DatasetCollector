# Dataset Collector for HiGitClass

A simple application to collect Github repositories as input datasets of HiGitClass.

## Getting Started

These instructions will set the project up for use or development.

### Build Prerequisites

- Java Development Kit 11+

### Runtime Prerequisites

- Java Runtime Environment 11+

### Building the Application

At the root of the project directory, run the following command:

```
./gradlew shadowJar
```

A packaged JAR will be generated in `build/libs`.

### Running the Application

```
java -jar build/libs/HiGitClass-DatasetCollector.jar
# or
cd build/libs && java -jar HiGitClass-DatasetCollector.jar
```

Run `java -jar HiGitClass-DatasetCollector.jar --help` for explanation of options, or read below.

## Command-Line Options

There are three subcommands supported by this application.

### Fetching Repositories (`fetch`)

This subcommand fetches a list of repositories from GitHub based on a given query.

Required Arguments:

- `NUM_TO_FETCH`: Number of repositories to fetch

Optional Arguments:

- `--token`: Specifies the Github Personal Access Token, if any.
    - This may be useful if fetching from a large amount of repositories, as it raises the limit of available requests
- `--query`: Specifies a custom query to execute.
    - Do not use this option to override the sort field or order!
- `--output` Specifies a file to output the JSON dataset to.
- `--parallel`: Download README and repo topics in parallel
- `--pretty`: Output pretty JSON

### Document Format Transformation (`transform-doc`)

This subcommand transforms the `output.json` emitted by the `fetch` command to the format accepted by HiGitClass, which 
is a list of JSON objects delimited by newlines.

Required Arguments:

- `--input`: The input file emitted by `fetch`
- `--output`: The location to the output file

### Dataset Format Transformation (`transform-dataset`)

This subcommand outputs `dataset.txt` and `labels.txt` based on the given *transformed* dataset.

Required Arguments:

- `--input`: The path to the input *transformed* JSON
    - Note that the file must be transformed (by running through `transform-doc`)

Optional Arguments:

- `--output`: The directory to output `dataset.txt` and `labels.txt`
