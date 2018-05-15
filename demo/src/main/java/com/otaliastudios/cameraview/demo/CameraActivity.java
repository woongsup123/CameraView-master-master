package com.otaliastudios.cameraview.demo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Size;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class CameraActivity extends AppCompatActivity implements View.OnClickListener, ControlView.Callback {

    private CameraView camera;
    private ViewGroup controlPanel;

    private boolean mCapturingPicture;

    // To show stuff in the callback
    private Size mCaptureNativeSize;
    private long mCaptureTime;

    private final int THICKNESS = 30;

    private final int frameX = 280;
    private final int frameY = 200;
    private final int cropX = frameX+THICKNESS;
    private final int cropY = frameY-THICKNESS;
    private static int frameWidth = 0;
    private static int frameHeight = 0;
    private static int rootWidth = 0;
    private static int rootHeight = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_camera);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        camera = findViewById(R.id.camera);
        camera.addCameraListener(new CameraListener() {
            public void onCameraOpened(CameraOptions options) { onOpened(); }
            public void onPictureTaken(byte[] jpeg) {
                savePicture(jpeg, "original");
                byte[] croppedImg = cropPicture(jpeg,
                                                    cropX*1.0 / rootWidth,
                                                    cropY*1.0 / rootHeight,
                                                    (rootWidth-cropX*2.0) / rootWidth,
                                                    (rootHeight-cropY*2.0) / rootHeight);
                savePicture(croppedImg, "cropped");
                onPicture(jpeg);
            }
        });

        findViewById(R.id.capturePhoto).setOnClickListener(this);

        controlPanel = findViewById(R.id.controls);
        ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
        Control[] controls = Control.values();
        for (Control control : controls) {
            ControlView view = new ControlView(this, control, this);
            group.addView(view, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        controlPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
                b.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
    }

    private void createOverlay(int frameX, int frameY) {
        ImageView overlayTop = findViewById(R.id.top);
        ImageView overlayLeft = findViewById(R.id.left);
        ImageView overlayRight = findViewById(R.id.right);

        CoordinatorLayout root = findViewById(R.id.root);
        int width = root.getWidth();
        int height = root.getHeight();

        rootWidth = width;
        rootHeight = height;

        frameWidth = width - frameX*2;
        frameHeight = height - frameY*2;

        CoordinatorLayout.LayoutParams topLP = new CoordinatorLayout.LayoutParams(frameWidth, THICKNESS);
        topLP.setMargins(frameX, frameY-THICKNESS, 0, 0);
        overlayTop.setLayoutParams(topLP);

        CoordinatorLayout.LayoutParams leftLP = new CoordinatorLayout.LayoutParams(THICKNESS, frameHeight+THICKNESS);
        leftLP.setMargins(frameX-THICKNESS, frameY-THICKNESS, 0, 0);
        overlayLeft.setLayoutParams(leftLP);

        CoordinatorLayout.LayoutParams rightLP = new CoordinatorLayout.LayoutParams(THICKNESS, frameHeight+THICKNESS);
        rightLP.setMargins(frameX + frameWidth, frameY-THICKNESS, 0, 0);
        overlayRight.setLayoutParams(rightLP);
    }

    private void message(String content, boolean important) {
        int length = important ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(this, content, length).show();
    }

    private void onOpened() {
        ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
        for (int i = 0; i < group.getChildCount(); i++) {
            ControlView view = (ControlView) group.getChildAt(i);
            view.onCameraOpened(camera);
        }
        createOverlay(frameX, frameY);
    }

    private void onPicture(byte[] jpeg) {
        mCapturingPicture = false;

        PicturePreviewActivity.setImage(jpeg);
        Intent intent = new Intent(CameraActivity.this, PicturePreviewActivity.class);
        intent.putExtra("delay", mCaptureTime);
        intent.putExtra("nativeWidth", mCaptureNativeSize.getWidth());
        intent.putExtra("nativeHeight", mCaptureNativeSize.getHeight());
        startActivity(intent);

        mCaptureTime = 0;
        mCaptureNativeSize = null;
    }

    private void savePicture(byte[] jpeg, String filename) {
        File storeDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "hana");

        File file = createFile(storeDir, filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jpeg);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createFile(File storeDir, String filename) {
        if (!storeDir.exists()) {
            storeDir.mkdirs();
        }
        long time = System.currentTimeMillis();
        File file = new File(storeDir.getPath() + File.separator + filename + "_" + Long.toString(time) + ".jpg");
        return file;
    }

    private byte[] cropPicture (byte[] jpeg, double startXRatio, double startYRatio, double widthRatio, double heightRatio){
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        //Matrix matrix = new Matrix();
        //matrix.postRotate(90);
        //bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        int bmWidth = bitmap.getWidth();
        int bmHeight = bitmap.getHeight();

        int bmStartX = (int) (bmWidth * startYRatio);
        int bmStartY = (int) (bmHeight * startXRatio);

        int bmCropWidth = (int) (bmWidth * heightRatio);
        int bmCropHeight = (int) (bmHeight * widthRatio);

        Bitmap croppedImg = Bitmap.createBitmap(bitmap, bmStartX, bmStartY, bmCropWidth, bmCropHeight);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        croppedImg.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] byteImg = baos.toByteArray();
        croppedImg.recycle();
        return byteImg;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.capturePhoto: capturePhoto(); break;
        }
    }

    @Override
    public void onBackPressed() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            b.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }
        super.onBackPressed();
    }

    private void capturePhoto() {
        if (mCapturingPicture) return;
        mCapturingPicture = true;
        mCaptureTime = System.currentTimeMillis();
        mCaptureNativeSize = camera.getPictureSize();
        message("Capturing picture...", false);
        camera.capturePicture();
    }


    @Override
    public boolean onValueChanged(Control control, Object value, String name) {
        if (!camera.isHardwareAccelerated() && (control == Control.WIDTH || control == Control.HEIGHT)) {
            if ((Integer) value > 0) {
                message("This device does not support hardware acceleration. " +
                        "In this case you can not change width or height. " +
                        "The view will act as WRAP_CONTENT by default.", true);
                return false;
            }
        }
        control.applyValue(camera, value);
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_HIDDEN);
        message("Changed " + control.getName() + " to " + name, false);
        return true;
    }

    //region Boilerplate

    @Override
    protected void onResume() {
        super.onResume();
        camera.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera.destroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !camera.isStarted()) {
            camera.start();
        }
    }

    //endregion
}
