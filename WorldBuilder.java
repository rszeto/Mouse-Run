/* Generates a WackoWorld from an xml file. Modified from sample StaX
 * file for my purposes.
 *
 * @source http://www.vogella.de/articles/JavaXML/article.html
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.URL;
import java.awt.Image;
import javax.imageio.ImageIO;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import net.phys2d.math.*;
import net.phys2d.raw.*;
import net.phys2d.raw.shapes.*;

public class WorldBuilder {

	private static final DynamicShape DEFAULT_SHAPE = new Circle(0);
	private static final float DEFAULT_MASS = 0f;

	@SuppressWarnings({ "unchecked", "null" })
	public static WackoWorld buildWorld(String file, int currentLevel)
	{
		try {
			// First create a new XMLInputFactory
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			// Setup a new eventReader
			InputStream in = Class.forName("WorldBuilder").getResourceAsStream(file);
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
			WackoWorld w = null;
			int levelIndex = 0;
			
			// Read the XML document
			while (eventReader.hasNext())
			{
				XMLEvent event = eventReader.nextEvent();

				if (event.isStartElement())
				{
					StartElement startElement = event.asStartElement();
					String localPart = startElement.getName().getLocalPart();
					// If we have a item element we create a new item
					if (localPart.equals("world"))
					{
						if(levelIndex != currentLevel)
						{
							levelIndex++;
							while(eventReader.hasNext())
							{
								event = eventReader.nextEvent();
								if (event.isEndElement()) {
									EndElement endElement = event.asEndElement();
									if (endElement.getName().getLocalPart().equals("world"))
										break;
								}
							}
							continue;
						}
						int wW = 0, wH = 0, iter = 1;
						float gX = 0f, gY = 0f, antiGX = 0f, antiGY = 0f;
						String bgFileName = "";
						// We read the attributes from this tag and add the date
						// attribute to our object
						Iterator<Attribute> attributes = startElement.getAttributes();
						while (attributes.hasNext()) {
							Attribute attribute = attributes.next();
							String attName = attribute.getName().toString();
							String attValue = attribute.getValue();
							
							if (attName.equals("gravityY"))
								gY = Float.parseFloat(attValue);
							if (attName.equals("gravityX"))
								gX = Float.parseFloat(attValue);
							if (attName.equals("iterations"))
								iter = Integer.parseInt(attValue);
							if (attName.equals("width"))
								wW = Integer.parseInt(attValue);
							if (attName.equals("height"))
								wH = Integer.parseInt(attValue);
							if (attName.equals("antiGY"))
								antiGY = Float.parseFloat(attValue);
							if (attName.equals("antiGX"))
								antiGX = Float.parseFloat(attValue);
							if (attName.equals("background"))
								bgFileName = attValue;
						}
						w = new WackoWorld(
							gX, gY, wW, wH, iter, antiGX, antiGY, bgFileName);
					}
					else if (localPart.equals("body"))
					{
						Body b = null;
						Iterator<Attribute> attributes = startElement.getAttributes();
						String bodyType = attributes.next().getValue();
						if(bodyType.equals("CharBody"))
							b = new CharBody(DEFAULT_SHAPE, DEFAULT_MASS);
						else if(bodyType.equals("StaticBody"))
							b = new StaticBody(DEFAULT_SHAPE);
						else if(bodyType.equals("default"))
							b = new Body(DEFAULT_SHAPE, DEFAULT_MASS);
						w.add(b);
						modifyBody(eventReader, b);
					}
					else if (localPart.equals("goal"))
					{
						Goal g = buildGoal(eventReader);
						w.add(g);
					}
				}
				// If we reach the end of an item element we add it to the list
				if (event.isEndElement()) {
					EndElement endElement = event.asEndElement();
					if (endElement.getName().getLocalPart().equals("world"))
						return w;
				}
			}
		}
		catch (ClassNotFoundException e) {}
		catch (XMLStreamException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void modifyBody(XMLEventReader eventReader, Body body)
		throws XMLStreamException
	{
		while(eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			if(event.isEndElement())
			{
				EndElement endElement = event.asEndElement();
				if(endElement.getName().getLocalPart().equals("body"))
					return;
			}
			else if(event.isStartElement())
			{
				StartElement startElement = event.asStartElement();
				String localPart = startElement.getName().getLocalPart();
				if(localPart.equals("shape"))
				{
					Shape s = null;
					Iterator<Attribute> attributes = startElement.getAttributes();
					String shapeType = attributes.next().getValue();
					if(shapeType.equals("Box"))
						s = buildBox(eventReader);
					else if(shapeType.equals("Circle"))
						s = buildCircle(eventReader);
					else if(shapeType.equals("Line"))
						s = buildLine(eventReader);
					else if(shapeType.equals("Polygon"))
						s = buildPoly(eventReader);
					body.setShape(s);
				}
				else if(localPart.equals("image"))
				{
					if(body instanceof DrawableBody)
					{
						event = eventReader.nextEvent();
						String fileDir = event.asCharacters().getData();
						((DrawableBody)body).setImage(fileDir);
					}
				}
				else if(localPart.equals("mass"))
				{
					event = eventReader.nextEvent();
					float mass = Float.parseFloat(event.asCharacters().getData());
					body.set(body.getShape(), mass);
				}
				else if(localPart.equals("x"))
				{
					event = eventReader.nextEvent();
					float xPos = Float.parseFloat(event.asCharacters().getData());
					body.setPosition(xPos, body.getPosition().getY());
				}
				else if(localPart.equals("y"))
				{
					event = eventReader.nextEvent();
					float yPos = Float.parseFloat(event.asCharacters().getData());
					body.setPosition(body.getPosition().getX(), yPos);
				}
				else if(localPart.equals("restitution"))
				{
					event = eventReader.nextEvent();
					float rest = Float.parseFloat(event.asCharacters().getData());
					body.setRestitution(rest);
				}
				else if(localPart.equals("friction"))
				{
					event = eventReader.nextEvent();
					float fric = Float.parseFloat(event.asCharacters().getData());
					body.setFriction(fric);
				}
				else if(localPart.equals("rotation"))
				{
					event = eventReader.nextEvent();
					float rot = Float.parseFloat(event.asCharacters().getData());
					body.setRotation(rot);
				}
				else if(localPart.equals("canFloat"))
				{
					event = eventReader.nextEvent();
					boolean b = event.asCharacters().getData().equals("true");
					body.setGravityEffected(b);
				}
				else if(localPart.equals("canRotate"))
				{
					event = eventReader.nextEvent();
					boolean b = event.asCharacters().getData().equals("true");
					body.setRotatable(b);
				}
				else if(localPart.equals("canMove"))
				{
					event = eventReader.nextEvent();
					boolean b = event.asCharacters().getData().equals("true");
					body.setMoveable(b);
				}
			}
		}
	}

	public static Box buildBox(XMLEventReader eventReader)
		throws XMLStreamException
	{
		float width = 0, height = 0;
		while(eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			if(event.isStartElement())
			{
				StartElement startElement = event.asStartElement();
				String aspect = startElement.getName().getLocalPart();
				if(aspect.equals("width"))
				{
					event = eventReader.nextEvent();
					float w = Float.parseFloat(event.asCharacters().getData());
					width = w;
				}
				else if(aspect.equals("height"))
				{
					event = eventReader.nextEvent();
					float h = Float.parseFloat(event.asCharacters().getData());
					height = h;
				}
			}
			else if(event.isEndElement())
			{
				EndElement endElement = event.asEndElement();
				if(endElement.getName().getLocalPart().equals("shape"))
				{
					if(width > 0 && height > 0)
						return new Box(width, height);
					System.out.println("Illegal width or height");
					break;
				}
			}
		}
		return null;
	}
	
	public static Circle buildCircle(XMLEventReader eventReader)
		throws XMLStreamException
	{
		float radius = 0;
		while(eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			if(event.isStartElement())
			{
				StartElement startElement = event.asStartElement();
				String aspect = startElement.getName().getLocalPart();
				if(aspect.equals("radius"))
				{
					event = eventReader.nextEvent();
					float r = Float.parseFloat(event.asCharacters().getData());
					radius = r;
				}
			}
			else if(event.isEndElement())
			{
				EndElement endElement = event.asEndElement();
				if(endElement.getName().getLocalPart().equals("shape"))
				{
					if(radius > 0)
						return new Circle(radius);
					System.out.println("Illegal radius! " + radius);
					break;
				}
			}
		}
		return null;
	}
	
	public static Line buildLine(XMLEventReader eventReader)
		throws XMLStreamException
	{
		float width = 0, height = 0;
		while(eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			if(event.isStartElement())
			{
				StartElement startElement = event.asStartElement();
				String aspect = startElement.getName().getLocalPart();
				if(aspect.equals("width"))
				{
					event = eventReader.nextEvent();
					float w = Float.parseFloat(event.asCharacters().getData());
					width = w;
				}
				else if(aspect.equals("height"))
				{
					event = eventReader.nextEvent();
					float h = Float.parseFloat(event.asCharacters().getData());
					height = h;
				}
			}
			else if(event.isEndElement())
			{
				EndElement endElement = event.asEndElement();
				if(endElement.getName().getLocalPart().equals("shape"))
				{
					if(width > 0 && height > 0)
						return new Line(width, height);
					System.out.println("Illegal width or height");
					break;
				}
			}
		}
		return null;
	}
	
	public static Polygon buildPoly(XMLEventReader eventReader)
		throws XMLStreamException
	{
		ArrayList<Vector2f> vertsAL = new ArrayList<Vector2f>();
		float ptX = 0f, ptY = 0f;
		while(eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			if(event.isStartElement())
			{
				StartElement startElement = event.asStartElement();
				String aspect = startElement.getName().getLocalPart();
				if(aspect.equals("x"))
				{
					event = eventReader.nextEvent();
					ptX = Float.parseFloat(event.asCharacters().getData());
				}
				else if(aspect.equals("y"))
				{
					event = eventReader.nextEvent();
					ptY = Float.parseFloat(event.asCharacters().getData());
				}
			}
			else if(event.isEndElement())
			{
				EndElement endElement = event.asEndElement();
				if(endElement.getName().getLocalPart().equals("Vector2f"))
				{
					vertsAL.add(new Vector2f(ptX, ptY));
				}
				else if(endElement.getName().getLocalPart().equals("shape"))
				{
					ROVector2f[] verts = new ROVector2f[vertsAL.size()];
					for(int i = 0; i < verts.length; i++)
						verts[i] = vertsAL.get(i);
					return new Polygon(verts);
				}
			}
		}
		return null;
	}
	
	public static Goal buildGoal(XMLEventReader eventReader)
		throws XMLStreamException
	{
		int xPos = 0, yPos = 0, width = 0, height = 0;
		String imgFileName = "";
		while(eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			if(event.isStartElement())
			{
				StartElement startElement = event.asStartElement();
				String aspect = startElement.getName().getLocalPart();
				if(aspect.equals("x"))
				{
					event = eventReader.nextEvent();
					xPos = Integer.parseInt(event.asCharacters().getData());
				}
				else if(aspect.equals("y"))
				{
					event = eventReader.nextEvent();
					yPos = Integer.parseInt(event.asCharacters().getData());
				}
				else if(aspect.equals("image"))
				{
					event = eventReader.nextEvent();
					imgFileName = event.asCharacters().getData();
				}
				else if(aspect.equals("width"))
				{
					event = eventReader.nextEvent();
					width = Integer.parseInt(event.asCharacters().getData());
				}
				else if(aspect.equals("height"))
				{
					event = eventReader.nextEvent();
					height = Integer.parseInt(event.asCharacters().getData());
				}
			}
			else if(event.isEndElement())
			{
				EndElement endElement = event.asEndElement();
				if(endElement.getName().getLocalPart().equals("goal"))
					return new Goal(xPos, yPos, width, height, imgFileName);
			}
		}
		return null;
	}


}