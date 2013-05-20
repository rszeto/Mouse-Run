
public class MyApplet extends javax.swing.JApplet
{
	private Phys2DGamePanel content;

	public MyApplet()
	{
		content = new Phys2DGamePanel("rsrc/level.xml");
		add(content);
	}

	public void start()
	{
		content.start();
	}

	public void stop()
	{
		content.stop();
	}

}