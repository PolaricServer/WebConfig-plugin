 
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




package no.polaric.webconfig
{

  class Webserver 
      ( val api: ServerAPI ) extends ServerBase(api) with ServerUtils
  {
      val _wcp = api.properties().get("webconfig.plugin").asInstanceOf[WebconfigPlugin];
      
      
 
      protected def refreshPage(resp: Response, t: Int, url: String) = 
        resp.addValue("Refresh", t+";url=\""+url+"\"")
        ;
      
          
          
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
         
          
          
          
          
          
      val TEXT = ".*"
      val NAME = "[A-Za-z0-9_\\-\\.\\/]+"
      val LIST = "([A-Za-z0-9_\\-\\.]+)(,\\s?([A-Za-z0-9_\\-\\.]+))*"
      val NUMBER = "\\-?[0-9]+"
      val BOOLEAN = "true|false|TRUE|FALSE"
      val CALLSIGN = "[A-Za-z0-9]{3,6}(\\-[0-9]{1,2})?"
      val CHANTYPE = "APRSIS|KISS|TNC2"
      val UTMPOS = "[0-9]{2}[A-Za-z]\\s+[0-9]{6}\\s+[0-9]{7}"
      
      
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
               if (!x.equals(xold)) {
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
           
      
      
      def handle_restartServer(req : Request, res: Response) =
      {
          // val head = 
          refreshPage(res, 7, "admin?cmd=info")
          
          def action(req : Request): NodeSeq = {
             val cmd = "/usr/bin/sudo /etc/init.d/polaric-aprsd restart"
             Runtime.getRuntime().exec(cmd)
             <br/>
             <h2>Restart server...</h2>
          }
             
          printHtml (res, htmlBody (req, null, IF_ADMIN(action)(req) ))
      }
      
      
      
      
      def handle_config_menu(req : Request, res: Response) =
      {
          val target = "config_main"
          val head = <link href={fprefix(req)+"/config_menu.css"} rel="stylesheet" type="text/css" />
          
          
          /*
           * Meny genereres dynamisk. Avhengig av konfig. 
           */
          def action(req : Request): NodeSeq =
             <div id="config_menu">
             <ul class="menu">
               <li><a href="admin?cmd=info" target={target}>Status info</a></li>
               <li><a href="config" target={target}>Server konfig</a></li>
               <li><a href="config_posreport" target={target}>Egen posisjon</a></li>
               <li><a href="config_mapdisplay" target={target}>Visning på kart</a></li>
               <li>Datakanaler...</li>
               <ul>
               {
                  val chs = _api.getProperty("channels", null).split(",(\\s)*")
                  for (ch <- chs) yield
                     <li><a href={"config_chan?chan="+ch} target={target}>{ch}</a></li>
               }
               </ul>
             </ul>
             <br/>
             <a id="restart" href="restartServer" target={target}><b>[RESTART]</b></a>
             
             </div>
             <div id="config_main">
                <iframe id="config_main" name="config_main" src="admin?cmd=info"/>
             </div>
             ;
             
             
          printHtml (res, htmlBody(req, head, action(req)))
      }
      
      
      
      def handle_config(req : Request, res: Response) =
      { 
          val prefix = <h3>Konfigurasjon av Polaric APRSD</h3>
          
          def fields(req : Request): NodeSeq =
                textField("default.mycall", "item1", "Kallesignal:", "", 10, 10, CALLSIGN) ++
                textField("user.admin", "item2", "Admin brukere:", "Fulle admin-rettigheter", 30, 200, TEXT, "(regex)") ++  
                textField("user.update", "item3", "SAR brukere:", "Vanlige SAR brukere", 30, 200, TEXT, "(regex)") ++
                br ++
                textField("channels", "item4", "Datakanaler:", "Datakanaler for sporing", 30, 50, LIST, "(liste)")   ++
                textField("channel.default.inet", "item5", "Primær APRS/IS kanal:", "", 10, 10, NAME) ++ 
                textField("channel.default.rf", "item6", "Primær RF kanal:", "", 10, 10, NAME) ++ 
                br ++
                label("item7", "leftlab", "Igate:", "Kryss av for å aktivere RF<->internett gateway") ++
                boolField("igate.on", "item7", "Aktivert.") ++ br ++ 
                label("item8", "leftlab", "Igating til RF:", "Kryss av for å aktivere internet->RF igating") ++
                boolField("igate.rfgate.allow", "item8", "Aktivert.") ++
                boolField("objects.rfgate.allow", "item9", "RF igating for objekter.") ++ br ++
                textField("objects.rfgate.range", "item10", "Radius objekter:", "Område for utsending av objekter på RF", 6, 10, NUMBER, "(km)") ++
                textField("igate.rfgate.path", "item11", "Digipeater sti, igate:", "Default (se også neste felt)", 20, 30, LIST) ++
                textField("message.rfpath", "item12", "Digipeater sti, meldinger:", "...gjelder også meldinger til RF igate", 20, 30, LIST) ++
                textField("objects.rfpath", "item13", "Digipeater sti, objekter: ", "...gjelder også objekter til RF igate", 20, 30, LIST) ++
                br ++ 
                label("item14", "leftlab", "Fjernkontroll:", "Kryss av for å aktivere fjernkontroll") ++
                boolField("remotectl.on", "item14", "Aktivert.") ++
                br ++
                textField("remotectl.connect", "item15", "Fk server:", "Fk server (kallesignal)", 10, 10, NAME) ++
                textField("message.auth.key", "item16", "Autentiseringsnøkkel:", "Nøkkel for autentisering (for fjernkontroll)", 20, 30, TEXT)
               ;
              
              
         def action(req : Request): NodeSeq = 
         {
               refreshPage(res, 3, "config")
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
              
         printHtml (res, htmlBody (req, null, htmlForm(req, prefix, IF_ADMIN(fields), IF_ADMIN(action), simple_submit)))
     }
     
     
      
      def handle_config_mapdisplay(req : Request, res: Response) =
      { 
          val prefix = <h3>Kartvisnings innstillinger</h3>
          
          def fields(req : Request): NodeSeq =
                textField("aprs.expiretime", "item1", "Maks inaktivitet:", "Hvor lenge kan objekt være inaktivt før det forsvinner", 4, 4, NUMBER, "(minutter)") ++
                br ++
                textField("map.trail.maxPause", "item2", "Maks inaktivitet for spor:", "Hvor lenge kan objekt være inaktivt før spor forsvinner", 4, 4, NUMBER, "(minutter)") ++
                textField("map.trail.maxPause.extended", "item3", ".. sakte fart:", "Maks inaktivitetstid når fart er lav", 4, 4, NUMBER, "(minutter)") ++          
                br ++
                textField("map.trail.maxAge", "item4", "Sporlengde:", "Hvor langt tidsrom skal et spor tegnes for", 4, 4, NUMBER, "(minutter)") ++
                textField("map.trail.maxAge.extended", "item5", ".. sakte fart:", "Sporlengde når fart er lav", 4, 4, NUMBER, "(minutter)") 
         ;
              
              
          def action(req : Request): NodeSeq = 
          {
               refreshPage(res, 3, "config_mapdisplay")
               br ++ br ++
               getField(req, "item1", "aprs.expiretime", 0, 1440) ++ 
               getField(req, "item2", "map.trail.maxPause", 0, 1440) ++
               getField(req, "item3", "map.trail.maxPause.extended", 0, 1440) 
               getField(req, "item4", "map.trail.maxAge", 0, 1440) ++
               getField(req, "item5", "map.trail.maxAge.extended", 0, 1440) 
          }
              
          printHtml (res, htmlBody (req, null, htmlForm(req, prefix, IF_ADMIN(fields), IF_ADMIN(action), simple_submit)))
      }     
      
      
      
      
      def handle_passwd(req: Request, res: Response) = 
      {
          val prefix = <h3>Registrer bruker/passord</h3>
          var username = getAuthUser(req)
          
          
          
          def fields(req : Request): NodeSeq =
             label("item1", "lleftlab", "Brukernavn:", "Brukernavn for ny eller eksisterende bruker") ++
             { if (authorizedForAdmin(req))
                  textInput("item1", 20, 20, NAME, "")
               else 
                  <label id="item1">{username}</label>
             } ++
             br ++
             label("item2", "lleftlab", "Passord:", "") ++
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
                 <h3>Passord for bruker '{username}' oppdatert</h3>
             else if (res == 5)
                 <h3>Feil: Oppgitt verdi er for lang</h3>
             else if (res == 6)
                 <h3>Feil: Oppgitt verdi inneholder ulovlige tegn</h3>
             else 
                 <h3>Feil: Kunne ikke oppdatere (server problem)</h3>
       
          }
 
 
          printHtml (res, htmlBody (req, null, htmlForm(req, prefix, IF_AUTH(fields), IF_AUTH(action))))
      }
      
      
      
      
      
      def handle_config_posreport(req : Request, res: Response) =
      { 
          val prefix = <h3>Sporing av egen posisjon</h3>
          
          
          def fields(req : Request): NodeSeq =
                label("item1", "leftlab", "Posisjonsrapport:", "Kryss av for å aktivere posisjonsrapportering") ++
                boolField("ownposition.tx.on", "item1", "Aktivert.") ++  
                boolField("ownposition.tx.allowrf", "item2", "Tillat sending på RF.") ++
                br ++ br ++
                label("item4", "leftlab", "Symbol:", "APRS symboltabell og symbol") ++
                textInput("item4", 1, 1, ".", ""+_api.getProperty("ownposition.symbol", "/c")(0)) ++
                textInput("item5", 1, 1, ".", ""+_api.getProperty("ownposition.symbol", "/c")(1)) ++
                br ++
                textField("ownposition.tx.rfpath", "item6", "Digipeater sti:", "", 20, 30, LIST) ++
                textField("ownposition.tx.comment", "item7", "Beskrivelse:", "", 20, 40, TEXT) ++ br ++
                label("utmz", "leftlab", "Min posisjon:", "Serverens posisjon i UTM format") ++        
                utmField("ownposition.pos") ++ br ++ 
                br ++
                label("item8", "leftlab", "Tracking med GPS:", "Kryss av for å bruke posisjon fra GPS") ++
                boolField("ownposition.gps.on", "item8", "Aktivert.") ++ 
                boolField("ownposition.gps.adjustclock", "item9", "Juster klokke fra GPS.") ++ br ++
                textField("ownposition.gps.port", "item10", "GPS Port:", "Serieport enhetsnavn (f.eks. /dev/ttyS0)", 12, 20, NAME) ++
                textField("ownposition.gps.baud", "item11", "GPS Baud:", "", 6, 8, NUMBER) ++ br ++
                textField("ownposition.minpause", "item12", "Min pause:", "Minimum tid mellom sendinger", 4, 5, NUMBER, "(sekunder)") ++
                textField("ownposition.maxpause", "item13", "Maks pause:", "Maksimum tid mellom sendinger", 4, 5, NUMBER, "(sekunder)") ++
                textField("ownposition.mindist", "item14", "Min distanse:", "Distanse mellom sendinger når fart er lav", 4, 5, NUMBER, "(meter)") ++
                textField("ownposition.maxturn", "item15", "Maks turn:", "Maks endring i retning før sending", 4, 5, NUMBER, "(grader)")
              ;
              
              
         def action(req : Request): NodeSeq = 
         {       
              refreshPage(res, 3, "config_posreport")
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
              
         printHtml (res, htmlBody (req, null, htmlForm(req, prefix, IF_ADMIN(fields), IF_ADMIN(action), simple_submit)))
     }
     
     
     
     
     def handle_config_chan(req: Request, res: Response) = 
     {
         val cid = req.getParameter("chan")
         val chp = "channel."+cid
         val prefix = <h3>Kanal '{cid}'</h3>

         val is_aprsis = _api.getProperty(chp+".type", "APRSIS").equals("APRSIS")
         val ch = _api.getChanManager().get(cid)

         
         def fields(req : Request): NodeSeq = 
         {      
               if (ch != null) 
                   refreshPage(res, 60, "config_chan?chan="+cid);
               { if (ch != null) 
                    simpleLabel("info1", "leftlab", "Hørte stasjoner:", TXT(""+ch.nHeard())) ++
                    simpleLabel("info2", "leftlab", "Trafikk inn:", TXT(""+ch.nHeardPackets()+"  ("+ch.nDuplicates()+" duplikater)")) ++
                    simpleLabel("info3", "leftlab", "Trafikk ut:", TXT(""+ch.nSentPackets())) ++ br
                 else <span></span>
               } ++
               label("item1", "leftlab", "Kanal:", "Kryss av for å aktivere kanal") ++
               boolField(chp+".on", "item1", "Aktivert") ++ br ++
               textField(chp+".type", "item2", "Type:", "Type (APRSIS, TNC2 eller KISS)", 10, 10, CHANTYPE) ++
               { 
                 if (is_aprsis) 
                    textField(chp+".host", "item4", "Server adresse:", "DNS navn eller IP adresse for APRS/IS server", 20, 30, NAME) ++
                    textField(chp+".port", "item5", "Server port:", "Portnr", 6, 6, NUMBER) ++
                    textField(chp+".pass", "item6", "Passkode:", "APRS/IS verifikasjonskode", 6, 6, NUMBER) ++
                    textField(chp+".filter", "item7", "Filter:", "APRS/IS filter-streng", 30, 50, TEXT) 
                 else 
                    textField(chp+".port", "item8", "Port:", "Serieport enhetsnavn (f.eks. /dev/ttyS0)", 12, 20, NAME) ++
                    textField(chp+".baud", "item9", "Baud:", "", 6, 8, NUMBER) 
               } ++ br ++
               boolField(chp+".restrict", "item10", "Synlig bare for innloggede brukere") ++
               textField(chp+".style", "item11", "CSS stilnavn:", "", 10, 10, NAME)
         }
         
         
         
         def action(req : Request): NodeSeq = 
         {
              refreshPage(res, 3, "config_chan?chan="+cid)
              br ++ br ++
              getField(req, "item1", chp+".on", BOOLEAN) ++
              getField(req, "item2", chp+".type", CHANTYPE) ++
              { if (is_aprsis)
                   getField(req, "item4", chp+".host", NAME) ++ 
                   getField(req, "item5", chp+".port", 1024, 65535) ++ 
                   getField(req, "item6", chp+".pass", 0, 99999) ++ 
                   getField(req, "item7", chp+".filter", TEXT) 
                else
                   getField(req, "item8", chp+".port", NAME) ++
                   getField(req, "item9", chp+".baud", 300, 999999)
              } ++
              getField(req, "item10", chp+".restrict", BOOLEAN) ++
              getField(req, "item11", chp+".style", NAME) 
         }
              
         printHtml (res, htmlBody (req, null, htmlForm(req, prefix, IF_ADMIN(fields), IF_ADMIN(action), simple_submit)))     
     }
     
     
  }

}