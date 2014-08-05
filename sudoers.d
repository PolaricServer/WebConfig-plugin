# Allow polaric user to restart server and to change user pw
polaric	ALL = (ALL) NOPASSWD: /usr/bin/polaric-restart, /usr/bin/htpasswd *
