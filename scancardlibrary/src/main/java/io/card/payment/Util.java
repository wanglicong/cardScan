package io.card.payment;

/* Util.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Debug;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * This class has various static utility methods.
 */

class Util {
    private static final String TAG = Util.class.getSimpleName();
    private static final boolean TORCH_BLACK_LISTED = (Build.MODEL.equals("DROID2"));

    public static final String PUBLIC_LOG_TAG = "card.io";

    private static Boolean sHardwareSupported;

    public static boolean deviceSupportsTorch(Context context) {
        return !TORCH_BLACK_LISTED
                && context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @SuppressWarnings("rawtypes")
    public static String manifestHasConfigChange(ResolveInfo resolveInfo, Class activityClass) {
        String error = null;
        if (resolveInfo == null) {
            error = String.format("Didn't find %s in the AndroidManifest.xml",
                    activityClass.getName());
        } else if (!Util.hasConfigFlag(resolveInfo.activityInfo.configChanges,
                ActivityInfo.CONFIG_ORIENTATION)) {
            error = activityClass.getName()
                    + " requires attribute android:configChanges=\"orientation\"";
        }
        if (error != null) {
            Log.e(Util.PUBLIC_LOG_TAG, error);
        }
        return error;
    }

    public static boolean hasConfigFlag(int config, int configFlag) {
        return ((config & configFlag) == configFlag);
    }

    /* --- HARDWARE SUPPORT --- */

    public static boolean hardwareSupported() {
        if (sHardwareSupported == null) {
            sHardwareSupported = hardwareSupportCheck();
        }
        return sHardwareSupported;
    }

    private static boolean hardwareSupportCheck() {
        if (!CardScanner.processorSupported()) {
            Log.w(PUBLIC_LOG_TAG, "- Processor type is not supported");
            return false;
        }

        // Camera needs to open
        Camera c = null;
        try {
            c = Camera.open();
        } catch (RuntimeException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return true;
            } else {
                Log.w(PUBLIC_LOG_TAG, "- Error opening camera: " + e);
                throw new CameraUnavailableException();
            }
        }
        if (c == null) {
            Log.w(PUBLIC_LOG_TAG, "- No camera found");
            return false;
        } else {
            List<Camera.Size> list = c.getParameters().getSupportedPreviewSizes();
            c.release();

            boolean supportsVGA = false;

            for (Camera.Size s : list) {
                if (s.width == 640 && s.height == 480) {
                    supportsVGA = true;
                    break;
                }
            }

            if (!supportsVGA) {
                Log.w(PUBLIC_LOG_TAG, "- Camera resolution is insufficient");
                return false;
            }
        }
        return true;
    }

    public static String getNativeMemoryStats() {
        return "(free/alloc'd/total)" + Debug.getNativeHeapFreeSize() + "/"
                + Debug.getNativeHeapAllocatedSize() + "/" + Debug.getNativeHeapSize();
    }

    public static void logNativeMemoryStats() {
        Log.d("MEMORY", "Native memory stats: " + getNativeMemoryStats());
    }

    static public Rect rectGivenCenter(Point center, int width, int height) {
        return new Rect(center.x - width / 2, center.y - height / 2, center.x + width / 2, center.y
                + height / 2);
    }

    static public void setupTextPaintStyle(Paint paint) {
        paint.setColor(Color.WHITE);
        //paint.setStyle(Paint.Style.FILL);
        //paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setAntiAlias(true);
        float[] black = {0f, 0f, 0f};
        paint.setShadowLayer(1.5f, 0.5f, 0f, Color.HSVToColor(200, black));
    }

