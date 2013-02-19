 


ctxtMenu.addCallback("TOOLBAR", function (m)
   {
      m.add("Konfigurasjon...", function()
        { setTimeout('webConfig();', 100); });
   });



function webConfig()
{
   fullPopupWindow('Konfig', server_url + 'srv/config_menu', 900, 700);
}

