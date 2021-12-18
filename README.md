# BungeeDynamicSync

Use pub/sub channel send data to spigot server  

Before Spigot server closing, first Spigot plugin teleport player to hub, second send pub/sub message to bungeecord, third bungeecord remove it from server list  
Spigot server's plugin will get container name by reading ENV CONTAINER_NAME value

```  
Pub/Sub message format:

Change master controller: 								      | CONTROLLER | UPDATE | <proxy id>            |
Add a new dynamic server(broadcast by master controller):     | SERVER     | ADD    | <CONTAINER_NAME>      | <ip> | <port> | <motd> |
Remove a dynamic server(broadcast by spigot dynamic server):  | SERVER     | DEL    | <CONTAINER_NAME>      |
Sync: | SYNC | BUNGEE | DYNAMIC_SERVER_LIST |

```  
