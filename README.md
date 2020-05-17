# AWS S3 Liquibase Extension
[![Build Status](https://travis-ci.org/miron4dev/liquibase-s3.svg?branch=develop)](https://travis-ci.org/miron4dev/liquibase-s3)

A liquibase extension adding integration with AWS S3


## Integration

### Add dependency:

```xml
<dependency>
    <groupId>com.miron4dev</groupId>
    <artifactId>liquibase-s3</artifactId>
    <version>${liquibase-s3.version}</version>
</dependency>
```

### Changeset example:

```xml
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:s3="http://www.liquibase.org/xml/ns/dbchangelog-ext/s3"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.3.xsd">

    <changeSet id="insert some data" author="miron4dev">
        <s3:s3File path="s3://miron-test/data/dummy.sql"/>
    </changeSet>
</databaseChangeLog>
```

## Implemented Changes:

### s3File
The `s3File` tag allows you to specify any sql statements and have it stored on AWS S3 in a file.

| Attribute name  | Description | Default value |
| ------------- | ------------- | ------------- |
| path  | Path to S3 sql file. It must have the following format: s3://some_bucket_name/path/to/change.sql  | It must be explicitly defined |
| stripComments  | Set whether SQL should be split into multiple statements.  | true |
| splitStatements  | Set whether comments should be stripped from the SQL before passing it to the database. | false |
| encoding  | The encoding of the file containing SQL statements. | UTF-8 |
| endDelimiter  | The end delimiter used to split statements. | null |
| dbms  | The DBMS for whose SQL dialect the statement is to be made  | null |
