package com.otaliastudios.cameraview.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.otaliastudios.cameraview.CameraUtils;

import java.lang.ref.WeakReference;


public class PicturePreviewActivity extends Activity {

    private static WeakReference<byte[]> image;

    public static void setImage(@Nullable byte[] im) {
        image = im != null ? new WeakReference<>(im) : null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_preview);
        final ImageView imageView = findViewById(R.id.image);
        final MessageView typeMessageView = findViewById(R.id.type);
        final MessageView resultMessageView = findViewById(R.id.result);
        final MessageView metaMessageView = findViewById(R.id.meta);
        final String type = getIntent().getStringExtra("type");
        final String result = getIntent().getStringExtra("result");
        final String meta = getIntent().getStringExtra("meta");
        byte[] b = image == null ? null : image.get();
        if (b == null) {
            finish();
            return;
        }

        CameraUtils.decodeBitmap(b, 1000, 1000, new CameraUtils.BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);

                // approxUncompressedSize.setTitle("Approx. uncompressed size");
                // approxUncompressedSize.setMessage(getApproximateFileMegabytes(bitmap) + "MB");

                typeMessageView.setTitle("고지서 or 지폐");
                if (type.equals("NOTE")) {
                    typeMessageView.setMessage("지폐");
                }
                else{
                    if (result.equals("")){
                        typeMessageView.setMessage("지폐");
                    }
                    else {
                        typeMessageView.setMessage("고지서");
                        resultMessageView.setTitle("고지서 종류");
                        resultMessageView.setMessage(result);
                    }
                }
                metaMessageView.setTitle("결과 값");
                metaMessageView.setMessage(meta);

                // AspectRatio finalRatio = AspectRatio.of(bitmap.getWidth(), bitmap.getHeight());
                // actualResolution.setTitle("Actual resolution");
                // actualResolution.setMessage(bitmap.getWidth() + "x" + bitmap.getHeight() + " (" + finalRatio + ")");
            }
        });

    }

    private static float getApproximateFileMegabytes(Bitmap bitmap) {
        return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024 / 1024;
    }

}
