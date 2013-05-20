import java.io.IOException;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class MyFrame extends JFrame
{
	private Phys2DGamePanel content;

	public MyFrame()
	{
		content = new Phys2DGamePanel("rsrc/level.xml");
		content.setPreferredSize(new Dimension(500, 400));
		setResizable(false);
		add(content);
		pack();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void runContent()
	{
		content.start();
	}

	public static void main(String[] args)
	{
		MyFrame mf = new MyFrame();
		mf.setVisible(true);
		mf.runContent();
	}

}