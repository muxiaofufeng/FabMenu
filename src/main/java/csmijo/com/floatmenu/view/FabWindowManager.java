package csmijo.com.floatmenu.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import csmijo.com.floatmenu.R;

/**
 * Created by chengqianqian-xy on 2016/12/26.
 */

public class FabWindowManager {
    private Context mContext;
    private WindowManager mWindowManager;
    private FabMenu mFabMenu;
    private FabManager mFabManager;

    private View mFabBgView;  // 透明背景
    private RelativeLayout mFabActionRl;
    private ImageView mFabBtn;
    private ProgressBar mFabPrg;

    private int dir; // {0：靠左；1：靠右}
    private static int TOUCHSLOP;
    private static int FABACTIONSIZE;
    private static int FABACTIONOFFSET;
    private static int CLICKSLOP;


    public FabWindowManager(Context context, FabManager fabManager) {

        this.mContext = context;
        this.mFabManager = fabManager;
        this.mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        TOUCHSLOP = ViewConfiguration.get(mContext).getScaledTouchSlop();
        CLICKSLOP = ViewConfiguration.getTapTimeout();
        FABACTIONSIZE = mContext.getResources().getDimensionPixelOffset(R.dimen.btg_fab_action_size);
        FABACTIONOFFSET = mContext.getResources().getDimensionPixelOffset(R.dimen.btg_fab_action_offset);
    }

    public void load() {
        loadFabBg(true);
        loadFabMenu(true);
        loadFabAction(true);
        updateFabMenuPosition();
    }

    public void clearViews() {
        this.mWindowManager.removeView(mFabBgView);
        this.mWindowManager.removeView(mFabMenu);
        this.mWindowManager.removeView(mFabActionRl);
    }

    // 加载透明背景
    private void loadFabBg(boolean flag) {
        if (flag || mFabBgView == null) {
            this.mFabBgView = ((LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.btg_view_fab_bg, null);
            this.mFabBgView.findViewById(R.id.closeTrigger).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFabManager.onClose();
                }
            });

