package communication;

import java.util.Collection;
import java.util.UUID;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import communication.commands.CommandType;
import communication.commands.iCommand;

public class CardNetServer{
	private SocketIOServer ioServer;
	private Configuration config;
	private boolean debugModeEnabled;
	
	public CardNetServer(){
		this(false);
	}
	
	/**
	 * Creates a new instance of CardNetServer.
	 * This acts as an interface around socket.io
	 * @param enableDebug enables or disables console logging from cardNet 
	 */
	public CardNetServer(boolean enableDebug){
		System.out.println("Initializing CardNet Server-Side");
		this.debugModeEnabled = enableDebug;
		//currently statically set, eventually load this from settings.json
		config = new Configuration();
		config.setHostname("localhost");
		config.setPort(11899);
		System.out.println("Creating server");
		ioServer = new SocketIOServer(this.config);
		
		//listen for clients connecting and disconnecting
		ioServer.addConnectListener(new ConnectListener(){
			public void onConnect(SocketIOClient arg0) {
				debugLog("Client " + arg0.getSessionId() + " has connected.");
			}
		});
		
		System.out.println("Done, starting...");
		ioServer.start();
	}
	
	/**
	 * Register a listener function for a specific command.
	 * @param command The CommandType to listen  for.
	 * @param listener The callback function to execute when a command is recieved.
	 */
	public void addOn(CommandType command, DataListener listener){
		System.out.println("Adding listener for " + command.getName() + "of class type " + command.getType() + " commands.");
		ioServer.addEventListener(command.getName(), command.getType(), listener);
	}
	
	/**
	 * retrieve the Socket.io engine object for advanced manipulation.
	 * @return the SocketIOServer bound to this instance.
	 */
	public SocketIOServer getServer(){
		return this.ioServer;
	}
	
	/**
	 * Retrieves a listing of all connected clients.
	 * @return a Collection<SocketIOClient> of all currently connected users.
	 */
	public Collection<SocketIOClient> getAllClients(){
		return this.ioServer.getAllClients();
	}
	
	/**
	 * Given a UUID, find the SocketIOClient connected.
	 * @param uuid the UUID of the user you're searching for.
	 * @return a SocketIOClient containing all the details regarding the user with the specified UUID.
	 */
	public SocketIOClient getClient(UUID uuid){
		return this.ioServer.getClient(uuid);
	}
	
	/**
	 * broadcasts a command to all connected clients
	 * @param c the command to send
	 */
	public void sendbroadcast(iCommand c){
		ioServer.getBroadcastOperations().sendEvent(c.getCommandType().getName(), utility.GsonHelper.gson.toJson(c));
	}
	/**
	 * send a command to a namespace
	 * @param c command to send
	 * @param namespace namespace to send to
	 */
	public void sendToNamespace(iCommand c, String namespace){
		ioServer.getNamespace(namespace).getBroadcastOperations().sendEvent(c.getCommandType().getName(), utility.GsonHelper.gson.toJson(c));
	}
	/**
	 * send a command to a room
	 * @param c command to send
	 * @param roomName room to send to
	 */
	public void sendToRoom(iCommand c, String roomName){
		ioServer.getRoomOperations(roomName).sendEvent(c.getCommandType().getName(), utility.GsonHelper.gson.toJson(c));
	}
	/**
	 * send a command to a specific client
	 * @param c command to send
	 * @param uuid the UUID of the client to send to
	 */
	public void sendToClient(iCommand c, UUID uuid){
		ioServer.getClient(uuid).sendEvent(c.getCommandType().getName(), utility.GsonHelper.gson.toJson(c));
	}
	
	/**
	 * Adds a name space to the server
	 * @param name the name to add space for
	 * @return the SocketIONamespace added
	 */
	public SocketIONamespace createGameServerNamespace(String name){
		return ioServer.addNamespace(name);
	}
	
	
	/**
	 * method to log data to console if debug mode is enabled
	 * @param s the string to print
	 */
	private void debugLog(String s){
		if(this.debugModeEnabled)
			System.out.println(s);
	}
}
