import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import hypermedia.video.Blob;
import javax.media.jai.*;

import org.yaml.snakeyaml.Yaml;

import processing.core.PApplet;
import processing.core.PImage;



public class FieldModel {
	private SheepTest parent;
	private ArrayList<Point2D> sheepList = new ArrayList<Point2D>();
	public ArrayList<Tank> tankList = new ArrayList<Tank>();
	PerspectiveTransform sheepTransform, tankTransform;
	PImage sheepImage, tankImage;
	private int compassCorrection = 0;
	public Point flockCenter = new Point(0,0);
	private Yaml yaml;
	Point basePos;
	
	public TankController tankController;
	private String serialPort = "";
	
	public int width = 800;
	public int height = 600;
	
	public Point sheepCenter = new Point(400,300);
	public Point penLocation = new Point(400,10);


	public FieldModel(SheepTest parent){
		this.parent = parent;
		sheepTransform = new PerspectiveTransform();
		Point.Float[] p = new Point.Float[4];
		p[0] = new Point.Float(0,0);
		p[1] = new Point.Float(640,0);
		p[2] = new Point.Float(640,480);
		p[3] = new Point.Float(0,480);
		setSheepTransform(new CalibrationQuad(p));
		sheepImage = parent.loadImage("sheep.png");
		tankImage = parent.loadImage("tank.png");
		yaml = new Yaml();
		loadSettings();
		
		tankController = new TankController(parent, "/dev/FUCKSOCKS");
		tankController.start();
		
	}

	public void loadSettings(){
		try{
			Point.Float[] gpsPoints = new Point.Float[4];
			InputStream input = new FileInputStream("settings.yaml");
			

			for(Object obj : yaml.loadAll(input)){				
				Map<String, Object> objMap = (Map<String, Object>)obj;
				serialPort = (String)objMap.get("serialPort");
				parent.log("Serial port is : " + serialPort);
				
				ArrayList quadList = (ArrayList)objMap.get("gpsQuad");
				
			}
		
		} catch (Exception e){
			e.printStackTrace();
		}
		
		
	}

	public void setCompassCorrection(int in){
		compassCorrection = in;
	}

	public void updateTankPositions(ArrayList<Tank> tankList){
		this.tankList = tankList;
		for(Tank t : tankList){
			t.parent = this;
		}
	}


	/*
	 * takes a list of sheep blobs, transforms them to field-space and stores them
	 */
	public void updateSheepPositions(ArrayList<Point> sheepListIn){
		//take each entry and run it through the transformation to field-space
		sheepList.clear();
		int flockCount = 0;
		flockCenter = new Point(0,0);


		for (Point b : sheepListIn){
			Point2D p = null;

			p = sheepTransform.transform(b, p);

			sheepList.add(p);
			flockCenter.x += p.getX();
			flockCenter.y += p.getY();

			flockCount ++;


		}
		if(flockCount > 0){
			flockCenter.x = flockCenter.x / flockCount;
			flockCenter.y = flockCenter.y / flockCount;
		}
	}

	public void setSheepTransform(Point.Float[] p){
		setSheepTransform(new CalibrationQuad(p));
	}

	/*
	 * Maps all sheep coordinates from camera space to field space
	 */
	public void setSheepTransform(CalibrationQuad quad){
		sheepTransform = new PerspectiveTransform();
		sheepTransform = PerspectiveTransform.getQuadToQuad(quad.p1.x, quad.p1.y, 
				quad.p2.x, quad.p2.y, 
				quad.p3.x, quad.p3.y,
				quad.p4.x, quad.p4.y, 
				0.0f, 0.0f, 
				800.0f, 0.0f, 
				800.0f, 600.0f, 
				0.0f, 600.0f);


		System.out.println("SheepTransform: " + sheepTransform);
	}

	

	public ArrayList<Point2D> getSheepList(){
		return sheepList;
	}

	public void draw(Point basePos){
		//update the tanks
		if(parent.mode == SheepTest.MODE_RUNNING){
			for(Tank t : tankList){
				t.run(tankList, sheepList);
				
			}
		}
		
		
		this.basePos = basePos;
		parent.pushMatrix();
		parent.translate(basePos.x, basePos.y);
		parent.pushStyle();
		parent.stroke(255,255,255);
		parent.fill(0,150,0);
		parent.rect(0,0,800,600);
		parent.fill(255,255,255);
		for(Point2D p : sheepList){

			//parent.ellipse((float)p.getX(),(float)p.getY(),10,10);
			parent.image(sheepImage, (float)p.getX() - 20, (float)p.getY() - 16);
		}

		for(Tank t : tankList){
			parent.noFill();
			parent.stroke(255,0,0);
			parent.rect(t.currentTarget.x, t.currentTarget.y,5,5);
			
			parent.pushMatrix();
			parent.translate((float)t.fieldPosition.getX() ,(float)t.fieldPosition.getY());
			parent.rotate(PApplet.radians(t.heading));
			parent.translate(-16,-16);
			
			parent.image(tankImage, 0,0 );
			
			if(t.selected){
				parent.stroke(255,0,0);
				parent.noFill();
				parent.rect(0, 0, 32, 32);
				
			}
						
			parent.popMatrix();
			parent.pushMatrix();
			parent.translate((float)t.fieldPosition.getX() ,(float)t.fieldPosition.getY());
			parent.rotate(t.desiredAngle);
			parent.line(0,0,0,20);
			parent.popMatrix();
			parent.textFont(parent.niceFont,20);
			parent.text(t.tankId, (float)t.fieldPosition.getX() + 20,(float)t.fieldPosition.getY() + 20);

		}

		parent.fill(0,255,0);
		parent.ellipse(flockCenter.x, flockCenter.y, 10,10);

		parent.fill(0,0,255);
		parent.rect(penLocation.x - 5, penLocation.y - 5, 10, 10);
		
		
		parent.popStyle();
		parent.popMatrix();


	}

	public void mouseClicked(int mouseX, int mouseY) {
		for(Tank tank : tankList){
			tank.selected = false;
			if(mouseX - basePos.x > tank.fieldPosition.x -16  && mouseX - basePos.x < tank.fieldPosition.x + 16){
				if(mouseY - basePos.y > tank.fieldPosition.y -16 && mouseY - basePos.y < tank.fieldPosition.y + 16){
					tank.selected = true;
				}
			}
		}
	}

	public void keyPressed(int keyCode) {
		for(Tank t : tankList){
			if(t.selected){
				switch (keyCode){
					case 38:	//up
						tankController.go(t.tankId, 1);
						break;
					
					case 39:	//right
						tankController.rotate(t.tankId, 1);
						break;
					case 40:	//down
						tankController.go(t.tankId, -1);
						break;
					case 37:	//left
						tankController.rotate(t.tankId, -1);
						break;
					case 32:		//space
						tankController.stopMoving(t.tankId);
						tankController.stopRotate(t.tankId);
						break;
				}
			}
		}
	}

}
