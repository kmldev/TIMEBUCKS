package org.mintsoft.mintly.chatsupp;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;

import java.io.IOException;

import io.supercharge.shimmerlayout.ShimmerLayout;

public class RecordView extends RelativeLayout {

    public static final int DEFAULT_CANCEL_BOUNDS = 8; //8dp
    private ImageView smallBlinkingMic, basketImg;
    private Chronometer counterTime;
    private TextView slideToCancel;
    private ShimmerLayout slideToCancelLayout;
    private ImageView arrow;
    private float initialX, basketInitialY, difX = 0;
    private float cancelBounds = DEFAULT_CANCEL_BOUNDS;
    private long startTime, elapsedTime = 0;
    private final Context context;
    private Orl recordListener;
    private Rph rph;
    private boolean isSwiped, isLessThanSecondAllowed = false;
    private boolean isSoundEnabled = true;
    private int RECORD_START = R.raw.record_start;
    private int RECORD_FINISHED = R.raw.record_finished;
    private int RECORD_ERROR = R.raw.record_error;
    private AnimHelper animHelper;
    private boolean isRecordButtonGrowingAnimationEnabled = true;
    private boolean shimmerEffectEnabled = true;
    private long timeLimit = -1;
    private Runnable runnable;
    private Handler handler;
    private org.mintsoft.mintly.chatsupp.RecordButton recordButton;

    private boolean canRecord = true;

    public RecordView(Context context) {
        super(context);
        this.context = context;
        init(context, null, -1, -1);
    }


