package kaplans.plotterdraw;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class DrawActivity extends AppCompatActivity {
    private CanvasView paintCanvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_draw);

        paintCanvas = (CanvasView) findViewById(R.id.canvas);
    }

    public void backX(View view) {
        paintCanvas.sendAdjustAxis("X");
    }

    public void backY(View view) {
        paintCanvas.sendAdjustAxis("Y");
    }

    public void reset(View view) {
        paintCanvas.sendAdjustAxis("reset");
    }

    public void penAdjustUp(View view) {
        paintCanvas.sendAdjustPen("up");
    }

    public void penAdjustDown(View view) {
        paintCanvas.sendAdjustPen("down");
    }

    public void penUp(View view) {
        paintCanvas.sendGcode("M300 S50.0");
    }

    public void penDown(View view) {
        paintCanvas.sendGcode("M300 S30.0");
    }
    public void stop(View view) {
        paintCanvas.clearGcode();
    }

}
