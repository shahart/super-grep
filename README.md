A grep like tool with extra options like search for term1 (and (or and not) term2), context around the line, ...
(and super fast, 1 second vs 65)

**RUST kickoff**


    C:\\>\\Users\\user\\.cargo\\bin\\rustc.exe sg.rs
    C:\\>sg container

    1
    15/Lines4Client.java


      31       // Set up user-interface and board
      32       public void init()
      33       {
    * 34              Container container = getContentPane();
      35
      36              // set up JTextArea to display messages to user
      37              displayArea = new JTextArea( 5, 30 );
      38              displayArea.setEditable( false );
    * 39              container.add( new JScrollPane( displayArea ),
      40                     BorderLayout.SOUTH );
      41
      42              // set up panel for squares in board
      43              boardPanel = new JPanel();

      68              // textfield to display player's mark
      69              idField = new JTextField();
      70              idField.setEditable( false );
    * 71              container.add( idField, BorderLayout.NORTH );
      72
      73              // set up panel to contain boardPanel (for layout purposes)
      74              panel2 = new JPanel();
      75              panel2.add( boardPanel, BorderLayout.CENTER );
    * 76              container.add( panel2, BorderLayout.CENTER );
      77
      78              setSize( 300, 300 );
      79              setVisible( true );
      80       }


