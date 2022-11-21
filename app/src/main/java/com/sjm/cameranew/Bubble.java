package com.sjm.cameranew;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Bubble {
    public Bubble() {

    }

    public void showBubble(Activity a,String msg) {
        Runnable r1 = new ThreadBubble(a,msg);
        new Thread(r1).start();

    }
}
class ThreadBubble  implements Runnable{
    private Activity act ;
    private String message;
    @SuppressWarnings("deprecation")
    public ThreadBubble(Activity a,String msg )
    {
        act = a;
        message = msg;

    }
    public void run() {
            try {
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        ListView lv = (ListView) act.findViewById(R.id.lvSources);
                        TextView txt = (TextView) act.findViewById(R.id.bubbleText) ;

                        txt.setText(message);
                        txt.setVisibility(View.VISIBLE);
                        lv.setVisibility(View.GONE);

                        txt.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                txt.setVisibility(View.GONE);
                                lv.setVisibility(View.VISIBLE);
                            }
                        });
                        final Animation animBounce = AnimationUtils.loadAnimation(act, R.anim.bouncefast);
                        txt.startAnimation(animBounce);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    //private void runOnUiThread(Runnable runnable) {
    //}
}
