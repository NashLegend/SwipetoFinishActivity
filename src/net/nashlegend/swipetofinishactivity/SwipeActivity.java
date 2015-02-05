package net.nashlegend.swipetofinishactivity;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.ArrayList;

import net.nashlegend.swipetofinishactivity.R;

public class SwipeActivity extends Activity {

    private SwipeLayout swipeLayout;
    private int layerColor = Color.parseColor("#88000000");
    protected boolean swipeAnyWhere = false;//是否可以在页面任意位置右滑关闭页面，如果是false则从左边滑才可以关闭

    public SwipeActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        swipeLayout = new SwipeLayout(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        swipeLayout.replaceLayer(this);
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context
                .getSystemService(WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    private boolean swipeFinished = false;

    @Override
    public void finish() {
        if (swipeFinished) {
            super.finish();
        } else {
            swipeLayout.cancelPotentialAnimation();
            super.finish();
            overridePendingTransition(0, R.anim.slide_out_right);
        }
    }

    class SwipeLayout extends FrameLayout {

        private View backgroundLayer;

        public SwipeLayout(Context context) {
            super(context);
        }

        public SwipeLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public SwipeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        public void replaceLayer(Activity activity) {
            touchSlop = (int) (touchSlopDP * activity.getResources().getDisplayMetrics().density);
            sideWidth = (int) (sideWidthInDP * activity.getResources().getDisplayMetrics().density);
            mActivity = activity;
            screenWidth = getScreenWidth(activity);
            setClickable(true);
            backgroundLayer = new View(activity);
            backgroundLayer.setBackgroundColor(layerColor);
            final ViewGroup root = (ViewGroup) activity.getWindow().getDecorView();
            content = root.getChildAt(0);
            //在Android5.0上，content的高度不再是屏幕高度，而是变成了Activity高度，比屏幕高度低一些，
            //如果this.addView(content),就会使用以前的params，这样content会像root一样比content高出一部分，导致底部空出一部分
            //在装有Android 5.0的Nexus5上，root,SwipeLayout和content的高度分别是1920、1776、1632，144的等差数列……
            //在装有Android4.4.3的HTC One M7上，root,SwipeLayout和content的高度分别相同，都是1920
            //所以我们要做的就是给content一个新的LayoutParams，Match_Parent那种，也就是下面的-1
            ViewGroup.LayoutParams params = content.getLayoutParams();
            ViewGroup.LayoutParams params2 = new ViewGroup.LayoutParams(-1, -1);
            ViewGroup.LayoutParams params3 = new ViewGroup.LayoutParams(-1, -1);
            root.removeView(content);
            this.addView(backgroundLayer, params3);
            this.addView(content, params2);
            root.addView(this, params);
        }

        boolean canSwipe = false;
        View content;
        Activity mActivity;
        int sideWidthInDP = 20;
        int sideWidth = 72;
        int screenWidth = 1080;
        VelocityTracker tracker;

        float downX;
        float downY;
        float lastX;
        float currentX;
        float currentY;


        int touchSlopDP = 30;
        int touchSlop = 60;

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (swipeAnyWhere) {
                switch (ev.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = ev.getX();
                        downY = ev.getY();
                        currentX = downX;
                        currentY = downY;
                        lastX = downX;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = ev.getX() - downX;
                        float dy = ev.getY() - downY;
                        if ((dy == 0f || Math.abs(dx / dy) > 1) && (dx * dx + dy * dy > touchSlop * touchSlop)) {
                            downX = ev.getX();
                            downY = ev.getY();
                            currentX = downX;
                            currentY = downY;
                            lastX = downX;
                            canSwipe = true;
                            tracker = VelocityTracker.obtain();
                            return true;
                        }
                        break;
                }
            } else if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getX() < sideWidth) {
                canSwipe = true;
                tracker = VelocityTracker.obtain();
                return true;
            }
            return super.onInterceptTouchEvent(ev);
        }

        @Override
        public boolean onTouchEvent(@NonNull MotionEvent event) {
            if (canSwipe) {
                tracker.addMovement(event);
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        currentX = downX;
                        currentY = downY;
                        lastX = downX;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        currentX = event.getX();
                        currentY = event.getY();
                        float dx = currentX - lastX;
                        if (content.getX() + dx < 0) {
                            setContentX(0);
                        } else {
                            setContentX(content.getX() + dx);
                        }
                        lastX = currentX;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        tracker.computeCurrentVelocity(10000);
                        tracker.computeCurrentVelocity(1000, 20000);
                        canSwipe = false;
                        int mv = screenWidth / 200 * 1000;
                        if (Math.abs(tracker.getXVelocity()) > mv) {
                            animateFromVelocity(tracker.getXVelocity());
                        } else {
                            if (content.getX() > screenWidth / 2) {
                                animateFinish(false);
                            } else {
                                animateBack(false);
                            }
                        }
                        tracker.recycle();
                        break;
                    default:
                        break;
                }
            }
            return super.onTouchEvent(event);
        }

        AnimatorSet animator;

        public void cancelPotentialAnimation() {
            if (animator != null) {
                animator.removeAllListeners();
                animator.cancel();
            }
        }

        private void setContentX(float x) {
            content.setX(x);
            if (backgroundLayer != null) {
                backgroundLayer.setAlpha(1 - x / getWidth());
            }
        }


        /**
         * 弹回，不关闭，因为left是0，所以setX和setTranslationX效果是一样的
         *
         * @param withVel
         */
        private void animateBack(boolean withVel) {
            cancelPotentialAnimation();
            animator = new AnimatorSet();
            ObjectAnimator animatorX = ObjectAnimator.ofFloat(content, "x", content.getX(), 0);
            ObjectAnimator animatorA = ObjectAnimator.ofFloat(backgroundLayer, "alpha", backgroundLayer.getAlpha(), 1);
            ArrayList<Animator> animators = new ArrayList<>();
            animators.add(animatorX);
            animators.add(animatorA);
            if (withVel) {
                animator.setDuration((long) (duration * content.getX() / screenWidth));
            } else {
                animator.setDuration(duration);
            }
            animator.playTogether(animators);
            animator.start();
        }

        private void animateFinish(boolean withVel) {
            cancelPotentialAnimation();
            animator = new AnimatorSet();

            ObjectAnimator animatorX = ObjectAnimator.ofFloat(content, "x", content.getX(), screenWidth);
            ObjectAnimator animatorA = ObjectAnimator.ofFloat(backgroundLayer, "alpha", backgroundLayer.getAlpha(), 0);
            ArrayList<Animator> animators = new ArrayList<>();
            animators.add(animatorX);
            animators.add(animatorA);
            if (withVel) {
                animator.setDuration((long) (duration * (screenWidth - content.getX()) / screenWidth));
            } else {
                animator.setDuration(duration);
            }
            animator.playTogether(animators);

            animator.addListener(new AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!mActivity.isFinishing()) {
                        swipeFinished = true;
                        mActivity.finish();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }
            });
            animator.start();
        }

        private int duration = 200;

        private void animateFromVelocity(float v) {
            if (v > 0) {
                if (content.getX() < screenWidth / 2
                        && v * duration / 1000 + content.getX() < screenWidth) {
                    animateBack(false);
                } else {
                    animateFinish(true);
                }
            } else {
                if (content.getX() > screenWidth / 2
                        && v * duration / 1000 + content.getX() > screenWidth / 2) {
                    animateFinish(false);
                } else {
                    animateBack(true);
                }
            }

        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public SwipeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }
    }

}
