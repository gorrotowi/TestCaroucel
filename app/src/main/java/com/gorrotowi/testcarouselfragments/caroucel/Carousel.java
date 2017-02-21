package com.gorrotowi.testcarouselfragments.caroucel;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Transformation;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author kushnarev
 *         Carousel class
 */
public class Carousel extends CarouselSpinner implements GestureDetector.OnGestureListener {

    private long mLastEventTime = 0;
    // Static private members

    /**
     * Tag for a class logging
     */
    private static final String TAG = Carousel.class.getSimpleName();

    /**
     * If logging should be inside class
     */
    private static final boolean localLOGV = false;

    /**
     * Default min quantity of images
     */
    private static final int MIN_QUANTITY = 3;

    /**
     * Default max quantity of images
     */
    private static final int MAX_QUANTITY = 12;

    /**
     * Max theta
     */
    private static final float MAX_THETA = 25.0f;

    /**
     * Duration in milliseconds from the start of a scroll during which we're
     * unsure whether the user is scrolling or flinging.
     */
    private static final int SCROLL_TO_FLING_UNCERTAINTY_TIMEOUT = 250;

    // Private members

    /**
     * The info for adapter context menu
     */
    private AdapterContextMenuInfo mContextMenuInfo;

    /**
     * How long the transition animation should run when a child view changes
     * position, measured in milliseconds.
     */
    private int mAnimationDuration = 200;

    /**
     * Camera to make 3D rotation
     */
    private Camera mCamera = new Camera();

    /**
     * Sets mSuppressSelectionChanged = false. This is used to set it to false
     * in the future. It will also trigger a selection changed.
     */
    private Runnable mDisableSuppressSelectionChangedRunnable = new Runnable() {
        public void run() {
            mSuppressSelectionChanged = false;
            selectionChanged();
        }
    };

    /**
     * The position of the item that received the user's down touch.
     */
    private int mDownTouchPosition;

    /**
     * The view of the item that received the user's down touch.
     */
    private View mDownTouchView;

    /**
     * Executes the delta rotations from a fling or scroll movement.
     */
    private FlingRotateRunnable mFlingRunnable = new FlingRotateRunnable();

    /**
     * Helper for detecting touch gestures.
     */
    private GestureDetector mGestureDetector;

    /**
     * Gravity for the widget
     */
    private int mGravity;

    /**
     * If true, this onScroll is the first for this user's drag (remember, a
     * drag sends many onScrolls).
     */
    private boolean mIsFirstScroll;

    /**
     * Set max qantity of images
     */
    private int mMaxQuantity = MAX_QUANTITY;

    /**
     * Set min quantity of images
     */
    private int mMinQuantity = MIN_QUANTITY;

    /**
     * If true, we have received the "invoke" (center or enter buttons) key
     * down. This is checked before we action on the "invoke" key up, and is
     * subsequently cleared.
     */
    private boolean mReceivedInvokeKeyDown;

    /**
     * The currently selected item's child.
     */
    private View mSelectedChild;


    /**
     * Whether to continuously callback on the item selected listener during a
     * fling.
     */
    private boolean mShouldCallbackDuringFling = true;

    /**
     * Whether to callback when an item that is not selected is clicked.
     */
    private boolean mShouldCallbackOnUnselectedItemClick = true;

    /**
     * When fling runnable runs, it resets this to false. Any method along the
     * path until the end of its run() can set this to true to abort any
     * remaining fling. For example, if we've reached either the leftmost or
     * rightmost item, we will set this to true.
     */
    private boolean mShouldStopFling;

    /**
     * If true, do not callback to item selected listener.
     */
    private boolean mSuppressSelectionChanged;

    /**
     * The axe angle
     */
    private float mTheta = (float) (90.0f * (Math.PI / 180.0));
    private static ArrayList<CarouselItem> OriginalmImages;
    private boolean hasScrolled = false;
    int lastMeasureHeigth = 0;
    int lastMeasureWidth = 0;
    public boolean isDraggin = false;
    public boolean isScrolling = false;
    // Constructors


    public Carousel(Context context) {
        this(context, null);
    }

    public Carousel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Carousel(Context context, AttributeSet attrs, int defStyle) {

        super(context, attrs, defStyle);

        // It's needed to make items with greater value of
        // z coordinate to be behind items with lesser z-coordinate
        setChildrenDrawingOrderEnabled(true);

        // Making user gestures available
        mGestureDetector = new GestureDetector(this.getContext(), this);
        mGestureDetector.setIsLongpressEnabled(true);

        // It's needed to apply 3D transforms to items
        // before they are drawn
        setStaticTransformationsEnabled(true);

        setNextSelectedPositionInt(0);


    }

