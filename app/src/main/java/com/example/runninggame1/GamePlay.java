package com.example.runninggame1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;

public class GamePlay extends View {
    private static int runnerRadius=50;
    private static int obstacleWidth=100,obstacle1Height=80,obstacle2Height=200;
    private static int obstacleSpeed=10;
    private static int maxObstacles=1;
    private static int jumpVelocity=-50;
    private static int maxObstacleSpeed=35;
    private Paint paintRunner;
    private Paint paintObstacle;
    private List<RectF> obstacles;
    private int velocityY,velocityY1;
    private int runnerX,runnerY;
    private int chaserX,chaserY;
    private int runnerVelocity,chaserVelocity;
    private boolean isJumping,isJumping1,isSmallObstacle=true,collision=false,gameOver;
    private long startTime;
    private int collisionCount=0,score=0;
    private float prevRunnerX1=0;
    private int jumpCount=0;
    private boolean canJump=true;
    private static final int autoJumpDistance=400;

    public GamePlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        paintRunner=new Paint();
        paintRunner.setColor(Color.RED);

        paintObstacle=new Paint();
        paintObstacle.setColor(Color.BLUE);

        obstacles = new ArrayList<>();
        startTime=System.currentTimeMillis();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        int spacing=100;
        runnerX=(width-runnerRadius-400)/2;
        runnerY=height-spacing-runnerRadius-85;

