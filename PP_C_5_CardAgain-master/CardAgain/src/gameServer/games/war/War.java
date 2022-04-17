package gameServer.games.war;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import communication.commands.CommandType;
import communication.commands.iCommand;
import communication.commands.game.DisplayCardCommand;
import communication.commands.game.EndGameCommand;
import communication.commands.game.EndTurnCommand;
import communication.commands.game.ForwardingCommand;
import communication.commands.game.RecieveCardCommand;
import communication.commands.game.StartGameCommand;
import communication.commands.game.StartTurnCommand;
import communication.commands.game.war.EndWarCommand;
import communication.commands.game.war.RoundStatusCommand;
import communication.commands.game.war.StartWarCommand;
import dataModels.User;
import gameObjects.Card;
import gameObjects.Deck;
import gameObjects.DeckTest;
import gameObjects.Games;
import gameServer.GameServer;
import gameServer.GameState;
import io.socket.client.Ack;
import server.MySQLGameAccess;

/**
 * War handles the game logic for the card game
 * and responds to commands given to it by the server
 * 
 * @author Jacob Richards
 *
 */
public class War implements GameState{
	private Map<UUID, WarPlayer> playerMap;
	private GameServer gameServer;
	private WarPlayer currentPlayer;
	private WarPlayer player1;
	private WarPlayer player2;
	private Deck gameDeck;
	private DeckTest gameDeckTest;
	private ArrayList<Card> playBase = new ArrayList<Card>();
	private boolean isWar;
	private boolean player1Win;
	private boolean player2Win;
	
	/**
	 * Constructor for War
	 * @param gs - a GameServer object
	 */
	public War(GameServer gs){
		this.gameServer = gs;
		this.playerMap = Collections.synchronizedMap(new LinkedHashMap<UUID,WarPlayer>());
	}
	
	/**
	 * Deals the given player their cards to be placed in their hand
	 * @param player
	 */
	// Deals half of the deck to the player
	public void DealCards(WarPlayer player)
	{
		/*
		for (int i = 0; i < 26; i++)
		{			
			player.playerHand.addCardToHand(gameDeck.draw());
		}
		*/
		for (int i = 0; i < 6; i++)
		{
			player.playerHand.addCardToHand(gameDeckTest.draw());
		}
	}
	
	/**
	 * This method performs the actions player 1 needs to progress their turn
	 * @param p1 - player 1
	 * @return - Returns the card that player 1 had drawn
	 */
	// Player one's turn, process of drawing card
	public static Card player1Turn(WarPlayer p1)
	{
		Card card = p1.playerHand.drawFromHand();
		
		return card;
	}
	
	/**
	 * This method performs the actions player 2 needs to progress their turn
	 * @param p2 - player 2
	 * @return - Returns the card that player 2 had drawn
	 */
	// Player two's turn, process of drawing card
	public static Card player2Turn(WarPlayer p2)
	{
		Card card = p2.playerHand.drawFromHand();
		
		return card;
	}
	
