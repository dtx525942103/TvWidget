package org.evilbinary.tv.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import org.evilbinary.tv.widget.BorderView.Effect;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者:evilbinary on 3/26/16.
 * 邮箱:rootdebug@163.com
 */
public class BorderEffect implements Effect {
    private String TAG = BorderEffect.class.getSimpleName();


    protected boolean mScalable = true;
    protected float mScale = 1.1f;

    protected long mDurationTraslate = 200;
    protected int mMargin = 0;
    private View lastFocus, oldLastFocus;
    private AnimatorSet mAnimatorSet;
    List<Animator> mAnimatorList = new ArrayList<Animator>();
    private View mTarget;

    public BorderEffect() {

        mFocusListener.add(focusMoveListener);
        mFocusListener.add(focusScaleListener);
        mFocusListener.add(focusPlayListener);
    }

    public interface FocusListener {
        public void onFocusChanged(View oldFocus, View newFocus);
    }

    private List<FocusListener> mFocusListener = new ArrayList<FocusListener>(1);
    private List<Animator.AnimatorListener> mAnimatorListener = new ArrayList<Animator.AnimatorListener>(1);


    public FocusListener focusScaleListener = new FocusListener() {
        @Override
        public void onFocusChanged(View oldFocus, View newFocus) {
            mAnimatorList.addAll(getScaleAnimator(newFocus, true));
            if (oldFocus != null) {
                mAnimatorList.addAll(getScaleAnimator(oldFocus, false));
            }
        }
    };
    public FocusListener focusPlayListener = new FocusListener() {
        @Override
        public void onFocusChanged(View oldFocus, View newFocus) {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setInterpolator(new DecelerateInterpolator(1));
            animatorSet.setDuration(mDurationTraslate);
            animatorSet.playTogether(mAnimatorList);
            for (Animator.AnimatorListener listener : mAnimatorListener) {
                animatorSet.addListener(listener);
            }
            mAnimatorSet = animatorSet;
            if (oldFocus == null) {
                animatorSet.setDuration(0);
                mTarget.setVisibility(View.VISIBLE);
            }
            animatorSet.start();
        }
    };
    public FocusListener focusMoveListener = new FocusListener() {
        @Override
        public void onFocusChanged(View oldFocus, View newFocus) {
            if (newFocus == null) return;
            try {

                mAnimatorList.addAll(getMoveAnimator(oldFocus, newFocus));

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };

    private List<Animator> getScaleAnimator(View view, boolean isScale) {
        List<Animator> animatorList = new ArrayList<Animator>(2);
        float scaleBefore = 1.0f;
        float scaleAfter = mScale;
        if (!isScale) {
            scaleBefore = mScale;
            scaleAfter = 1.0f;
        }
        ObjectAnimator scaleX = new ObjectAnimator().ofFloat(view, "scaleX", scaleBefore, scaleAfter);
        ObjectAnimator scaleY = new ObjectAnimator().ofFloat(view, "scaleY", scaleBefore, scaleAfter);
        animatorList.add(scaleX);
        animatorList.add(scaleY);
        return animatorList;
    }

    private List<Animator> getMoveAnimator(View oldFocus, View newFocus) {
        List<Animator> animatorList = new ArrayList<Animator>();
        int newXY[];
        int oldXY[];
        newXY = getGlobalLocation(newFocus);
        oldXY = getGlobalLocation(mTarget);

        int newWidth;
        int newHeight;

        if (mScalable) {
            newWidth = (int) (newFocus.getWidth() * mScale) + mMargin * 2;
            newHeight = (int) (newFocus.getHeight() * mScale + mMargin * 2);
            newXY[0] = newXY[0] - (newWidth - newFocus.getWidth()) / 2;
            newXY[1] = newXY[1] - (newHeight - newFocus.getHeight()) / 2;
        } else {
            newWidth = newFocus.getWidth();
            newHeight = newFocus.getHeight();
        }

        PropertyValuesHolder valuesWithdHolder = PropertyValuesHolder.ofInt("width", mTarget.getWidth(), newWidth);
        PropertyValuesHolder valuesHeightHolder = PropertyValuesHolder.ofInt("height", mTarget.getHeight(), newHeight);
        PropertyValuesHolder valuesXHolder = PropertyValuesHolder.ofInt("x", oldXY[0], newXY[0]);
        PropertyValuesHolder valuesYHolder = PropertyValuesHolder.ofInt("y", oldXY[1], newXY[1]);
        final ObjectAnimator scaleAnimator = ObjectAnimator.ofPropertyValuesHolder(mTarget, valuesWithdHolder, valuesHeightHolder, valuesYHolder, valuesXHolder);

        scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public synchronized void onAnimationUpdate(ValueAnimator animation) {
                int width = (int) animation.getAnimatedValue("width");
                int height = (int) animation.getAnimatedValue("height");
                int x = (int) animation.getAnimatedValue("x");
                int y = (int) animation.getAnimatedValue("y");

                View view = (View) scaleAnimator.getTarget();
                view.setX(x);
                view.setY(y);
                int w = view.getLayoutParams().width;
                view.getLayoutParams().width = width;
                view.getLayoutParams().height = height;
                if (w > 0) {
                    view.requestLayout();
                    view.invalidate();
                }
            }
        });
        animatorList.add(scaleAnimator);
        return animatorList;
    }

