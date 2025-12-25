import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ErasePic extends JPanel implements ActionListener {
    private Timer timer;
    private Image img;
    private boolean frozen=true;
    
    private static final int MAX_X=300;
	private static final int MAX_Y=100;
    
    public ErasePic() {
    	super();
    	img=Toolkit.getDefaultToolkit().getImage("icons2.gif");
    	addMouseListener(new MouseAdapter() {
    		public void mousePressed(MouseEvent e) { frozen=!frozen; }
          });
    	startAnim();
    }
    
    public void paintComponent(Graphics g) {
    	if (frozen) {
         // super.paintComponent(g);  // אין שימוש (!) בגלל שהציור על כל משטח היישום
            g.drawImage(img,0,0,MAX_X,MAX_Y,this);
    	}
        else {
      	    g.setColor(getBackground());
			int x=(int)(Math.random()*MAX_X);    // הגרלת נקודה למחיקה 
			int y=(int)(Math.random()*MAX_Y);
    	    g.fillRect(x,y,4,4);    // מחיקת ריבוע קטן
        }
    }
    
    public void actionPerformed(ActionEvent e) {
    	repaint();
    }
    
    public void startAnim() {
    	if (timer==null) {
    		// "erase" כפתור
    		timer=new Timer(5,this);
    		timer.start();
    	}
    	else if (!timer.isRunning())
    	    // "reset" כפתור
    	    timer.restart();
    }
    
	public static void main(String[] args) {
		JFrame frame=new JFrame("Erase/Reset by mouse click");
		frame.getContentPane().add(new ErasePic());
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) { System.exit(0); }
		  });
		frame.setSize(MAX_X,MAX_Y);
		frame.show();
	}
}
