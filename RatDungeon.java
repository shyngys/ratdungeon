import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.threed.jpct.Camera;
import com.threed.jpct.CollisionEvent;
import com.threed.jpct.CollisionListener;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.IRenderer;
import com.threed.jpct.Loader;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.KeyMapper;
import com.threed.jpct.util.KeyState;


public class RatDungeon implements CollisionListener {

	private final Object3D rat;
	private final Object3D cheese;

	private Camera camera;
	private final Camera attached = new Camera();

	private final World theWorld = new World();

	private final KeyMapper keyMapper;

	private boolean forward = false;
	private boolean backward = false;
	private boolean left = false;
	private boolean right = false;

	private boolean doLoop = true;

    private final SimpleVector ellipsoid = new SimpleVector(15, 18, 32);

    private final float SPEED = 2.0f;

    private final float MAXSPEED = 3.0f;

	private boolean switchCamera = false;

	private final int high_camera = 1;
	private final int attached_camera = 2;

	private int cameraType = high_camera;

	private final float cellSize = 60f;

	private boolean gameOver = false;

	public RatDungeon() throws Exception {
		 textures ();

	     rat = loadObject( "Nave.3ds" );
	     rat.scale(0.3f);
	     rat.setCollisionMode( Object3D.COLLISION_CHECK_SELF );
	     theWorld.addObject(rat);

	     cheese = loadObject( "queso.3DS" );
	     cheese.scale(0.8f);
	     cheese.setCollisionMode( Object3D.COLLISION_CHECK_OTHERS );
	     cheese.addCollisionListener(this);
	     theWorld.addObject(cheese);

	     loadWorld();

	     Config.linearDiv = 700;
	     Config.lightDiscardDistance = 650;
	     Config.collideOffset = 800;

	     theWorld.addLight( new SimpleVector (175, -250, -175), 15, 15, 15 );
	     theWorld.addLight( new SimpleVector (350, -250, 0), 15, 15, 15 );
	     theWorld.addLight( new SimpleVector (0, -250, 0), 15, 15, 15 );
	     theWorld.addLight( new SimpleVector (350, -250, -350), 15, 15, 15 );
	     theWorld.addLight( new SimpleVector (0, -250, -350), 15, 15, 15 );
	     theWorld.buildAllObjects();

	     keyMapper = new KeyMapper();
	}

	private void loop() throws Exception {
		 final FrameBuffer buffer = new FrameBuffer(800, 600, FrameBuffer.SAMPLINGMODE_NORMAL);
		 buffer.disableRenderer(IRenderer.RENDERER_SOFTWARE);
		 buffer.enableRenderer(IRenderer.RENDERER_OPENGL);

		 moveCamera();

		 final long delay = 20;
		 long previous = System.currentTimeMillis();

		 while( doLoop ) {
			 buffer.clear(java.awt.Color.BLUE);
			 theWorld.renderScene(buffer);
			 theWorld.draw(buffer);
			 buffer.update();
			 buffer.displayGLOnly();

			 final long current = System.currentTimeMillis();

			 if( current - previous > delay )
			 {
				 pollControls();
				 move();
		         moveCamera();

		         if( switchCamera ) {
					 switchCamera = false;

					 if( cameraType == high_camera ) {
						 cameraType = attached_camera;
						 theWorld.setCameraTo(attached);
					 }
					 else if( cameraType == attached_camera ) {
						 cameraType = high_camera;
						 theWorld.setCameraTo(camera);
					 }
		         }

		         previous = current;

		         if( gameOver ) {
		        	 System.out.println( "Game over!!!" );
		        	 gameOver = false;
		         }
			 }
		 }

		 buffer.disableRenderer(IRenderer.RENDERER_OPENGL);
		 buffer.dispose();
		 System.exit(0);
	}

	private void loadWorld() throws Exception {
		final BufferedReader in = new BufferedReader( new FileReader( "level.txt" ) );

		String s;
		int width = 0, height = 0;
		int ratX = 0, ratY = 0;
		int cheeseX = 0, cheeseY = 0;
		int line_num = -1;

		final List< int[] > walls = new ArrayList<int []>();

		while( ( s = in.readLine() ) != null ) {
			if( s.startsWith("rat", 0) ) {
				final StringTokenizer st = new StringTokenizer(s);
				st.nextToken();

				ratX = Integer.parseInt( st.nextToken() );
				ratY = Integer.parseInt( st.nextToken() );
			}
			else if( s.startsWith("cheese", 0) ) {
				final StringTokenizer st = new StringTokenizer(s);
				st.nextToken();

				cheeseX = Integer.parseInt( st.nextToken() );
				cheeseY = Integer.parseInt( st.nextToken() );
			}
			else {
				if( ( s.indexOf('_') != -1 ) || ( s.indexOf('|') != -1 ) ) {
					if( width == 0 )
						width = s.length()/2;

					if( s.indexOf('|') != -1 )
						line_num++;

					for( int i = 0; i < s.length(); i++ ) {
						if( s.charAt(i) == '_' ) {
							final int coords[] = { i/2, line_num, i/2, line_num + 1 };
							walls.add( coords );
						}
						else if( s.charAt(i) == '|' ) {
							final int coords[] = { i/2 - 1, line_num, i/2, line_num + 1 };
							walls.add( coords );
						}
					}
				}
			}
		}

		height = line_num + 1;

		createTable( width, height, walls );

		rat.translate( cellSize*ratX, 0, - cellSize*ratY );
		cheese.translate( cellSize*cheeseX, -5, - cellSize*cheeseY );

		camera = theWorld.getCamera ();

	    camera.rotateX( (-1.1f-0.14f) );
	    camera.setPosition( new SimpleVector( cellSize*(width/2), -cellSize*(height + 2), - cellSize*height ) );
	}

