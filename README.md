# clientServerTCP
Send files between client and server (JAVA) (TCP)
Multiple clients simultaneous

## Arguments:
java LFTClient modo=SSL host=192.168.1.33 puerto=1721 carpeta_cliente=c:\lft
java LFTServer puerto=1721 carpeta_servidor=/var/lft/carpeta max_clientes=10

### Client
carpeta_cliente=xxx
puerto=xxxx
host=xxx.xxx.xxx.xxx
modo=xxx (not working)

### Server
carpeta_servidor=xxx
puerto=xxxx
max_clientes=xx

## Comandos:
get <file>
put <file>
list
exit
