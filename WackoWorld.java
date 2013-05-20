import net.phys2d.math.*;
import net.phys2d.raw.*;
import net.phys2d.raw.shapes.*;
import net.phys2d.raw.strategies.BruteCollisionStrategy;

import java.awt.Image;
import java.util.ArrayList;
import java.net.URL;

public class WackoWorld extends World
{
	public static final int DEFAULT_WORLD_WIDTH = 600;
	public static final int DEFAULT_WORLD_HEIGHT = 800;
	
	private Gravity currentG;
	private int worldWidth, worldHeight;
	private Image background;
	private CharBody bob;
	private ArrayList<Goal> goals;
	private boolean isComplete;

	public WackoWorld(float gX, float gY, int wW, int wH, int iter, 
		float antiGX, float antiGY, String bgFile)
	{
		super(new Vector2f(0, 0), iter, new BruteCollisionStrategy());
		setGravity(gX, gY);
		setAntigravity(antiGX, antiGY);
		setWidth(wW);
		setHeight(wH);
		goals = new ArrayList<Goal>();
		setBackground(bgFile);
	}

	public void setGravity(float x, float y)
	{
		remove(currentG);
		currentG = new Gravity(x,y);
		add(currentG);
	}
	
	public void setAntigravity(float x, float y)
	{
		super.setGravity(x, y);
	}

	public void add(Body body)
	{
		if(body instanceof CharBody)
		{
			((CharBody)body).setWorld(this);
			setBob((CharBody)body);
		}
		super.add(body);
		body.setGravityEffected(false);
	}
	
	public void add(Goal g) {
		goals.add(g);
	}
	
	public int getWidth() {
		return worldWidth;
	}
	
	public int getHeight() {
		return worldHeight;
	}
	
	public void setWidth(int w) {
		worldWidth = w;
	}
	
	public void setHeight(int h) {
		worldHeight = h;
	}
	
	public CharBody getBob() {
		return bob;
	}
	
	public void setBob(CharBody b) {
		bob = b;
	}
	
	public ArrayList<Goal> getGoals() {
		return goals;
	}

	// Precondition: bob is NOT a line (circle, box, or polygon)
	// and there are still goals
	private boolean wasCurrentGoalHit()
	{
		Goal goal = goals.get(0);
		Vector2f dist = MathUtil.sub(bob.getPosition(), goal.getPosition());
		float goalSize = Math.min(goal.getWidth(), goal.getHeight());
		ROVector2f bobSize = ((Box)bob.getShape()).getSize();
		float charSize = Math.min(bobSize.getX(), bobSize.getY());
		return dist.length() < (goalSize + charSize)/2;
	}

	// Precondition: There are still goals
	public void updateGoals()
	{
		if(goals.size() == 0)
			return;
		else if(!isComplete && wasCurrentGoalHit())
		{
			if(goals.size() == 1)
				isComplete = true;
			else
				goals.remove(0);
		}
	}
	
	public boolean isComplete() {
		return isComplete;
	}
	
	public void setBackground(String bgFileName)
	{
		try {
			URL imgURL = getClass().getResource(bgFileName);
			background = javax.imageio.ImageIO.read(imgURL);
		}
		catch(java.io.IOException e) {}
	}
	
	public Image getBackground() {
		return background;
	}

	public void step(float dt)
	{
		bob.update();
		if(goals.size() > 0)
			updateGoals();
		super.step(dt);
	}

}