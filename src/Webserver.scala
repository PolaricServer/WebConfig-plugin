 
import java.util._
import java.io._
import scala.xml._
import scala.collection.JavaConversions._
import no.polaric.aprsd._
import no.polaric.aprsd.http.ServerUtils
import no.polaric.aprsd.http.ServerBase
import org.simpleframework.http.core.Container
import org.simpleframework.transport.connect.Connection
import org.simpleframework.transport.connect.SocketConnection
import org.simpleframework.http._
import org.xnap.commons.i18n._


package no.polaric.webconfig
{

  class Webserver 
      ( val api: ServerAPI ) extends ServerBase(api) with ServerUtils
  {
      val _wcp = api.properties().get("webconfig.plugin").asInstanceOf[WebconfigPlugin];
      final val PLUGIN = "no.polaric.webconfig"
      
 
      protected def refreshPage(req: Request, resp: Response, t: Int, url: String) = 
      {
         val mid = req.getParameter("mid");
         val cid = req.getParameter("chan");
         var lang = req.getParameter("lang");
         lang = if (lang==null) "en" else lang
         val uparm = "?lang=" + lang + 
           { if (mid != null) "&mid="+mid else "" } + 
           { if (cid != null) "&chan="+cid else "" }
            
         resp.addValue("Refresh", t+";url=\""+url + uparm+"\"")
      }
      
          
          
      /** Text field with label */
      protected def textField(propname: String, id: String, lbl: String, title: String, 
                              length: Int, maxlength: Int, pattern: String, ptext: String): NodeSeq = 
          label(id, "leftlab", lbl, title) ++ 
          textInput(id, length, maxlength, pattern, _api.getProperty(propname, "")) ++ 
          {
             if (ptext != null)
                <span class="postfield">{ptext}</span>
             else null
          } ++ br
          ;     
          
          
      protected def textField(propname: String, id: String, lbl: String, title: String, 
                              length: Int, maxlength: Int, pattern: String): NodeSeq = 
          textField(propname, id, lbl, title, length, maxlength, pattern, "")
          ;
          
          
      /** Boolean field without label */    
      protected def boolField(propname: String, id: String, lbl: String): NodeSeq = 
          checkBox(id, _api.getBoolProperty(propname,false), TXT(lbl))
          ;
          
          
      /** Boolean field with label */
      protected def sBoolField(propname: String, id: String, lbl: String, title: String): NodeSeq =
          label(id, "leftlab", lbl, title) ++
          boolField(propname, id, "Aktivert.") ++ br
          ;
          
          
      /** UTM position field */    
      protected def utmField(propname: String): NodeSeq = 
          if (_api.getProperty(propname, null) == null)
             utmForm('W',33)
          else
             utmForm(_api.getProperty(propname, null))
          ;
         
      
      protected def printState(st: Channel.State, I:I18n): NodeSeq = 
         st match {
            case Channel.State.OFF => TXT( I.tr("Inactive (off)"))
            case Channel.State.STARTING => TXT( I.tr("Connecting..."))
            case Channel.State.RUNNING => 
                <span>{ I.tr("Active (ok)") }<img class="state" src="../aprsd/dicons/ok.png"/></span>  
            case Channel.State.FAILED => 
                <span>{ I.tr("Inactive (failed)") }<img class="state" src="../aprsd/dicons/fail.png"/></span>
         }
         ;
          
          
          
      /* Regular expressions that define format of input of different types */    
      val TEXT = ".*"
      val NAME = "[A-Za-z0-9_\\-\\.\\/]+"
      val LIST = "([A-Za-z0-9_\\-\\.]+)(,\\s?([A-Za-z0-9_\\-\\.]+))*"
      val NUMBER = "\\-?[0-9]+"
      val BOOLEAN = "true|false|TRUE|FALSE"
      val CALLSIGN = "[A-Za-z0-9]{3,6}(\\-[0-9]{1,2})?"
      val CHANTYPE = "APRSIS|KISS|TCPKISS|TNC2"
      val UTMPOS = "[0-9]{2}[A-Za-z]\\s+[0-9]{6}\\s+[0-9]{7}"
      
      /* Set to true if value of one or more fields has changed */
      var changed = false;
      
      
      /**
       * Get value of a given field from HTTP request parameters.
       * Check if format of input is correct and if field value has changed. 
       */
      protected def _getField(req : Request, value: String, propname: String, pattern: String, 
          isnum: Boolean, min: Int, max: Int): NodeSeq = 
      {          
          val xold = _api.getProperty(propname,"")
          var x = value;
          
          
          def checkNum(x: String): Boolean = 
             if (isnum) {
                val xx = x.toInt
                xx >= min && xx <= max
             }
             else true
             ;
             
             
          x = if (x == null || (pattern.equals(BOOLEAN) && x.equals(""))) "false" 
              else x
              
          if (("".equals(x) && !"".equals(xold)) || (x != null && x.matches(pattern) && checkNum(x))) 
               /* Test om vi har endret verdi på et felt */
               if (!x.equals(xold)) {
                   changed = true
                   _api.getConfig().setProperty(propname, x)
                    <span class="fieldsuccess">Field <b>{propname}</b> = '{x}'. Changed.<br/></span>
               }               
               else <span>Field <b>{propname}</b>. Unchanged.<br/></span>          
          else if (x != null && !"".equals(x))
             <span class="fielderror">Field <b>{propname}</b>. Value out of range or format error. Value = '{x}'<br/></span>
          else <span></span>
      }
      
      
      
      protected def getField(req : Request, id: String, propname: String, pattern: String): NodeSeq =
          _getField(req, req.getParameter(id), propname, pattern, false, 0,0)
          ;
          
          
      protected def getField(req : Request, id1: String, id2: String, propname: String, pattern: String): NodeSeq =
          _getField(req, req.getParameter(id1)+req.getParameter(id2), propname, pattern, false, 0,0)
          ;
   
   
      protected def getUtmField(req : Request, id1: String, id2: String, id3: String, id4: String, propname: String, pattern: String): NodeSeq =
          _getField(req, req.getParameter(id1)+req.getParameter(id2)+" "+req.getParameter(id3)+" "+req.getParameter(id4), 
                    propname, pattern, false, 0,0)
          ;
          
          
      protected def getField(req : Request, id: String, propname: String, min: Int, max: Int): NodeSeq = 
          _getField(req, req.getParameter(id), propname, NUMBER, true, min, max)
          ; 
           
      
    
      /**
       * Handle restarting of the server by calling external script. 
       * Note that this must use sudo to operate with root privileges. 
       */
      def handle_restartServer(req : Request, res: Response) =
      {
          val I = getI18n(req, PLUGIN)
          refreshPage(req, res, 10, "config_menu")
          
          def action(req : Request): NodeSeq = {

             val pb = new ProcessBuilder("/usr/bin/sudo", "-n", "/usr/bin/polaric-restart")
             pb.inheritIO();
             pb.start(); 
             
             <br/>
             <h2>{ I.tr("Restart server...") }</h2>
          }
             
          printHtml (res, htmlBody (req, null, IF_ADMIN(action)(req) ))
      }
      
      
      
          
      
      /** 
       * Override htmlBody, in order to add a menu/status on the left side. 
       */
      override def htmlBody(req: Request, xhead : NodeSeq, content : NodeSeq) : Node =
      {
           val I = getI18n(req, PLUGIN)
           val head = <link href={fprefix(req)+"/config_menu.css"} rel="stylesheet" type="text/css" />
           val heads = if (xhead==null) head else head ++ xhead 
           
           var selected = req.getParameter("mid")           
           selected = if (selected==null) "1" else selected
           var lang = req.getParameter("lang")
           lang = if (lang==null) "en" else lang
           
           
           def mitem(url:String, id: Integer, txt: String) : Node = 
           {
              val delim = if (url.contains("?")) "&" else "?"
              val cls = if ((""+id).equals(selected)) "selected" else ""
              
              <li class={cls}>
                <a href={ url + delim + "lang="+lang+"&mid="+id }>{txt}</a>
              </li>
           }
           
           
           
           def body = 
             <div id="config_menu">
             <ul class="menu">
               { mitem("config_menu", 1, I.tr("Status info")) ++
                 mitem("config", 2, I.tr("Server config")) ++
                 mitem("config_posreport", 3, I.tr("Own position")) ++
                 mitem("config_mapdisplay", 4, I.tr("Display on map"))
               }
               <li>{ I.tr("Data channels...") }</li>
               <ul>
               {
                  val chs = _api.getProperty("channels", null).split(",(\\s)*")
                  var mid=4
                  for (ch <- chs) yield {
                     mid += 1;
                     mitem("config_chan?chan="+ch, mid, ch)
                  }
               }
               </ul>
             </ul>
             <br/>
             <a id="restart" href={"restartServer?lang="+lang} ><b>[RESTART]</b></a>
             {
                if (changed)
                   <div class="status">
                      { I.tr("Changes are done. Restart to activate.") }
                   </div>
                else null
             }
             
             </div>
             <div id="config_main">
                { content }
             </div>
             ;
             
             super.htmlBody(req, heads, body) 
      }
      
      
      
      /**
       * This is the first menu choice. It just puts the admin status from aprsd in am iframe 
       */
      def handle_config_menu(req : Request, res: Response) =
      {
          var lang = req.getParameter("lang")
          lang = if (lang==null) "en" else lang
          
          def action(req : Request): NodeSeq =
             <iframe id="config_main" name="config_main" src={"admin?cmd=info&lang="+lang} />
             ;   
                          
          printHtml (res, htmlBody(req, null, action(req)))
      }
      
      
      
      /**
       * The main configuration of aprsd. Callsign, users, channels. Igate. Remote control.
       */
      def handle_config(req : Request, res: Response) =
      { 
          val prefix = <h3>Konfigurasjon av Polaric APRSD</h3>
          val I = getI18n(req, PLUGIN)
          
          def fields(req : Request): NodeSeq =
                textField("default.mycall", "item1", 
                      I.tr("Callsign")+":", "", 10, 10, CALLSIGN) ++
                textField("user.admin", "item2", 
                      I.tr("Admin users")+":", 
                      I.tr("Full admin-rights"), 30, 200, TEXT, "(regex)") ++  
                textField("user.update", "item3", 
                      I.tr("SAR users")+":", 
                      I.tr("Ordinary SAR users"), 30, 200, TEXT, "(regex)") ++
                br ++
                textField("channels", "item4", 
                      I.tr("Data channels")+":", 
                      I.tr("Data channels for tracking"), 30, 50, LIST, "(liste)")   ++
                textField("channel.default.inet", "item5", 
                      I.tr("Primary APRS/IS channel")+":", "", 10, 10, NAME) ++ 
                textField("channel.default.rf", "item6", 
                      I.tr("Primary RF channel")+":", "", 10, 10, NAME) ++ 
                br ++
                label("item7", "leftlab", 
                      I.tr("Igate")+":", 
                      I.tr("Tick to activate RF<->internet gateway")) ++
                boolField("igate.on", "item7", 
                      I.tr("Activated.")) ++ br ++ 
                label("item8", "leftlab", 
                      I.tr("Igating to RF")+":", 
                      I.tr("Tick to activate internet->RF igating")) ++
                boolField("igate.rfgate.allow", "item8", 
                      I.tr("Activated.")) ++
                boolField("objects.rfgate.allow", "item9", 
                      I.tr("RF igating for objects.")) ++ br ++
                textField("objects.rfgate.range", "item10", 
                      I.tr("Radius objects")+":", 
                      I.tr("Area for sending of objects RF"), 6, 10, NUMBER, "(km)") ++
                textField("igate.rfgate.path", "item11", 
                      I.tr("Digipeater path, igate")+":", 
                      I.tr("Default (see also next field)"), 20, 30, LIST) ++
                textField("message.rfpath", "item12", 
                      I.tr("Digi path, messages")+":", 
                      I.tr("...has also effect for messages to RF igate"), 20, 30, LIST) ++
                textField("objects.rfpath", "item13", 
                      I.tr("Digipeater path, objects")+":", 
                      I.tr("...has also effect for objects to RF igate"), 20, 30, LIST) ++
                br ++ 
                label("item14", "leftlab", 
                      I.tr("Remote control")+":", 
                      I.tr("Tick to activate remote control")) ++
                boolField("remotectl.on", "item14", 
                      I.tr("Activated.")) ++
                br ++
                textField("remotectl.connect", "item15", 
                      I.tr("Rc server")+":", 
                      I.tr("Rc server (callsign of another PS instance)"), 10, 10, NAME) ++
                textField("message.auth.key", "item16", 
                      I.tr("Authentication key")+":", 
                      I.tr("Key for authentication (for remote control)"), 20, 30, TEXT)
               ;
              
              
         def action(req : Request): NodeSeq = 
         {
               refreshPage(req, res, 3, "config")
               br ++ br ++
               getField(req, "item1", "default.mycall", CALLSIGN) ++ 
               getField(req, "item2", "user.admin", TEXT) ++ 
               getField(req, "item3", "user.update", TEXT) ++ 
               getField(req, "item4", "channels", LIST) ++ 
               getField(req, "item5", "channel.default.inet", NAME) ++ 
               getField(req, "item6", "channel.default.rf", NAME) ++ 
               getField(req, "item7", "igate.on", BOOLEAN) ++
               getField(req, "item8", "igate.rfgate.allow", BOOLEAN) ++ 
               getField(req, "item9", "objects.rfgate.allow", BOOLEAN) ++ 
               getField(req, "item10", "objects.rfgate.range", 0, 99999) ++ 
               getField(req, "item11", "igate.rfgate.path", LIST) ++ 
               getField(req, "item12", "message.rfpath", LIST) ++
               getField(req, "item13", "objects.rfpath", LIST) ++
               getField(req, "item14", "remotectl.on", BOOLEAN) ++
               getField(req, "item15", "remotectl.connect", NAME) ++
               getField(req, "item16", "message.auth.key", TEXT)
         }
              
         printHtml (res, htmlBody (req, null, htmlForm(req, prefix, IF_ADMIN(fields), IF_ADMIN(action), false, default_submit)))
     }
     
     
      /**
       * Parameters related to showing tracks on map. 
       */
      def handle_config_mapdisplay(req : Request, res: Response) =
      {          
          val I = getI18n(req, PLUGIN)
          val prefix = <h3>{ I.tr("Map display settings") }</h3>
          
          def fields(req : Request): NodeSeq =
                textField("aprs.expiretime", "item1", 
                     I.tr("Max inactivity")+":", 
                     I.tr("How long can an object be inactive before it disappears"), 
                     4, 4, NUMBER, I.tr("(minutes)")) ++
                br ++
                textField("map.trail.maxPause", "item2", 
                     I.tr("Max inactivity for trail")+":", 
                     I.tr("How long can an object be inactive before its trail disappears"), 
                     4, 4, NUMBER, I.tr("(minutes)")) ++
                textField("map.trail.maxPause.extended", "item3", 
                     I.tr(".. slow speed")+":", 
                     I.tr("Max inactivity time when speed is low"), 4, 4, NUMBER, I.tr("(minutes)")) ++          
                br ++
                textField("map.trail.maxAge", "item4", 
                     I.tr("Trail length")+":", 
                     I.tr("How long timespan to draw a trail for"), 
                     4, 4, NUMBER, I.tr("(minutes)")) ++
                textField("map.trail.maxAge.extended", "item5", 
                     I.tr(".. slow speed")+":", 
                     I.tr("Trail length when speed is low"), 
                     4, 4, NUMBER, I.tr("(minutes)")) 
         ;
              
              
          def action(req : Request): NodeSeq = 
          {
               refreshPage(req, res, 3, "config_mapdisplay")
               br ++ br ++
               getField(req, "item1", "aprs.expiretime", 0, 1440) ++ 
               getField(req, "item2", "map.trail.maxPause", 0, 1440) ++
               getField(req, "item3", "map.trail.maxPause.extended", 0, 1440) ++
               getField(req, "item4", "map.trail.maxAge", 0, 1440) ++
               getField(req, "item5", "map.trail.maxAge.extended", 0, 1440) 
          }
              
          printHtml (res, htmlBody (req, null, htmlForm(req, prefix, IF_ADMIN(fields), IF_ADMIN(action), false, default_submit)))
      }     
      
      
      
      
      /**
       * Using the Polaric APRSD as a tracker that send position reports. 
       */
      def handle_config_posreport(req : Request, res: Response) =
      {   
          val I = getI18n(req, PLUGIN)
          val prefix = <h3>{ I.tr("Tracking of own position") }</h3>
          
          def fields(req : Request): NodeSeq =
                label("item1", "leftlab", 
                      I.tr("Position report")+":", 
                      I.tr("Tick to activate position reporting")) ++
                boolField("ownposition.tx.on", "item1", 
                      I.tr("Activated")) ++  
                boolField("ownposition.tx.allowrf", "item2", 
                      I.tr("Allow transmission on RF")) ++
                br ++ br ++
                label("item4", "leftlab", 
                      I.tr("Symbol")+":", 
                      I.tr("APRS symbol-table and symbol")) ++
                textInput("item4", 1, 1, ".", ""+_api.getProperty("ownposition.symbol", "/c")(0)) ++
                textInput("item5", 1, 1, ".", ""+_api.getProperty("ownposition.symbol", "/c")(1)) ++
                br ++
                textField("ownposition.tx.rfpath", "item6", 
                      I.tr("Digipeater path")+":", "", 20, 30, LIST) ++
                textField("ownposition.tx.comment", "item7", 
                      I.tr("Description")+":", "", 20, 40, TEXT) ++ br ++
                label("utmz", "leftlab", 
                      I.tr("My position")+":", 
                      I.tr("Server's position in UTM format")) ++        
                utmField("ownposition.pos") ++ br ++ 
                br ++
                label("item8", "leftlab", I.tr("Tracking with GPS")+":", 
                      I.tr("Tick to use position from GPS")) ++
                boolField("ownposition.gps.on", "item8", 
                      I.tr("Activated")) ++ 
                boolField("ownposition.gps.adjustclock", "item9", 
                      I.tr("Adjust clock from GPS")) ++ br ++
                textField("ownposition.gps.port", "item10", 
                      I.tr("GPS Port")+":", 
                      I.tr("Serial port device-name (e.g. /dev/ttyS0)"), 12, 20, NAME) ++
                textField("ownposition.gps.baud", "item11", 
                      I.tr("GPS Baud")+":", "", 6, 8, NUMBER) ++ br ++
                textField("ownposition.minpause", "item12", 
                      I.tr("Min pause")+":", 
                      I.tr("Minimum time between transmissions"), 4, 5, NUMBER, I.tr("(seconds)")) ++
                textField("ownposition.maxpause", "item13", 
                      I.tr("Max pause")+":", 
                      I.tr("Maximum time between transmissions"), 4, 5, NUMBER, I.tr("(seconds)")) ++
                textField("ownposition.mindist", "item14", 
                      I.tr("Min distance")+":", 
                      I.tr("Distance between transmissions when speed is low"), 4, 5, NUMBER, I.tr("(meter)")) ++
                textField("ownposition.maxturn", "item15", 
                      I.tr("Max turn")+":", 
                      I.tr("Max change in direction before transmission"), 4, 5, NUMBER, I.tr("(degrees)"))
              ;
              
              
         def action(req : Request): NodeSeq = 
         {       
              refreshPage(req, res, 3, "config_posreport")
              br ++ br ++
              getField(req, "item1", "ownposition.tx.on", BOOLEAN) ++
              getField(req, "item2", "ownposition.tx.allowrf", BOOLEAN) ++
              getField(req, "item4", "item5", "ownposition.symbol", "..") ++ 
              getField(req, "item6", "ownposition.tx.rfpath", LIST) ++ 
              getField(req, "item7", "ownposition.tx.comment", TEXT) ++ 
              getUtmField(req, "utmz", "utmnz", "x", "y", "ownposition.pos", UTMPOS) ++
              getField(req, "item8", "ownposition.gps.on", BOOLEAN) ++
              getField(req, "item9", "ownposition.gps.adjustclock", BOOLEAN) ++
              getField(req, "item10", "ownposition.gps.port", NAME) ++
              getField(req, "item11", "ownposition.gps.baud", 300, 999999) ++
              getField(req, "item12", "ownposition.minpause", 10, 60*60*60) ++
              getField(req, "item13", "ownposition.maxpause", 20, 60*60*60) ++
              getField(req, "item14", "ownposition.mindist", 10, 999999) ++
              getField(req, "item15", "ownposition.maxturn", 0, 360) 
         }
              
         printHtml (res, htmlBody (req, null, htmlForm(req, prefix, IF_ADMIN(fields), IF_ADMIN(action), false, default_submit)))
     }
     
     
     
     /**
      * Configuration of each individual channel. 
      */
     def handle_config_chan(req: Request, res: Response) = 
     {
         val I = getI18n(req, PLUGIN)
         val cid = req.getParameter("chan")
         val chp = "channel."+cid
         val prefix = <h3>{I.tr("Channel")+ " '"+cid+"'"}</h3>

         val is_aprsis = _api.getProperty(chp+".type", "APRSIS").equals("APRSIS")
         val is_tcpkiss = _api.getProperty(chp+".type", "APRSIS").equals("TCPKISS")
         val ch = _api.getChanManager().get(cid)

         
         def fields(req : Request): NodeSeq = 
         {      
               if (ch != null) 
                   refreshPage(req, res, 60, "config_chan");
               { if (ch != null) 
                    simpleLabel("info4", "leftlab", I.tr("State")+":", printState(ch.getState(), I)) ++ 
                    simpleLabel("info1", "leftlab", I.tr("Heard stations")+":", TXT(""+ch.nHeard())) ++
                    simpleLabel("info2", "leftlab", 
                       I.tr("Traffic in")+":", TXT(""+ch.nHeardPackets()+"  ("+ch.nDuplicates()+" "+I.tr("duplicates")+")")) ++
                    simpleLabel("info3", "leftlab", I.tr("Traffic out")+":", TXT(""+ch.nSentPackets())) ++ br 
                    
                 else <span></span>
               } ++
               label("item1", "leftlab", I.tr("Channel")+":", I.tr("Tick to activate channel")) ++
               boolField(chp+".on", "item1", I.tr("Activated")) ++ br ++
               textField(chp+".type", "item2", 
                     I.tr("Type")+":", 
                     I.tr("Type (APRSIS, TNC2, KISS or TCPKISS)"), 10, 10, CHANTYPE) ++
               { 
                 if (is_aprsis || is_tcpkiss) 
                    textField(chp+".host", "item4", 
                          I.tr("Server address")+":", 
                          I.tr("DNS name or IP address for server"), 20, 30, NAME) ++
                    textField(chp+".port", "item5", 
                          I.tr("Server port")+":", 
                          I.tr("Port number"), 6, 6, NUMBER) ++
                    { if (is_aprsis)
                        textField(chp+".pass", "item6", 
                              I.tr("Passcode")+":", 
                              I.tr("APRS/IS verification code"), 6, 6, NUMBER) ++
                        textField(chp+".filter", "item7", 
                              I.tr("Filter")+":", 
                              I.tr("APRS/IS filter-string"), 30, 50, TEXT) 
                      else <span></span> 
                    } 
                 else 
                    textField(chp+".port", "item8", 
                          I.tr("Port")+":", 
                          I.tr("Serial port device-name (e.g. /dev/ttyS0)"), 12, 20, NAME) ++
                    textField(chp+".baud", "item9", 
                          I.tr("Baud")+":", "", 6, 8, NUMBER) 
               } ++ br ++
               label("item10", "leftlab", 
                     I.tr("Visibility")+":", 
                     I.tr("Tick to limit access to logged in users")) ++
               boolField(chp+".restrict", "item10", I.tr("Only for logged in users")) ++ br ++
               textField(chp+".style", "item11", I.tr("CSS style name")+":", "", 10, 10, NAME)
         }
         
         
         
         def action(req : Request): NodeSeq = 
         {
              val wasOn = _api.getBoolProperty(chp+".on", false)
              val chan = _api.getChanManager().get(cid)
              val wasChg = changed 
              changed = false
              
              refreshPage(req, res, 3, "config_chan")
              br ++ br ++
              getField(req, "item1", chp+".on", BOOLEAN) ++
              getField(req, "item2", chp+".type", CHANTYPE) ++
              { if (is_aprsis || is_tcpkiss)
                   getField(req, "item4", chp+".host", NAME) ++ 
                   getField(req, "item5", chp+".port", 1024, 65535) ++ 
                   { if (is_aprsis) 
                        getField(req, "item6", chp+".pass", 0, 99999) ++ 
                        getField(req, "item7", chp+".filter", TEXT)
                     else <span></span>
                   }
                else
                   getField(req, "item8", chp+".port", NAME) ++
                   getField(req, "item9", chp+".baud", 300, 999999)
              } ++
              getField(req, "item10", chp+".restrict", BOOLEAN) ++
              getField(req, "item11", chp+".style", NAME) ++ 
              {
                  if (changed && wasOn) {
                      chan.deActivate();
                      <span class="fieldsuccess">Deactivating channel<br/></span>
                  }
                  else <span></span>
              } ++
              {  
                  val isOn = _api.getBoolProperty(chp+".on", false)
                  if (changed && isOn) {
                      chan.activate(_api);
                      <span class="fieldsuccess">Activating channel<br/></span>
                  }
                  else <span></span>
              } ++
              {
                  changed = wasChg
                  <span></span>
              }
         }
              
         printHtml (res, htmlBody (req, null, htmlForm(req, prefix, IF_ADMIN(fields), IF_ADMIN(action), false, default_submit)))     
     }
     
     
    
     /**
      * Handle password chancge (to be placed in separate window). 
      */
      def handle_passwd(req: Request, res: Response) = 
      {
          val I = getI18n(req, PLUGIN)
          val prefix = <h3>{ I.tr("Register user/password") }</h3>
          var username = getAuthUser(req)
          
          
          
          def fields(req : Request): NodeSeq =
             label("item1", "lleftlab", I.tr("Username")+":", I.tr("Username for new or existing user")) ++
             { if (authorizedForAdmin(req))
                  textInput("item1", 20, 20, NAME, "")
               else 
                  <label id="item1">{username}</label>
             } ++
             br ++
             label("item2", "lleftlab", I.tr("Password")+":", "") ++
             textInput("item2", 20, 30, ".*", "")
          ;
          
          
          
          def action(req : Request): NodeSeq = 
          {
             username = if (authorizedForAdmin(req)) req.getParameter("item1") 
                        else username
             val passwd = req.getParameter("item2")             
             val cmd = "/usr/bin/sudo /usr/bin/htpasswd -b /etc/polaric-webapp/users "+username+" "+passwd
             val p = Runtime.getRuntime().exec(cmd)
             val res = p.waitFor()
             
             if (res == 0)
                 <h3>{ I.tr("Password for user '{0}' updated", username) }</h3>
             else if (res == 5)
                 <h3>{ I.tr("Error: Your input is too long") }</h3>
             else if (res == 6)
                 <h3>{ I.tr("Error: Your input contains illegal characters") }</h3>
             else 
                 <h3>{ I.tr("Error: Couldn't update (server problem)") }</h3>
       
          }
 
 
          printHtml (res, super.htmlBody (req, null, htmlForm(req, prefix, IF_AUTH(fields), IF_AUTH(action))))
      }
      
     
     
     
  }

}