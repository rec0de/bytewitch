# Bundled Kaitai Structs

This directory contains Kaitai Struct definitions that are bundled with the application.

## Usage
To use these definitions, you need to include the name of the file without the `.ksy` extension in the code.
The list is available in the file `src/jsMain/kotlin/main.kt`.

Filename: `src/jsMain/resources/kaitai/your_file.ksy`
```kotlin
val bundledKaitaiStructs = listOf<String>("your_file", "another_file")
```
