# floodingmap
This is a part of the submission for the Bachelor Project in Software Development at the IT University of Copenhagen.

Authors/Students:
- Marcus Andreas Aandahl (maraa@itu.dk)
- Villads Emil Grum-Schwensen (vilg@itu.dk)
- Andreas Bartholdy Christensen (anbc@itu.dk)

It uses OpenStreetMap and Dataforsyningen data to showcase map features along with terrain height, in order to ultimately visualise floodings.

## Setup
This project uses maven and Java 23.
Furthermore, it requires a few additional steps in order to run the project:
1. Setup of JVM arguments
2. Setup of ENV variables
3. Setup of data files

### Setup of JVM arguments
The following argument is recommended as minimum:
```
-XX:+DisableExplicitGC
```
It disables the overuse of explicit calls to the Garbage Collector (GC), which some Java libraries use too much.

Moreover, it is recommended when running Denmark to add the following JVM arguments:
```
-Xms6g
-Xmx12g
```
This is because, currently the parsing of Denmark takes just under 6GB. However, it runs with around 2GB. This can be tuned according to computer specifications.

Lastly, since explicit GC calls have been disabled, it is possible, but not required, to add the following JVM arguments, which should improve GC performance slightly:
```
-XX:+UnlockExperimentalVMOptions
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m
-XX:G1NewSizePercent=20
-XX:G1MaxNewSizePercent=30
-XX:G1MixedGCCountTarget=8
-XX:G1MixedGCLiveThresholdPercent=85
-XX:+G1UseAdaptiveIHOP
-XX:G1HeapWastePercent=5
```

### Setup of ENV variables
This project requires a minimum of one variable if it should be run with dynamic loading of height curves, which likely will be the usual use case.
This variable is an API token to [dataforsyningen.dk](https://dataforsyningen.dk/).
This token can be obtained by creating an user, and going to [management of tokens](https://dataforsyningen.dk/user#token).
In there, the token can be created. The environement variable can be then added as such:
```
dataForsyningenToken=YOUR_TOKEN
```

### Setup data files
Data files such as `.osm` and `.gml` can be added in the `common` project's resources folder, at the path:
```
./common/src/main/resources/{osm|gml}
```
in the `/osm` and `/gml` folders respectively.
These will be picked up by the program on start, and can be loaded in from there.

The project starts up with whatever file is chosen in the `ui/src/main/java/dk/itu/ui/FloodingApp.java` file at:
```
services.getOsmService(state.isWithDb()).loadOsmData("{start-file}.osm")
```
This line can optionally be removed.

### Optional - Setup of Database
Some code is implemented for using a database. This is however not the main use case, which will more often than not be in-memory.
It can be flaky at times due to the focus put on the in-memory implementation, rendering the database implementation buggy.

Feel free to contact the developers to instructions on how to run with the database.

## Using the application
The usage is fairly straight-forward. There are utilities at the bottom of the screen those including:
- Re-centering of map around the loaded spatial data.
- Re-setting of loaded osm or height curve data.
- Loading of own files.
- API loading of height curves.
- Other visualisation debugging tools for routing and others.

There are also hotkeys at the top of the screen, which can be used for what they are labelled as.