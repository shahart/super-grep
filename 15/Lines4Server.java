// This class maintains a game of Lines4 for two
// client applets.

// Java core packages
import java.awt.*;
import java.net.*;
import java.io.*;

// Java extension packages
import javax.swing.*;

public class Lines4Server extends JFrame {
   private byte board[][];           
   private JTextArea outputArea;
   private Player players[];
   private ServerSocket server;
   private int currentPlayer[];
   private boolean gameOver[];
   private int occupied[];

   final private int ROWS = 6;
   final private int COLS = 7;
   private final int SEQ  = 4; // length of SEQuence
   
   private final int COUPLES = 3;

   // set up Lines4 server and GUI that displays messages
   public Lines4Server()
   {
	  super( "Lines4 Server" );

	  board = new byte[ COUPLES ][ ROWS*COLS ]; 
	  players = new Player[ 2*COUPLES ];
	  currentPlayer = new int[ COUPLES ];
	  
	  int index;
	  for ( index = 0; index < players.length; index+=2 )
	     currentPlayer[ index/2 ] = index;
	     
	  gameOver = new boolean[ COUPLES ];
	  occupied = new int[ COUPLES ];
 
	  // set up ServerSocket
	  try {
		 server = new ServerSocket( 1800, 2 );
	  }

	  // process problems creating ServerSocket
	  catch( IOException ioException ) {
		 // ioException.printStackTrace();
		 System.exit( 1 );
	  }

	  // set up JTextArea to display messages during execution
	  outputArea = new JTextArea();
	  getContentPane().add( outputArea, BorderLayout.CENTER );
	  outputArea.setText( "Server awaiting connections\n" );

	  setSize( 250, 350 );
	  setVisible( true );
   }

   // wait for two connections so game can be played
   public void execute()
   {
	  // wait for each client to connect
	  for ( int i = 0; i < players.length; i++ ) {

		 // wait for connection, create Player, start thread
		 try {
			players[ i ] = new Player( server.accept(), i );
			players[ i ].start();
		 }

		 // process problems receiving connection from client
		 catch( IOException ioException ) {
			// ioException.printStackTrace();
			System.exit( 1 );
		 }
		 
		 if ( i % 2 != 0 )

	        // Player X is suspended until Player O connects.
	        // Resume player X now.          
	        synchronized ( players[ i-1 ] ) {
		       players[ i-1 ].setSuspended( false );   
		       players[ i-1 ].notify();
	        }
      }
        
   }  // end method execute
   
   // display a message in outputArea
   public void display( String message )
   {
	  outputArea.append( message + "\n" );
   }
 
   // Determine if a move is valid.
   // This method is synchronized because only one move can be
   // made at a time.
   public synchronized int validMove( 
	  int location, int player )
   {
	  // while not current player, must wait for turn
	  while ( player != currentPlayer[player/2] ) {
         
		 // wait for turn
		 try {
			wait();
		 }

		 // catch wait interruptions
		 catch( InterruptedException interruptedException ) {
			// interruptedException.printStackTrace();
		 }
	  }

	  // if location not occupied, make move
	  if ( !isFullBoard( player ) && !isOccupied( location, player ) && !gameOver[player/2] ) {

         while ( location+COLS < ROWS*COLS && !isOccupied( location+COLS, player ) )
            location += COLS;
  
		 // set move in board array
		 board[ currentPlayer[player/2] / 2 ][ location ] =
			( byte ) ( currentPlayer[player/2] % 2 == 0 ? 'X' : 'O' );
			
		 occupied[player/2]++;

		 // change current player
		 if ( currentPlayer[player/2] % 2 == 0 )
		    currentPlayer[player/2]++;
		 else
		    currentPlayer[player/2]--;

		 gameOver[player/2] = isGameOver( location, player );

		 // let new current player know that move occurred
		 players[ currentPlayer[player/2] ].otherPlayerMoved( location );

		 // tell waiting player to continue
		 notify();             

		 // tell player that made move that the move was valid
		 return location;
	  }

	  // tell player that made move that the move was not valid
	  else 
		 return -1;
   }

   public boolean isFullBoard( int player )
   {
	  if ( occupied[player/2] >= ROWS*COLS )
	     return true;
	  else
	     return false;
   }

   // determine whether location is occupied
   public boolean isOccupied( int location, int player )
   {
	  if ( board[ currentPlayer[player/2] / 2 ][ location ] == 'X'  
	    || board[ currentPlayer[player/2] / 2 ][ location ] == 'O' )
		  return true;
	  else
		  return false;
   }

   private boolean isMine( int loc, int player )
   {
   	  if ( 0 <= loc && loc < ROWS*COLS ) 
   	     if ( board[ currentPlayer[player/2] / 2 ][ loc ] == ( byte ) ( currentPlayer[player/2] %2 != 0 ? 'X' : 'O' ) )
   	        return true;
   	        
   	  return false;
   }

   private boolean checkSeq( int loc, int offset, int player )
   {
   	  int index;
   	  
   	  for ( index = 0; index < SEQ; index++ ) 
   	     if ( ! isMine( loc + index*offset, player ) )
   	        return false;

      display( "game over for player " + (player+1) + " by offset: " + offset);
      display( "   row: " + loc / COLS + " col: " + loc % COLS );
   	  
   	  return true;
   }
   
