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

..

Truncated, so stats will be on C and Rust