    /**
     * Writes {@link CardIOActivity#EXTRA_CAPTURED_CARD_IMAGE} to dataIntent if
     * origIntent has {@link CardIOActivity#EXTRA_RETURN_CARD_IMAGE}.
     * <p>
     * 将图片 压缩成字节数组
     *
     * @param origIntent
     * @param dataIntent
     * @param mOverlay
     */
    static void writeCapturedCardImageIfNecessary(Intent origIntent, Intent dataIntent, OverlayView mOverlay) {
        if (origIntent.getBooleanExtra(CardIOActivity.EXTRA_RETURN_CARD_IMAGE, false) && mOverlay != null && mOverlay.getBitmap() != null) {
            ByteArrayOutputStream scaledCardBytes = new ByteArrayOutputStream();
            mOverlay.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, scaledCardBytes);
            dataIntent.putExtra(CardIOActivity.EXTRA_CAPTURED_CARD_IMAGE, scaledCardBytes.toByteArray());
        }

    }






    /**
     * 获取字节数组的角度
     */
    public static int getOrientation(byte[] jpeg) {
        if (jpeg == null) {
            return 0;
        }

        int offset = 0;
        int length = 0;

        // ISO/IEC 10918-1:1993(E)
        while (offset + 3 < jpeg.length && (jpeg[offset++] & 0xFF) == 0xFF) {
            int marker = jpeg[offset] & 0xFF;

            // Check if the marker is a padding.
            if (marker == 0xFF) {
                continue;
            }
            offset++;

            // Check if the marker is SOI or TEM.
            if (marker == 0xD8 || marker == 0x01) {
                continue;
            }
            // Check if the marker is EOI or SOS.
            if (marker == 0xD9 || marker == 0xDA) {
                break;
            }

            // Get the length and check if it is reasonable.
            length = pack(jpeg, offset, 2, false);
            if (length < 2 || offset + length > jpeg.length) {
                Log.e(TAG, "Invalid length");
                return 0;
            }

            // Break if the marker is EXIF in APP1.
            if (marker == 0xE1 && length >= 8
                    && pack(jpeg, offset + 2, 4, false) == 0x45786966
                    && pack(jpeg, offset + 6, 2, false) == 0) {
                offset += 8;
                length -= 8;
                break;
            }

            // Skip other markers.
            offset += length;
            length = 0;
        }

        // JEITA CP-3451 Exif Version 2.2
        if (length > 8) {
            // Identify the byte order.
            int tag = pack(jpeg, offset, 4, false);
            if (tag != 0x49492A00 && tag != 0x4D4D002A) {
                Log.e(TAG, "Invalid byte order");
                return 0;
            }
            boolean littleEndian = (tag == 0x49492A00);

            // Get the offset and check if it is reasonable.
            int count = pack(jpeg, offset + 4, 4, littleEndian) + 2;
            if (count < 10 || count > length) {
                Log.e(TAG, "Invalid offset");
                return 0;
            }
            offset += count;
            length -= count;

            // Get the count and go through all the elements.
            count = pack(jpeg, offset - 2, 2, littleEndian);
            while (count-- > 0 && length >= 12) {
                // Get the tag and check if it is orientation.
                tag = pack(jpeg, offset, 2, littleEndian);
                if (tag == 0x0112) {
                    // We do not really care about type and count, do we?
                    int orientation = pack(jpeg, offset + 8, 2, littleEndian);
                    switch (orientation) {
                        case 1:
                            return 0;
                        case 3:
                            return 180;
                        case 6:
                            return 90;
                        case 8:
                            return 270;
                        default:
                            return 0;
                    }
                }
                offset += 12;
                length -= 12;
            }
        }

        Log.i(TAG, "Orientation not found");
        return 0;
    }

    private static int pack(byte[] bytes, int offset, int length, boolean littleEndian) {
        int step = 1;
        if (littleEndian) {
            offset += length - 1;
            step = -1;
        }

        int value = 0;
        while (length-- > 0) {
            value = (value << 8) | (bytes[offset] & 0xFF);
            offset += step;
        }
        return value;
    }



    /**
     * 获取原始图片的角度（解决三星手机拍照后图片是横着的问题）
     * @param path 图片的绝对路径
     * @return 原始图片的角度
     */
    public static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            Log.e("jxf", "orientation" + orientation);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }


}
