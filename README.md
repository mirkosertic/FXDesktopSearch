# FXDesktopSearch - The free search application for your desktop

FXDesktopSearch is a Java and JavaFX based desktop search application. It crawls a configured set of directories and allows you to do fulltext search on the content.

During the fulltext search, you can do drilldown queries on the found documents do restrict search results by date, author, document type or language.

FXDesktopSearch can crawl the local and remote filesystems. It also watches already crawled files for change, so once a file is indexed and changed on the filesystem, the index is updated.

## Download

The latest FXDesktopSearch packages can be downloaded here:

Operating system            | Link                                       
----------------------------------------------------------------------------------
Microsoft Windows 64bit     |
Linux RPM 64 bit            |

## Installation

### Windows

Installation on Windows systems is quite easy. Download the .exe installier and execute it with administrator permissions.
This will install FXDesktopSearch an the required Java runtime on your machine,

### Linux

Installation on Unix systems is also easy. Just download the .rpm file and execute it using the following command:

Operating system            | Link                                       
----------------------------------------------------------------------------------
RedHat package manager      | sudo rpm -i <downloadedrpmfile>
Yum                         | sudo yum install <downloadedrpmfile>
Debian                      | use "apt-get install alien" to install the alien package converter. Then use "alien --to-deb --keep-version <downloadedrpmfile>" to convert the rpm to a deb file. Finally use "sudo dpkg --install <createddebfilefromalien>" to install the file.


## Usage

TODO

## Configuration

The configuration screen is triggered by using File -> Configure. The following dialog will appear:

!(documentation/configurationscreen.png)

The following options are available:

Option                                | Description                                       
----------------------------------------------------------------------------------
Show similar search results           | Can be enabled if you want to include similar search results for every match. Please not that this is very processing insensitive.
Limit search results to               | This is the number of search results presented to the user.
Number of suggestions                 | This is the number of search phrase suggestions. They are shown as soon as you start to type words into the query text field.
Number of words before suggestion span| Include this number of words in the search phrase suggestion before a matching word
Number of words after suggestion span | Include this number of words in the search phrase suggestion after the last match
Slop for suggestion spans             | Allow this number of words between entered words for matching search phrase suggestions
Indexed directories                   | This is the list of directories to crawl and index
Scanned documents                     | Check every document type you want to index
Language analyzers                    | Advanced: enable or disable language specific analyzers.


## Under the hood

FXDesktopSearch has a hybrid JavaFX2 user interface. This means that the UI is basically a JavaFX scene with an embedded JavaFX WebView. The WebView renders a HTML page,
which is delivered by an embedded Jetty WebServer. Using HTML allows us to generate and style complex user interfaces without creating new JavaFX controls.

Under the hood FXDesktopSearch uses Apache Lucene to build the fulltext index for the crawled documents. It also uses Apache Tika for content and metadata extraction.

The FileCrawler reads from a parallel Java 8 stream of files and passes them to the ContentExtractor. 
The ContentExtractor extracts the content and the metadata and passes the result to the LuceneIndexHandler. 
The LuceneIndexHandler writes or updates the file in the Lucene index and also generates the search facets for later drilldown queries.

Modified files are tracked by the Java NIO WatchService API. Every file modification is send to the ContentExtractor and the final results are also updated by the LuceneIndexHandler in the fulltext index.