    private int[] getGlobalLocation(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return location;
    }

    public void addOnFocusChanged(FocusListener focusListener) {
        this.mFocusListener.add(focusListener);
    }

    public void removeOnFocusChanged(FocusListener focusListener) {
        this.mFocusListener.remove(focusListener);
    }

    public void addAnimatorListener(Animator.AnimatorListener animatorListener) {
        this.mAnimatorListener.add(animatorListener);
    }

    public void removeAnimatorListener(Animator.AnimatorListener animatorListener) {
        this.mAnimatorListener.remove(animatorListener);
    }

    private class VisibleScope {
        public boolean isVisible;
        public View oldFocus;
        public View newFocus;
    }

    private VisibleScope checkVisibleScope(View oldFocus, View newFocus) {
        VisibleScope scope = new VisibleScope();
        scope.oldFocus = oldFocus;
        scope.newFocus = newFocus;
        scope.isVisible = true;
        if (oldFocus != null) {
            if (oldFocus.getParent() != newFocus.getParent()) {
                Log.d(TAG, "=====>" + attacheViews.indexOf(newFocus.getParent()) + "=" + attacheViews.indexOf(oldFocus.getParent()));

                if ( (attacheViews.indexOf(newFocus.getParent()) < 0) || (attacheViews.indexOf(oldFocus.getParent()) < 0 && attacheViews.indexOf(newFocus.getParent()) > 0)) {
                    mTarget.setVisibility(View.INVISIBLE);
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(getScaleAnimator(oldFocus, false));
                    animatorSet.setDuration(0).start();
                    Log.d(TAG, "=====>1");
                    scope.isVisible = false;
                    return scope;
                } else {
                    Log.d(TAG, "=====>2");

                    mTarget.setVisibility(View.VISIBLE);
                }
                if (attacheViews.indexOf(oldFocus.getParent() ) < 0) {
                    scope.oldFocus = null;
                }

            } else {
                if (attacheViews.indexOf(newFocus.getParent()) < 0 ) {
                    mTarget.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "=====>3");
                    scope.isVisible = false;
                    return scope;
                }
                Log.d(TAG, "=====>4");

            }
            Log.d(TAG, "=====>5");
        }
        mTarget.setVisibility(View.VISIBLE);
        return scope;
    }

    @Override
    public void onFocusChanged(View target, View oldFocus, View newFocus) {
        try {
            Log.d(TAG, "onFocusChanged");

            if(newFocus==null||newFocus.getScaleX()==mScale){
                return;
            }

            lastFocus = newFocus;
            oldLastFocus = oldFocus;
            mTarget = target;

            VisibleScope scope = checkVisibleScope(oldFocus, newFocus);
            if (!scope.isVisible) {
                return;
            } else {
                oldFocus = scope.oldFocus;
                newFocus = scope.newFocus;
                oldLastFocus = scope.oldFocus;
            }

            if (isScrolling || newFocus == null || newFocus.getWidth() <= 0 || newFocus.getHeight() <= 0)
                return;



            mAnimatorList.clear();

            for (FocusListener f : this.mFocusListener) {
                f.onFocusChanged(oldFocus, newFocus);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public void onScrollChanged(View target, View attachView) {
        try {
//            Log.d(TAG, "onScrollChanged");


        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onLayout(View target, View attachView) {
        try {
//            Log.d(TAG, "onLayout");
            ViewGroup viewGroup = (ViewGroup) attachView.getRootView();
            if (target.getParent() != null && target.getParent() != viewGroup) {
                target.setVisibility(View.GONE);
                if (mFirstFocus)
                    viewGroup.requestFocus();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean mFirstFocus = true;

    public void setFirstFocus(boolean b) {
        this.mFirstFocus = b;
    }

    @Override
    public void onTouchModeChanged(View target, View attachView, boolean isInTouchMode) {
        try {
            if (isInTouchMode) {
                target.setVisibility(View.INVISIBLE);
                if (lastFocus != null) {
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(getScaleAnimator(lastFocus, false));
                    animatorSet.setDuration(0).start();
                }

            } else {
                target.setVisibility(View.VISIBLE);
            }


        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private boolean isScrolling = false;

    private List<View> attacheViews = new ArrayList<>();

    @Override
    public void onAttach(View target, View attachView) {

        target.setVisibility(View.GONE);
        if (attachView instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) attachView;
            RecyclerView.OnScrollListener recyclerViewOnScrollListener = null;
            recyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        //Log.d(TAG, "========>SCROLL_STATE_IDLE");
                        isScrolling = false;
                        View oldFocus=oldLastFocus;
                        View newFocus=lastFocus;
                        VisibleScope scope = checkVisibleScope(oldFocus, newFocus);
                        if (!scope.isVisible) {
                            return;
                        } else {
                            oldFocus = scope.oldFocus;
                            newFocus = scope.newFocus;
                        }
                        AnimatorSet animatorSet = new AnimatorSet();
                        List<Animator> list = new ArrayList<>();
//                            list.addAll(getScaleAnimator(oldLastFocus, false));
                        list.addAll(getScaleAnimator(newFocus, true));
                        list.addAll(getMoveAnimator(oldFocus, newFocus));
                        animatorSet.setDuration(mDurationTraslate);
                        animatorSet.playTogether(list);
                        animatorSet.start();


                    } else if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                        //Log.d(TAG, "========>SCROLL_STATE_SETTLING=" + mAnimatorSet.isRunning());
                        isScrolling = true;
                        List<Animator> list = getScaleAnimator(lastFocus, false);
                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.setDuration(150);
                        animatorSet.playTogether(list);
                        animatorSet.start();
                    }
                }
            };
            recyclerView.addOnScrollListener(recyclerViewOnScrollListener);
        }
        attacheViews.add(attachView);

    }

    @Override
    public void OnDetach(View targe, View view) {
        attacheViews.remove(view);
    }

    private int scrollingX = 0;


    public boolean isScalable() {
        return mScalable;
    }

    public void setScalable(boolean scalable) {
        this.mScalable = scalable;
    }

    public float getScale() {
        return mScale;
    }

    public void setScale(float scale) {
        this.mScale = scale;
    }

    public int getMargin() {
        return mMargin;
    }

    public void setMargin(int mMargin) {
        this.mMargin = mMargin;
    }

}