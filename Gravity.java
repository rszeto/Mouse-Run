import net.phys2d.math.Vector2f;

// Source: Windsource.java by Kevin

public class Gravity implements net.phys2d.raw.forcesource.ForceSource
{
	private static Vector2f force = new Vector2f();
	
	public Gravity(float x, float y) {
		force.set(x, y);
	}
	
	public Gravity(Vector2f v) {
		force = v;
	}
	
	public void apply(net.phys2d.raw.Body body, float dt) {
		Vector2f temp = new Vector2f(force);
		temp.scale(dt);
		body.adjustVelocity(temp);
	}
}