    public RecordView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context, attrs, -1, -1);
    }

    public RecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(context, attrs, defStyleAttr, -1);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        View view = View.inflate(context, R.layout.record_view_layout, null);
        addView(view);
        ViewGroup viewGroup = (ViewGroup) view.getParent();
        viewGroup.setClipChildren(false);
        arrow = view.findViewById(R.id.arrow);
        slideToCancel = view.findViewById(R.id.slide_to_cancel);
        smallBlinkingMic = view.findViewById(R.id.glowing_mic);
        counterTime = view.findViewById(R.id.counter_tv);
        basketImg = view.findViewById(R.id.basket_img);
        slideToCancelLayout = view.findViewById(R.id.shimmer_layout);
        hideViews(true);
        if (attrs != null && defStyleAttr == -1 && defStyleRes == -1) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordView,
                    defStyleAttr, defStyleRes);
            int slideArrowResource = typedArray.getResourceId(R.styleable.RecordView_slide_to_cancel_arrow, -1);
            String slideToCancelText = DataParse.getStr(context, "slide_to_cancel", Home.spf);
            int slideMarginRight = (int) typedArray.getDimension(R.styleable.RecordView_slide_to_cancel_margin_right, 30);
            int counterTimeColor = typedArray.getColor(R.styleable.RecordView_counter_time_color, -1);
            int arrowColor = typedArray.getColor(R.styleable.RecordView_slide_to_cancel_arrow_color, -1);
            int cancelBounds = typedArray.getDimensionPixelSize(R.styleable.RecordView_slide_to_cancel_bounds, -1);
            if (cancelBounds != -1)
                setCancelBounds(cancelBounds, false);//don't convert it to pixels since it's already in pixels
            if (slideArrowResource != -1) {
                Drawable slideArrow = AppCompatResources.getDrawable(getContext(), slideArrowResource);
                arrow.setImageDrawable(slideArrow);
            }
            if (slideToCancelText != null)
                slideToCancel.setText(slideToCancelText);
            if (counterTimeColor != -1)
                setCounterTimeColor(counterTimeColor);
            if (arrowColor != -1)
                setSlideToCancelArrowColor(arrowColor);
            setMarginRight(slideMarginRight, true);
            typedArray.recycle();
        }
        animHelper = new AnimHelper(context, basketImg, smallBlinkingMic, isRecordButtonGrowingAnimationEnabled);
    }

    private boolean isTimeLimitValid() {
        return timeLimit > 0;
    }

    private void initTimeLimitHandler() {
        handler = new Handler();
        runnable = () -> {
            if (recordListener != null && !isSwiped)
                recordListener.onFinish(elapsedTime, true);
            removeTimeLimitCallbacks();
            animHelper.setStartRecorded(false);
            if (!isSwiped)
                playSound(RECORD_FINISHED);
            if (recordButton != null) {
                resetRecord(recordButton);
            }
            isSwiped = true;
        };
    }

    private void hideViews(boolean hideSmallMic) {
        slideToCancelLayout.setVisibility(GONE);
        counterTime.setVisibility(GONE);
        if (hideSmallMic)
            smallBlinkingMic.setVisibility(GONE);
    }

    private void showViews() {
        slideToCancelLayout.setVisibility(VISIBLE);
        smallBlinkingMic.setVisibility(VISIBLE);
        counterTime.setVisibility(VISIBLE);
    }

    private boolean isLessThanOneSecond(long time) {
        return time <= 1000;
    }

    private void playSound(int soundRes) {
        if (isSoundEnabled) {
            if (soundRes == 0)
                return;
            try {
                MediaPlayer player = new MediaPlayer();
                AssetFileDescriptor afd = context.getResources().openRawResourceFd(soundRes);
                if (afd == null) return;
                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                player.prepare();
                player.start();
                player.setOnCompletionListener(mp -> mp.release());
                player.setLooping(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    protected void onActionDown(org.mintsoft.mintly.chatsupp.RecordButton recordBtn, MotionEvent motionEvent) {
        if (!isRecordPermissionGranted()) {
            return;
        }
        this.recordButton = recordBtn;
        if (recordListener != null)
            recordListener.onStart();
        if (isTimeLimitValid()) {
            removeTimeLimitCallbacks();
            handler.postDelayed(runnable, timeLimit);
        }
        animHelper.setStartRecorded(true);
        animHelper.resetBasketAnimation();
        animHelper.resetSmallMic();
        if (isRecordButtonGrowingAnimationEnabled) {
            recordBtn.startScale();
        }
        if (shimmerEffectEnabled) {
            slideToCancelLayout.startShimmerAnimation();
        }
        initialX = recordBtn.getX();
        basketInitialY = basketImg.getY() + 90;
        playSound(RECORD_START);
        showViews();
        animHelper.animateSmallMicAlpha();
        counterTime.setBase(SystemClock.elapsedRealtime());
        startTime = System.currentTimeMillis();
        counterTime.start();
        isSwiped = false;
    }


    protected void onActionMove(org.mintsoft.mintly.chatsupp.RecordButton recordBtn, MotionEvent motionEvent) {
        if (!canRecord) {
            return;
        }
        long time = System.currentTimeMillis() - startTime;
        if (!isSwiped) {
            //Swipe To Cancel
            if (slideToCancelLayout.getX() != 0 && slideToCancelLayout.getX() <= counterTime.getRight() + cancelBounds) {
                //if the time was less than one second then do not start basket animation
                if (isLessThanOneSecond(time)) {
                    hideViews(true);
                    animHelper.clearAlphaAnimation(false);
                    animHelper.onAnimationEnd();
                } else {
                    hideViews(false);
                    animHelper.animateBasket(basketInitialY);
                }
                animHelper.moveRecordButtonAndSlideToCancelBack(recordBtn, slideToCancelLayout, initialX, difX);
                counterTime.stop();
                if (shimmerEffectEnabled) {
                    slideToCancelLayout.stopShimmerAnimation();
                }
                isSwiped = true;
                animHelper.setStartRecorded(false);
                if (recordListener != null)
                    recordListener.onCancel();
                if (isTimeLimitValid()) {
                    removeTimeLimitCallbacks();
                }
            } else {
                //if statement is to Prevent Swiping out of bounds
                if (motionEvent.getRawX() < initialX) {
                    recordBtn.animate()
                            .x(motionEvent.getRawX())
                            .setDuration(0)
                            .start();
                    if (difX == 0)
                        difX = (initialX - slideToCancelLayout.getX());
                    slideToCancelLayout.animate()
                            .x(motionEvent.getRawX() - difX)
                            .setDuration(0)
                            .start();
                }
            }
        }
    }

    protected void onActionUp(org.mintsoft.mintly.chatsupp.RecordButton recordBtn) {
        if (!canRecord) {
            return;
        }
        elapsedTime = System.currentTimeMillis() - startTime;
        if (!isLessThanSecondAllowed && isLessThanOneSecond(elapsedTime) && !isSwiped) {
            if (recordListener != null)
                recordListener.onLessThanSecond();
            removeTimeLimitCallbacks();
            animHelper.setStartRecorded(false);
            playSound(RECORD_ERROR);
        } else {
            if (recordListener != null && !isSwiped)
                recordListener.onFinish(elapsedTime, false);
            removeTimeLimitCallbacks();
            animHelper.setStartRecorded(false);
            if (!isSwiped)
                playSound(RECORD_FINISHED);
        }
        resetRecord(recordBtn);
    }

    private void resetRecord(RecordButton recordBtn) {
        //if user has swiped then do not hide SmallMic since it will be hidden after swipe Animation
        hideViews(!isSwiped);
        if (!isSwiped)
            animHelper.clearAlphaAnimation(true);
        animHelper.moveRecordButtonAndSlideToCancelBack(recordBtn, slideToCancelLayout, initialX, difX);
        counterTime.stop();
        if (shimmerEffectEnabled) {
            slideToCancelLayout.stopShimmerAnimation();
        }
    }

    private void removeTimeLimitCallbacks() {
        if (isTimeLimitValid()) {
            handler.removeCallbacks(runnable);
        }
    }


    private boolean isRecordPermissionGranted() {
        if (rph == null) {
            canRecord = true;
        }
        canRecord = rph.isPermissionGranted();
        return canRecord;
    }

    private void setMarginRight(int marginRight, boolean convertToDp) {
        LayoutParams layoutParams = (LayoutParams) slideToCancelLayout.getLayoutParams();
        if (convertToDp) {
            layoutParams.rightMargin = (int) org.mintsoft.mintly.chatsupp.DpUtil.toPixel(marginRight, context);
        } else
            layoutParams.rightMargin = marginRight;
        slideToCancelLayout.setLayoutParams(layoutParams);
    }

    public void setOnRecordListener(Orl recrodListener) {
        this.recordListener = recrodListener;
    }

    public void setRecordPermissionHandler(Rph rph) {
        this.rph = rph;
    }

    public void setOnBasketAnimationEndListener(Obae obaeListener) {
        animHelper.setOnBasketAnimationEndListener(obaeListener);
    }

    public void setSoundEnabled(boolean isEnabled) {
        isSoundEnabled = isEnabled;
    }

    public void setLessThanSecondAllowed(boolean isAllowed) {
        isLessThanSecondAllowed = isAllowed;
    }

    public void setSlideToCancelText(String text) {
        slideToCancel.setText(text);
    }

    public void setSlideToCancelTextColor(int color) {
        slideToCancel.setTextColor(color);
    }

    public void setSmallMicColor(int color) {
        smallBlinkingMic.setColorFilter(color);
    }

    public void setSmallMicIcon(int icon) {
        smallBlinkingMic.setImageResource(icon);
    }

    public void setSlideMarginRight(int marginRight) {
        setMarginRight(marginRight, true);
    }

    public void setCustomSounds(int startSound, int finishedSound, int errorSound) {
        //0 means do not play sound
        RECORD_START = startSound;
        RECORD_FINISHED = finishedSound;
        RECORD_ERROR = errorSound;
    }

    public float getCancelBounds() {
        return cancelBounds;
    }

    public void setCancelBounds(float cancelBounds) {
        setCancelBounds(cancelBounds, true);
    }

    //set Chronometer color
    public void setCounterTimeColor(int color) {
        counterTime.setTextColor(color);
    }

    public void setSlideToCancelArrowColor(int color) {
        arrow.setColorFilter(color);
    }

    private void setCancelBounds(float cancelBounds, boolean convertDpToPixel) {
        this.cancelBounds = convertDpToPixel ? DpUtil.toPixel(cancelBounds, context) : cancelBounds;
    }

    public boolean isRecordButtonGrowingAnimationEnabled() {
        return isRecordButtonGrowingAnimationEnabled;
    }

    public void setRecordButtonGrowingAnimationEnabled(boolean recordButtonGrowingAnimationEnabled) {
        isRecordButtonGrowingAnimationEnabled = recordButtonGrowingAnimationEnabled;
        animHelper.setRecordButtonGrowingAnimationEnabled(recordButtonGrowingAnimationEnabled);
    }

    public boolean isShimmerEffectEnabled() {
        return shimmerEffectEnabled;
    }

    public void setShimmerEffectEnabled(boolean shimmerEffectEnabled) {
        this.shimmerEffectEnabled = shimmerEffectEnabled;
    }

    public long getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(long timeLimit) {
        this.timeLimit = timeLimit;
        if (handler != null && runnable != null) {
            removeTimeLimitCallbacks();
        }
        initTimeLimitHandler();
    }

    public void setTrashIconColor(int color) {
        animHelper.setTrashIconColor(color);
    }
}


