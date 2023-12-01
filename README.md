# QuPath extension for stitching

This repo contains a basic extension for [QuPath](https://qupath.github.io) which can perform some of the same basic stitching functions as
Grid/Collection stitching in Fiji. Namely, it will be able to either use the TileConfiguration.txt file within a folder
of tiles, use the positional metadata in the tile file name, or use the metadata stored within the .tif file, as
demonstrated in the original stitching script written by Pete Bankhead that started all of this, [found here](https://gist.github.com/petebankhead/b5a86caa333de1fdcff6bdee72a20abe).

> The three options are available by dropdown when you want to begin stitching. Later versions may test each folder for which method should be applied, if there is enough interest in that.
> 

## Using the extension

If you want to, building the extension yourself with Gradle should be pretty easy - you don't even need to install Gradle separately, because the 
[Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) will take care of that.

Open a command prompt, navigate to where the code lives, and use
```bash
gradlew build
```

The built extension should be found inside `build/libs`.
You can drag this onto QuPath to install it.
You'll be prompted to create a user directory if you don't already have one.

Alternatively, download the jar file from the Releases page, and drag and drop it into an active QuPath window, or place
it in the extensions directory for the appropriate version of QuPath.

## How to use the plugin
TODO visual of GUI here

1. Rough description of steps
2. Visual of folder structure expected
3. Discussion of the three types of files expected
4. warning about pixel size metadata being missing from certain stitching methods

## Getting help

For questions about QuPath and/or creating new extensions, please use the forum at https://forum.image.sc/tag/qupath

------

## License
