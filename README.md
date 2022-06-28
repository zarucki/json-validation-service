# Engineering technical test instructions - JSON validation service

Task description [here](task-description.md)

## How to run

To run server:
```bash
sbt run
```

Implementation only uses file system to store schema files. By default schema-root directory is created in project directory.

To override root directory you can use environment variable:
```bash
export SCHEMA_ROOT_PATH="./some/other/path"
```

To run tests:
```bash
sbt test
```

While server is running you can run bash script with example curls:
```bash
./runTestScenario.sh
```