            final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    40, PixelFormat.TRANSLUCENT);
            this.mFabBgView.setVisibility(View.GONE);
            mWindowManager.addView(mFabBgView, layoutParams);
        }
    }

    // 加载fabAction
    private void loadFabAction(boolean flag) {
        if (flag || this.mFabActionRl == null) {
            this.mFabActionRl = (RelativeLayout) ((LayoutInflater) this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.btg_view_fab_action, null);
            this.mFabActionRl.setVisibility(View.VISIBLE);
            this.mFabActionRl.setOnTouchListener(getOnTouchListener());

            this.mFabBtn = (ImageView) this.mFabActionRl.findViewById(R.id.fabBtnImgView);
            this.mFabPrg = (ProgressBar) this.mFabActionRl.findViewById(R.id.progressBar);


            int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            Log.i("FabWindowManager", "loadFabAction: flags = " + flags);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, flags,
                    PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
            layoutParams.x = 0;
            layoutParams.y = 100;
            this.dir = 0;

            this.mWindowManager.addView(mFabActionRl, layoutParams);
        }
    }

    // 加载FabMenu
    private void loadFabMenu(boolean flag) {
        if (flag || this.mFabMenu == null) {
            if (Build.VERSION.SDK_INT <= 11) {
                this.mFabMenu = new FabMenuApi9(mContext);
            } else {
                this.mFabMenu = new FabMenu(mContext);
            }

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, 40, PixelFormat.TRANSLUCENT);

            layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
            this.mFabMenu.setVisibility(View.GONE);
            this.mWindowManager.addView(this.mFabMenu, layoutParams);
            this.mFabManager.createMenuOne();
        }
    }


    private View.OnTouchListener getOnTouchListener() {
        return new View.OnTouchListener() {

            int lastX, lastY;
            int paramX, paramY;
            long startTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mFabActionRl.getLayoutParams();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        paramX = layoutParams.x;
                        paramY = layoutParams.y;
                        startTime = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX()) - lastX;
                        int dy = (int) (event.getRawY()) - lastY;

                        // 改变位置
                        if (!mFabManager.isOpen()) {
                            updatePosition(paramX + dx, paramY + dy, layoutParams);
                            break;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        int x = (int) event.getRawX();
                        int y = (int) event.getRawY();

                        if (!mFabManager.isOpen()) {
                            updatePositionAndDir(x, layoutParams);
                        }

                        if (Math.abs(x - lastX) < TOUCHSLOP && Math.abs(y - lastY) < TOUCHSLOP
                                && (System.currentTimeMillis() - startTime) < CLICKSLOP) {
                            //点击
                            mFabManager.toggle();
                        }
                        break;
                }

                return true;
            }
        };
    }

    private void updatePositionAndDir(int x, WindowManager.LayoutParams params) {
        int width = getScreenWidth();
        if (x < (width / 2)) {
            // 靠左
            new MoveLeftTrail(x - FABACTIONSIZE, 5, params).start();
        } else {
            new MoveRightTrail(width - x - FABACTIONSIZE, 5, params).start();
        }
    }


    // 更新fabAction，FabMenu的位置
    private void updatePosition(int x, int y, WindowManager.LayoutParams params) {
        params.x = x;
        params.y = y;
        this.mWindowManager.updateViewLayout(this.mFabActionRl, params);  // 更新fabAction的位置
        updateFabMenuPosition();
    }

    // 更新FabMenu的位置
    private void updateFabMenuPosition() {
        WindowManager.LayoutParams fabActionRl_lp = (WindowManager.LayoutParams) this.mFabActionRl.getLayoutParams();
        WindowManager.LayoutParams fabMenu_lp = (WindowManager.LayoutParams) this.mFabMenu.getLayoutParams();

        int x1 = fabActionRl_lp.x;
        int y1 = fabActionRl_lp.y;
        int measuredWidth = this.mFabMenu.getMeasuredWidth() != 0 ? this.mFabMenu.getMeasuredWidth() :
                this.mFabMenu.getExpectedWidth();
        int measuredHeight = this.mFabMenu.getMeasuredHeight() != 0 ? this.mFabMenu.getMeasuredHeight() :
                this.mFabMenu.getExpectedHeight();

        fabMenu_lp.x = this.dir == 0 ? -FABACTIONOFFSET : (x1 - measuredWidth) + FABACTIONSIZE;
        fabMenu_lp.y = y1 + (FABACTIONSIZE - measuredHeight) / 2;

        this.mFabMenu.setExpandDir(this.dir);
        this.mWindowManager.updateViewLayout(this.mFabMenu, fabMenu_lp);
    }

    private int getScreenWidth() {
        return mWindowManager.getDefaultDisplay().getWidth();
    }


    public FabMenu getFabMenu() {
        return mFabMenu;
    }

    public View getFabBgView() {
        return mFabBgView;
    }

    public ImageView getFabBtn() {
        return mFabBtn;
    }

    public ProgressBar getFabPrg() {
        return mFabPrg;
    }

    public int getDir() {
        return dir;
    }


    private class MoveLeftTrail extends CountDownTimer {

        WindowManager.LayoutParams mLayoutParams;

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public MoveLeftTrail(long millisInFuture, long countDownInterval, WindowManager.LayoutParams layoutParams) {
            super(millisInFuture, countDownInterval);
            this.mLayoutParams = layoutParams;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            this.mLayoutParams.x = (int) millisUntilFinished;
            mWindowManager.updateViewLayout(mFabActionRl, this.mLayoutParams);
        }

        @Override
        public void onFinish() {
            this.mLayoutParams.x = 0;
            mWindowManager.updateViewLayout(mFabActionRl, this.mLayoutParams);
            dir = 0;
            updateFabMenuPosition();
        }
    }

    private class MoveRightTrail extends CountDownTimer {

        WindowManager.LayoutParams mLayoutParams;

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public MoveRightTrail(long millisInFuture, long countDownInterval, WindowManager.LayoutParams layoutParams) {
            super(millisInFuture, countDownInterval);
            this.mLayoutParams = layoutParams;
        }


        @Override
        public void onTick(long millisUntilFinished) {
            this.mLayoutParams.x = getScreenWidth() - FABACTIONSIZE - (int) millisUntilFinished;
            mWindowManager.updateViewLayout(mFabActionRl, mLayoutParams);
        }

        @Override
        public void onFinish() {
            this.mLayoutParams.x = getScreenWidth() - FABACTIONSIZE;
            mWindowManager.updateViewLayout(mFabActionRl, mLayoutParams);
            dir = 1;
            updateFabMenuPosition();
        }
    }
}
