# Allow polaric user to restart server and to change user pw
polaric	ALL = (ALL) NOPASSWD: /etc/init.d/polaric-aprsd
polaric	ALL = (ALL) NOPASSWD: /usr/bin/htpasswd