	/**
	 * Checks if the game should be over
	 * @return true if the game should be ended, false otherwise
	 */
	public boolean isGameOver()
	{
		if(player1.playerHand.isEmpty())
		{
			player1Win = false;
			player2Win = true;
			return true;
		}
		else if(player2.playerHand.isEmpty())
		{
			player1Win = true;
			player2Win = false;
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Ends the game and sends out messages declaring who won
	 */
	public void GameOver()
	{
		if(player1Win)
		{
			MySQLGameAccess.updateGamesPlayed(player1.uID, true);
			MySQLGameAccess.updateGamesPlayed(player2.uID, false);
			gameServer.broadcastCommandToAll(new EndGameCommand());
		}
		else if(player2Win)
		{
			MySQLGameAccess.updateGamesPlayed(player2.uID, true);
			MySQLGameAccess.updateGamesPlayed(player1.uID, false);
			gameServer.broadcastCommandToAll(new EndGameCommand());
		}
	}
	
	/**
	 * Compares the values of the 2 cards currently in play
	 * 
	 */
	public void CompareCards()
	{
		int cardCompare;
		
		if(isWar)
		{
			cardCompare = playBase.get(playBase.size() - 3).compareTo(playBase.get(playBase.size() - 1));
		}
		else
		{	// Player 1's card is first, Player 2's card is second
			cardCompare = playBase.get(0).compareTo(playBase.get(1));
		}
		
		// If Player 1's card is greater, he gets both cards
		if(cardCompare == 1)
		{
			if(isWar)
			{
				gameServer.broadcastCommandToAll(new EndWarCommand());
				isWar = false;
			}
			WinningCardsToDiscardPile(player1);
			DiscardPileToHand(player1);
			player1.hasPlayed = false;
			player2.hasPlayed = false;
			gameServer.sendCommandToPlayer(new RoundStatusCommand(true), player1.playerID);
			gameServer.sendCommandToPlayer(new RoundStatusCommand(false), player2.playerID);
			if(isGameOver())
			{
				GameOver();
			}
		}
		// If Player 2's card is greater, he gets both cards
		else if(cardCompare == -1)
		{
			if(isWar)
			{
				gameServer.broadcastCommandToAll(new EndWarCommand());
				isWar = false;
			}
			WinningCardsToDiscardPile(player2);
			DiscardPileToHand(player2);
			player1.hasPlayed = false;
			player2.hasPlayed = false;
			gameServer.sendCommandToPlayer(new RoundStatusCommand(false), player1.playerID);
			gameServer.sendCommandToPlayer(new RoundStatusCommand(true), player2.playerID);
			if(isGameOver())
			{
				GameOver();
			}
		}
		// If equal, go into a tie and both players lay down a face down card
		// both players then draw a face up card and compare values
		// The winner takes all cards laid down
		else if(cardCompare == 0)
		{
			if(!isWar)
			{
				//gameServer.broadcastCommandToAll(new StartWarCommand());
				gameServer.sendCommandToPlayer(new StartWarCommand(), player1.playerID);
				gameServer.sendCommandToPlayer(new StartWarCommand(), player2.playerID);
				isWar = true;
			}
			player1.hasPlayed = false;
			player2.hasPlayed = false;
						
		}
	}
	
	/**
	 * Transfers the current cards in play to the players dicardpile
	 * @param player - the winning player
	 */
	public void WinningCardsToDiscardPile(WarPlayer player)
	{
		int size = playBase.size();
		// Loops through all of the cards that have been played 
		// and adds them to the winners discard pile
		for(int i = 0; i < size; i++)
		{
			player.playerDiscPile.add(playBase.remove(0));
		}
	}
	
	/**
	 * Transfers cards from the discard pile to the give players hand
	 * @param player - player who is transferring cards
	 */
	public void DiscardPileToHand(WarPlayer player)
	{
		player.playerDiscPile.shuffle();
		
		// Add discard piles to each players hand
		if(player.playerDiscPile.size() > 0)
		{
			player.playerHand.addDiscardToHand(player.playerDiscPile);
		}
	}
	
	/**
	 * Progresses the game, ends the current players turn and moves on to the next player.
	 * If both players have gone, CompareCards() is called.
	 */
	//tell the current player to end their turn, tell the next player to start their turn
	//this method is really only set up to work with 2 players because War.
	//It loops through all connected players, and if the player is currently going it'll tell them their turn is over.
	//conversely, if the player isn't going, it'll tell them to start the game and update nextPlayer appropriately
	//Call this after you've done EVERYTHING ELSE
	public void advanceTurn(){
		if(player1.hasPlayed && player2.hasPlayed)
		{
			CompareCards();
		}
		
		if(currentPlayer.equals(player1)){
			sendCommandToPlayer(new StartTurnCommand(), player2);
			currentPlayer = player2;
		}
		else{
			sendCommandToPlayer(new StartTurnCommand(), player1);
			currentPlayer = player1;
		}
	}
	
	/**
	 * Maps UUID to a player when they connect
	 * @param uuid
	 * @param user
	 * @return - returns the Warplayer created for the user
	 */
	//maps UUID to a player when they connect
	public WarPlayer connectPlayer(UUID uuid, User user){
		System.out.println("Game Server: Mapping " + uuid + " to user " + user.getUsername() + ":" + user.getUserID());
		String playerName = user.getUsername();
		WarPlayer temp = new WarPlayer(playerName, uuid, user.getUserID());
		this.playerMap.put(uuid, temp);
		
		return temp;
	}
	/**
	 * Retrieves the WarPlayers listing for the user
	 * @param u - User
	 * @return - The listing for the user
	 */
	//use to retrieve the WarPlayer listing for the specified client
	public WarPlayer getMappedPlayer(UUID u){
		return this.playerMap.get(u);
	}
	
	
		
		//These event handlers will be called when the appropriate events are recieved from clients
		//This event handler  is called when the Game starts. Use it to manage anything we need to manage on startup.

	/**
	 * Initiates the creation of the game and all of the initial objects that need to be made
	 * to start the game.
	 */
	public void startGame() {
		int playerNum = 1;
		for(UUID u : this.gameServer.getConnectedClients().keySet()){
			gameServer.sendCommandToPlayer(new StartGameCommand(playerNum,getGameType(),this.gameServer.getConnectedClients().size()),u);
			playerNum++;
		}
		// Connects the 2 players, assigns them to player 1 or player 2
		Map<UUID,User> clientList = gameServer.getConnectedClients();
		ArrayList<UUID> uuidList = new ArrayList<UUID>(clientList.keySet());
		
		player1 = connectPlayer(uuidList.get(0),clientList.get(uuidList.get(0)));
		player2 = connectPlayer(uuidList.get(1),clientList.get(uuidList.get(1)));
		currentPlayer = player1;
		
		System.out.println("Player 1 is " + uuidList.get(0));
		System.out.println("Player 2 is " + uuidList.get(1));
		// Create and shuffle new deck
		//gameDeck = new Deck();
		//gameDeck.shuffle();
		
		gameDeckTest = new DeckTest();
		gameDeckTest.shuffle();
		
		// Does not start in War state
		isWar = false;
		
		// Deal half of the cards to each player
		DealCards(player1);
		DealCards(player2);
		
		//Tell player 1 to start their turn
		sendCommandToPlayer(new StartTurnCommand(),player1);
		
	}
		
		
	/**
	 * Draws a card for the player and sends the card to the server
	 * @param user
	 * @param ack
	 */
		//This event handler will fire when a user wants to Draw a card.
	public void drawCard(UUID user, Ack ack) {
		//This method is called when a user wants to draw a card.
		//The parameters you'll be given are:
		//client - the client who sent the command. They can be identified by client.UUID
		//data - The drawCardCommand sent. Doesn't really hold anything important so it's not really useful.
		//ack - this is how you implement the callback. In the case of this command, our Callback needs to tell the user what card they drew.
		WarPlayer player = getMappedPlayer(user);
		
		// If in a war, first card is face down, so does not need to be sent to client
		// just add card to playBase for winner to claim later
		if(isWar)
		{
			playBase.add(player.playerHand.drawFromHand());
		}
		
		// Gets top card from Hand
		Card drawnCard = player.playerHand.drawFromHand();
		
		//put it on the table
		playBase.add(drawnCard);
		
		// mark that the current player has played
		player.hasPlayed = true;
		
		//Network the card out to the players
		//tell the player what they drew
		sendCommandToPlayer(new RecieveCardCommand(drawnCard), player);
		//tell the other players what this person drew
		gameServer.broadcastCommandToAllBut(new DisplayCardCommand(drawnCard), user);
		
	}
	
	/**
	 * Sends the specified command to the specified player
	 * @param cmd - command
	 * @param p - player
	 */
	public void sendCommandToPlayer(iCommand cmd, WarPlayer p){
		this.gameServer.sendCommandToPlayer(cmd, p.playerID);
	}
	
	
	/**
	 * Handles War specific commands and calls the correct places
	 */
	//handles war specific commands and calls the right places
	@Override
	public void handleForwarding(ForwardingCommand cmd, Object... args) {
		String cmds = cmd.getCommand();
		String cmdType = cmd.getTypeString();
		iCommand comd = (iCommand) utility.GsonHelper.gson.fromJson(cmds, CommandType.getEnumByString(cmdType).getType());
		UUID sender = cmd.getUUID();
		switch(comd.getCommandType()){
		case DRAWCARDCOMMAND:
			if(args.length > 1){	
				drawCard(sender, (Ack)args[1]);
			}
			else{
				System.out.println("An ACK must be included when drawing a card.");
			}
			break;
		case ENDTURNCOMMAND:
			advanceTurn();
			break;
		default:
			System.out.println("No handler registered for " + cmdType + " command.");
			break;
		}
	}

	/**
	 * Returns the game type of the current game
	 */
	@Override
	public Games getGameType() {
		return Games.WAR;
	}
}
