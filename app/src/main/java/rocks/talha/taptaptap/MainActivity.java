package rocks.talha.taptaptap;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.RelativeLayout;

import rocks.talha.taptaptap.view.TapView;

public class MainActivity extends Activity {
    private TapView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        RelativeLayout layout = findViewById(R.id.relativeLayout);
        gameView = new TapView(this, getPreferences(Context.MODE_PRIVATE), layout);

        layout.addView(gameView, 0);

    }

    @Override
    protected void onPause() {

        super.onPause();
        gameView.pause();

    }

    @Override
    protected void onResume() {

        super.onResume();
        gameView.resume(this);

    }
}
