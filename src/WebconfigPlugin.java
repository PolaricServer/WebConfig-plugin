
package no.polaric.webconfig;
import no.polaric.aprsd.*;
import no.polaric.aprsd.http.*;
import java.util.*;
import java.io.*;



public class WebconfigPlugin implements PluginManager.Plugin
{
     private ServerAPI _api; 
     private String _filename = System.getProperties().getProperty("datadir", ".")+"/"+"config.xml";; 
     
     
     /** Start the plugin  */
      public void activate(ServerAPI api)
      {        
        _api = api; 
         try {
           _api.log().info("WebconfigPlugin", "Activate...");

           /* 
            * Activate the web services 
            */
           api.properties().put("webconfig.plugin", this); 
           api.addHttpHandlerCls("no.polaric.webconfig.Webservices", null);   
        }
        catch (Exception e) {
            _api.log().error("WebconfigPlugin", ""+e);
            e.printStackTrace(System.out); 
        }  
      }
      
          
      
     /**  Stop the plugin. 
      *   this should only be called at shutdown ! */ 
      public void deActivate() 
      {
          _api.saveConfig();
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
 