    // View overrides

    // These are for use with horizontal scrollbar

    /**
     * Compute the horizontal extent of the horizontal scrollbar's thumb
     * within the horizontal range. This value is used to compute
     * the length of the thumb within the scrollbar's track.
     */
    @Override
    protected int computeHorizontalScrollExtent() {
        // Only 1 item is considered to be selected
        return 1;
    }

    /**
     * Compute the horizontal offset of the horizontal scrollbar's
     * thumb within the horizontal range. This value is used to compute
     * the position of the thumb within the scrollbar's track.
     */
    @Override
    protected int computeHorizontalScrollOffset() {
        // Current scroll position is the same as the selected position
        return mSelectedPosition;
    }

    /**
     * Compute the horizontal range that the horizontal scrollbar represents.
     */
    @Override
    protected int computeHorizontalScrollRange() {
        // Scroll range is the same as the item count
        return mItemCount;
    }

    /**
     * Implemented to handle touch screen motion events.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {


        // Give everything to the gesture detector
        boolean retValue = mGestureDetector.onTouchEvent(event);

        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            // Helper method for lifted finger
            onUp();
        } else if (action == MotionEvent.ACTION_CANCEL) {
            onCancel();
        }


        return true;
    }

    /**
     * Extra information about the item for which the context menu should be shown.
     */
    @Override
    protected ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    /**
     * Bring up the context menu for this view.
     */
    @Override
    public boolean showContextMenu() {

        if (isPressed() && mSelectedPosition >= 0) {
            int index = mSelectedPosition - mFirstPosition;
            View v = getChildAt(index);
            return dispatchLongPress(v, mSelectedPosition, mSelectedRowId);
        }

        return false;
    }

