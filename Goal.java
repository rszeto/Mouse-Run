import java.awt.Image;
import net.phys2d.math.Vector2f;

public class Goal
{
	private int xPosition, yPosition, width, height;
	private Image image;
	
	public Goal(int x, int y, int w, int h, String i)
	{
		xPosition = x;
		yPosition = y;
		width = w;
		height = h;
		setImage(i);
	}
	
	public int getX() {
		return xPosition;
	}
	
	public int getY() {
		return yPosition;
	}
	
	public void setX(int x) {
		xPosition = x;
	}
	
	public void setY(int y) {
		yPosition = y;
	}
	
	public Image getImage() {
		return image;
	}
	
	public Vector2f getPosition() {
		return new Vector2f(xPosition, yPosition);
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void setImage(String imgFileName)
	{
		try {
			java.net.URL imgURL = getClass().getResource(imgFileName);
			Image i = javax.imageio.ImageIO.read(imgURL);
			image = i.getScaledInstance(width, height, Image.SCALE_DEFAULT);
		}
		catch(java.io.IOException e) {}
	}
}