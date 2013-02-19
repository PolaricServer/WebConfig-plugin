
package no.polaric.webconfig;
import no.polaric.aprsd.*;
import no.polaric.aprsd.http.*;
import java.util.*;
import java.io.*;



public class WebconfigPlugin implements PluginManager.Plugin
{
     private ServerAPI _api; 
     private String _filename; 
     private Properties _config; 
     
     
     /** Start the plugin  */
      public void activate(ServerAPI api)
      {
         try {
           System.out.println("*** WebconfigPlugin.activate");
           _api = api; 
           
           /* 
            * Allow default config properties to be overridden
            * programmatically and saved elsewhere. The original config file
            * now functions as default values! 
            */
           _config = api.getConfig();
           _filename = System.getProperties().getProperty("datadir", ".")+"/"+"config.xml";
           System.out.println("*** FILENAME = "+_filename);
           try { 
               FileInputStream cfin = new FileInputStream(_filename); 
               _config.loadFromXML(cfin);
           }
           catch (java.io.FileNotFoundException e) {}
           
           
           /* 
            * Activate the web services 
            */
           api.properties().put("webconfig.plugin", this); 
           api.addHttpHandlerCls("no.polaric.webconfig.Webserver", null);   
        }
        catch (Exception e) {
            System.out.println("*** Activate WebconfigPlugin: "+e);
            e.printStackTrace(System.out); 
        }  
      }
      
          
      
     /**  Stop the plugin */ 
      // FIXME
      public void deActivate() 
      {
         System.out.println("*** WebconfigPlugin.deactivate");
         try {
            FileOutputStream cfout = new FileOutputStream(_filename);
            _config.storeToXML(cfout, "Configuration for Polaric APRSD");
         }
         catch (java.io.IOException e) {System.out.println("*** WARNING: Cannot write file "+e);}
      }
      
      
      // FIXME
      public boolean isActive()
       { return true; }

         
       
      private String[] _dep = {};
      
      
      
     /** Return an array of other component (class names) this plugin depends on */
      public String[] getDependencies() { return _dep; }
      
      
      public String getDescr() {
         return "WebconfigPlugin"; 
      }
      
      
}
 
