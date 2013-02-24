 


ctxtMenu.addCallback("TOOLBAR", function (m)
{     
      if (!isAdmin() && !canUpdate())
          return;
      m.add(null);
      if (isAdmin() || canUpdate()) {
         m.add("Bruker/passord..", passwd);
         if (isAdmin())
            m.add("Admin/konfigurasjon..", webConfig);
         else
            m.add("Server status info..", adminWindow);
      }
});


function passwd()
  { fullPopupWindow('Passord', server_url + 'srv/passwd', 430, 250); }

function webConfig()
  { fullPopupWindow('Konfig', server_url + 'srv/config_menu', 900, 700); }

