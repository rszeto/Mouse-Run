import java.awt.event.*;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import net.phys2d.raw.*;
import net.phys2d.math.*;
import net.phys2d.raw.shapes.*;
import java.awt.Image;

public  class Phys2DGamePanel extends javax.swing.JPanel
	implements Runnable, KeyListener
{
	private static final int ITERATIONS = 5;
	private static final int SLEEP_DELAY = 6;
	private static final int MIN_CHAR_FRM_DIST = 180;
	private static final int BG_SCROLL_FACTOR = 4;

	private long lastUnpauseTime, playTime;
	
	private Thread animator;
	private boolean isRunning, isPaused;
	private boolean shouldReset;
	private AffineTransform aft;
	private double panelX, panelY;
	private int frameCount;
	private int appWidth, appHeight;
	private String worldsFile;
	
	protected WackoWorld world;
	private int currentLevel;

	public Phys2DGamePanel(String xmlFile)
	{
		setFocusable(true);
		addKeyListener(this);
		aft = new AffineTransform();
		currentLevel = 0;
		worldsFile = xmlFile;
		lastUnpauseTime = System.currentTimeMillis();
		resetWorld();
		setVisible(true);
	}

	public void keyTyped(KeyEvent e)
	{
		char key = e.getKeyChar();
		if(key == 'p')
		{
			if(isPaused)
				unpause();
			else
				pause();
		}
		if(isPaused)
			return;
		if(key == 'F')
		{
			boolean b = world.getBob().getGravityEffected();
			world.getBob().setGravityEffected(!b);
		}
		else if(key == 'r')
			shouldReset = true;
		else if(key == 'd')
			world.getBob().switchDancing();
		else if(key == 'n')
			advanceLevel();
	}
	
	public void keyPressed(KeyEvent e)
	{
		if(isPaused)
			return;
		int keyCode = e.getKeyCode();
		switch(keyCode)
		{
			case KeyEvent.VK_UP:
				world.getBob().jump();
				break;
			case KeyEvent.VK_RIGHT:
				world.getBob().setRightPressed(true);
				break;
			case KeyEvent.VK_LEFT:
				world.getBob().setLeftPressed(true);
				break;
		}
	}
	
	public void keyReleased(KeyEvent e)
	{
		if(e.getKeyCode() == KeyEvent.VK_RIGHT)
			world.getBob().setRightPressed(false);
		if(e.getKeyCode() == KeyEvent.VK_LEFT)
			world.getBob().setLeftPressed(false);
	}

	public void start()
	{
		if (animator == null || !isRunning)
		{
			animator = new Thread(this);
			animator.start();
		}
	}

	public void stop()
	{	isRunning = false;
	}

	public void run()
	{
		isRunning = true;
		while(isRunning)
		{	repaint();
			try
			{	Thread.sleep(SLEEP_DELAY);
			}
			catch(InterruptedException e){}
			update();
		}
	}

	public void update()
	{
		if( !isFocusOwner() || isPaused)
			return;
		
		float bobX = world.getBob().getPosition().getX();
		float bobY = world.getBob().getPosition().getY();
		float halfBobWidth = world.getBob().getWidth()/2;
		float halfBobHeight = world.getBob().getHeight()/2;
		if(bobX-halfBobWidth > world.getWidth() || bobX+halfBobWidth < 0
			|| bobY-halfBobHeight > world.getHeight() || bobY+halfBobHeight < 0)
			shouldReset = true;
		if(world.isComplete())
			advanceLevel();
		if(shouldReset)
		{
			shouldReset = false;
			resetWorld();
		}
		frameCount++;
		world.step();
	}
	
	public void pause()
	{
		long currTime = System.currentTimeMillis();
		playTime += (currTime - lastUnpauseTime);
		isPaused = true;
	}
	
	public void unpause()
	{
		lastUnpauseTime = System.currentTimeMillis();
		isPaused = false;
	}
	
	private void resetWorld()
	{
		world = WorldBuilder.buildWorld(worldsFile, currentLevel);
		panelX = (double) world.getBob().getPosition().getX() - world.getWidth()/2;
		panelY = (double) world.getBob().getPosition().getY() - world.getHeight()/2;
	}
	
	private void advanceLevel()
	{
		currentLevel++;
		WackoWorld w = WorldBuilder.buildWorld(worldsFile, currentLevel);
		if(w == null)
		{
			currentLevel = 0;
			resetWorld();
		}
		else
			world = w;
	}
	
	public void paint(Graphics g)
	{
		Graphics2D g2d = (Graphics2D)g;
		g2d.setColor(Color.WHITE);
		g2d.fill(new java.awt.Rectangle(getSize()));
		
		AffineTransform a = new AffineTransform(aft);
		a.setToTranslation(-panelX/BG_SCROLL_FACTOR, -panelY/BG_SCROLL_FACTOR);
		int addedWidth = (world.getWidth() - getSize().width)/BG_SCROLL_FACTOR;
		int addedHeight = (world.getHeight() - getSize().height)/BG_SCROLL_FACTOR;
		double imgW = (double)getSize().width + addedWidth;
		double imgH = (double)getSize().height + addedHeight;
		Image bg = world.getBackground();
		if(bg != null)
		{
			a.scale(imgW/bg.getWidth(this), imgH/bg.getHeight(this));
			g2d.drawImage(bg, a, this);
		}
		setTranslation(g2d, aft);
		for(Goal gol : world.getGoals())
		{
			Image goalImg = gol.getImage();
			int halfGoalImgWidth = goalImg.getWidth(this)/2;
			int halfGoalImgHeight = goalImg.getHeight(this)/2;
			int goalCornerX = gol.getX() - halfGoalImgWidth;
			int goalCornerY = gol.getY() - halfGoalImgHeight;
			aft.setToTranslation(goalCornerX, goalCornerY);
			g2d.drawImage(goalImg, aft, this);
		}
		BodyList bodies = world.getBodies();
		for(int i = 0; i < bodies.size(); i++)
		{
			setTranslation(g2d, aft);
			Body b = bodies.get(i);
			if(b instanceof DrawableBody)
				((DrawableBody)b).draw(g2d, frameCount);
			else
				drawStandardBody(g2d, b);
		}
		JointList joints = world.getJoints();
		for(int i = 0; i < joints.size(); i++)
		{
			setTranslation(g2d, aft);
			if(joints.get(i) instanceof SpringJoint)
				drawSpringJoint(g2d, joints.get(i));
			if(joints.get(i) instanceof FixedJoint)
				drawFixedJoint(g2d, joints.get(i));
			if(joints.get(i) instanceof BasicJoint)
				drawBasicJoint(g2d, joints.get(i));
		}
		setTranslation(g2d, aft);
		world.getBob().draw(g2d, frameCount);
		
		aft.setToTranslation(0, 0);
		g2d.setTransform(aft);
		g2d.setColor(Color.BLACK);
		java.awt.FontMetrics fm = getFontMetrics(getFont());
		int fontBottom = fm.getHeight();
		int timeToPrint = (int)playTime;
		if(!isPaused)
			timeToPrint += (int) (System.currentTimeMillis() - lastUnpauseTime);
		int decimal = (timeToPrint % 1000) / 10;
		int seconds = (timeToPrint % 60000) / 1000;
		int minutes = timeToPrint / 60000;
		String gameTime = "";
		if(minutes > 0)
			gameTime += minutes + ":";
		if(seconds < 10)
			gameTime += "0";
		gameTime += seconds + ".";
		if(decimal < 10)
			gameTime += "0";
		gameTime += decimal;
		g2d.drawString(gameTime, 0, fontBottom);
	}
	
	public void setTranslation(Graphics2D g, AffineTransform a)
	{
		//account for size of character?
		double panelWidth, panelHeight, xPos, yPos, deltaWidth, deltaHeight;
		int worldWidth = world.getWidth();
		int worldHeight = world.getHeight();
		xPos = (double)world.getBob().getPosition().getX();
		yPos = (double)world.getBob().getPosition().getY();
		deltaWidth = xPos - panelX;
		deltaHeight = yPos - panelY;
		
		if(deltaWidth < MIN_CHAR_FRM_DIST)
			panelX = xPos - MIN_CHAR_FRM_DIST;
		else if(deltaWidth > getSize().width - MIN_CHAR_FRM_DIST)
			panelX = xPos + MIN_CHAR_FRM_DIST - getSize().width;
		if(deltaHeight < MIN_CHAR_FRM_DIST)
			panelY = yPos - MIN_CHAR_FRM_DIST;
		else if(deltaHeight > getSize().height - MIN_CHAR_FRM_DIST)
			panelY = yPos + MIN_CHAR_FRM_DIST - getSize().height;

		if(worldWidth <= getSize().width || panelX < 0)
			panelX = 0;
		else if(panelX + getSize().width > worldWidth)
			panelX = worldWidth - getSize().width;
		if(worldHeight < getSize().height || panelY < 0)
			panelY = 0;
		else if(panelY + getSize().height > worldHeight)
			panelY = worldHeight - getSize().height;
		a.setToTranslation(-panelX, -panelY);
		g.setTransform(a);
	}
	
	private void drawStandardBody(Graphics2D g, Body b)
	{
		Shape s = b.getShape();
		if(s instanceof Box)
			drawBox(g, b);
		else if(s instanceof Line)
			drawLine(g, b);
		else if(s instanceof Circle)
			drawCircle(g, b);
		else if(s instanceof Polygon)
			drawPoly(g, b);
		
	}
	
	private void drawBox(Graphics2D g, Body b)
	{		
		Box box = (Box)b.getShape();
		Vector2f[] pts = box.getPoints(b.getPosition(), b.getRotation());
		
		Vector2f v1 = pts[0];
		g.translate(v1.getX(), v1.getY());
		g.rotate(b.getRotation());
		g.setColor(Color.WHITE);
		g.fillRect(0,0,(int)box.getSize().getX(), (int)box.getSize().getY());
		g.setColor(Color.BLACK);
		g.drawRect(0,0,(int)box.getSize().getX(), (int)box.getSize().getY());
	}
	
	private void drawLine(Graphics2D g, Body b)
	{
		Line line = (Line)b.getShape();
		Vector2f[] verts = line.getVertices(b.getPosition(), b.getRotation());
		g.setColor(Color.BLACK);
		g.drawLine(
				(int) verts[0].getX(), (int) verts[0].getY(),
				(int) verts[1].getX(), (int) verts[1].getY());
	}
	
	private void drawCircle(Graphics2D g, Body b)
	{
		g.translate(b.getPosition().getX(), b.getPosition().getY());
		int radius = (int)((Circle)b.getShape()).getRadius();
		g.setColor(Color.WHITE);
		g.fillOval(-radius, -radius, 2*radius, 2*radius);
		g.setColor(Color.BLACK);
		g.drawOval(-radius, -radius, 2*radius, 2*radius);
		g.rotate((double)b.getRotation());
		g.drawLine(0, 0, radius, 0);
	}
	
	private void drawPoly(Graphics2D g, Body b)
	{
		g.setColor(Color.BLACK);
		ROVector2f[] verts = ((Polygon)b.getShape()).getVertices(
			b.getPosition(), b.getRotation());
		java.awt.Polygon polyToDraw = DrawableBody.convert(verts);
		g.setColor(Color.WHITE);
		g.fill(polyToDraw);
		g.setColor(Color.BLACK);
		g.draw(polyToDraw);
	}
	
	private void drawBasicJoint(Graphics2D g, Joint j)
	{
		BasicJoint joint = (BasicJoint) j;
			
		Body b1 = joint.getBody1();
		Body b2 = joint.getBody2();
		
		Matrix2f R1 = new Matrix2f(b1.getRotation());
		Matrix2f R2 = new Matrix2f(b2.getRotation());

		ROVector2f x1 = b1.getPosition();
		Vector2f p1 = MathUtil.mul(R1,joint.getLocalAnchor1());
		p1.add(x1);

		ROVector2f x2 = b2.getPosition();
		Vector2f p2 = MathUtil.mul(R2,joint.getLocalAnchor2());
		p2.add(x2);

		g.setColor(Color.GREEN);
		g.drawLine((int) x1.getX(), (int) x1.getY(), (int) p1.x, (int) p1.y);
		g.drawLine((int) x2.getX(), (int) x2.getY(), (int) p2.x, (int) p2.y);
		
	}

	private void drawFixedJoint(Graphics2D g, Joint j)
	{
		Body a = j.getBody1(), b = j.getBody2();
		int aX = (int)a.getPosition().getX();
		int aY = (int)a.getPosition().getY();
		int bX = (int)b.getPosition().getX();
		int bY = (int)b.getPosition().getY();
		g.setColor(Color.BLACK);
		g.drawLine(aX, aY, bX, bY);
	}
	
	private void drawSpringJoint(Graphics2D g, Joint j)
	{
		SpringJoint joint = (SpringJoint) j;
			
		Body b1 = joint.getBody1();
		Body b2 = joint.getBody2();

		Matrix2f R1 = new Matrix2f(b1.getRotation());
		Matrix2f R2 = new Matrix2f(b2.getRotation());

		ROVector2f x1 = b1.getPosition();
		Vector2f p1 = MathUtil.mul(R1,joint.getLocalAnchor1());
		p1.add(x1);

		ROVector2f x2 = b2.getPosition();
		Vector2f p2 = MathUtil.mul(R2,joint.getLocalAnchor2());
		p2.add(x2);
		
		g.setColor(Color.RED);
		g.drawLine((int) x1.getX(), (int) x1.getY(), (int) p1.x, (int) p1.y);
		g.drawLine((int) p1.x, (int) p1.y, (int) p2.getX(), (int) p2.getY());
		g.drawLine((int) p2.getX(), (int) p2.getY(), (int) x2.getX(), (int) x2.getY());
	}
	
}