##########################################################################
## Change macros below according to your environment and your needs
##
## CLASSDIR if you want to compile to a class directory instead of generating
##          a jar, by using the 'test' target, you may set the directory here.
##
## CLASSPATH Specify where to find the servlet library and the java-cup
##           library. For Debian Linux platform you wont need to change
##           this.
##
## JAVAC: Java compiler
## JAR:   Jar archiver
##########################################################################
  CLASSDIR = classes
 CLASSPATH =  /usr/share/java/gettext-commons.jar:polaric-aprsd.jar:jcoord-polaric.jar:spark-core.jar
INSTALLDIR = /etc/polaric-aprsd/plugins
     JAVAC = javac -source 1.8 -target 1.8
       JAR = jar

# Review (and if necessary) change these if you are going to 
# install by using this makefile

   INSTALL_JAR = $(DESTDIR)/etc/polaric-aprsd/plugins
   INSTALL_WWW = $(DESTDIR)/usr/share/polaric
INSTALL_WEBAPP = $(DESTDIR)/etc/polaric-webapp/www/auto
   INSTALL_BIN = $(DESTDIR)/usr/bin
   INSTALL_IMG = $(DESTDIR)/usr/share/polaric/dicons
INSTALL_CONFIG = $(DESTDIR)/etc/polaric-aprsd
   INSTALL_LOG = $(DESTDIR)/var/log/polaric
  INSTALL_SUDO = $(DESTDIR)/etc/sudoers.d
INSTALL_PLUGDIR= $(INSTALL_CONFIG)/config.d
  
  
  
##################################################
##  things below should not be changed
##
##################################################
    LIBDIR = _lib
 JAVAFLAGS =
  PACKAGES = core i18n scala
 LANGUAGES = no


all: aprs

install: polaric-aprsd.jar
	install -d $(INSTALL_CONFIG)
	install -d $(INSTALL_PLUGDIR)
	install -d $(INSTALL_BIN)
	install -d $(INSTALL_IMG)
	install -d $(INSTALL_JAR)
	install -d $(INSTALL_WWW)
	install -d $(INSTALL_WEBAPP)
	install -d $(INSTALL_WEBAPP)/webconfig
	install -d $(INSTALL_WEBAPP)/webconfig/i18n
	install -d $(INSTALL_SUDO)
	install -m 644 webconfig.ini $(INSTALL_PLUGDIR)
	install -m 644 www/webconfig.js $(INSTALL_WEBAPP)
	install -m 644 www/i18n/msgs/*.json $(INSTALL_WEBAPP)/webconfig/i18n/
	install -m 644 www/config_menu.css $(INSTALL_WWW)
	install -m 644 www/PolaricServer.png $(INSTALL_WWW)
	install -m 644 www/pointer.gif $(INSTALL_WWW)
	install -m 644 images/*.png $(INSTALL_IMG)
	
	install -m 755 -d $(INSTALL_LOG)
	install -m 644 polaric-webconfig.jar $(INSTALL_JAR)
	cp sudoers.d $(INSTALL_SUDO)/polaric-plugin-webconfig
	chmod 644 $(INSTALL_SUDO)/polaric-plugin-webconfig

$(INSTALLDIR)/polaric-webconfig.jar: polaric-webconfig.jar
	cp polaric-webconfig.jar $(INSTALLDIR)/polaric-webconfig.jar

	
aprs: $(LIBDIR)
	@make TDIR=$(LIBDIR) CLASSPATH=$(LIBDIR):$(CLASSPATH) compile     
	cd $(LIBDIR);jar cvf ../polaric-webconfig.jar *;cd ..


compile: $(PACKAGES)
	

$(CLASSDIR): 
	mkdir $(CLASSDIR)
	
		
$(LIBDIR):
	mkdir $(LIBDIR)


.PHONY : core
core: 
	$(JAVAC) -d $(TDIR) $(JAVAFLAGS) src/*.java 

	
.PHONY : i18n
i18n: 
	bash msg-compile.sh $(LANGUAGES)
        # Repeat p2json for each po file to be used
	python po2json.py www/i18n/msgs/no.po
	
	
.PHONY : scala
scala:            
	scalac -d $(TDIR) -classpath $(LIBDIR):$(CLASSPATH) src/*.scala


clean:
	@if [ -e ${LIBDIR} ]; then \
		  rm -Rf $(LIBDIR); \
	fi 
	rm -f ./*~ src/*~
