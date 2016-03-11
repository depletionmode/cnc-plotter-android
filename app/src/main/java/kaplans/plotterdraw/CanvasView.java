package kaplans.plotterdraw;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;

public class CanvasView extends View {

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mPaint;
    private float mX, mY;
    private static final float TOLERANCE = 2;
    private Context context;

    private ParticleDevice device = null;
    Queue<String> gcode;

    public class GcodeHandler implements Runnable
    {
        private ParticleDevice device = null;

        public GcodeHandler(ParticleDevice d)
        {
            device = d;
            Thread consumer = new Thread(this);
            consumer.start();
            Log.d("GCODE_HANDLER", "Start");

        }

        public void run()
        {
            while(true)
            {
                if (device == null) {
                    try {
                        device = ParticleCloudSDK.getCloud().getDevice("48ff6e065067555037541287");
                        Log.d("DEVICE", device.getName());
                    } catch (ParticleCloudException e) {
                        Log.e("PARTICLE_CLOUD", e.getBestMessage());
                        continue;
                    } catch (Exception e) {
                        Log.e("PARTICLE_CLOUD", e.getMessage());
                        continue;
                    }
                }

                String ins = gcode.poll();
                if (ins != null) {
                    Log.d("GCODE", ins);

                    try {
                        if (device != null) {
                            List<String> args = new ArrayList<String>();
                            args.add(ins);
                            int res = device.callFunction("gcode", args);
                        }
                    }
                    catch (ParticleCloudException e) {Log.e("GCODE_HANDLER", e.toString());}
                    catch (ParticleDevice.FunctionDoesNotExistException e) {Log.e("GCODE_HANDLER", e.toString());}
                    catch (IOException e) {Log.e("GCODE_HANDLER", e.toString());}

                    try {
                        TimeUnit.MILLISECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        //Handle exception
                    }
                }
            }
        }
    }


    public CanvasView(Context c, AttributeSet attrs) {
        super(c, attrs);

        gcode = new ConcurrentLinkedQueue<>();
        context = c;
        ParticleCloudSDK.init(c);
        Async.executeAsync(ParticleCloud.get(c), new Async.ApiWork<ParticleCloud, Void>() {

            public Void callApi(ParticleCloud cloud) throws ParticleCloudException, IOException {

                ParticleCloudSDK.getCloud().logIn("davkaplan@gmail.com", "Xr32aa");
                return null;
            }

            @Override
            public void onSuccess(Void aVoid) {
                Log.e("PARTICLE_CLOUD", "Logged in");
                //try {
                    //device = ParticleCloudSDK.getCloud().getDevice("48ff6e065067555037541287");
                    // start gcode sender thread
                    GcodeHandler gcodeHandler = new GcodeHandler(device);
                /*}
                catch (ParticleCloudException e) {
                    Log.e("PARTICLE_CLOUD", e.getBestMessage());
                } catch (Exception e) {
                    Log.e("PARTICLE_CLOUD", e.getMessage());
                }*/
            }

            @Override
            public void onFailure(ParticleCloudException e) {
                Log.e("PARTICLE_CLOUD", e.getBestMessage());
            }
        });




        mPath = new Path();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(4f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        Log.d("SIZE CHANGE", "w " + w + " h " + h);
        if (w==0) w = 1;
        if (h==0) h = 1;
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // draw the mPath with the mPaint on the canvas when onDraw
        canvas.drawPath(mPath, mPaint);
    }

    // when ACTION_DOWN start touch according to the x,y values
    private void startTouch(float x, float y) {
        mPath.moveTo(x, y);
        float factor = mCanvas.getWidth() / 100;
        Log.d("FACTOR",  ""+factor);
        Log.d("WIDTH",  ""+mCanvas.getWidth());
        y /= factor;
        x /= factor;
        gcode.add("G00 X" + x + " Y" + y);
        mX = x;
        mY = y;
        gcode.add("M300 S30.0");
    }

    public void sendAdjustPen(final String dir) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ParticleDevice device = ParticleCloudSDK.getCloud().getDevice("48ff6e065067555037541287");
                    List<String> args = new ArrayList<String>();
                    args.add(dir);
                    Log.d("ADJUST_PEN", dir);
                    int res = device.callFunction("adjustPen", args);
                } catch (ParticleCloudException e) {
                    Log.e("GCODE_HANDLER", e.getBestMessage());
                } catch (ParticleDevice.FunctionDoesNotExistException e) {
                    Log.e("GCODE_HANDLER", e.getMessage());
                } catch (IOException e) {
                    Log.e("GCODE_HANDLER", e.getMessage());
                }
            }
        }).start();
    }

    public void sendAdjustAxis(final String axis) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ParticleDevice device = ParticleCloudSDK.getCloud().getDevice("48ff6e065067555037541287");
                    List<String> args = new ArrayList<String>();
                    args.add(axis);
                    Log.d("ADJUST_AXIS", axis);
                    int res = device.callFunction("adjustAxis", args);
                    if (axis.equals("reset")) {
                        mCanvas = new Canvas(mBitmap);
                    }
                } catch (ParticleCloudException e) {
                    Log.e("GCODE_HANDLER", e.getBestMessage());
                } catch (ParticleDevice.FunctionDoesNotExistException e) {
                    Log.e("GCODE_HANDLER", e.getMessage());
                } catch (IOException e) {
                    Log.e("GCODE_HANDLER", e.getMessage());
                }
            }
        }).start();
    }

    public void clearGcode() {
        gcode.clear();
    }

    public void sendGcode(String ins) {
        gcode.add(ins);
    }

    private void moveTouch(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOLERANCE || dy >= TOLERANCE) {
            mPath.lineTo(x, y);
            mX = x;
            mY = y;
            float factor = mCanvas.getWidth() / 100;
            Log.d("FACTOR",  ""+factor);
            Log.d("WIDTH",  ""+mCanvas.getWidth());
            y /= factor;
            x /= factor;
            gcode.add("G01 X" + x + " Y" + y);
        }
    }

    private void upTouch() {
        mPath.lineTo(mX, mY);
        gcode.add("M300 S50.0");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startTouch(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                moveTouch(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                upTouch();
                invalidate();
                break;
        }
        return true;
    }
}
