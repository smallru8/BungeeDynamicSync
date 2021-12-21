# BungeeDynamicSync

Use pub/sub channel send data to spigot server  

Before Spigot server closing, first Spigot plugin teleport player to hub, second send pub/sub message to bungeecord, third bungeecord remove it from server list  
Spigot server's plugin will get container name by reading ENV CONTAINER_NAME value

```  
Pub/Sub message format:

Change master controller: 								                     | CONTROLLER | UPDATE  | <proxy id>            |
Add a new dynamic server(broadcast by master controller):                    | SERVER     | ADD     | <CONTAINER_NAME>      | <ip> | <port> | <motd> |
Remove a dynamic server(broadcast by spigot dynamic server after game over): | SERVER     | DEL     | <CONTAINER_NAME>      |
Game has started(broadcast by spigot dynamic server when game start):        | SERVER     | STARTED | <CONTAINER_NAME>      |

```  
On game start, server will turn on white list
When game over, server will send plugin message (channel: BDS:channel,Sender: player,data: Connect,data: hub) after bungeecord received, it will connect player to random hub

server save serverName:(current player/max player) to redis
add server name to Redis list : DynServer