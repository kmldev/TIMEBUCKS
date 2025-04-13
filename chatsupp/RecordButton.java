package org.mintsoft.mintly.chatsupp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;

import org.mintsoft.mintly.R;

public class RecordButton extends AppCompatImageView implements View.OnTouchListener, View.OnClickListener {
    private ScaleAnim scaleAnim;
    private RecordView recordView;
    private boolean listenForRecord = true;
    private Orcl orcl;

    public void setRecordView(RecordView recordView) {
        this.recordView = recordView;
    }

    public RecordButton(Context context) {
        super(context);
        init(context, null);
    }

    public RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RecordButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordButton);
            int imageResource = typedArray.getResourceId(R.styleable.RecordButton_mic_icon, -1);
            if (imageResource != -1) {
                setMicIcon(imageResource);
            }
            typedArray.recycle();
        }
        scaleAnim = new ScaleAnim(this);
        this.setOnTouchListener(this);
        this.setOnClickListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setClip(this);
    }

    public void setClip(View v) {
        if (v.getParent() == null) {
            return;
        }

        if (v instanceof ViewGroup) {
            ((ViewGroup) v).setClipChildren(false);
            ((ViewGroup) v).setClipToPadding(false);
        }

        if (v.getParent() instanceof View) {
            setClip((View) v.getParent());
        }
    }


    public void setMicIcon(int imageResource) {
        Drawable image = AppCompatResources.getDrawable(getContext(), imageResource);
        setImageDrawable(image);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isListenForRecord()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    recordView.onActionDown((RecordButton) v, event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    recordView.onActionMove((RecordButton) v, event);
                    break;
                case MotionEvent.ACTION_UP:
                    recordView.onActionUp((RecordButton) v);
                    break;
            }

        }
        return isListenForRecord();
    }

    protected void startScale() {
        scaleAnim.start();
    }

    protected void stopScale() {
        scaleAnim.stop();
    }

    public void setListenForRecord(boolean listenForRecord) {
        this.listenForRecord = listenForRecord;
    }

    public boolean isListenForRecord() {
        return listenForRecord;
    }

    public void setOnRecordClickListener(Orcl orcl) {
        this.orcl = orcl;
    }

    @Override
    public void onClick(View v) {
        if (orcl != null)
            orcl.onClick(v);
    }
}
