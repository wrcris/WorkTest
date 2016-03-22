package com.wyf.study.my2048;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.GridLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Administrator on 2016/3/7.
 */
public class Grid2048 extends GridLayout {

    private int childWidth;
    private int padding;
    private int childRow = 4;
    private int margin = 10;
    private boolean isLayout = false;
    private Item2048[][] game2048Items = null;
    private GestureDetector gestureDetector;
    private boolean isMoveHappen = false;
    private boolean isMergeHappen = false;
    private boolean isFirst = true;
    private int score;
    private OnGame2048Listener onGame2048Listener;

    /*枚举手势操作类型*/
    private enum Action {
        UP, DOWN, LEFT, RIGHT
    }

    public Grid2048(Context context) {
        this(context, null);
    }

    public Grid2048(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Grid2048(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        padding = Math.min(getPaddingBottom(), getPaddingTop());
        gestureDetector = new GestureDetector(context, new MyGestureListener());
        score = 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        /*布局的实际长宽一致，为两者中的最小值*/
        int length = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        childWidth = (length - 2 * padding - (childRow - 1) * margin) / childRow;
        setMeasuredDimension(length, length);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        /*避免多次加载*/
        if (!isLayout) {
            if (game2048Items == null) {
                game2048Items = new Item2048[childRow][childRow];
            }

            for (int i = 0; i < childRow; i++) {
                for (int j = 0; j < childRow; j++) {
                    Item2048 child = new Item2048(getContext());
                    game2048Items[i][j] = child;

                    Spec row = GridLayout.spec(i);
                    Spec column = GridLayout.spec(j);
                    GridLayout.LayoutParams lp = new LayoutParams(row, column);
                    /*每个子View的宽高*/
                    lp.width = childWidth;
                    lp.height = childWidth;
                    if ((j + 1) != childRow) {
                        lp.rightMargin = margin;
                    }
                    if (i > 0) {
                        lp.topMargin = margin;
                    }
                    lp.setGravity(Gravity.FILL);
                    addView(child, lp);
                }
            }

            /*生成、显示数字*/
            generateNum();
        }
        isLayout = true;
    }

    private void generateNum() {
        if (isGameOver()) {
            onGame2048Listener.onGameOver();
            return;
        }

        /*初始化，随机产生四个非零子View*/
        if (isFirst) {
            for (int i = 0; i < 4; i++) {
                int randomRow = new Random().nextInt(childRow);
                int randomCol = new Random().nextInt(childRow);
                Item2048 item = game2048Items[randomRow][randomCol];
                while (item.getNum() != 0) {
                    randomRow = new Random().nextInt(childRow);
                    randomCol = new Random().nextInt(childRow);
                    item = game2048Items[randomRow][randomCol];
                }
                item.setNum(Math.random() > 0.75 ? 4 : 2);
                Animation scaleAnimation = new ScaleAnimation(0, 1, 0, 1,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(200);
                item.startAnimation(scaleAnimation);
                isMoveHappen = isMergeHappen = false;
            }
            isFirst = false;
        }

        if (!isFull()) {
            if (isMoveHappen || isMergeHappen) {
                int randomRow = new Random().nextInt(childRow);
                int randomCol = new Random().nextInt(childRow);
                Item2048 item = game2048Items[randomRow][randomCol];
                while (item.getNum() != 0) {
                    randomRow = new Random().nextInt(childRow);
                    randomCol = new Random().nextInt(childRow);
                    item = game2048Items[randomRow][randomCol];
                }
                item.setNum(Math.random() > 0.75 ? 4 : 2);
                Animation scaleAnimation = new ScaleAnimation(0, 1, 0, 1,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(200);
                item.startAnimation(scaleAnimation);
                isMoveHappen = isMergeHappen = false;
            }
        }
    }

    /*触摸事件交由手势监听器处理*/
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        final int MIN_DISTANCE = 50;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float x = e2.getX() - e1.getX();
            float y = e2.getY() - e1.getY();
            float absX = Math.abs(x);
            float absY = Math.abs(y);

            if (x > MIN_DISTANCE && absX > absY) {
                action(Action.RIGHT);
            }
            if (x < -MIN_DISTANCE && absX > absY) {
                action(Action.LEFT);
            }
            if (y > MIN_DISTANCE && absY > absX) {
                action(Action.DOWN);
            }
            if (y < -MIN_DISTANCE && absY > absX) {
                action(Action.UP);
            }
            return true;
        }
    }

    private void action(Action action) {
        for (int i = 0; i < childRow; i++) {
            /*用来存储行或列的临时列表*/
            List<Item2048> rowList = new ArrayList<>();
            for (int j = 0; j < childRow; j++) {
                int rowIndex = getRowIndexByAction(action, i, j);
                int colIndex = getColIndexByAction(action, i, j);
                Item2048 item = game2048Items[rowIndex][colIndex];
                /*将非零的子项存入临时列表*/
                if (item.getNum() != 0) {
                    rowList.add(item);
                }
            }
            for (int j = 0; j < rowList.size(); j++) {
                int rowIndex = getRowIndexByAction(action, i, j);
                int colIndex = getColIndexByAction(action, i, j);
                Item2048 item = game2048Items[rowIndex][colIndex];
                /*如果临时列表（除去数值为0的项）里的子项与移动前的此列或者行出现不同，说明移动发生*/
                if (item.getNum() != rowList.get(j).getNum()) {
                    isMoveHappen = true;
                }
            }

            /*在临时列表里合并相同数字*/
            mergeItem(rowList);

            for (int j = 0; j < childRow; j++) {
                /*依次将临时列表里的数字填入本列/行，剩余直接填0*/
                if (rowList.size() > j) {
                    switch (action) {
                        case LEFT :
                            game2048Items[i][j].setNum(rowList.get(j).getNum());
                            break;
                        case RIGHT:
                            game2048Items[i][childRow - 1 - j].setNum(rowList.get(j).getNum());
                            break;
                        case UP:
                            game2048Items[j][i].setNum(rowList.get(j).getNum());
                            break;
                        case DOWN:
                            game2048Items[childRow - 1 - j][i].setNum(rowList.get(j).getNum());
                            break;
                    }
                } else {
                    switch (action) {
                        case LEFT :
                            game2048Items[i][j].setNum(0);
                            break;
                        case RIGHT:
                            game2048Items[i][childRow - 1 - j].setNum(0);
                            break;
                        case UP:
                            game2048Items[j][i].setNum(0);
                            break;
                        case DOWN:
                            game2048Items[childRow - 1 - j][i].setNum(0);
                            break;
                    }
                }
            }
        }
        generateNum();
    }

    /*根据手势方向确定如何取得子项。例如向上滑动，则从列方向从上到下取得每一个子项在game2048Item里的对应坐标*/
    private int getRowIndexByAction(Action action, int i, int j) {
        int rowIndex = -1;

        switch (action) {
            case LEFT:
            case RIGHT:
                rowIndex = i;
                break;
            case UP:
                rowIndex = j;
                break;
            case DOWN:
                rowIndex = childRow - 1 - j;
                break;
        }
        return rowIndex;

    }

    private int getColIndexByAction(Action action, int i, int j) {
        int colIndex = -1;
        switch (action) {
            case LEFT:
                colIndex = j;
                break;
            case RIGHT:
                colIndex = childRow - 1 - j;
                break;
            case UP:
            case DOWN:
                colIndex = i;
                break;
        }
        return colIndex;
    }

    private void mergeItem(List<Item2048> rowList) {
        if (rowList.size() < 2) {
            return;
        }
        for (int i = 0; i < rowList.size() - 1; i++) {
            Item2048 item1 = rowList.get(i);
            Item2048 item2 = rowList.get(i + 1);
            if (item1.getNum() == item2.getNum()) {
                isMergeHappen = true;
                score += item1.getNum() * 2;
                if (item1.getNum()<2048){
                item1.setNum(item1.getNum() * 2);}
                else{
                    item1.setNum(0);
                }
                onGame2048Listener.onScoreChange(score);
                for (int j = i + 1; j < rowList.size() - 1; j++) {
                    /*将后一项的数字依次向前移*/
                    rowList.get(j).setNum(rowList.get(j + 1).getNum());
                }
                /*最后一项一定设置为0*/
                rowList.get(rowList.size() - 1).setNum(0);
                return;
            }
        }
    }


    private boolean isGameOver() {
        //*如果格子没有被非零数字填满*//*
        if (!isFull()) {
            return false;
        }
        //*如果填满了，检验是否有相邻子项具有相同数字*//*
        for (int i = 0; i < childRow; i++) {
            for (int j = 0; j < childRow; j++) {
                Item2048 item = game2048Items[i][j];
                // 如果不是最后一列，则与右边项相比
                if ((j + 1) != childRow) {
                    Item2048 itemRight = game2048Items[i][j + 1];
                    if (item.getNum() == itemRight.getNum())
                        return false;
                }
                // 如果不是最后一行，则与下边项相比
                if ((i + 1)  != childRow) {
                    Log.e("TAG", "DOWN");
                    Item2048 itemBottom = game2048Items[i + 1][j];
                    if (item.getNum() == itemBottom.getNum())
                        return false;
                }
                // 如果不是第一列，则与左边项相比
                if (j != 0) {
                    Log.e("TAG", "LEFT");
                    Item2048 itemLeft = game2048Items[i][j - 1];
                    if (itemLeft.getNum() == item.getNum())
                        return false;
                }
                // 如果不是第一行，则与上边项相比
                if (i != 0) {
                    Log.e("TAG", "UP");
                    Item2048 itemTop = game2048Items[i - 1][j];
                    if (item.getNum() == itemTop.getNum())
                        return false;
                }
            }
        }
        return true;
    }

    public boolean isFull() {
        for (int i = 0; i < childRow; i++) {
            for (int j = 0; j < childRow; j++) {
                Item2048 game2048Item = game2048Items[i][j];
                if (game2048Item.getNum() == 0)
                    return false;
            }
        }
        return true;
    }

    /*对外接口，由调用方决定具体逻辑*/
    public interface OnGame2048Listener {
        void onScoreChange(int score);

        void onGameOver();
    }

    public void setOnGame2048Listener(OnGame2048Listener onGame2048Listener) {
        this.onGame2048Listener = onGame2048Listener;
    }

    public void reStart() {


        for (int i = 0; i < childRow; i++) {
            for (int j = 0; j < childRow; j++) {
                Item2048 item = game2048Items[i][j];
                item.setNum(0);
            }
        }
        score = 0;
        onGame2048Listener.onScoreChange(0);
        isFirst = true;
        generateNum();
    }
}
