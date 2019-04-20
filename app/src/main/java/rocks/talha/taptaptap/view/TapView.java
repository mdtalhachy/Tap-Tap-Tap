package rocks.talha.taptaptap.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;


import rocks.talha.taptaptap.R;

//Developed by MD Talha Chowdhury on 14/04/2019

public class TapView extends View {
    public static final int INITIAL_ANIMATION_DURATION = 6000; //6 SECONDs
    public static final Random rando = new Random();
    public static final int SPOT_DIAMETER = 200;
    public static final float SCALE_X = 0.25f;
    public static final float SCALE_Y = 0.25f;
    public static final int INITIAL_SPOTS = 5;
    public static final int SPOT_DELAY = 500;
    public static final int LIVES = 3;
    public static final int MAX_LIVES = 7;
    public static final int NEW_LEVEL = 10;
    public static final int HIT_SOUND_ID = 1;
    public static final int MISS_SOUND_ID = 2;
    public static final int DISAPPEAR_SOUND_ID = 3;
    public static final int SOUND_PRIORITY = 1;
    public static final int SOUND_QUALITY = 100;
    public static final int MAX_STREAMS = 4;
    //Static instance variables
    private static final String HIGH_SCORE = "HIGH_SCORE";
    //Collections types for circles/spots (imageViews) and  Animators
    private final Queue<ImageView> spots = new ConcurrentLinkedDeque<>();
    private final Queue<Animator> animators = new ConcurrentLinkedDeque<>();
    private SharedPreferences preferences;
    //Variables to pilot the game
    private int spotsTouched;
    private int score;
    private int level;
    private int viewWidth;
    private int viewHeight;
    private long animationTime;
    private boolean gameOver;
    private boolean gamePaused;
    private boolean dialogDisplayed;
    private int highScore;
    private TextView highScoreTextView;
    private TextView currentScoreTextView;
    private TextView levelTextView;
    private LinearLayout livesLinearLayout;
    private RelativeLayout relativeLayout;
    private Resources resources;
    private LayoutInflater layoutInflater;
    private Handler spotHandler;
    private SoundPool soundPool;
    private int volume;
    private Map<Integer, Integer> soundMap;


    public TapView(Context context, SharedPreferences sharedPreferences, RelativeLayout parentLayout) {
        super(context);

        preferences = sharedPreferences;
        highScore = preferences.getInt(HIGH_SCORE, 0);

        //save resources for loading external values
        resources = context.getResources();


        //save LayoutInflater
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        //Setting UI components
        relativeLayout = parentLayout;
        livesLinearLayout = relativeLayout.findViewById(R.id.lifeLinearLayout);
        highScoreTextView = relativeLayout.findViewById(R.id.highScoreTextView);
        currentScoreTextView = relativeLayout.findViewById(R.id.scoreTextView);
        levelTextView = relativeLayout.findViewById(R.id.levelTextView);


        spotHandler = new Handler();

    } //End of TapView