	private void move() {
		SimpleVector moveRes = new SimpleVector(0, 0, 0);

        if( forward ) {
        	final SimpleVector t = rat.getZAxis();
            t.scalarMul(SPEED);
            moveRes.add(t);
        }

        if( backward ) {
        	final SimpleVector t = rat.getZAxis();
            t.scalarMul(-SPEED);
            moveRes.add(t);
        }

        if( left ) {
        	rat.rotateY( (float) Math.toRadians(-1.8f) );
        }

        if( right ) {
        	rat.rotateY( (float) Math.toRadians(1.8f) );
        }

        if( moveRes.length() > MAXSPEED ) {
        	moveRes.makeEqualLength( new SimpleVector(0, 0, MAXSPEED) );
        }

        moveRes = rat.checkForCollisionEllipsoid(moveRes, ellipsoid, 8);
        rat.translate(moveRes);
        moveRes = new SimpleVector(0, 0, 0);
	}

	private void moveCamera() {
		 attached.setPositionToCenter(rat);
         attached.align(rat);
         attached.rotateCameraX( (float) Math.toRadians(30) );
         attached.moveCamera( Camera.CAMERA_MOVEOUT, 150 );
	}

	private void pollControls() {
		KeyState ks = null;
		while( ( ks = keyMapper.poll() ) != KeyState.NONE ) {
	        if(ks.getKeyCode() == KeyEvent.VK_ESCAPE) {
	        	doLoop = false;
	        }

	        if(ks.getKeyCode() == KeyEvent.VK_C ) {
	        	if( cameraType == high_camera )
	        		switchCamera = true;
	        }

	        if(ks.getKeyCode() == KeyEvent.VK_D ) {
	        	if( cameraType == attached_camera )
	        		switchCamera = true;
	        }

	        if( ks.getKeyCode() == KeyEvent.VK_UP ) {
	            forward = ks.getState();
	        }

	        if (ks.getKeyCode() == KeyEvent.VK_DOWN) {
	                backward = ks.getState();
	        }

	        if (ks.getKeyCode() == KeyEvent.VK_LEFT) {
	                left = ks.getState();
	        }

	        if (ks.getKeyCode() == KeyEvent.VK_RIGHT) {
	                right = ks.getState();
	        }
		}

		if (org.lwjgl.opengl.Display.isCloseRequested()) {
		        doLoop = false;
		}
	}

	private void createTable( final int sizeX, final int sizeY, final List< int[] > walls ) {
		for( int x = 0; x < sizeX; x++ ) {
			 for( int y = 0; y < sizeY; y++ ) {
				 final Object3D cell = Primitives.getBox( cellSize/2, 0.1f );
				 cell.translate( x*cellSize, 0, - y*cellSize );
				 cell.rotateY( (float )Math.PI/4 );
				 if( ( x%2 + y%2 ) == 1 )
					 cell.setAdditionalColor( new Color( 0, 250, 0 ) );
				 else
					 cell.setAdditionalColor( new Color( 0, 0, 250 ) );

				 cell.setLighting( Object3D.LIGHTING_NO_LIGHTS );

				 theWorld.addObject(cell);
			 }
		}

		final Color wallColor = new Color( 230, 10, 10 );

		for( int i = 0; i < walls.size(); i++ ) {
			 final int cell1X = walls.get(i)[0];
			 final int cell1Y = walls.get(i)[1];
			 final int cell2X = walls.get(i)[2];
			 final int cell2Y = walls.get(i)[3];

			 final int x = cell2X;
			 final int y = cell1Y;

			 final Object3D wall = Primitives.getBox( cellSize/2, 0.1f );

			 wall.rotateY( (float )Math.PI/4 );
			 wall.translate( cellSize*x, -cellSize/2, -cellSize*y );

			 if( cell1X < cell2X ) {
			 	wall.rotateZ( (float )Math.PI/2 );
			 	wall.translate( new SimpleVector( -cellSize/2, 0, 0 ) );
			 }
			 else {
			 	wall.rotateX( (float )Math.PI/2 );
			 	wall.translate( new SimpleVector( 0, 0, -cellSize/2 ) );
			 }

			 wall.setAdditionalColor( wallColor );
			 wall.setCollisionMode( Object3D.COLLISION_CHECK_OTHERS );

			 theWorld.addObject(wall);
		}
	}

	private Object3D loadObject( final String fileName ) {

		final Object3D objParts [] = Loader.load3DS( fileName, 1f );

		Object3D ret = new Object3D (0);

		for( int i = 0; i < objParts.length; i++ ) {
			 final Object3D part = objParts [i];
			 part.setCenter( new SimpleVector(0, 0, 0) );
			 part.rotateX( (float)-Math.PI/2 );
			 part.rotateMesh();
			 part.setRotationMatrix( new Matrix() );
			 if( (i&1) == 1 ) {
				 part.setTransparency(0);
			 }
			 ret = Object3D.mergeObjects (ret, part);
		}

		return ret;
	}

	private void textures() {
		final TextureManager texMan = TextureManager.getInstance();
		texMan.addTexture( "ROJO.JPG", new Texture ( "ROJO.JPG" ) );
		texMan.addTexture( "QUESO.JPG", new Texture ( "QUESO.JPG" ) );
	}

	@Override
	public void collision(final CollisionEvent ce) {
		gameOver = true;
	}

	@Override
	public boolean requiresPolygonIDs() {
		return false;
	}

	public static void main(final String[] args) throws Exception {
		new RatDungeon().loop();
	}
}
