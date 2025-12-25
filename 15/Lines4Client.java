// Client for the Lines4 program

// Java core packages
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;

// Java extension packages
import javax.swing.*;

// Client class to let a user play Lines4 with
// another user across a network.
public class Lines4Client extends JApplet
   implements Runnable {

   private JTextField idField;
   private JTextArea displayArea;
   private JPanel boardPanel, panel2;
   private Square board[][];
   private Socket connection;
   private DataInputStream input;
   private DataOutputStream output;
   private Thread outputThread;
   private char myMark;
   private boolean myTurn;

   final private int ROWS = 6;
   final private int COLS = 7;

   // Set up user-interface and board
   public void init()
   {
	  Container container = getContentPane();
 
	  // set up JTextArea to display messages to user
	  displayArea = new JTextArea( 5, 30 );
	  displayArea.setEditable( false );
	  container.add( new JScrollPane( displayArea ),
		 BorderLayout.SOUTH );

	  // set up panel for squares in board
	  boardPanel = new JPanel();
	  boardPanel.setLayout( new GridLayout( ROWS, COLS, 0, 0 ) );

	  // create board
	  board = new Square[ ROWS ][ COLS ];

	  // When creating a Square, the location argument to the
	  // constructor is a value from 0 to 41 indicating the
	  // position of the Square on the board. Values 0, 1,
	  // and 2..6 are the first row, values 7, 8, and 9..13 are the
	  // second row...
	  for ( int row = 0; row < board.length; row++ ) {

		 for ( int column = 0;
				   column < board[ row ].length; column++ ) {

			// create Square
			board[ row ][ column ] =
			   new Square( ' ', row * COLS + column );

			boardPanel.add( board[ row ][ column ] );        
		 }

	  }

	  // textfield to display player's mark
	  idField = new JTextField();
	  idField.setEditable( false );
	  container.add( idField, BorderLayout.NORTH );
      
	  // set up panel to contain boardPanel (for layout purposes)
	  panel2 = new JPanel();
	  panel2.add( boardPanel, BorderLayout.CENTER );
	  container.add( panel2, BorderLayout.CENTER );
	  
	  setSize( 300, 300 );
	  setVisible( true );
   }

   // Make connection to server and get associated streams.
   // Start separate thread to allow this applet to
   // continually update its output in text area display.
   public void start()
   {
	  String host = getParameter( "host" ), port = getParameter( "port" );
	
	  if ( host == null )
	     host = "localhost";
      if ( port == null )
         port = "1800";

	  // connect to server, get streams and start outputThread
	  try {
		 // make connection
		 connection = new Socket( host, Integer.parseInt( port ) );

		 // get streams
		 input = new DataInputStream(
			connection.getInputStream() );
		 output = new DataOutputStream(
			connection.getOutputStream() );
	  }

	  // catch problems setting up connection and streams
	  catch ( IOException ioException ) {
		 // ioException.printStackTrace();         
	  }

	  // create and start output thread
	  outputThread = new Thread( this );
	  outputThread.start();
   }

   // control thread that allows continuous update of the
   // text area displayArea
   public void run()
   {
	  // get player's mark (X or O)
	  try {
	  	 int myNumber;
		 myNumber = input.readInt();
		 myMark = ( ( myNumber % 2 == 0 ) ? 'X' : 'O' );
  	     idField.setText( "You are player " + ( myMark == 'X' ? "Red" : "Blue" ) + ( myNumber+1 ) );
		 myTurn = ( myMark == 'X' ? true : false );
	  }

	  // process problems communicating with server
	  catch ( IOException ioException ) {
		 // ioException.printStackTrace();         
	  }

	  // receive messages sent to client and output them
	  while ( true ) {

		 // read message from server and process message
		 try {
			String message = input.readUTF();
			processMessage( message );
		 }

		 // process problems communicating with server
		 catch ( IOException ioException ) {
			displayArea.append( "Server problems.\n" );
			return;
			// ioException.printStackTrace();         
		 }
	  }

   }  // end method run

   // process messages received by client
   public void processMessage( String message )
   {
	  // valid move occurred
	  if ( message.equals( "Valid move." ) ) {
		 displayArea.append( "Valid move, please wait.\n" );

		// get move location and update board
		try {
		   final int newLocation = input.readInt();
         
		   // set mark in square from event-dispatch thread
		   SwingUtilities.invokeLater(
         
			new Runnable() {
         
			   public void run()
			   {
				int row    = newLocation / COLS;
				int column = newLocation % COLS;
         
				board[ row ][ column ].setMark( myMark );
			   }
         
			}
         
		   ); // end call to invokeLater
		   
		 }
		 
		 // process problems communicating with server
		 catch ( IOException ioException ) {
		 	message = "Server problems.";
			// ioException.printStackTrace();         
		 }

	  }

	  // invalid move occurred
	  else if ( message.equals( "Invalid move, try again" ) ) {
		 displayArea.append( message + "\n" );
		 myTurn = true;
	  }

  	  // opponent moved
	  else if ( message.equals( "Opponent moved. Game over" ) ) {
	  	
	     // get move location and update board
	     try {
		    final int location = input.readInt();
         
		    // set mark in square from event-dispatch thread
		    SwingUtilities.invokeLater(
         
			   new Runnable() {
         
				  public void run()
				  {
				     int row    = location / COLS;
				     int column = location % COLS;
         
				     board[ row ][ column ].setMark(
					    ( myMark == 'X' ? 'O' : 'X' ) );
			  	     displayArea.append( "Opponent moved\nand wins.\n" );
				  }
         
			   }
         
		    ); // end call to invokeLater
                 
		    myTurn = false;
	     }

	     // process problems communicating with server
	     catch ( IOException ioException ) {
			message = "Server problems.";
		    // ioException.printStackTrace();         
	      }

	  }

	  // opponent moved
	  else if ( message.equals( "Opponent moved" ) ) {

		 // get move location and update board
		 try {
			final int location = input.readInt();
         
			// set mark in square from event-dispatch thread
			SwingUtilities.invokeLater(
         
			   new Runnable() {
         
				  public void run()
				  {
					 int row    = location / COLS;
					 int column = location % COLS;
         
					 board[ row ][ column ].setMark(
						( myMark == 'X' ? 'O' : 'X' ) );
					 displayArea.append( "Opponent moved\n" );
				  }
         
			   }
         
			); // end call to invokeLater
                 
			myTurn = true;
		 }

		 // process problems communicating with server
		 catch ( IOException ioException ) {
			message = "Server problems.";
			// ioException.printStackTrace();         
		 }

	  }

	  // simply display message
	  else
		 displayArea.append( message + "\n" );

	  displayArea.setCaretPosition(
		 displayArea.getText().length() );

   }  // end method processMessage

   // send message to server indicating clicked square
   public void sendClickedSquare( int location )
   {
	  if ( myTurn ) {

		 // send location to server
		 try {
			output.writeInt( location );
			myTurn = false;
		 }

		 // process problems communicating with server
		 catch ( IOException ioException ) {
			// ioException.printStackTrace();
		 }
	  }
   }

   // private class for the sqaures on the board
   private class Square extends JPanel {
	  private char mark;
	  private int location;
	  final private int dimX = 30;
   
	  public Square( char squareMark, int squareLocation )
	  {
		 mark = squareMark;
		 location = squareLocation;

		 addMouseListener( 

			new MouseAdapter() {

			   public void mouseReleased( MouseEvent e )
			   {
				  sendClickedSquare( getSquareLocation() );
			   }

			}  // end anonymous inner class

		 ); // end call to addMouseListener

	  }  // end Square constructor

	  // return preferred size of Square
	  public Dimension getPreferredSize() 
	  { 
		 return new Dimension( dimX, dimX );
	  }

	  // return minimum size of Square
	  public Dimension getMinimumSize() 
	  {
		 return getPreferredSize();
	  }

	  // set mark for Square
	  public void setMark( char newMark ) 
	  { 
		 mark = newMark; 
		 repaint(); 
	  }
   
	  // return Square location
	  public int getSquareLocation() 
	  {
		 return location; 
	  }
   
	  // draw Square
	  public void paintComponent( Graphics g )
	  {
		 super.paintComponent( g );

		 g.drawLine( 0, 0, 0, dimX );
		 g.drawLine( dimX-1, 0, dimX-1, dimX );
		 
		 if ( location / COLS == ROWS-1 )
		    g.drawLine( 0, dimX-1, dimX-1, dimX-1 );
		 
		 if ( mark == 'X' ) { 
		    g.setColor( Color.red );
		    g.fillRect( dimX/3, dimX/3, dimX/3, dimX/3 );
	     }
		 else if ( mark == 'O' ) {
		    g.setColor( Color.blue );
			g.fillRect( dimX/3, dimX/3, dimX/3, dimX/3 );
		 }
	  }

   }  // end class Square
 
}  // end class Lines4Client
