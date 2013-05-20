import net.phys2d.raw.*;
import net.phys2d.math.*;
import net.phys2d.raw.shapes.*;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class CharBody extends DrawableBody
{
	public static final float DEFAULT_MAX_VX = 50f;
	public static final float DEFAULT_JUMP_SPEED = 60f;
	public static final float MAX_H_ACCEL = 70f;
	public static final float SPS = 60f; //steps per second taken
	
	private static final int BOUNDING_BORDER = 10;
	private static final double JUMP_THRESHOLD = 15; //in degrees
	private static final int MAX_JUMP_TIMER = 3;
	
	private float jumpSpeed;
	private float horizMovementPower;
	private float maxRunningVelocity;
	private boolean facingLeft, isRunning, canJump, isDancing;
	private boolean isLeftPressed, isRightPressed;
	private World world;
	private int jumpTimer;

	public CharBody(Shape s, float mass)
	{
		super(s, mass, BOUNDING_BORDER);
		maxRunningVelocity = DEFAULT_MAX_VX;
		jumpSpeed = DEFAULT_JUMP_SPEED;
		horizMovementPower = MAX_H_ACCEL;
		jumpTimer = 1;
		setRotatable(false);
	}
	
	public void jump()
	{
		if(canJump)
		{
			float yVelocity = getVelocity().getY();
			addForce(new Vector2f(0, -yVelocity*SPS*getMass()));
			addForce(new Vector2f(0, -jumpSpeed*SPS*getMass()));
			canJump = false;
			isDancing = false;
			jumpTimer = 0;
		}
	}
	
	public void moveRight()
	{
		facingLeft = false;
		isRunning = true;
		isDancing = false;
		float maxRunningForce = getMass() * horizMovementPower;
		float horizVel = getVelocity().getX();
		float horizForce = 0;
		if(horizVel > maxRunningVelocity)
			horizForce = 0;
		else if(horizVel < 0)
			horizForce = maxRunningForce;
		else
			horizForce = maxRunningForce*(1 - 1/maxRunningVelocity*horizVel);
		addForce(new Vector2f(horizForce, 0));
	}
	
	public void moveLeft()
	{
		facingLeft = true;
		isRunning = true;
		isDancing = false;
		float maxRunningForce = getMass() * horizMovementPower;
		float horizVel = getVelocity().getX();
		float horizForce = 0;
		if(horizVel < -maxRunningVelocity)
			horizForce = 0;
		else if(horizVel > 0)
			horizForce = -maxRunningForce;
		else
			horizForce = -maxRunningForce*(1 + 1/maxRunningVelocity*horizVel);
		addForce(new Vector2f(horizForce, 0));
			
	}
	
	private float findRunningForce()
	{
		float maxRunningForce = getMass() * MAX_H_ACCEL;
		float horizSpeed = Math.abs(getVelocity().getX());
		if(horizSpeed > maxRunningVelocity)
			return 0;
		else
			return maxRunningForce*(1 - 1/maxRunningVelocity*horizSpeed);
	}

	/*
	// Precondition: getShape() is NOT a line (circle, box, or polygon)
	public boolean contains(ROVector2f point)
	{
		Shape s = getShape();
		if(s instanceof Circle)
		{
			float radius = ((Circle)s).getRadius();
			Vector2f diff = MathUtil.sub(point, this.getPosition());
			return (radius >= diff.length());
		}
		else
		{
			int goalX = (int)point.getX();
			int goalY = (int)point.getY();
			Vector2f[] vertices;
			if(s instanceof Box)
				vertices = ((Box)s).getPoints(getPosition(), getRotation());
			else
				vertices = ((Polygon)s).getVertices(getPosition(), getRotation());
			java.awt.Polygon convertedPoly = CharBody.convert(vertices);
			return convertedPoly.contains(goalX, goalY);
		}
	}
	
	public boolean contains(ROVector2f[] points)
	{
		for(int i = 0; i < points.length; i++)
		{
			if(contains(points[i]))
				return true;
		}
		return false;
	}
	*/
	
	//if this cannot jump, check if he has landed on ground properly
	public void updateJump()
	{
		if(!canJump)
		{
			CollisionEvent[] contacts = world.getContacts(this);
			jumpTimer++;
			if(jumpTimer > MAX_JUMP_TIMER)
				jumpTimer = MAX_JUMP_TIMER;
			for(int i = 0; i < contacts.length; i++)
			{
				ROVector2f normal = contacts[i].getNormal();
				if(contacts[i].getBodyA().getID() == this.getID())
					normal = MathUtil.scale(normal, -1);
				double angle = getAngle(normal);
				double threshold = Math.toRadians(JUMP_THRESHOLD);
				if(angle > threshold && angle < Math.PI - threshold && jumpTimer == MAX_JUMP_TIMER)
					canJump = true;
			}
		}
	}
	
	// Precondition: Vector length is greater than 0
	// Range: -PI to PI... and -0.
	private static double getAngle(ROVector2f normal)
	{
		float x = normal.getX(), y = -normal.getY();
		if(Math.abs(x) == 0 && y > 0)
			return Math.PI/2;
		if(Math.abs(x) == 0 && y < 0)
			return -Math.PI/2;
		double stdAngle = Math.atan(y/x);
		if(x > 0)
			return stdAngle;
		else if(stdAngle < 0)
			return Math.PI + stdAngle;
		else
			return -Math.PI + stdAngle;
	}

	public BufferedImage getImage(int frameCount)
	{
		int motion = getMotion();
		return animArray[motion][getFrame(motion, frameCount)];
	}

	public int getMotion()
	{
		if(animArray.length == 1)
			return 0;
		int pos = 0;
		if(!canJump)
			pos = 2;
		else if(isRunning)
			pos = 4;
		if(!facingLeft)
			pos++;
		if(isDancing)
			pos = 6;
		return pos;
	}
	
	public int getFrame(int motion, int frameCount)
	{
		if(animArray.length == 1)
			return 0;
		int cycleTime = animArray[motion].length * frameTime[motion];
		return (frameCount % cycleTime) / frameTime[motion];
	}
	
	public void switchDancing()
	{
		isDancing = !isDancing;
	}
	
	public void setWorld(World w)
	{
		world = w;
	}
	
	public void setLeftPressed(boolean b) {
		isLeftPressed = b;
	}
	
	public void setRightPressed(boolean b) {
		isRightPressed = b;
	}
	
	public void update()
	{
		updateJump();
		if(isLeftPressed && !isRightPressed)
			moveLeft();
		if(isRightPressed && !isLeftPressed)
			moveRight();
		if(isLeftPressed == isRightPressed)
			isRunning = false;
	}

}