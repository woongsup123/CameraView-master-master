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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener, ControlView.Callback {

    private CameraView camera;
    private ViewGroup controlPanel;

    private boolean mCapturingPicture;

    // To show stuff in the callback
    private Size mCaptureNativeSize;
    private long mCaptureTime;

    private final int THICKNESS = 30;

    private final double frameX = 0.25;
    private final double frameY = 0.1;

    private final int reducedWidth = 1920;
    //private final int cropX = frameX+THICKNESS;
    //private final int cropY = frameY-THICKNESS;
    private static int frameWidth = 0;
    private static int frameHeight = 0;
    private static int rootWidth = 0;
    private static int rootHeight = 0;

    private final String IPADDR = "10.122.66.152";
    private final String DIR = "/api/pilot/upload/";
    private final int PORT = 8000;
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

                //savePicture(jpeg, "original");
                //byte[] croppedImg = cropPicture(jpeg,
                //                                    frameX,
                //                                    frameY,
                //                                    rootWidth*(1-frameX*2) / rootWidth,
                //                                    rootHeight*(1-frameY*2) / rootHeight);

                final byte[] resizedImg = resizePicture(jpeg);
                savePicture(resizedImg, "resized");
                Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendPicture(resizedImg);
                    }
                });
                th.start();
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

    private void sendPicture(byte[] jpeg) {
        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("http://"+IPADDR+":"+Integer.toString(PORT)+DIR);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .setCharset(Charset.forName("UTF-8"))
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        ContentBody cb = new ByteArrayBody(jpeg, "pic.jpg");
        builder.addPart("content", cb);

        builder.addTextBody("purpose", "idr");
        builder.addTextBody("frameX", Double.toString(frameY));
        builder.addTextBody("frameY", Double.toString(frameX));

        try {
            httpPost.setEntity(builder.build());
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            if (httpEntity != null){
                //InputStream is = httpEntity.getContent();
                String content = EntityUtils.toString(httpEntity);
                String str = content;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] resizePicture(byte[] jpeg) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        int bmWidth = bitmap.getWidth();
        int bmHeight = bitmap.getHeight();
        float reduceRatio = (float)reducedWidth / (float)bmWidth;
        int reducedHeight = (int) (bmHeight*reduceRatio);
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, reducedWidth, reducedHeight, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] byteImg = baos.toByteArray();
        newBitmap.recycle();
        return byteImg;
    }

    private void createOverlay(double frameX, double frameY) {
        ImageView overlayTop = findViewById(R.id.top);
        ImageView overlayLeft = findViewById(R.id.left);
        ImageView overlayRight = findViewById(R.id.right);

        CoordinatorLayout root = findViewById(R.id.root);
        int width = root.getWidth();
        int height = root.getHeight();

        rootWidth = width;
        rootHeight = height;

        frameWidth = (int)(width * (1.0 - frameX*2));
        frameHeight = (int)(height * (1.0 - frameY*2));

        CoordinatorLayout.LayoutParams topLP = new CoordinatorLayout.LayoutParams(frameWidth, THICKNESS);
        topLP.setMargins((int)(width*frameX), (int)(height*frameY-THICKNESS), 0, 0);
        overlayTop.setLayoutParams(topLP);

        CoordinatorLayout.LayoutParams leftLP = new CoordinatorLayout.LayoutParams(THICKNESS, frameHeight+THICKNESS);
        leftLP.setMargins((int)(width*frameX-THICKNESS), (int)(height*frameY-THICKNESS), 0, 0);
        overlayLeft.setLayoutParams(leftLP);

        CoordinatorLayout.LayoutParams rightLP = new CoordinatorLayout.LayoutParams(THICKNESS, frameHeight+THICKNESS);
        rightLP.setMargins((int)(width*frameX) + frameWidth, (int)(height*frameY-THICKNESS), 0, 0);
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

        intent.putExtra("type", "OCR 일반");
        intent.putExtra("result", "<5422751+ +00024500109998180115+ +38001< <11<");
        //intent.putExtra("nativeHeight", mCaptureNativeSize.getHeight());
        startActivity(intent);
        //AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setTitle("인식결과: OCR 일반").setMessage("<5422751+ +00024500109998180115+ +38001< <11<").show();
        //mCaptureTime = 0;
        //mCaptureNativeSize = null;
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
