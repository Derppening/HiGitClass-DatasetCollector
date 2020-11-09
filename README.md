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

Required Arguments:

- `NUM_TO_FETCH`: Number of repositories to fetch

Optional Arguments:

- `--token`: Specifies the Github Personal Access Token, if any.
    - This may be useful if fetching from a large amount of repositories, as it raises the limit of available requests
- `--query`: Specifies a custom query to execute.
    - Do not use this option to override the sort field or order!
- `--output` Specifies a file to output the JSON dataset to.

## Todo

- [ ] Handling rate-limiting
- [ ] Parallel Fetching Support
