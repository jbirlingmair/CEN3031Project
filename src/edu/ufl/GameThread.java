package edu.ufl;

import java.io.*;

import android.content.Context;
import android.graphics.Canvas;
import android.view.SurfaceHolder;
import android.graphics.RectF;
import android.content.res.Resources;

import edu.ufl.Tile.TileType;

public class GameThread extends Thread {

    public final static int FPS = 30;
    public final static int FPS_PERIOD = 1000/FPS;

    long beginTime;     // the time when the cycle begun
    long timeDiff;      // the time it took for the cycle to execute
    int sleepTime;      // ms to sleep (<0 if we're behind)

    // Surface holder that can access the physical surface
    private SurfaceHolder surfaceHolder;
    // The actual view that handles inputs and draws to the surface
    private GamePanel gamePanel;
    private Context context;
    private Camera camera;

    private LevelObject albert;
    private Level level;

    /* Whether or not the thread is currently alive */
    private boolean running;
    public void setRunning(boolean running) {
        this.running = running;
    }

    public GameThread(SurfaceHolder surfaceHolder, Context context, GamePanel gamePanel) {
        super();
        this.surfaceHolder = surfaceHolder;
        this.context   = context;
        this.gamePanel = gamePanel;
        this.albert = new LevelObject(10,10,10,20);
        this.camera = new Camera(gamePanel);

        try {
            BufferedInputStream bis = new BufferedInputStream( 
                                        context.getResources().openRawResource( 
                                          R.raw.level1) );
            this.level  = LevelReader.read(bis);
            bis.close();
        } catch (IOException e) {
            GameLog.d("GameThread","Could not create level for some reason");
            this.level  = LevelReader.blankLevel();
        } 
    }

    @Override
    public void run() {
        long tickCount = 0L;
        GameLog.d("GameThread", "Starting game loop");
        while (running) {
            beginTime = System.currentTimeMillis();

            /* Begin actual game loop stuff */

            tickCount++;
            Canvas c = null;
            try {
                c = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    // IF running - and not paused
                    update();
                    // END IF

                    draw(c);
                }
            } finally {
                // do this in a finally so that if an exception is thrown
                // during the above, we don't leave the Surface in an
                // inconsistent state
                if (c != null) {
                    surfaceHolder.unlockCanvasAndPost(c);
                }
            }

            /* End game loop stuff */

            //If there's time left over in this frame, sleep. This is what keeps us at
            //our set FPS
            timeDiff = System.currentTimeMillis() - beginTime;
            sleepTime = (int)(FPS_PERIOD - timeDiff);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) { /* Oh well */ }
            }

        }
        GameLog.d("GameThread", "Game loop executed " + tickCount + " times");
    }

    private void draw(Canvas canvas) {
        // draw background
        canvas.drawARGB(255, 0x38, 0xAC, 0xEC);
        albert.draw(canvas,camera);
        level.draw(canvas,camera);
    }
    
    private void update() {
        albert.update(gamePanel);

        int MAX_X = level.getMaxPixelsX();
        int MAX_Y = level.getMaxPixelsY();

        if (albert.getX() < 0) {
            albert.setX(0);
        }
        else if ((albert.getX()+albert.getWidth()) > MAX_X)  {
            albert.setX(MAX_X-albert.getWidth());
        }
        
        if (albert.getY()+albert.getHeight() > level.getMaxPixelsY()) {
            albert.setY(MAX_Y-albert.getHeight());
            albert.setDY(0);
        }

        //RectF tileRectF   = tile.getRectF();
        //RectF albertRectF = albert.getRectF();
        //switch(Util.intersect(albertRectF,tileRectF)) {
        //    case NONE:   break;

        //    case TOP:    albert.setY(tileRectF.top - albert.getHeight());
        //                 albert.setDY(0);
        //                 break;

        //    case BOTTOM: albert.setY(tileRectF.bottom);
        //                 if (albert.getDY() < 0) albert.setDY(0);
        //                 break;

        //    case LEFT:   albert.setX(tileRectF.left - albert.getWidth());
        //                 albert.setDX(0);
        //                 break;

        //    case RIGHT:  albert.setX(tileRectF.right);
        //                 albert.setDX(0);
        //                 break;
        //}

        camera.offset(albert);

    }
}
