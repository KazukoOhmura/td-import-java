# Treasure Data Bulk Import Tool in Java

## Overview

Many web/mobile applications generate huge amount of event logs (c,f. login,
logout, purchase, follow, etc).  Analyzing these event logs can be quite
valuable for improving services.  However, analyzing these logs easily and 
reliably is a challenging task.

Treasure Data Cloud solves the problem by having: easy installation, small 
footprint, plugins reliable buffering, log forwarding, the log analyzing, etc.

  * Treasure Data website: [http://treasure-data.com/](http://treasure-data.com/)
  * Treasure Data GitHub: [https://github.com/treasure-data/](https://github.com/treasure-data/)

**td-bulk-import-java** is a Java library, to access Treasure Data Cloud from Java application.

## Requirements

Java >= 1.6

## Install

### Install from GitHub repository

You can get latest source code using git.

    $ git clone https://github.com/treasure-data/td-bulk-import-java.git
    $ cd td-bulk-import-java
    $ mvn package

You will get the td-client jar file in td-bulk-import-java/target 
directory.  File name will be td-client-${td-bulk-import.version}-jar-with-dependencies.jar.
For more detail, see pom.xml.

## Quickstart

### 'prepare_parts' for bulk import

    $ java com.treasure_data.bulk_import.Main prepare_parts ./in/*.csv \
        -Dtd.bulk_import.prepare_parts.format=csv \
        -Dtd.bulk_import.prepare_parts.output_dir=./out/

### 'upload_parts' for bulk import

    $ java com.treasure_data.bulk_import.Main upload_parts ./out/*.msgpack.gz \
        -td.bulk_import.upload_parts.upload-parallel=4

### 'prepare_parts' and 'upload_parts' for bulk import

    $ java com.treasure_data.bulk_import.Main upload_parts ./in/*.csv \
        -Dtd.bulk_import.prepare_parts.format=csv \
        -Dtd.bulk_import.prepare_parts.output_dir=./out/
        -td.bulk_import.upload_parts.upload-parallel=4

## License

Apache License, Version 2.0

