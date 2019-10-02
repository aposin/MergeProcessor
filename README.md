# MergeProcessor Documentation
[![Build Status](https://travis-ci.org/aposin/MergeProcessor.svg?branch=master)](https://travis-ci.org/aposin/MergeProcessor)
[![codecov](https://codecov.io/gh/aposin/MergeProcessor/branch/master/graph/badge.svg)](https://codecov.io/gh/aposin/MergeProcessor)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/294e06969c674d16a08788d06d038665)](https://www.codacy.com/app/aposin-bot/MergeProcessor?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=aposin/MergeProcessor&amp;utm_campaign=Badge_Grade)
![GitHub top language](https://img.shields.io/github/languages/top/aposin/MergeProcessor.svg)
[![CLA assistant](https://cla-assistant.io/readme/badge/aposin/MergeProcessor)](https://cla-assistant.io/aposin/MergeProcessor)
[![GitHub](https://img.shields.io/github/license/aposin/MergeProcessor.svg)](https://github.com/aposin/MergeProcessor/blob/master/LICENSE)

## Introduction
The MergeProcessor is a tool to automatically merge svn changes from one branch to another.
One part of the MergeProcessor is running on the server the other one on the developers machine.

Everytime a developer commits a change to the svn repository the post-commit hook starts a script on the server.
This script analyses the commit and checks a file with merge rules if this commit should get merged into another branch.
If so the script creates a merge unit with all the commit and merge informations.
These merge units are stored on the server. Each user has its own folder with four subfolders: `todo`, `done`, `ignored` and `canceled`.
All new merge units are placed in the `todo` folder. E.g.: `/var/subversion/mergetrigger/merges/<USER_ID>/todo/<REPOSITORY_NAME>_<USER_ID>_r<REVISION_NUMBER>_<DATE>_<TIME>.merge`
The client-side MergeProcessor connects via SSH to the server and looks for merge units in the `todo` folder of the user.
The user can then start the merge process and the changes will be merged to the next branch. The corresponding merge unit will then be moved to the `done` folder.
If the developer decides that a merge unit must not be merge the developer can ignore this merge unit and it will be moved to the `ignored` folder.

## Setup & Contribution

See `CONTRIBUTING.md` and follow the instructions of the document.

## Server-Side

### Prerequisites | Server-Side
*  Linux (tested with 'SUSE Linux Enterprise Server 11 (x86_64)')
*  SSH access for every developer
*  running Apache Subversion repository (tested with SVN version 1.8)
*  Perl (tested with version 5.10)

### Installation | Server-Side

#### NOTE
This repository only contains the client part of the whole merge process. The client checks for new merge units on the server and executes them
on the local machine where it is installed.

#### Installation Steps
*  Place the content of the `server` folder under the path `/var/subversion/mergetrigger` of the subversion server.
*  Change the owner and group of the folder `/var/subversion/mergetrigger` and all of it's files so that the subversion process (e.g. `wwwrun:www`) can read, write files and execute the scripts.
*  Add the content of `server/scripts/post-commit` to the post-commit hook of the repository. E.g.: `/var/subversion/repositories/<REPOSITORY_NAME>/hooks/post-commit`
*  Create a cron job which periodically deletes merge units older than X days in the `done` and `ignored` folders under `/var/subversion/mergetrigger/merges/`. E.g.: `find /var/subversion/mergetrigger/merges/*/ignored*  -type f -mtime +62 -delete; find /var/subversion/mergetrigger/merges/*/done*  -type f -mtime +62 -delete`
*  Create a cron job which periodically deletes logs older than X days in the log folder under `/var/subversion/mergetrigger/logs/`. E.g.: `find /var/subversion/mergetrigger/logs/ -type f -mtime +62 -delete`

## Client-Side

### Prerequisites | Client-Side
*  Microsoft Windows (tested with 'Microsoft Windows 10 Professional x64')
*  AdoptOpenJDK 11 (tested with 'jdk-11.0.3+7')
*  Subversion 1.9.3 (tested with 'CollabNetSubversion Client 1.9.3 (r1718519)')
*  TortoiseSVN (tested with 'TortoiseSVN 1.9.3, Build 27038 - 64 Bit')

### Installation | Client-Side
*  Place the content of the `client/bin/MergeProcessor_%VERSION%.7z` folder under the path `%ProgramFiles(x86)%\MergeProcessor`.
*  Create a shortcut in the start menu of user to the `mergeprocessor.exe`.
*  If the MergeProcessor should automatically start place a shortcut in the autostart folder of the user.

### Configuration
*  Create a file and name it `org.aposin.mergeprocessor.prefs`. Save it under the path `%USERPROFILE%\AppData\Roaming\MergeProcessor`. After this, define the variables with a simple format, e.g. `WORKING_FOLDER=C:\\dev\\mp\\`. See `Variables` section below for more information on which variables are available to use.

#### Variables
*  `WORKING_FOLDER` the folder where the MergeProcessor can create a working copy and store files.
*  `SFTP_USERNAME` the username for accessing the SVN server (SFTP access, i.e. not used for SVN access)
*  `SFTP_PASSWORD` the password for accessing the SVN server (SFTP access, i.e. not used for SVN access)
*  `SFTP_HOST` the server where the merge units are stored.
*  `SFTP_MERGEFOLDER` directory on the SVN server where to look for the merge units
*  `GIT_REPOSITORIES_FOLDER` local directory where to clone required GIT repositories to merge on
*  `RENAME_DATABASE_URL` JDBC URL to the renaming database (optional)
*  `RENAME_DATABASE_USER` username for accessing the renaming database (optional)
*  `RENAME_DATABASE_PASSWORD` password for accessing the renaming database (optional)
*  `VERSION_INFO_FILES` files in the SVN repository for identifying the branch version (required when using renaming database). Multiple files can be defined, separated by a `,`.

#### Renaming database
The renaming database is used to merge renamed and linked files. Without the definition and usage of the database renamed files and directories
cannot be found correctly during the merge process and have to be done manually. 

##### Table RENAME_MAPPING
This table is used for renamed artifacts of a repository. It is based on the file structure and contains directories and files. The column "Version" defines the version of the branch, where the renaming of the artifact was done. Future renaming actions can be added to the table. 

| Column Name   | Description                                                      |
| ------------- | ---------------------------------------------------------------- |
| ID            | Primary Key                                                      |
| OLD_NAME      | the old name of an artifact including its path before renaming   |
| NEW_NAME      | the new name of an artifact including its path after renaming    |
| VERSION       | the branch version where the renaming of the artifact was done   |
| REPOSITORY    | the repository where the artifact is versioned                   |

###### Example
*  OLD_NAME = "platform/java/plugins/org.aposin.framework"
*  NEW_NAME = "platform/java/plugins/org.opin.framework"
*  VERSION = "18.5.105"

##### Table LINK_MAPPING
This table is used for linked artifacts of a repository. It is based on the file structure and contains directories and files. The column "Version" defines the version of the branch, where the linking of the artifact was done. Future renaming actions can be added to the table.

| Column Name   | Description                                                                      |
| ------------- | -------------------------------------------------------------------------------- |
| ID            | Primary Key                                                                      |
| NAME1         | name of an artifact including its path, showing the same content as column NAME2 |
| NAME2         | name of an artifact including its path, showing the same content as column NAME1 |
| VERSION       | the branch version where the linking of the artifact was done                    |
| REPOSITORY    | the repository where the artifact is versioned                                   |

### Usage

#### Main Window

##### Refresh
Searches for merge units on the server and updates the view.

##### Automatic
Default `ON`. When the MergeProcessor finds new merge units with status todo it tries to merge them automatically.

##### Display Done
Default `OFF`. Also show merged merge units.

##### Display Ignored
Default `OFF`. Also show ignored merge units.

##### Open Log Folder
Opens the log folder of the MergeProcessor. On every start the MergeProcessor creates a new log file.

##### Preferences
Opens the prefrences dialog with further configuration.

##### Delete Password Cache
Deletes the password cache of the MergeProcessor. When SVN server request should be done the MergeProcessor will ask for the user credentials and cache them internally.

##### Help
Shows a dialog with description of the toolbar buttons.

##### Merge Selection
Merges the currently in the table selected merge units.

##### Ignore Selection
Ignores the currently in the table selected merge units.

#### Preferences Dialog

##### Working Folder
Must be initially defined as property in the configuration. The folder where the MergeProcessor can create a working copy and store files.

##### User Id
Default `%USERNAME%`. The user id of the user for which the merge files should be merged.

##### Refresh Interval
Default `60`. Interval in seconds in which the MergeProcessor looks for new merge unit.

##### Log Level
Default `INFO`. Log level to control the verbosity of the logging output.

##### SFTP Username
Must be initially defined as property in the configuration. Username used to connect via SFTP to the server.

##### SFTP Password
Must be initially defined as property in the configuration. Password used to connect via SFTP to the server.

##### SFTP Host
Must be initially defined as property in the configuration. The server where the merge units are stored.

##### SFTP Merge Folder
Must be initially defined as property in the configuration. The path on the server where the merge units are stored.

##### Window Location
Default `50,50`. X and Y coordinates of the location of the MergeProcessor main window.

##### Window Size
Default `631,218`. Width and height of the MergeProcessor main window.

##### Sorted Table column
Default `Date`. The column by which the table should be ordered.

##### Sort Direction
Default `Down`. The direction in which the columns should be ordered.

##### Application paht (Eclipse Workspace Start)
Local path to an installed Eclipse application.

##### Parameters (Eclipse Workspace Start)
Parameters for starting an installed Eclipse application.

##### Database URL (Renaming)
JDBC URL to access the renaming database.

##### Database User Id (Renaming)
Username to access the renaming database.

##### Database Password (Renaming)
Password to access the renaming database.

### Limitations
*  The MergeProcessor merges all files changed with a specific SVN revision. There does not exist a filter mechanism to specify the content or files.
*  Renamed files and directories can only be identified correctly if the renaming database is used and filled with the renaming information. Otherwise the merges have to be done manually, because of tree conflicts.

### Known issues
*  The SVN password is cached on the users computer. Everytime the user changes this password those caches should be deleted. To delete the SVN cache delete the folder `%APPDATA%\Subversion\auth`. To delete the MergeProcessor cache click `Preferences - Security - Secure Storage - Contents - Delete`.
*  SVN sometime got problems merging deleted elements. During the merge it deletes the element and then tries to set properties on that element. Or it first sets the properties and tries to delete the element but can't because it has been modified (by the property). Both problems lead to a tree conflict. If this happens, right click on the working copy directory `C:\dev\mp\wc\` and open in the context menu `TortoiseSVN - Check for modifications`. Here you will find the element in a Tree Conflict state. You can right click the element and delete it. Then right click it again and resolve the conflict.