   // place code in this method to determine whether game over
   // CAN BE Optimized 
   public boolean isGameOver( int loc, int player )
   {
   	  boolean winner = false;
   	  int tempLoc, index;
   	  
   	  // rows
   	  tempLoc = loc / COLS * COLS;
   	  for ( index = 0; index <= COLS-SEQ; index++ )
    	 winner = winner || checkSeq( tempLoc + index, 1, player );
   	  
      // column down
      winner = winner || checkSeq( loc, COLS, player );      
      
      // diagonal /
      tempLoc = loc - Math.min(SEQ-1,COLS-loc%COLS-1)*(COLS-1);
	  for ( index = 0; index <= ROWS-SEQ+1; index++ )
	     winner = winner || checkSeq( tempLoc + index*(COLS-1), COLS-1, player ); 
 	  
      // diagonal \
	  tempLoc = loc - Math.min(SEQ-1,loc%COLS)*(COLS+1);
	  for ( index = 0; index <= ROWS-SEQ+1; index++ )
	     winner = winner || checkSeq( tempLoc + index*(COLS+1), COLS+1, player ); 
 	     
	  return winner;
   }

   // execute application
   public static void main( String args[] )
   {
	  Lines4Server application = new Lines4Server();

	  application.setDefaultCloseOperation( 
		 JFrame.EXIT_ON_CLOSE );

	  application.execute();
   }

   // private inner class Player manages each Player as a thread
   private class Player extends Thread {
	  private Socket connection;
	  private DataInputStream input;
	  private DataOutputStream output;
	  private int playerNumber;
	  private char mark;
	  protected boolean suspended = true;

	  // set up Player thread
	  public Player( Socket socket, int number )
	  {
		 playerNumber = number;

		 // specify player's mark
		 mark = ( playerNumber % 2 == 0 ? 'X' : 'O' );

		 connection = socket;
         
		 // obtain streams from Socket
		 try {
			input = new DataInputStream(
			   connection.getInputStream() );
			output = new DataOutputStream(
			   connection.getOutputStream() );
		 }

		 // process problems getting streams
		 catch( IOException ioException ) {
			// ioException.printStackTrace();
			System.exit( 1 );
		 }
	  }

	  // send message that other player moved; message contains
	  // a String followed by an int
	  public void otherPlayerMoved( int location )
	  {
		 // send message indicating move
		 try {
			if ( gameOver[playerNumber/2] )
			   output.writeUTF( "Opponent moved. Game over" );
			else
			   output.writeUTF( "Opponent moved" );
			output.writeInt( location );

			if ( gameOver[playerNumber/2] )
			   connection.close();
		 }

		 // process problems sending message
		 catch ( IOException ioException ) { 
			// ioException.printStackTrace();
		 }
	  }

	  // control thread's execution
	  public void run()
	  {
		 // send client message indicating its mark (X or O),
		 // process messages from client
		 try {
			display( "Player " + ( playerNumber % 2 == 0 ?
			  "Red" : "Blue" ) + ( playerNumber+1 ) + " connected" );
 
			// send player's mark
			output.writeInt( playerNumber );

			// send message indicating connection
			output.writeUTF( "Player " +
			   ( playerNumber % 2 == 0 ? "Red connected\n" :
				  "Blue connected, please wait\n" ) );

			// if player X, wait for another player to arrive
			if ( mark == 'X' ) {
			   output.writeUTF( "Waiting for another player" );
   
			   // wait for player O
			   try {
				  synchronized( this ) {   
					 while ( suspended )
						wait();  
				  }
			   } 

			   // process interruptions while waiting
			   catch ( InterruptedException exception ) {
				  // exception.printStackTrace();
			   }

			   // send message that other player connected and
			   // player X can make a move
			   output.writeUTF(
				  "Other player connected. Your move." );
			}

			// while game not over
			while ( ! gameOver[playerNumber/2] && ! isFullBoard(playerNumber) ) {

			   // get move location from client
			   int location = input.readInt();
			   
			   // check for valid move
			   int newLocationAndValidity = validMove( location, playerNumber );

			   if ( newLocationAndValidity != -1 ) {
				  // display( "loc: " + newLocationAndValidity );
				  output.writeUTF( "Valid move." );
				  output.writeInt( newLocationAndValidity );
			   }
			   else 
				  output.writeUTF( "Invalid move, try again" );
			}         

			if ( gameOver[playerNumber/2] ) {
	           output.writeUTF( "Game over.\nYou are the winner." );
			}
			else /* if ( isFullBoard(playerNumber) ) */ {
	           output.writeUTF( "Game over.\ntie. (or opponent was quit)" );
			}

			// close connection to client
			connection.close();
		 }

		 // process problems communicating with client
		 catch( IOException ioException ) {
			// ioException.printStackTrace();
			display( "Player " + (playerNumber+1) + " was quit. The game was stopped");
			occupied[playerNumber/2] = ROWS*COLS;
			return; // System.exit( 1 );
		 }
	  }

	  // set whether or not thread is suspended
	  public void setSuspended( boolean status )
	  {
		 suspended = status;
	  }
   
   }  // end class Player

}  // end class Lines4Server
