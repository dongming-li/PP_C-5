/**
 * The GameController is a parent class for more specific GameControllers of every game. The GameController class acts a go-between
 * of the game server and the graphics. This class has functions to send commands to the server and to send commands to the personal
 * client's graphics
 */

package client;

import javax.swing.JOptionPane;

import communication.Callback;
import communication.commands.CommandType;
import communication.commands.game.DisplayOpposingCardCommand;
import communication.commands.game.DrawCardCommand;
import communication.commands.game.EndGameCommand;
import communication.commands.game.EndTurnCommand;
import communication.commands.game.RecieveCardCommand;
import communication.commands.game.StartGameCommand;
import communication.commands.game.war.RoundStatusCommand;
import gameObjects.Card;
import graphics.GameGraphics;
import graphics.ParentLayout;
import io.socket.emitter.Emitter.Listener;
import utility.GsonHelper;

public abstract class GameController {
	
	
	protected GameGraphics gg;
	protected ParentLayout lay; //This way we can keep track of the layout in the parent, and in the subclass we'll assign this layout to the child class
	private boolean routesRegistered;
	
	/**
	 * The constructor sets routesRegistered to false so that the routes will be registered. We keep track of this to make sure the routes
	 * don't get registered more than once.
	 */
	public GameController(){
		routesRegistered = false;
	}
	
	/**
	 * @param startGameCmd
	 * The start method is used in the child classes. The start method does the set up in the graphics so that they can start recieving
	 * commands from the event handlers.
	 */
	public abstract void start(StartGameCommand startGameCmd);

	/**
	 * registerNetworkRoutes sets up the event handlers from the socket.io driven server. Each of these routes is an event handler and activates
	 * as soon as this client gets the specific command from the server
	 */
	protected void registerNetworkRoutes(){
		if(routesRegistered)
			return;
			
		Client.cardNet.addOnCmd(CommandType.ROUNDSTATUSCOMMAND, new Listener(){

			@Override
			public void call(Object... args) {
				//Round Status
				RoundStatusCommand rsc = utility.GsonHelper.fromJson((String)args[0], RoundStatusCommand.class);
				//gg.clearPlaySpace();
				gg.clearPlaySpace("War");
			}
			
		});
		
		/*Client.cardNet.addOnCmd(CommandType.DISPLAYCARDCOMMAND, new Listener(){
			@Override
			public void call(Object... args) {
				//Round Status
				DisplayCardCommand dcc = utility.GsonHelper.fromJson((String)args[0], DisplayCardCommand.class);	
				System.out.println("Other player had a " + dcc.getCard().getSuitAndValue());
				gg.displayCard(dcc.getCard());
			}
		});
		*/
		Client.cardNet.addOnCmd(CommandType.RECIEVECARDCOMMAND, new Listener(){
			@Override
			public void call(Object... args) {
				System.out.println("Recieved a card: " + (String)args[0]);
				String json = (String)args[0];
				System.out.println(json);
				RecieveCardCommand rcc = GsonHelper.fromJson(json, RecieveCardCommand.class);
				Card j = rcc.getCard();
				System.out.println(j.getSuitAndValue());
				gg.recieveCard("War", j);
				gg.startTurn("War");
			}
			
		});
		
		Client.cardNet.addOnCmd(CommandType.DISPLAYOPPOSINGCARD, new Listener(){
			@Override
			public void call(Object... args) {
				DisplayOpposingCardCommand docc = GsonHelper.fromJson((String)args[0], DisplayOpposingCardCommand.class);
				System.out.println("Opponent had a " + docc.getCard().getSuitAndValue());
				gg.displayCard("War", docc.getCard());
			}
			
		});
		
		Client.cardNet.addOnCmd(CommandType.ENDGAMECOMMAND, new Listener(){

			@Override
			public void call(Object... args) {
				//End war
				EndGameCommand egc = utility.GsonHelper.fromJson((String)args[0], EndGameCommand.class);	
				String message = "End of Game!";
				JOptionPane.showMessageDialog(null, message, "InfoBox: " + "War", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		
		System.out.println("Successfully registered all Game networking routes");
		routesRegistered = true;
	}
	
	/**
	 * This method requests, for displaying graphically, the current card that the current player would be using.
	 */
	public void requestDrawFromServer(){
			System.out.println("Requesting a card from the server.");
			Client.cardNet.forwardCommand(utility.GsonHelper.toJson(new DrawCardCommand()),CommandType.DRAWCARDCOMMAND.getName(), new Callback(){
			@Override
			public void call(Object... args) {
				
			}
		});
	}
	
	/**
	 * This method informs the server that the current players turn is complete.
	 */
	public void turnComplete(){
		Client.cardNet.forwardCommand(GsonHelper.toJson(new EndTurnCommand()), CommandType.ENDTURNCOMMAND.getName());
	}
	
	/**
	 * @param cardToDisplay
	 * This method is similar to requestDrawFromServer except that it gets the opponents card to dispaly instead of your own.
	 */
	public void requestOpponentDisplay(Card cardToDisplay){
		Client.cardNet.forwardCommand(GsonHelper.toJson(new DisplayOpposingCardCommand(cardToDisplay)), CommandType.DISPLAYOPPOSINGCARD.getName());
	}
	
}