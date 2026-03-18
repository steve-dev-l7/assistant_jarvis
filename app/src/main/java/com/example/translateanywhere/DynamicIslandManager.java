package com.example.translateanywhere;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

public class DynamicIslandManager {

    private static final String TAG = "DynamicIslandManager";
    private final Context context;
    private WindowManager windowManager;

    private FrameLayout container;
    private FrameLayout islandView;
    private TextView statusText;
    private GradientDrawable islandShape;

    private final int originalMargin = -90;

    public DynamicIslandManager(Context context, WindowManager windowManager) {
        this.context = context;
        this.windowManager = windowManager;
    }

    public void createDynamicIsland() {
        // 1. Detection Area
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        container = new FrameLayout(context);

        // 2. Island View
        islandView = new FrameLayout(context);
        islandView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        islandShape = new GradientDrawable();
        islandShape.setShape(GradientDrawable.RECTANGLE);
        islandShape.setColor(Color.parseColor("#60FFFFFF"));
        islandShape.setStroke(6, Color.parseColor("#CC000000"));
        islandShape.setCornerRadius(1000f);

        islandView.setBackground(islandShape);

        GradientDrawable glowLayer = new GradientDrawable();
        glowLayer.setShape(GradientDrawable.RECTANGLE);
        glowLayer.setColor(Color.TRANSPARENT);
        glowLayer.setStroke(12, Color.parseColor("#33000000"));
        glowLayer.setCornerRadius(1000f);

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{glowLayer, islandShape});
        islandView.setBackground(layerDrawable);

        // 🔴 Apply the 3D Glass Look (This overwrites the old shapes above)
        applyGlassLook(islandView, 1000f);

