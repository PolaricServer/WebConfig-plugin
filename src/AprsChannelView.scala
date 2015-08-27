 
/* 
 * Copyright (C) 2015 by LA7ECA, Øyvind Hanssen (ohanssen@acm.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import java.util._
import java.io._
import scala.xml._
import org.simpleframework.transport.connect.Connection
import org.simpleframework.transport.connect.SocketConnection
import org.simpleframework.http._
import uk.me.jstott.jcoord._
import no.polaric.aprsd._
import no.polaric.aprsd.http._
import org.xnap.commons.i18n._



/**
 * Web view for Aprs Channels.
 */
package no.polaric.webconfig
{

   class AprsChannelView 
      ( override val api: ServerAPI, override val model: AprsChannel, override val req: Request) 
             extends ChannelView(api, model, req) with ConfigUtils
   {
   
         val is_aprsis = _api.getProperty(chp+".type", "APRSIS").equals("APRSIS")
         val is_tcpkiss = _api.getProperty(chp+".type", "APRSIS").equals("TCPKISS")

            
         protected def aprstraffic: NodeSeq = 
             simpleLabel("info1", "leftlab", I.tr("Heard stations")+":", TXT(""+model.nHeard())) ++
             simpleLabel("info2", "leftlab", 
                I.tr("Traffic in")+":", TXT(""+model.nHeardPackets()+"  ("+model.nDuplicates()+" "+I.tr("duplicates")+")")) ++
             simpleLabel("info3", "leftlab", I.tr("Traffic out")+":", TXT(""+model.nSentPackets())) ++ br     
             ;
         
                    
         
         protected def aprsis: NodeSeq = 
             if (is_aprsis)
                 textField(chp+".pass", "item6", 
                    I.tr("Passcode")+":", 
                    I.tr("APRS/IS verification code"), 6, 6, NUMBER) ++
                 textField(chp+".filter", "item7", 
                    I.tr("Filter")+":", 
                    I.tr("APRS/IS filter-string"), 30, 50, TEXT) 
             else EMPTY
             ;
           
              
             
         override def fields(req : Request): NodeSeq =   
              state ++
              showtype ++
              aprstraffic ++
              activate ++ 
              { 
                 if (is_aprsis || is_tcpkiss) 
                    backupchan ++
                    inetaddr ++
                    aprsis
                 else 
                    serialport
              } ++ br ++
              visibility
              ;
         
         
         
         override def action(req : Request): NodeSeq = 
              br ++ br ++
              getField(req, "item1", chp+".on", BOOLEAN) ++
              { if (is_aprsis || is_tcpkiss)
                   getField(req, "item3", chp+".backup", NAME) ++
                   action_inetaddr(chp) ++
                   { if (is_aprsis) 
                        getField(req, "item6", chp+".pass", 0, 99999) ++ 
                        getField(req, "item7", chp+".filter", TEXT)
                     else EMPTY
                   }
                else
                   getField(req, "item8", chp+".port", NAME) ++
                   getField(req, "item9", chp+".baud", 300, 999999)
              } ++
              getField(req, "item10", chp+".restrict", BOOLEAN) ++
              getField(req, "item11", chp+".style", NAME) ++ 
              action_activate
              ; 
             
   }
  
}