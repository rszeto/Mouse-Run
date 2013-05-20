import net.phys2d.raw.shapes.*;
import net.phys2d.math.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

public abstract class DrawableBody extends net.phys2d.raw.Body
{
	private static final String TERMIN_SYMBOL = "$$";
	
	protected BufferedImage[][] animArray;
	protected int[] frameTime; //how long a frame in a strip should last
	private int boundingBorder;
	private double scaledImgWidth, scaledImgHeight; //size of scaled image
	
	public DrawableBody(Shape s, float mass, int bb, String fileDir)
	{
		super(s, mass);
		boundingBorder = bb;
		setImage(fileDir);
		setShape(s);		
	}
	
	public DrawableBody(Shape s, float mass, int bb)
	{
		super(s, mass);
		boundingBorder = bb;
	}
	
	// Gets the image for this body. Override for animated objects
	public BufferedImage getImage(int frameCount)
	{
		if(animArray == null)
			return null;
		else
			return animArray[0][0];
	}
	
	/* Precondition: getShape() is not a line
	 * Note: Not guaranteed to look pretty with a polygon, especially if
	 * the center is a bit strange
	 */
	public void draw(Graphics2D g, int frameCount)
	{
		BufferedImage img = getImage(frameCount);
		if(img == null)
			return;
		double x = (double)getPosition().getX();
		double y = (double)getPosition().getY();
		double theta = (double)getRotation();
		double halfImgHeight = scaledImgHeight/2;
		double halfImgWidth = scaledImgWidth/2;
		AffineTransform a = new AffineTransform();
		a.translate(x-halfImgWidth, y-halfImgHeight);
		a.rotate(theta, halfImgWidth, halfImgHeight);
		a.scale(scaledImgWidth/img.getWidth(), scaledImgHeight/img.getHeight());
		g.drawRenderedImage(img, a);
	}
	
	public static java.awt.Polygon convert(ROVector2f[] pts)
	{
		int n = pts.length;
		int[] xpts = new int[n];
		int[] ypts = new int[n];
		for(int i = 0; i < n; i++)
		{
			xpts[i] = (int) pts[i].getX();
			ypts[i] = (int) pts[i].getY();
		}
		return new java.awt.Polygon(xpts, ypts, n);
	}
	
	private void buildArrays(String fileDir) throws IOException
	{
		BufferedImage[][] tempAnimA = new BufferedImage[5][];
		int[] tempFrameTimeA = new int[tempAnimA.length];
		Scanner s = new Scanner(getClass().getResourceAsStream(fileDir));
		String currItem = s.next();
		int currRow = 0;
		while(!currItem.equals(TERMIN_SYMBOL))
		{
			//expand arrays if necessary
			if(currRow == tempAnimA.length)
			{
				BufferedImage[][] tempA = new BufferedImage[2*tempAnimA.length][];
				int[] tempF = new int[2*tempAnimA.length];
				for(int i = 0; i < tempAnimA.length; i++)
				{
					tempA[i] = tempAnimA[i];
					tempF[i] = tempFrameTimeA[i];
				}
				tempAnimA = tempA;
				tempFrameTimeA = tempF;
			}
			int numFramesInStrip = Integer.parseInt(currItem);
			//array built for current animation strip
			BufferedImage[] a = new BufferedImage[numFramesInStrip];
			//retrieves image form of full animation strip
			BufferedImage fullImageStrip;
			try {
				URL currImgURL = getClass().getResource(s.next());
				fullImageStrip = ImageIO.read(currImgURL);
			}
			catch(IOException e) {
				return;
			}
			//File currentImage = new File(s.next());
			//BufferedImage fullImageStrip = ImageIO.read(currentImage);
			//width and height of each frame in animation
			int fw = fullImageStrip.getWidth()/numFramesInStrip;
			int fh = fullImageStrip.getHeight();
			for(int i = 0; i < numFramesInStrip; i++)
			{
				//fills array with each frame in the full animation strip
				a[i] = fullImageStrip.getSubimage(i*fw, 0, fw, fh);
			}
			//puts array for current animation into the full array of animations
			tempAnimA[currRow] = a;
			
			currItem = s.next();
			tempFrameTimeA[currRow] = Integer.parseInt(currItem);
			//if(numFramesInStrip == 1)
			//	tempFrameTimeA[currRow] = 1;

			currRow++;
			currItem = s.next();
		}
		animArray = tempAnimA;
		frameTime = tempFrameTimeA;
	}
	
	public void setImage(String fileDir)
	{
		int nameLen = fileDir.length();
		String fileType = fileDir.substring(nameLen-3, nameLen).toLowerCase();
		if(fileType.equals("txt"))
		{
			//file is information to convert into an animation array
			try {
				buildArrays(fileDir);
			} catch (IOException e) {}
		}
		else
		{
			//file is a picture
			try {
				URL imgURL = getClass().getResource(fileDir);
				BufferedImage i = ImageIO.read(imgURL);
				BufferedImage[] temp = {i};
				animArray = new BufferedImage[1][];
				animArray[0] = temp;
			} catch (IOException e) {}
		}
	}
	
	public void setShape(Shape s)
	{
		super.setShape(s);
		setScaledImageSize(s);
	}
	
	public void setScaledImageSize(Shape s)
	{
		scaledImgWidth = getWidth()+2*boundingBorder;
		scaledImgHeight = getHeight()+2*boundingBorder;
	}
	
	public float getWidth()
	{
		Shape s = getShape();
		if(s instanceof Box)
			return ((Box)s).getSize().getX();
		else if(s instanceof Circle)
			return 2*((Circle)s).getRadius();
		else
		{
			java.awt.Polygon temp = convert(((Polygon)s).getVertices());
			java.awt.geom.Rectangle2D enclosingBox = temp.getBounds2D();
			return (float)enclosingBox.getWidth();
		}
	}
	
	public float getHeight()
	{
		Shape s = getShape();
		if(s instanceof Box)
			return ((Box)s).getSize().getY();
		else if(s instanceof Circle)
			return 2*((Circle)s).getRadius();
		else
		{
			java.awt.Polygon temp = convert(((Polygon)s).getVertices());
			java.awt.geom.Rectangle2D enclosingBox = temp.getBounds2D();
			return (float)enclosingBox.getHeight();
		}
	}	
}