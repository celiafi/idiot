# Introduction

IDIOT (Intelligent Directory Input-Output Transformer) is a cross-platform directory watcher service. Idiot watches directories for file system events (creation, modification or deletion of files) and acts upon these events. For example, Idiot can be configured so that whenever a file is created (on a file system events level; in practice "creating" a file can also mean moving or copying a file into a directory) in a watched directory, the file is automatically zipped, encrypted and moved somewhere else.

# Installation

Idiot requires Java 7. That provided, download the `idiot.jar` and `idiot.properties` files and put them wherever you want. The core of Idiot itself is cross-platform ~~and should work on both Windows and *nix.~~ but due to some hard-coded path separators won't work on *nix.

For encryption mode's default behaviour, however, Axcrypt is Windows-only and hence encrypt mode won't work on *nix (see Configuration). Axcrypt installation is required (but not checked!) for encryption mode.

# Usage

Run with `java -jar idiot.jar` on command line. There are no command line flags. Keep the CLI window open. It is probably possible to run Idiot as a daemon (on *nix) or a service (Windows) as well, but this is untested.

It is possible to have multiple running instances of Idiot at the same time. However, if multiple instances are watching and acting upon the same directories, *thread-safety is not guaranteed*.

## Configuration

Idiot is configured in `idiot.properties` file. The configuration file is parsed on startup, not dynamically loaded. This means that changing the configuration necessitates restarting Idiot.

The properties file consists of equality-sign-separated key-value pairs. Backslash is the escape character, and as such all equality-sign literals and literal backspaces must be escaped.

Make sure you have file writing rights in log location. Idiot will most probably get very confused if log location is inaccessible and crash.

### Common properties

#### logLevel

The verbosity of logging. Possible values (standard Java log levels) in increasing verbosity: `SEVERE`, `WARNING`, `INFO`, `CONFIG`, `FINE`, `FINER`, `FINEST`. For daily usage, `INFO` works well.

#### logLocation

The location for log file. Requires access rights. A file, not a directory.

#### mode

Either `encrypt` or `generic`. Encrypt watches for files and directories created in watched directories, encrypts them with Axcrypt using supplied passphrase and moves the encrypted file to a remote directory. If mode is set to `generic`, any commands may be specified for creation, modification and deletion of files and directories.

#### watchDirN

There can be any number of watched directories. These must be indexed by subsequent integers, starting from 1.

#### remoteDirN

There must be exactly same amount of remote directories as there are watched directories. There is one-to-one correspondence between the two. Remote directories with different indices may be the same actual directory. Note that "remote" directory can also be physically a local directory; "remote" only refers to the logical remoteness, a destination, if you will. May be used in commands.

### Encrypt-specific properties

#### passphraseN

The passphrase to be used for encryption for watched directory in index N.

#### encryptedExtension

The extension for already encrypted files. This is dependent on encryption software. For Axcrypt, default is `axx`.

#### encryptionString

The command string used for encryption. Passphrases and file names can be referenced with ¤PASSPHRASE¤ and ¤FILENAME¤.

### Generic-mode specific properties

### createFileCommand

Command to be run on file created events.

### modifyFileCommand

Command to be run on file modified events.

### deleteFileCommand

Command to be run on file deleted events.

### createDirCommand

Command to be run on directory created events.

### modifyDirCommand

Command to be run on directory modified events.

### deleteDirCommand

Command to be run on directory deleted events.

In commands you can refer to the created/deleted/modified file/directory with ¤FILE and to the remote for the directory the event happened in with ¤REMOTE. ¤ is used for parsing command string, so if it is required in command, it may need to be escaped or it might not work at all.

# Building & hacking

No known dependencies. Run-of-the-mill Java build should do.

The codebase contains some unconventional solutions affixed to each other with bubble gum and duct tape.

# Known bugs

No known bugs.

# License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.