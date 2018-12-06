package in.omerjerk.libscreenshotter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.text.BoringLayout;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Created by omerjerk on 17/2/16.
 */
public class Screenshotter implements ImageReader.OnImageAvailableListener {

    private static final String TAG = "LibScreenshotter";

    private VirtualDisplay virtualDisplay;

    private int width;
    private int height;

    private Context context;

    private int resultCode;
    private Intent data;
    private ScreenshotCallback cb;

    private static Screenshotter mInstance;

    private int count = 0;
    private ImageReader mImageReader;
    private MediaProjection mMediaProjection;
    private volatile int imageAvailable = 0;

    /**
     * Get the single instance of the Screenshotter class.
     * @return the instance
     */
    public static Screenshotter getInstance() {
        if (mInstance == null) {
            mInstance = new Screenshotter();
        }
        return mInstance;
    }

    private Screenshotter() {}

    /**
     * Takes the screenshot of whatever currently is on the default display.
     * @param resultCode The result code returned by the request for accessing MediaProjection permission
     * @param data The intent returned by the same request
     */
    public Screenshotter takeScreenshot(Context context, int resultCode, Intent data, final ScreenshotCallback cb) {
        this.context = context;
        this.cb = cb;
        this.resultCode = resultCode;
        this.data = data;

        imageAvailable = 0;
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.RGB_565, 2);
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (mMediaProjection == null) {
            mMediaProjection = mediaProjectionManager.getMediaProjection(this.resultCode, this.data);
            if (mMediaProjection == null) {
                Log.e(TAG, "MediaProjection null. Cannot take the screenshot.");
            }
        }
        try {
            virtualDisplay = mMediaProjection.createVirtualDisplay("Screenshotter",
                    width, height, 50,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), null, null);
            mImageReader.setOnImageAvailableListener(Screenshotter.this, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    /**
     * Set the size of the screenshot to be taken
     * @param width width of the requested bitmap
     * @param height height of the request bitmap
     * @return the singleton instance
     */
    public Screenshotter setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Log.i(TAG, "in OnImageAvailable");
        FileOutputStream fos = null;
        Bitmap bitmap = null;
        Image img = null;
        try {
            img = reader.acquireLatestImage();
            if (img != null) {
                Image.Plane[] planes = img.getPlanes();
                if (planes[0].getBuffer() == null) {
                    return;
                }
                int width = img.getWidth();
                int height = img.getHeight();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * width;

                int offset = 0;
                DisplayMetrics metrics = new DisplayMetrics();
                bitmap = Bitmap.createBitmap(metrics,width, height, Bitmap.Config.ARGB_8888);
                ByteBuffer buffer = planes[0].getBuffer();
                for (int i = 0; i < height; ++i) {
                    for (int j = 0; j < width; ++j) {
                        int pixel = 0;
                        pixel |= (buffer.get(offset) & 0xff) << 16;     // R
                        pixel |= (buffer.get(offset + 1) & 0xff) << 8;  // G
                        pixel |= (buffer.get(offset + 2) & 0xff);       // B
                        pixel |= (buffer.get(offset + 3) & 0xff) << 24; // A
                        bitmap.setPixel(j, i, pixel);
                        offset += pixelStride;
                    }
                    offset += rowPadding;
                }

                String name = "/myscreen" + count + ".png";
                count++;
                File file = new File(Environment.getExternalStorageDirectory(), name);
                fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                Log.i(TAG, "image saved in" + Environment.getExternalStorageDirectory() + name);
                img.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != bitmap) {
                bitmap.recycle();
            }
            if (null != img) {
                img.close();
            }

        }

        cb.onScreenshot(bitmap);
    }

    private void tearDown() {
        virtualDisplay.release();
        mMediaProjection.stop();
        mMediaProjection = null;
        mImageReader = null;
    }
}
