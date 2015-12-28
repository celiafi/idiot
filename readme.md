# Introduction

IDIOT (Intelligent Directory Input-Output Transformer) is a cross-platform directory watcher service. Idiot watches directories for file system events (creation, modification or deletion of files) and acts upon these events. For example, Idiot can be configured so that whenever a file is created (on a file system events level; in practice "creating" a file can also mean moving or copying a file into a directory) in a watched directory, the file is automatically zipped, encrypted and moved somewhere else.

# Installation

Idiot requires Java 7. That provided, download the `idiot.jar` and `idiot.properties` files and put them wherever you want. The core of Idiot itself is cross-platform ~~and should work on both Windows and *nix.~~ but due to some hard-coded path separators won't work on *nix.

For encryption mode, however, Axcrypt is Windows-only and hence encrypt mode won't work on *nix (see Configuration). Axcrypt installation is required (but not checked!) for encryption mode.

# Usage

Run with `java -jar idiot.jar` on command line. There are no command line flags. Keep the CLI window open. It is probably possible to run Idiot as a daemon (on *nix) or a service (Windows) as well, but this is untested.

It is possible to have multiple running instances of Idiot at the same time. However, if multiple instances are watching and acting upon the same directories, *thread-safety is not guaranteed*.

## Configuration

Idiot is configured in `idiot.properties` file. The configuration file is parsed on startup, not dynamically loaded. This means that changing the configuration necessitates restarting Idiot.

See example file

Possible modes:
    - encrypt
    - generic
    
MAKE SURE YOU HAVE RIGHTS TO WRITE FILES IN LOG LOCATION

Command: ¤FILE is file name, ¤REMOTE is remote. ¤ is used for parsing command string, so if it is required in command, it may need to be escaped or it might not work at all.

# Building & hacking

No known dependencies. Run-of-the-mill Java build should do.

The codebase contains some unconventional solutions affixed to each other with bubble gum and duct tape.

# Known bugs

# License