        chaserX=(width-runnerRadius-1200)/2;
        chaserY=height-spacing-runnerRadius-145;
        //generateObstacles(width,height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(gameOver){
            GAME_OVER(canvas);
        }

        //to get baseline position
        int baseLineY=getHeight()*4/5;
        // Calculate the distance between the chaser and the obstacles
        float distanceToObstacles = calculateDistanceToObstacles();

        //Fill the background with the image
        Drawable backgroundDrawable=getResources().getDrawable(R.drawable.background,null);
        backgroundDrawable.setBounds(0,0,getWidth(),baseLineY);
        backgroundDrawable.draw(canvas);
        //Fill the bottom part with the selected color
        Paint paintBottom=new Paint();
        paintBottom.setColor(Color.parseColor("#fbc5ce"));
        canvas.drawRect(0,baseLineY,getWidth(),getHeight(),paintBottom);
        //Draw a base line
        Paint baseline=new Paint();
        baseline.setColor(Color.parseColor("#9355d8"));
        canvas.drawLine(0, baseLineY, getWidth(), baseLineY,baseline);
        //Draw the runner
        Drawable runner=getResources().getDrawable(R.drawable.runner,null);
        runner.setBounds(runnerX-runnerRadius,runnerY-runnerRadius,runnerX+runnerRadius,runnerY+runnerRadius);
        runner.draw(canvas);
        //Draw the chaser
        Drawable chaser=getResources().getDrawable(R.drawable.chaser,null);
        chaser.setBounds(chaserX-100,chaserY-100,chaserX+100,chaserY+100);
        chaser.draw(canvas);

        long elapsedTime=System.currentTimeMillis()-startTime;
        int obstacleVelocity=obstacleSpeed+(int) elapsedTime/2000;
        obstacleVelocity=Math.min(obstacleVelocity,obstacleSpeed+maxObstacleSpeed);

        collision=false;
        int intersectionCount=0;

        for(RectF obstacleRect : obstacles){
            obstacleRect.offset(-obstacleVelocity,0);

            if(obstacles.indexOf(obstacleRect)%2==0){
                Drawable smallObstacleDrawable=getResources().getDrawable(R.drawable.bricks,null);
                smallObstacleDrawable.setBounds((int) obstacleRect.left,(int)obstacleRect.top,(int)obstacleRect.right,(int)baseLineY);
                smallObstacleDrawable.draw(canvas);
            }
            else{
                float top=obstacleRect.top-50;
                RectF tallObstacleRect=new RectF(obstacleRect.left,top,obstacleRect.right,obstacleRect.bottom);
                Drawable tallObstacleDrawable=getResources().getDrawable(R.drawable.bricks,null);
                tallObstacleDrawable.setBounds((int) tallObstacleRect.left,(int) tallObstacleRect.top,(int) tallObstacleRect.right,(int) baseLineY);
                tallObstacleDrawable.draw(canvas);
            }
            if(RectF.intersects(obstacleRect,getCircleRect())){
                paintObstacle.setColor(Color.BLUE);
                collision=true;
                intersectionCount++;
                chaserX+=20;
            }
            else{
                paintObstacle.setColor(Color.BLUE);
            }
            if (obstacleRect.right<runnerX+runnerRadius && collision==false){
                score+=50;
            }
            if(intersectionCount==2){
                gameOver=true;
                break;
            }
        }
        if(gameOver){
            GAME_OVER(canvas);
        }
        //To remove the objects that have gone out of screen
        for (int i=obstacles.size()-1;i>=0;i--){
            RectF obstacleRect=obstacles.get(i);
            if(obstacleRect.right<=0){
                obstacles.remove(i);
            }
        }
        //Generate obstacles when needed
        if(obstacles.size()<maxObstacles){
            generateObstacles(getWidth(),getHeight());
        }
        //Applying gravity and updating the circle position
        if(isJumping){
            velocityY+=4;
            runnerY+=velocityY;
            int baselineBottom=getHeight()*4/5;
            if(runnerY>=baselineBottom-runnerRadius){
                runnerY=baselineBottom-runnerRadius;
                isJumping=false;
                canJump=true;
            }
        }
        //checking for chaser's auto jump
        if(!isJumping1&&canJump&&distanceToObstacles<=autoJumpDistance){
            jump1();
        }
        if(isJumping1){
            velocityY1+=4;
            chaserY+=velocityY1;
            int baselineBottom=getHeight()*4/5;
            if(chaserY>=baselineBottom-runnerRadius-20){
                chaserY=baselineBottom-runnerRadius-20;
                isJumping=false;
                canJump=true;
            }
        }
        //Draw the score on top right corner
        Paint paintScore=new Paint();
        paintScore.setColor(Color.BLACK);
        paintScore.setTextSize(50);
        String scoreText="Score: "+score;
        float textWidth=paintScore.measureText(scoreText);
        float x=getWidth()-textWidth-20;
        float y=50+Math.abs(paintScore.ascent());
        canvas.drawText(scoreText,x,y,paintScore);

        prevRunnerX1=runnerX;
        //invalidate to redraw
        invalidate();
    }
    private RectF getCircleRect(){
        return new RectF(runnerX-runnerRadius,runnerY-runnerRadius,runnerY+runnerRadius,runnerY+runnerRadius);
    }
    private void generateObstacles(int width,int height){
        int obstacleY=height-obstacle1Height-70;
        Random random=new Random();
        boolean isSmallObstacle=random.nextBoolean();
        if(isSmallObstacle){
            //Generate small obstacles
            RectF smallObstacleRect = new RectF(width,obstacleY,width+obstacleWidth,obstacleY+obstacle1Height);
            obstacles.add(smallObstacleRect);
        }
        else {
            obstacleY=height-obstacle2Height-70;
            RectF tallObstaclesRect= new RectF(width,obstacleY,width+obstacleWidth,obstacleY+obstacle2Height);
            obstacles.add(tallObstaclesRect);
        }
        //Toggle for next obstacle
        isSmallObstacle=!isSmallObstacle;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN&&!isJumping&&!gameOver){
            jump();
        }
        return super.onTouchEvent(event);
    }
    private void jump(){
        velocityY=jumpVelocity;
        isJumping=true;
    }
    private void jump1(){
        if(!isJumping1){
            velocityY1=jumpVelocity;
            isJumping=true;
            canJump=false;
            jumpCount++;
            if (jumpCount >= 1) {
                jumpCount = 0;
                Handler handler=new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        isJumping1 = false;
                    }
                },500);
            }
        }
    }
    public void startMoving() {
        // Start the animation by repeatedly redrawing the view
        ScheduledExecutorService executorService=null;
        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!gameOver) {
                    invalidate();
                }
            }
        },500);
    }

    private float calculateDistanceToObstacles() {
        float closestObstacleX = Float.MAX_VALUE;
        for (RectF obstacleRect : obstacles) {
            float obstacleX = obstacleRect.left;
            closestObstacleX = Math.min(closestObstacleX, obstacleX);
        }
        return closestObstacleX - (chaserX - runnerRadius);
    }
    private void GAME_OVER(Canvas canvas){

        Paint displayScore=new Paint();
        displayScore.setColor(Color.BLACK);
        displayScore.setTextSize(100);
        String game="Game Over!!!!";
        float textWidth=displayScore.measureText(game);
        float x=getWidth()/2-textWidth;
        float y=getHeight()/2;
        canvas.drawText(game,x,y,displayScore);
    }
}
