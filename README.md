# chat
A simple real-time chat server.

## Running the code 
`lein uberjar`  
`java -jar target/chat-standalone.jar host <someHostIP> port <somePort>`

## Usages
Please note that we will have separate endpoint for managing channels later.
For now, joining of channels should be notified via WebSocket connection.

Once websocket connection is established, server will send a JSON string of object: 
`{"event": "connected", "data": {"channels": {"id": ...,  "name": ... }}}`

To join a channel, send a message in JSON string of the following form:
`{"event": "join-channel", "data": {"channel-id": ...}}`

Once user joins a channel, server will send a JSON string of all messages currently in the channel:
`{"event": "channel-messages", "data": {"messages": [{"content": ..., "username": ...}]} }`

To send a chat message, send a message in JSON string of the following form:
`{"event": "add-message", "data": {"content": ... , "username": ...} }`



## License

Copyright © 2016

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