        // 3. Status Text
        statusText = new TextView(context);
        statusText.setTextColor(Color.parseColor("#E0E7FF")); // Ice White
        statusText.setTextSize(14f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setTypeface(Typeface.create("sans-serif-condensed-medium", Typeface.BOLD));
        statusText.setLetterSpacing(0.05f);

        statusText.setShadowLayer(5, 0, 0, Color.BLACK);

        islandView.addView(statusText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // 4. Add to Container
        FrameLayout.LayoutParams islandParams = new FrameLayout.LayoutParams(180, 100);
        islandParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        islandParams.topMargin = originalMargin;
        container.addView(islandView, islandParams);

        windowManager.addView(container, params);
        Log.d(TAG, "Overlay added successfully");
    }

    @SuppressLint("SetTextI18n")
    public void updateState(String state) {
        if (islandView == null || statusText == null) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            switch (state.toUpperCase()) {
                case "LISTEN":
                    smartExpandIsland("Listening...");
                    break;
                case "PROCESS":
                    smartExpandIsland("Processing...");
                    break;
                case"THINK":
                    smartExpandIsland("Thinking...");
                    break;
                case "IDLE":
                    statusText.setText("");
                    statusText.setAlpha(0f);
                    animateIslandCollapse();
                    break;
            }
        });
    }

    public void smartExpandIsland(String text) {
        if (islandView == null || statusText == null) return;

        statusText.setText(text);
        statusText.setAlpha(0f);

        // 🔴 1. Allow text to wrap to multiple lines
        statusText.setSingleLine(false);
        statusText.setMaxLines(8); // Max lines for bigger messages
        statusText.setEllipsize(TextUtils.TruncateAt.END);
        statusText.setGravity(Gravity.CENTER); // Center text inside the wrapping box

        // 🔴 2. Define the MAXIMUM limits (So it doesn't go off-screen)
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int maxAllowedWidth = screenWidth - 60; // Leave 30px gap on left & right

        int textHorizontalPadding = 100; // 50px padding inside the glass on each side
        int textVerticalPadding = 60;    // 30px padding inside top & bottom

        // 🔴 3. THE TRUE WRAP_CONTENT MAGIC
        // Tell Android: "Measure exactly how much space this specific text needs,
        // but DO NOT exceed the maxAllowedWidth."
        statusText.measure(
                View.MeasureSpec.makeMeasureSpec(maxAllowedWidth - textHorizontalPadding, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        // 🔴 4. Set target dimensions to EXACTLY what the text measured
        int targetWidth = statusText.getMeasuredWidth() + textHorizontalPadding;
        int targetHeight = statusText.getMeasuredHeight() + textVerticalPadding;
        int targetMargin = 65;

        // Keep a minimum size so a tiny word like "Hi" still looks like a proper pill
        if (targetWidth < 220) targetWidth = 220;
        if (targetHeight < 110) targetHeight = 110;

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) islandView.getLayoutParams();

        // Clipping removal to allow smooth scaling
        if (islandView.getParent() instanceof ViewGroup) {
            ((ViewGroup) islandView.getParent()).setClipChildren(false);
            ((ViewGroup) islandView.getParent()).setClipToPadding(false);
        }

        ValueAnimator widthAnim = ValueAnimator.ofInt(params.width, targetWidth);
        ValueAnimator heightAnim = ValueAnimator.ofInt(params.height, targetHeight);
        ValueAnimator marginAnim = ValueAnimator.ofInt(params.topMargin, targetMargin);

        ObjectAnimator textFade = ObjectAnimator.ofFloat(statusText, "alpha", 0f, 1f);
        textFade.setDuration(300);
        textFade.setStartDelay(200);

        widthAnim.addUpdateListener(animation -> {
            params.width = (int) animation.getAnimatedValue();
            params.height = (int) heightAnim.getAnimatedValue();
            params.topMargin = (int) marginAnim.getAnimatedValue();
            islandView.setLayoutParams(params);

            // 🔴 Smooth corner radius transition (Max 50f for a modern card look)
            float radius = Math.min(params.height / 2f, 50f);
            applyGlassLook(islandView, radius);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                islandView.setElevation(40f);
                islandView.setOutlineSpotShadowColor(Color.parseColor("#66000000"));
                islandView.setOutlineAmbientShadowColor(Color.parseColor("#99000000"));
            }
        });

        AnimatorSet set = new AnimatorSet();
        set.playTogether(widthAnim, heightAnim, marginAnim, textFade);
        set.setDuration(450);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    private void animateIslandCollapse() {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) islandView.getLayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            islandView.setElevation(0f);
        }

        ValueAnimator widthAnim = ValueAnimator.ofInt(params.width, 180);
        ValueAnimator heightAnim = ValueAnimator.ofInt(params.height, 100);
        ValueAnimator marginAnim = ValueAnimator.ofInt(params.topMargin, originalMargin);

        widthAnim.addUpdateListener(animation -> {
            params.width = (int) animation.getAnimatedValue();
            params.height = (int) heightAnim.getAnimatedValue();
            params.topMargin = (int) marginAnim.getAnimatedValue();
            islandView.setLayoutParams(params);

            // 🔴 Only apply glass look. (Removed the line that set the old shape back)
            applyGlassLook(islandView, params.height / 2f);
        });

        AnimatorSet collapseSet = new AnimatorSet();
        collapseSet.playTogether(widthAnim, heightAnim, marginAnim);
        collapseSet.setDuration(400);
        collapseSet.setInterpolator(new DecelerateInterpolator());
        collapseSet.start();
    }

    // Call this if you ever need to completely remove the overlay
    public void destroy() {
        if (container != null && windowManager != null) {
            windowManager.removeView(container);
            container = null;
        }
    }

    // Dynamic Island class-la ithai add pannunga
    public void showCustomMessage(String msg) {
        if (islandView == null || statusText == null) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            smartExpandIsland(msg); // Expand aagi message kaatum

            // 4 seconds kalichu auto-va collapse aagidum
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateState("IDLE");
                }
            },5000);
        });
    }

    private void applyGlassLook(FrameLayout view, float radius) {
        // 1. Bottom Layer: The "Edge Reflection" (Light Highlight)
        GradientDrawable edgeReflection = new GradientDrawable();
        edgeReflection.setShape(GradientDrawable.RECTANGLE);
        edgeReflection.setCornerRadius(radius);
        edgeReflection.setColor(Color.parseColor("#66FFFFFF")); // 40% White (The reflection)

        // 2. Top Layer: The Transparent Glass Core
        GradientDrawable glassCore = new GradientDrawable();
        glassCore.setShape(GradientDrawable.RECTANGLE);
        glassCore.setCornerRadius(radius);
        // Dark glass body - 40% opacity for better text visibility
        glassCore.setColor(Color.parseColor("#660A0F18"));

        // 3. Combine Layers
        Drawable[] layers = {edgeReflection, glassCore};
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        // 🔴 THE MAGIC: Inset the top layer by 1.5dp
        layerDrawable.setLayerInset(1, 2, 2, 2, 2); // left, top, right, bottom in pixels

        view.setBackground(layerDrawable);

        // 4. Glow Shadow (Optional but looks mass)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.setElevation(35f);
            view.setOutlineSpotShadowColor(Color.parseColor("#4D00F5FF"));
        }
    }
}