    //store SpotOnView's width/height
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        viewWidth = w; //save the new width
        viewHeight = h; //save the new height

    } //end of onSizeChanged


    //called when receives a call on onPause
    public void pause() {

        gamePaused = true;
        soundPool.release(); // release audio resources
        soundPool = null;
        cancelAnimations();  //cancel all animations

    } //end of pause



    //cancel animations and remove ImageViews representing spots
    private void cancelAnimations() {

        //cancel remaining animations
        for (Animator animator : animators) {
            animator.cancel();
        }

        //remove remaining spots from the screen
        for (ImageView view : spots) {
            relativeLayout.removeView(view);

            spotHandler.removeCallbacks(addSpotRunnable);
            animators.clear();
            spots.clear();
        }
    } //End of cancelAnimations

    //called when receives a call to onResume
    public void resume(Context context) {

        gamePaused = false;
        initializeSoundEffects(context);

        if(!dialogDisplayed)
            resetGame(); //start the game
    } //end of resume


    //start a new game
    public void resetGame() {

        spots.clear(); //empty the list of spots
        animators.clear(); //empty the list of Animatiors
        livesLinearLayout.removeAllViews(); // clear old lives from screen

        animationTime = INITIAL_ANIMATION_DURATION; // init animation length
        spotsTouched = 0; // reset the number of spots touched
        score = 0; // reset the score
        level = 1; // reset the level
        gameOver = false; // the game is not over
        displayScores(); // display scores and level

        //add lives
        for (int i = 0; i < LIVES; i++)
        {
            //add life indicator to screen
            livesLinearLayout.addView((ImageView) layoutInflater.inflate(R.layout.life, null));
        }//end of for

        //add INITIAL_SPOTS new spots at SPOT_DELAY time intervals in ms
        for (int i = 1; i <= INITIAL_SPOTS; ++i)
            spotHandler.postDelayed(addSpotRunnable, i * SPOT_DELAY);
    } //End of onRestGame


    //Working with soundeffects
    private void initializeSoundEffects(Context context) {

        soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, SOUND_QUALITY);

        //setting sound effect volume
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

        //Creating sound map
        soundMap = new HashMap<>();
        soundMap.put(HIT_SOUND_ID, soundPool.load(context, R.raw.hit, SOUND_PRIORITY));
        soundMap.put(MISS_SOUND_ID, soundPool.load(context, R.raw.miss, SOUND_PRIORITY));
        soundMap.put(DISAPPEAR_SOUND_ID, soundPool.load(context, R.raw.disappear, SOUND_PRIORITY));


    } // end of initiallizeSoundEffect


    //Displaying High Score, Score, and level
    private void displayScores() {
        highScoreTextView.setText(resources.getString(R.string.high_score) + " " + highScore);
        currentScoreTextView.setText(resources.getString(R.string.score) + " " + score);
        levelTextView.setText(resources.getString(R.string.level) + " " + level);

    } //End of displayScores


    //Runnable is used to add new spots to the game at the start
    private Runnable addSpotRunnable = new Runnable() {
        @Override
        public void run() {
            addNewSpot();
        } //End of run method
    }; //End of Runnable



    //adds a new spot at a random location and starts its animation
    public void addNewSpot() {

        //choosing two random coordinates for start and end point
        int x = rando.nextInt(viewWidth - SPOT_DIAMETER);
        int y = rando.nextInt(viewHeight - SPOT_DIAMETER);
        int x2 = rando.nextInt(viewWidth - SPOT_DIAMETER);
        int y2 = rando.nextInt(viewHeight - SPOT_DIAMETER);

        //create a new spot
        final ImageView spot = (ImageView) layoutInflater.inflate(R.layout.nottouched, null);

        //creating the actual spot/circle
        spots.add(spot); //add spot to our list of spots
        spot.setLayoutParams(new RelativeLayout.LayoutParams(SPOT_DIAMETER, SPOT_DIAMETER));


        spot.setImageResource(rando.nextInt(2) == 0 ? R.drawable.green_spot : R.drawable.red_spot);

        spot.setX(x); //Setting spot's staring x location
        spot.setY(y); //Setting spot's starting y location

        spot.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        touchedSpot(spot);
                                    } //End of onClick
                                } //end of onClickListener
        ); //End of setOnClickListener

        relativeLayout.addView(spot); //adding circle to the screen

        //configuring and starting spot's animation
        spot.animate().x(x2).y(y2).scaleX(SCALE_X).scaleY(SCALE_Y)
                .setDuration(animationTime).setListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        animators.add(animation); // save for possible cancel
                    } // end method onAnimationStart


                    public void onAnimationEnd(Animator animation) {
                        animators.remove(animation); //animation done, remove

                        if (!gamePaused && spots.contains(spot)) { //Can't be touched
                            missedSpot(spot); // lose a life
                        } //End of if

                    } //End of onAnimationEnd

                } //End of AnimationListenerAdapter

        ); //End of setListener

    } //End of addNewSpot



    // called when user touches the screen, but not a spot
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (soundPool != null)
            soundPool.play(HIT_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1.0f);


        score -= 15 * level; //remove some points
        score = Math.max(score, 0); //not letting score to go below zeor
        displayScores(); //updating scores/levels on screen
        return true;
    } //End of onTouchEvent



    //called when a spot is touched
    private void touchedSpot(ImageView spot) {
        relativeLayout.removeView(spot); // remove touched spot from screen
        spots.remove(spot);

        ++spotsTouched; //increment the number of spots touched
        score += 10 * level; // icrement the score

        // play the hit sounds
        if(soundPool != null){
            soundPool.play(HIT_SOUND_ID, volume, volume,
                    SOUND_PRIORITY, 0, 1f);
        }


        //increment level if player touched 10 spots in the current level
        if (spotsTouched % NEW_LEVEL == 0) {
            ++level; //levelling up
            animationTime *= 0.95; //speed up 5%

            //if the maximum number of lives has not been reached
            if (livesLinearLayout.getChildCount() < MAX_LIVES) {
                ImageView life = (ImageView) layoutInflater.inflate(R.layout.life, null);

                livesLinearLayout.addView(life);
            } //end of if
        } //end of if


        displayScores();   // update scores, level on the screen

        if (!gameOver) {
            addNewSpot(); // add another untouched spot
        }
    }//end of touchedSpot



    //called when a spot finishes its animations without being touched
    public void missedSpot(ImageView spot) {
        spots.remove(spot); // removing spot from spots list
        relativeLayout.removeView(spot); //removing spot from screen

        if (gameOver)  //if the game is already over, exit
            return;


        //play and disappear sound effect
        if (soundPool != null) {
            soundPool.play(DISAPPEAR_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1f);
        }

        //if game has been lost
        if (livesLinearLayout.getChildCount() == 0) { //Game lost
            gameOver = true; //if game is over

            //If previous game's score is greater than the high score
            if (score > highScore) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(HIGH_SCORE, score);
                editor.apply();
                highScore = score;
            } //End of if

            cancelAnimations();


            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Game Over");
            builder.setMessage("Score: " + score);
            builder.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    displayScores();
                    dialogDisplayed = false;
                    resetGame();
                }
            });

            dialogDisplayed = true;
            builder.show();


        } else {
            livesLinearLayout.removeViewAt(livesLinearLayout.getChildCount() - 1);
            addNewSpot();
        }

    } //End of missedSpot

}