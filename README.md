# BungeeDynamicSync

Use pub/sub channel send data to spigot server  

Before Spigot server closing, first Spigot plugin teleport player to hub, second send pub/sub message to bungeecord, third bungeecord remove it from server list  


```  
Pub/Sub message format:

Change master controller: 								      | CONTROLLER | UPDATE | <proxy id>            |
Add a new dynamic server(broadcast by master controller):     | SERVER     | ADD    | <dynamic server type> | <container id> | <ip> | <port> |
Remove a dynamic server(broadcast by spigot dynamic server):  | SERVER     | DEL    | <dynamic server type> | <container id> |

```  
