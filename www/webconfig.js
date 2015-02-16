 


ctxtMenu.addCallback("MAIN", function (m)
{     
      if (!isAdmin() && !canUpdate())
          return;
      m.add(null);
      if (isAdmin() || canUpdate()) {
         m.add("Bruker/passord..", setPasswd);
         if (isAdmin())
            m.add("Admin/konfigurasjon..", webConfig);
         else
            m.add("Server status info..", adminWindow);
      }
});


function setPasswd()
  { fullPopupWindow('Passord', server_url + 'srv/passwd'+'?lang='+language, 430, 250); }

function webConfig()
  { fullPopupWindow('Konfig', server_url + 'srv/config_menu'+'?lang='+language, 900, 700); }

function adminWindow()
  { fullPopupWindow('Status', server_url + 'srv/admin?cmd=info'+'&lang='+language, 800, 600); }