    /**
     * Handles left, right, and clicking
     *
     * @see View#onKeyDown
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {

            case KeyEvent.KEYCODE_DPAD_LEFT:
                ////if (movePrevious()) {
                playSoundEffect(SoundEffectConstants.NAVIGATION_LEFT);
                ////}
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                /////if (moveNext()) {
                playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT);
                ////}
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                mReceivedInvokeKeyDown = true;
                // fallthrough to default handling
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: {

                if (mReceivedInvokeKeyDown) {
                    if (mItemCount > 0) {

                        dispatchPress(mSelectedChild);
                        postDelayed(new Runnable() {
                            public void run() {
                                dispatchUnpress();
                            }
                        }, ViewConfiguration.getPressedStateDuration());

                        int selectedIndex = mSelectedPosition - mFirstPosition;

                        if (mDownTouchPosition == mSelectedPosition)
                            performItemClick(getChildAt(selectedIndex), mSelectedPosition, mAdapter
                                    .getItemId(mSelectedPosition));
                    }
                }

                // Clear the flag
                mReceivedInvokeKeyDown = false;

                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        
        /*
         * The gallery shows focus by focusing the selected item. So, give
         * focus to our selected item instead. We steal keys from our
         * selected item elsewhere.
         */
        if (gainFocus && mSelectedChild != null) {
            mSelectedChild.requestFocus(direction);
        }

    }

    // ViewGroup overrides

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public void dispatchSetSelected(boolean selected) {
        /*
         * We don't want to pass the selected state given from its parent to its
         * children since this widget itself has a selected state to give to its
         * children.
         */
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {

        // Show the pressed state on the selected child
        if (mSelectedChild != null) {
            mSelectedChild.setPressed(pressed);
        }
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {

        final int longPressPosition = getPositionForView(originalView);
        if (longPressPosition < 0) {
            return false;
        }

        final long longPressId = mAdapter.getItemId(longPressPosition);
        return dispatchLongPress(originalView, longPressPosition, longPressId);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Gallery steals all key events
        return event.dispatch(this, null, null);
    }

    /**
     * Index of the child to draw for this iteration
     */
    @Override
    protected int getChildDrawingOrder(int childCount, int i) {

        // Sort Carousel items by z coordinate in reverse order
        ArrayList<CarouselItem> sl = new ArrayList<CarouselItem>();
        for (int j = 0; j < childCount; j++) {
            CarouselItem view = (CarouselItem) getAdapter().getView(j, null, null);
            if (i == 0)
                view.setDrawn(false);
            sl.add((CarouselItem) getAdapter().getView(j, null, null));
        }

        Collections.sort(sl);

        // Get first undrawn item in array and get result index
        int idx = 0;

        for (CarouselItem civ : sl) {
            if (!civ.isDrawn()) {
                civ.setDrawn(true);
                idx = civ.getIndex();
                break;
            }
        }

        return idx;

    }

    /**
     * Transform an item depending on it's coordinates
     */
    @Override
    protected boolean getChildStaticTransformation(View child, Transformation transformation) {

        transformation.clear();
        transformation.setTransformationType(Transformation.TYPE_MATRIX);

        // Center of the view
        float centerX = (float) getWidth() / 2, centerY = (float) getHeight() / 2;

        // Save camera
        mCamera.save();

        // Translate the item to it's coordinates
        final Matrix matrix = transformation.getMatrix();

        mCamera.translate(((CarouselItem) child).getItemX(), ((CarouselItem) child).getItemY(),
                ((CarouselItem) child).getItemZ());

        // Align the item
        mCamera.getMatrix(matrix);

        matrix.preTranslate(-centerX, -centerY);

        matrix.postTranslate(centerX, centerY);

        float[] values = new float[9];
        matrix.getValues(values);

        // Restore camera
        mCamera.restore();

        Matrix mm = new Matrix();
        mm.setValues(values);
        ((CarouselItem) child).setCIMatrix(mm);

        child.invalidate();


        return true;
    }

    // CarouselAdapter overrides

    /**
     * Setting up images
     */
    void layout(int delta, boolean animate) {

        if (mDataChanged) {
            handleDataChanged();
        }

        // Handle an empty gallery by removing all views.
        if (getCount() == 0) {
            resetList();
            return;
        }


        if (mNextSelectedPosition >= 0) {
            setSelectedPositionInt(mNextSelectedPosition);
        }

        // All views go in recycler while we are in layout
        recycleAllViews();

        // Clear out old views
        detachAllViewsFromParent();


        float angleUnit = 360.0f / 18;

        float angleOffset = mNextSelectedPosition * angleUnit;


        for (int i = 0; i < getAdapter().getCount(); i++) {

            boolean visible = true;
            float angle = -angleUnit * i + angleOffset;


            if (i > 4) {
                angle = 180;
            }

            if (angle < 0.0f)
                angle = 360.0f + angle;


            makeAndAddView(i, angle, visible);
        }


        // Flush any cached views that did not get reused above
        mRecycler.clear();

        invalidate();

        setNextSelectedPositionInt(mSelectedPosition);

        checkSelectionChanged();

        ////////mDataChanged = false;
        mNeedSync = false;

        updateSelectedItemMetadata();

    }

    /**
     * Setting up images after layout changed
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        /*
         * Remember that we are in layout to prevent more layout request from
         * being generated.
         */
        mInLayout = true;
        if (!hasScrolled)
            layout(0, false);
        mInLayout = false;
    }

    @Override
    void selectionChanged() {
        if (!mSuppressSelectionChanged) {
            super.selectionChanged();
        }
    }

    @Override
    void setSelectedPositionInt(int position) {
        super.setSelectedPositionInt(position);
        super.setNextSelectedPositionInt(position);

        // Updates any metadata we keep about the selected item.
        updateSelectedItemMetadata();
    }


    // Rotation class for the Carousel

    private class FlingRotateRunnable implements Runnable {

        /**
         * Tracks the decay of a fling rotation
         */
        private Rotator mRotator;

        /**
         * Angle value reported by mRotator on the previous fling
         */
        private float mLastFlingAngle;

        /**
         * Constructor
         */
        public FlingRotateRunnable() {
            mRotator = new Rotator(getContext());
        }

        private void startCommon() {
            // Remove any pending flings
            removeCallbacks(this);
        }

        public void startUsingVelocity(float initialVelocity) {
            if (initialVelocity == 0) return;

            startCommon();

            mLastFlingAngle = 0.0f;

            mRotator.fling(initialVelocity);

            post(this);
        }


        public void startUsingDistance(float deltaAngle) {
            if (deltaAngle == 0) return;

            startCommon();

            mLastFlingAngle = 0;
            synchronized (this) {
                mRotator.startRotate(0.0f, -deltaAngle, mAnimationDuration);
            }
            post(this);
        }

        public void stop(boolean scrollIntoSlots) {
            removeCallbacks(this);
            endFling(scrollIntoSlots);
        }

        private void endFling(boolean scrollIntoSlots) {
            /*
             * Force the scroller's status to finished (without setting its
             * position to the end)
             */
            synchronized (this) {
                mRotator.forceFinished(true);
            }

            if (scrollIntoSlots) scrollIntoSlots();
        }

        public void run() {
            if (Carousel.this.getChildCount() == 0) {
                endFling(true);
                return;
            }

            mShouldStopFling = false;

            final Rotator rotator;
            final float angle;
            boolean more;
            synchronized (this) {
                rotator = mRotator;
                more = rotator.computeAngleOffset();
                angle = rotator.getCurrAngle();
            }


            // Flip sign to convert finger direction to list items direction
            // (e.g. finger moving down means list is moving towards the top)
            float delta = mLastFlingAngle - angle;

            //////// Shoud be reworked
            trackMotionScroll(delta);

            if (more && !mShouldStopFling) {
                mLastFlingAngle = angle;
                post(this);
            } else {
                mLastFlingAngle = 0.0f;
                endFling(true);
            }

        }

    }

    // Image adapter class for the Carousel

    public static class ImageAdapter extends BaseAdapter {

        private Context mContext;
        private ArrayList<CarouselItem> mImages;


        public ImageAdapter(Context c, ArrayList<CarouselItem> _mImages) {

            OriginalmImages = _mImages;
            mImages = _mImages;

            mContext = c;
            for (int i = 0; i < _mImages.size(); i++) {

                mImages.get(i).setIndex(i);
            }


        }


        public int getCount() {
            if (mImages == null)
                return 0;
            else
                return mImages.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (position >= mImages.size())
                return new CarouselItem(mContext);
            else
                return mImages.get(position);
        }

    }

    // OnGestureListener implementation

    public boolean onDown(MotionEvent e) {
        // Kill any existing fling/scroll
        mFlingRunnable.stop(false);

        ///// Don't know yet what for it is
        // Get the item's view that was touched
        mDownTouchPosition = pointToPosition((int) e.getX(), (int) e.getY());

        if (mDownTouchPosition >= 0) {
            mDownTouchView = getChildAt(mDownTouchPosition - mFirstPosition);
            mDownTouchView.setPressed(true);
        }


        // Reset the multiple-scroll tracking state
        mIsFirstScroll = true;

        // Must return true to get matching events for this down event.
        return true;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        if (!mShouldCallbackDuringFling) {
            // We want to suppress selection changes

            // Remove any future code to set mSuppressSelectionChanged = false
            removeCallbacks(mDisableSuppressSelectionChangedRunnable);

            // This will get reset once we scroll into slots
            if (!mSuppressSelectionChanged) mSuppressSelectionChanged = true;
        }


        mFlingRunnable.startUsingVelocity((int) velocityX);

        return true;
    }


    public void onLongPress(MotionEvent e) {

        if (mDownTouchPosition < 0) {
            return;
        }

        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        long id = getItemIdAtPosition(mDownTouchPosition);
        dispatchLongPress(mDownTouchView, mDownTouchPosition, id);

    }


    public void onStopDraggin() {
        isDraggin = false;
        isScrolling = false;
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {

        if (localLOGV) Log.v(TAG, String.valueOf(e2.getX() - e1.getX()));

        /*
         * Now's a good time to tell our parent to stop intercepting our events!
         * The user has moved more than the slop amount, since GestureDetector
         * ensures this before calling this method. Also, if a parent is more
         * interested in this touch's events than we are, it would have
         * intercepted them by now (for example, we can assume when a Gallery is
         * in the ListView, a vertical scroll would not end up in this method
         * since a ListView would have intercepted it by now).
         */
        float y = e1.getY();
        float x = e1.getX();
        float h = getHeight();
        float w = getWidth();
        hasScrolled = true;
        if (isDraggin)
            return false;


        if (mOnDragListener != null && (distanceX < 15 && distanceX > -15) && (y > 0 && y < ((lastMeasureHeigth)) && x > ((getWidth() / 2) - lastMeasureWidth / 2) && x < ((getWidth() / 2) + lastMeasureWidth / 2) && !isDraggin) && !isScrolling) {

            if (!isDraggin) {

                CarouselItem lastCarouselItem = (CarouselItem) getSelectedView();

                if (lastCarouselItem.getGestureType() != CarouselItem.DRAG)
                    return true;

                CarouselItem item2 = lastCarouselItem;


                ClipData data = ClipData.newPlainText("", "");
                DragShadowBuilder shadowBuilder = new DragShadowBuilder(
                        item2);

                //shadowBuilder.getView().setAlpha(1);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    item2.startDragAndDrop(data, shadowBuilder, item2, 0);
                } else {
                    item2.startDrag(data, shadowBuilder, item2, 0);
                }
                mOnDragListener.onStarDrag(item2);
            }

            isDraggin = true;

            return false;
        }


        isScrolling = true;
        getParent().requestDisallowInterceptTouchEvent(true);

        // As the user scrolls, we want to callback selection changes so related-
        // info on the screen is up-to-date with the gallery's selection
        if (!mShouldCallbackDuringFling) {
            if (mIsFirstScroll) {
                /*
                 * We're not notifying the client of selection changes during
                 * the fling, and this scroll could possibly be a fling. Don't
                 * do selection changes until we're sure it is not a fling.
                 */
                if (!mSuppressSelectionChanged) mSuppressSelectionChanged = true;
                postDelayed(mDisableSuppressSelectionChangedRunnable, SCROLL_TO_FLING_UNCERTAINTY_TIMEOUT);
            }
        } else {
            if (mSuppressSelectionChanged) mSuppressSelectionChanged = false;
        }


        if (SystemClock.elapsedRealtime() - mLastEventTime < 50) {
            return true;
        }
        mLastEventTime = SystemClock.elapsedRealtime();

        trackMotionScroll(/* -1 * */ (int) distanceX);

        mIsFirstScroll = false;
        return true;
    }


    public boolean onSingleTapUp(MotionEvent e) {
        if (mDownTouchPosition >= 0) {

            // Pass the click so the client knows, if it wants to.
            if (mShouldCallbackOnUnselectedItemClick || mDownTouchPosition == mSelectedPosition) {

                if (mDownTouchPosition == mSelectedPosition)
                    performItemClick(mDownTouchView, mDownTouchPosition, mAdapter
                            .getItemId(mDownTouchPosition));

                scrollToChild(mDownTouchPosition);
            }

            return true;
        }

        return false;
    }


    ///// Unused gestures
    public void onShowPress(MotionEvent e) {
    }


    private void Calculate3DPosition(CarouselItem child, int diameter, float angleOffset) {

        angleOffset = angleOffset * (float) (Math.PI / 180.0f);

        float offsetX = child.getWidth() / 1.0f * ((float) Math.sin(angleOffset));
        float x = -(float) (diameter / 2 * (float) Math.sin(angleOffset)) + diameter / 2 - child.getWidth() / 2 - offsetX;
        float y = (float) (diameter / 1.2 * (1.0f - (float) Math.cos(angleOffset)) * (float) Math.sin(-mTheta));
        float z = 1300 * (1.0f - (float) Math.cos(angleOffset));

        child.setItemX(x);
        child.setItemZ(z);
        child.setItemY(y);


    }


    /**
     * Figure out vertical placement based on mGravity
     *
     * @param child Child to place
     * @return Where the top of the child should be
     */
    private int calculateTop(View child, boolean duringLayout) {
        int myHeight = duringLayout ? getMeasuredHeight() : getHeight();
        int childHeight = duringLayout ? child.getMeasuredHeight() : child.getHeight();

        int childTop = 0;


        switch (mGravity) {
            case Gravity.TOP:
                childTop = mSpinnerPadding.top;
                break;
            case Gravity.CENTER_VERTICAL:
                int availableSpace = myHeight - mSpinnerPadding.bottom
                        - mSpinnerPadding.top - childHeight;
                childTop = mSpinnerPadding.top + (availableSpace / 2);
                break;
            case Gravity.BOTTOM:
                childTop = myHeight - mSpinnerPadding.bottom - childHeight;
                break;
        }
        return childTop;
    }

    private boolean dispatchLongPress(View view, int position, long id) {
        boolean handled = false;

        if (mOnItemLongClickListener != null) {
            handled = mOnItemLongClickListener.onItemLongClick(this, mDownTouchView,
                    mDownTouchPosition, id);
        }

        if (!handled) {
            mContextMenuInfo = new AdapterContextMenuInfo(view, position, id);
            handled = super.showContextMenuForChild(this);
        }

        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }

        return handled;
    }

    private void dispatchPress(View child) {

        if (child != null) {
            child.setPressed(true);
        }

        setPressed(true);
    }

    private void dispatchUnpress() {

        for (int i = getChildCount() - 1; i >= 0; i--) {
            getChildAt(i).setPressed(false);
        }

        setPressed(false);
    }

    /**
     * @return The center of this Gallery.
     */
    private int getCenterOfGallery() {
        return (getWidth() - Carousel.this.getPaddingLeft() - Carousel.this.getPaddingRight()) / 2 +
                Carousel.this.getPaddingLeft();
    }

    /**
     * @return The center of the given view.
     */
    private static int getCenterOfView(View view) {
        return view.getLeft() + view.getWidth() / 2;
    }


    float getLimitedMotionScrollAmount(boolean motionToLeft, float deltaX) {
        int extremeItemPosition = motionToLeft ? Carousel.this.getCount() - 1 : 0;
        View extremeChild = getChildAt(extremeItemPosition - Carousel.this.getFirstVisiblePosition());

        if (extremeChild == null) {
            return deltaX;
        }

        int extremeChildCenter = getCenterOfView(extremeChild);
        int galleryCenter = getCenterOfGallery();

        if (motionToLeft) {
            if (extremeChildCenter <= galleryCenter) {

                // The extreme child is past his boundary point!
                return 0;
            }
        } else {
            if (extremeChildCenter >= galleryCenter) {

                // The extreme child is past his boundary point!
                return 0;
            }
        }
        int centerDifference = galleryCenter - extremeChildCenter;

        return motionToLeft
                ? Math.max(centerDifference, deltaX)
                : Math.min(centerDifference, deltaX);
    }


    int getLimitedMotionScrollAmount(boolean motionToLeft, int deltaX) {
        int extremeItemPosition = motionToLeft ? mItemCount - 1 : 0;
        View extremeChild = getChildAt(extremeItemPosition - mFirstPosition);

        if (extremeChild == null) {
            return deltaX;
        }

        int extremeChildCenter = getCenterOfView(extremeChild);
        int galleryCenter = getCenterOfGallery();

        if (motionToLeft) {
            if (extremeChildCenter <= galleryCenter) {

                // The extreme child is past his boundary point!
                return 0;
            }
        } else {
            if (extremeChildCenter >= galleryCenter) {

                // The extreme child is past his boundary point!
                return 0;
            }
        }

        int centerDifference = galleryCenter - extremeChildCenter;

        return motionToLeft
                ? Math.max(centerDifference, deltaX)
                : Math.min(centerDifference, deltaX);
    }

    private void makeAndAddView(int position, float angleOffset, boolean visible) {
        CarouselItem child;


        if (!mDataChanged) {
            child = (CarouselItem) mRecycler.get(position);
            if (child != null) {

                lastMeasureHeigth = child.getMeasuredHeight();
                lastMeasureWidth = child.getMeasuredWidth();
                child.setVisible(visible);
                if (position > 4) {
                    child.setVisible(false);
                    child.setAlpha(0.0f);
                }
                // Position the view
                setUpChild(child, child.getIndex(), angleOffset, lastMeasureHeigth, lastMeasureWidth);

            } else {
                // Nothing found in the recycler -- ask the adapter for a view
                child = (CarouselItem) mAdapter.getView(position, null, this);
                child.setVisible(visible);
                if (position > 4) {
                    child.setVisible(false);
                    child.setAlpha(0.0f);
                }

                // Position the view
                setUpChild(child, child.getIndex(), angleOffset, lastMeasureHeigth, lastMeasureWidth);
            }
            return;
        }

        // Nothing found in the recycler -- ask the adapter for a view
        child = (CarouselItem) mAdapter.getView(position, null, this);
        child.setVisible(visible);
        if (position > 4) {
            child.setVisible(false);
            child.setAlpha(0.0f);
        }
        // Position the view
        setUpChild(child, child.getIndex(), angleOffset, lastMeasureHeigth, lastMeasureWidth);

    }

    public void onCancel() {
        onUp();
    }

    /**
     * Called when rotation is finished
     */
    private void onFinishedMovement() {
        if (mSuppressSelectionChanged) {
            mSuppressSelectionChanged = false;

            // We haven't been callbacking during the fling, so do it now
            super.selectionChanged();
        }
        checkSelectionChanged();
        invalidate();

    }

    public void onUp() {

        onStopDraggin();
        if (mFlingRunnable.mRotator.isFinished()) {
            scrollIntoSlots();
        }
        dispatchUnpress();
    }

    /**
     * Brings an item with nearest to 0 degrees angle to this angle and sets it selected
     */
    private void scrollIntoSlots() {

        // Nothing to do
        if (getChildCount() == 0 || mSelectedChild == null) return;

        // get nearest item to the 0 degrees angle
        // Sort itmes and get nearest angle
        float angle;
        int position;

        ArrayList<CarouselItem> arr = new ArrayList<CarouselItem>();

        for (int i = 0; i < getAdapter().getCount(); i++)
            arr.add(((CarouselItem) getAdapter().getView(i, null, null)));

        Collections.sort(arr, new Comparator<CarouselItem>() {

            public int compare(CarouselItem c1, CarouselItem c2) {
                int a1 = (int) c1.getCurrentAngle();
                if (a1 > 180)
                    a1 = 360 - a1;
                int a2 = (int) c2.getCurrentAngle();
                if (a2 > 180)
                    a2 = 360 - a2;
                return (a1 - a2);
            }

        });


        angle = arr.get(0).getCurrentAngle();

        // Make it minimum to rotate
        if (angle > 180.0f)
            angle = -(360.0f - angle);

        // Start rotation if needed
        if (angle != 0.0f) {
            mFlingRunnable.startUsingDistance(-angle);
        } else {
            // Set selected position
            position = arr.get(0).getIndex();
            setSelectedPositionInt(position);
            onFinishedMovement();
        }


    }

    public void scrollToChild(int i) {


        CarouselItem view = (CarouselItem) getAdapter().getView(i, null, null);
        float angle = view.getCurrentAngle();

        if (angle == 0)
            return;

        if (angle > 180.0f)
            angle = 360.0f - angle;
        else
            angle = -angle;

        mFlingRunnable.startUsingDistance(angle);


    }

    /**
     * Whether or not to callback on any {@link #getOnItemSelectedListener()}
     * while the items are being flinged. If false, only the final selected item
     * will cause the callback. If true, all items between the first and the
     * final will cause callbacks.
     *
     * @param shouldCallback Whether or not to callback on the listener while
     *                       the items are being flinged.
     */
    public void setCallbackDuringFling(boolean shouldCallback) {
        mShouldCallbackDuringFling = shouldCallback;
    }

    /**
     * Whether or not to callback when an item that is not selected is clicked.
     * If false, the item will become selected (and re-centered). If true, the
     * {@link #getOnItemClickListener()} will get the callback.
     *
     * @param shouldCallback Whether or not to callback on the listener when a
     *                       item that is not selected is clicked.
     * @hide
     */
    public void setCallbackOnUnselectedItemClick(boolean shouldCallback) {
        mShouldCallbackOnUnselectedItemClick = shouldCallback;
    }

    /**
     * Sets how long the transition animation should run when a child view
     * changes position. Only relevant if animation is turned on.
     *
     * @param animationDurationMillis The duration of the transition, in
     *                                milliseconds.
     * @attr ref android.R.styleable#Gallery_animationDuration
     */
    public void setAnimationDuration(int animationDurationMillis) {
        mAnimationDuration = animationDurationMillis;
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            requestLayout();
        }
    }


    private void setUpChild(CarouselItem child, int index, float angleOffset,
                            int lastMeasureHeigth,
                            int lastMeasureWidth) {

        // Ignore any layout parameters for child, use wrap content


        addViewInLayout(child, -1 /*index*/, generateDefaultLayoutParams());

        child.setSelected(index == mSelectedPosition);

        int h;
        int w;
        int d;

        if (mInLayout) {
            if (child.getMeasuredWidth() == 0) {
                w = lastMeasureWidth;
                h = lastMeasureHeigth;
                d = getMeasuredWidth();
            } else {
                w = child.getMeasuredWidth();
                h = child.getMeasuredHeight();
                d = getMeasuredWidth();
            }


        } else {
            w = child.getMeasuredWidth();
            h = child.getMeasuredHeight();
            d = getWidth();

        }

        child.setCurrentAngle(angleOffset);

        // Measure child
        child.measure(w, h);

        int childLeft;

        // Position vertically based on gravity setting
        int childTop = calculateTop(child, true);


        childLeft = 0;

        child.layout(childLeft, childTop, w, h);

        Calculate3DPosition(child, d, angleOffset);

    }

    /**
     * Tracks a motion scroll. In reality, this is used to do just about any
     * movement to items (touch scroll, arrow-key scroll, set an item as selected).
     *
     * @param deltaAngle Change in X from the previous event.
     */

    private int lastIndexVisible = 4;
    private int firstIndexVisible = 0;

    synchronized void trackMotionScroll(float deltaAngle) {

        if (deltaAngle > 10) {
            deltaAngle = 10;
        }

        int count = getAdapter().getCount();

        if (deltaAngle < -10) {
            deltaAngle = -10;
        }
        if (deltaAngle == 0) {
            deltaAngle = 0;
        }
        if (getChildCount() == 0) {
            return;
        }

        for (int i = 0; i < getAdapter().getCount(); i++) {

            CarouselItem child = (CarouselItem) getAdapter().getView(i, null, null);

            float OriginalAngle = child.getCurrentAngle();
            float angle = child.getCurrentAngle();
            angle += deltaAngle;

            while (angle > 360.0f)
                angle -= 360.0f;

            while (angle < 0.0f)
                angle += 360.0f;

            child.setCurrentAngle(angle);


        }

        boolean setted = false;
        for (int i = 0; i < getAdapter().getCount(); i++) {
            CarouselItem child = (CarouselItem) getAdapter().getView(i, null, null);

            if (child.getCurrentAngle() > 49 && child.getCurrentAngle() < 309) {

                child.setEmpty(true);
                child.setVisible(false);

                float alpha = child.getAlpha();


                if (alpha == 1.0) {
                    if (deltaAngle < 0) {

                        if (!setted) {
                            int nextItem = i - 5;


                            if (nextItem < 0)
                                nextItem = getAdapter().getCount() + nextItem;

                            int nextItemAngle = nextItem + 1;

                            if (nextItemAngle >= getAdapter().getCount()) {
                                nextItemAngle = getAdapter().getCount() - nextItemAngle;
                            }
                            CarouselItem childNext = (CarouselItem) getAdapter().getView(nextItem, null, null);
                            childNext.setAlpha(1.0f);
                            childNext.setCurrentAngle(((CarouselItem) getAdapter().getView(nextItemAngle, null, null)).getCurrentAngle() + 20f);
                            setted = true;
                        }
                    } else {
                        if (!setted) {

                            int nextItem = i + 5;


                            if (nextItem >= getAdapter().getCount())
                                nextItem = nextItem - getAdapter().getCount();

                            int nextItemAngle = nextItem - 1;

                            if (nextItemAngle < 0) {
                                nextItemAngle = getAdapter().getCount() - 1;
                            }
                            CarouselItem childNext = (CarouselItem) getAdapter().getView(nextItem, null, null);
                            childNext.setAlpha(1.0f);
                            childNext.setCurrentAngle(((CarouselItem) getAdapter().getView(nextItemAngle, null, null)).getCurrentAngle() - 20f);
                            setted = true;
                        }
                    }

                }

                child.setAlpha(0.0f);

            } else {


                float alpha = child.getAlpha();


                if (alpha < 1.0) {


                    /*

                    if(deltaAngle >= 0&&child.isEmpty())
                    {
                        if (lastIndexVisible + 1 >= OriginalmImages.size()) {
                            lastIndexVisible= OriginalmImages.size() - lastIndexVisible + 1;

                            firstIndexVisible= lastIndexVisible+4;
                            int drawable = OriginalmImages.get(lastIndexVisible).getDrawable();
                            child.setImageDrawable(drawable);

                        } else  {
                            lastIndexVisible +=1;
                            firstIndexVisible+= 1;
                            int drawable = OriginalmImages.get(lastIndexVisible).getDrawable();
                            child.setImageDrawable(drawable);




                        }

                    }
                    else  if(deltaAngle <= -1 &&child.isEmpty())
                    {

                        if(firstIndexVisible -1 < 0)
                        {
                            firstIndexVisible = OriginalmImages.size()-1;
                            lastIndexVisible=firstIndexVisible-4;
                            int drawable = OriginalmImages.get(firstIndexVisible).getDrawable();
                            child.setImageDrawable(drawable);

                        }
                        else {
                            firstIndexVisible -= 1;
                            lastIndexVisible-=1;
                            int drawable = OriginalmImages.get(firstIndexVisible).getDrawable();
                            child.setImageDrawable(drawable);
                        }
                    } else
                    {

                    }
                */

                    child.setEmpty(false);
                }

                child.setVisible(true);
                child.setAlpha(1.0f);
            }
        }

        for (int i = 0; i < getAdapter().getCount(); i++) {
            CarouselItem child = (CarouselItem) getAdapter().getView(i, null, null);
            Calculate3DPosition(child, getWidth(), child.getCurrentAngle());
        }

        // Clear unused views
        mRecycler.clear();

        invalidate();
    }

    private void updateSelectedItemMetadata() {

        View oldSelectedChild = mSelectedChild;

        View child = mSelectedChild = getChildAt(mSelectedPosition - mFirstPosition);
        if (child == null) {
            return;
        }

        child.setSelected(true);
        child.setFocusable(true);

        if (hasFocus()) {
            child.requestFocus();
        }

        // We unfocus the old child down here so the above hasFocus check
        // returns true
        if (oldSelectedChild != null) {

            // Make sure its drawable state doesn't contain 'selected'
            oldSelectedChild.setSelected(false);

            // Make sure it is not focusable anymore, since otherwise arrow keys
            // can make this one be focused
            oldSelectedChild.setFocusable(false);
        }

    }


}
