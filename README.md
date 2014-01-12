# Webconfig plugin for Polaric Server

The "Polaric Server" is mainly a web based service to present (APRS) 
tracking information on maps and where the information is updated at real-
time. It is originally targeted for use by radio amateurs in voluntary search
and rescue service in Norway. It consists of a web application and a server 
program (APRS daemon). 
 
This is a plugin that adds a web-based config and administration interface. This currently 
supports setting the most important config parameters for polaric-aprsd and restart the server.
 
More documentation on the project can be found here: 
http://aprs.no/dokuwiki/doku.php?id=polaricserver

## System requirements

Linux/Java platform (tested with Debian/Ubuntu) with
* Java Runtime environment version 7 or later. 
* Scala library version 2.8 or later. 
* polaric-aprsd and polaric-webapp installed.

## Installation

We provide Debian files. For information on getting started on a Debian/Ubuntu 
platform please see: http://aprs.no/dokuwiki/doku.php?id=installation

Add the following to the server.ini file and restart: 

plugins = no.polaric.webconfig.WebconfigPlugin


## Building from source 

Build from the source is done by a plain old makefile. Yes I know :)
Maybe I move to Ant or Maven a little later. Setup for generating Debian
packages is included. You may use the 'debuild' command.

You will need JDK (Oracle or OpenJDK) version 7 or later, the Scala
programming language version 2.8 or later (scala and scala-library). 

Note. To compile this package you need to symlink or copy the following
files into the directory where makefile is located. You find them in the polaric-aprsd package. 
* polaric-aprsd.jar
* jcoord.jar
* simple.jar
