package io.card.payment;

/* CardIOActivity.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.riso.scancardlibrary.BuildConfig;
import com.riso.scancardlibrary.R;
import com.riso.scancardlibrary.cutviews.views.CropView;
import com.riso.scancardlibrary.cutviews.views.FrameOverlayView;
import com.riso.scancardlibrary.cutviews.views.MaskView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Date;

import io.card.payment.i18n.LocalizedStrings;
import io.card.payment.i18n.StringKey;
import io.card.payment.ui.ActivityHelper;

/**
 * import com.riso.scancardlibrary.payment.i18n.StringKey;
 * import com.riso.scancardlibrary.payment.i18n.SupportedLocale;
 * This is the entry point {@link Activity} for a card.io client to use <a
 * href="https://card.io">card.io</a>.
 *
 * @author 王黎聪
 * @version 1.0
 */
public class CardIOActivity extends Activity implements View.OnClickListener {
    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. If set, the card will not be scanned
     * with the camera.
     * 扫不扫描 卡片
     */
    public static final String EXTRA_NO_CAMERA = "io.card.payment.noCamera";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. If
     * set to <code>false</code>, expiry information will not be required.
     * 是否 显示银行卡 过期时间   默认false
     */
    public static final String EXTRA_REQUIRE_EXPIRY = "io.card.payment.requireExpiry";

    /**
     * Boolean extra. Optional. Defaults to <code>true</code>. If
     * set to <code>true</code>, and {@link #EXTRA_REQUIRE_EXPIRY} is <code>true</code>,
     * an attempt to extract the expiry from the card image will be made.
     * 是否 提取 过期时间  默认true
     */
    public static final String EXTRA_SCAN_EXPIRY = "io.card.payment.scanExpiry";

    /**
     * Integer extra. Optional. Defaults to <code>-1</code> (no blur). Privacy feature.
     * How many of the Card number digits NOT to blur on the resulting image.
     * Setting it to <code>4</code> will blur all digits except the last four.
     * 是否 设置卡号 模糊  默认 4位   默认 false
     */
    public static final String EXTRA_UNBLUR_DIGITS = "io.card.payment.unblurDigits";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. If set, the user will be prompted
     * for the card CVV.
     * 是否 提示用户 CVV 默认 false
     */
    public static final String EXTRA_REQUIRE_CVV = "io.card.payment.requireCVV";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. If set, the user will be prompted
     * for the card billing postal code.
     * 是否 信用卡帐单的邮政编码  默认false
     */
    public static final String EXTRA_REQUIRE_POSTAL_CODE = "io.card.payment.requirePostalCode";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. If set, the postal code will only collect numeric
     * input. Set this if you know the <a href="https://en.wikipedia.org/wiki/Postal_code">expected country's
     * postal code</a> has only numeric postal codes.
     * 是否 信用卡帐单的邮政编码  默认false
     */
    public static final String EXTRA_RESTRICT_POSTAL_CODE_TO_NUMERIC_ONLY = "io.card.payment.restrictPostalCodeToNumericOnly";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. If set, the user will be prompted
     * for the cardholder name.
     * 是否 将提示用户 持卡人的名字。  默认false
     */
    public static final String EXTRA_REQUIRE_CARDHOLDER_NAME = "io.card.payment.requireCardholderName";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. If set, the card.io logo will be
     * shown instead of the PayPal logo.
     * 是否显示 用户 标志  默认 false
     */
    public static final String EXTRA_USE_CARDIO_LOGO = "io.card.payment.useCardIOLogo";

    /**
     * Parcelable extra containing {@link CreditCard}. The data intent returned to your {@link Activity}'s
     * {@link Activity#onActivityResult(int, int, Intent)} will contain this extra if the resultCode is
     * {@link #RESULT_CARD_INFO}.
     */
    public static final String EXTRA_SCAN_RESULT = "io.card.payment.scanResult";

    /**
     * Boolean extra indicating card was not scanned.
     */
    private static final String EXTRA_MANUAL_ENTRY_RESULT = "io.card.payment.manualEntryScanResult";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. Removes the keyboard button from the
     * scan screen.
     * <br><br>
     * If scanning is unavailable, the {@link Activity} result will be {@link #RESULT_SCAN_NOT_AVAILABLE}.
     * 是否 从键盘上删除键盘按钮 扫描屏幕。   默认 false
     */
    public static final String EXTRA_SUPPRESS_MANUAL_ENTRY = "io.card.payment.suppressManual";

    /**
     * String extra. Optional. The preferred language for all strings appearing in the user
     * interface. If not set, or if set to null, defaults to the device's current language setting.
     * <br><br>
     * Can be specified as a language code ("en", "fr", "zh-Hans", etc.) or as a locale ("en_AU",
     * "fr_FR", "zh-Hant_TW", etc.).
     * <br><br>
     * If the library does not contain localized strings for a specified locale, then will fall back
     * to the language. E.g., "es_CO" -&gt; "es".
     * <br><br>
     * If the library does not contain localized strings for a specified language, then will fall
     * back to American English.
     * <br><br>
     * If you specify only a language code, and that code matches the device's currently preferred
     * language, then the library will attempt to use the device's current region as well. E.g.,
     * specifying "en" on a device set to "English" and "United Kingdom" will result in "en_GB".
     * <br><br>
     * These localizations are currently included:
     * <br><br>
     * ar, da, de, en, en_AU, en_GB, es, es_MX, fr, he, is, it, ja, ko, ms, nb, nl, pl, pt, pt_BR, ru,
     * sv, th, tr, zh-Hans, zh-Hant, zh-Hant_TW.
     */
    public static final String EXTRA_LANGUAGE_OR_LOCALE = "io.card.payment.languageOrLocale";

    /**
     * Integer extra. Optional. Defaults to {@link Color#GREEN}. Changes the color of the guide overlay on the
     * camera.
     * 控制相机的颜色
     */
    public static final String EXTRA_GUIDE_COLOR = "io.card.payment.guideColor";

    /**
     * Boolean extra. Optional. If this value is set to <code>true</code> the user will not be prompted to
     * confirm their card number after processing.
     * 卡号相关
     */
    public static final String EXTRA_SUPPRESS_CONFIRMATION = "io.card.payment.suppressConfirmation";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. When set to <code>true</code> the card.io logo
     * will not be shown overlaid on the camera.
     * 是否隐藏标志
     */
    public static final String EXTRA_HIDE_CARDIO_LOGO = "io.card.payment.hideLogo";

    /**
     * String extra. Optional. Used to display instructions to the user while they are scanning
     * their card.
     * 额外的字符串。可选的。用于在扫描时向用户显示指令
     * 他们的卡片。
     */
    public static final String EXTRA_SCAN_INSTRUCTIONS = "io.card.payment.scanInstructions";

    /**
     * Boolean extra. Optional. Once a card image has been captured but before it has been
     * processed, this value will determine whether to continue processing as usual. If the value is
     * <code>true</code> the {@link CardIOActivity} will finish with a {@link #RESULT_SCAN_SUPPRESSED} result code.
     * 一旦一张卡片图像被捕捉了，但在它之前
     * 处理后，该值将决定是否继续按常规进行处理。如果该值为
     */
    public static final String EXTRA_SUPPRESS_SCAN = "io.card.payment.suppressScan";

    /**
     * 返回照片的质量
     */
    public static final String EXTRA_CRAD_QUALITY = "io.card.quality";

    /**
     * String extra. If {@link #EXTRA_RETURN_CARD_IMAGE} is set to <code>true</code>, the data intent passed to your
     * {@link Activity} will have the card image stored as a JPEG formatted byte array in this extra.
     * 额外的字符串。如果将@link extrareturncardimage设置为true/code，则将数据传递给您
     * { @link android.app。活动将把卡片图像存储为一个JPEG格式的字节数组。
     */
    public static final String EXTRA_CAPTURED_CARD_IMAGE = "io.card.payment.capturedCardImage";

    /**
     * Boolean extra. Optional. If this value is set to <code>true</code> the card image will be passed as an
     * extra in the data intent that is returned to your {@link Activity} using the
     * {@link #EXTRA_CAPTURED_CARD_IMAGE} key.
     */
    public static final String EXTRA_RETURN_CARD_IMAGE = "io.card.payment.returnCardImage";

    /**
     * Integer extra. Optional. If this value is provided the view will be inflated and will overlay
     * the camera during the scan process. The integer value must be the id of a valid layout
     * resource.
     * <p>
     * 设置自定义布局
     */
    public static final String EXTRA_SCAN_OVERLAY_LAYOUT_ID = "io.card.payment.scanOverlayLayoutId";

    /**
     * Boolean extra. Optional. Use the PayPal icon in the ActionBar.
     */
    public static final String EXTRA_USE_PAYPAL_ACTIONBAR_ICON =
            "io.card.payment.intentSenderIsPayPal";

    /**
     * Boolean extra. Optional. If this value is set to <code>true</code>, and the application has a theme,
     * the theme for the card.io {@link Activity}s will be set to the theme of the application.
     */
    public static final String EXTRA_KEEP_APPLICATION_THEME = "io.card.payment.keepApplicationTheme";


    /**
     * Boolean extra. Used for testing only.   仅用于测试。
     */
    static final String PRIVATE_EXTRA_CAMERA_BYPASS_TEST_MODE = "io.card.payment.cameraBypassTestMode";

    private static int lastResult = 0xca8d10; // arbitrary. chosen to be well above
    // Activity.RESULT_FIRST_USER.
    /**
     * result code supplied to {@link Activity#onActivityResult(int, int, Intent)} when a scan request completes.
     */
    public static final int RESULT_CARD_INFO = lastResult++;

    /**
     * result code supplied to {@link Activity#onActivityResult(int, int, Intent)} when the user presses the cancel
     * button.
     */
    public static final int RESULT_ENTRY_CANCELED = lastResult++;

    /**
     * result code indicating that scan is not available. Only returned when
     * {@link #EXTRA_SUPPRESS_MANUAL_ENTRY} is set and scanning is not available.
     * <br><br>
     * This error can be avoided in normal situations by checking
     * {@link #canReadCardWithCamera()}.
     */
    public static final int RESULT_SCAN_NOT_AVAILABLE = lastResult++;

    /**
     * result code indicating that we only captured the card image.
     */
    public static final int RESULT_SCAN_SUPPRESSED = lastResult++;

    /**
     * result code indicating that confirmation was suppressed.
     */
    public static final int RESULT_CONFIRMATION_SUPPRESSED = lastResult++;

    private static final String TAG = CardIOActivity.class.getSimpleName();

    private static final int DEGREE_DELTA = 15;

    private static final int ORIENTATION_PORTRAIT = 1;
    private static final int ORIENTATION_PORTRAIT_UPSIDE_DOWN = 2;
    private static final int ORIENTATION_LANDSCAPE_RIGHT = 3;
    private static final int ORIENTATION_LANDSCAPE_LEFT = 4;

    private static final int FRAME_ID = 1;
    private static final int UIBAR_ID = 2;
    private static final int KEY_BTN_ID = 3;

    private static final String BUNDLE_WAITING_FOR_PERMISSION = "io.card.payment.waitingForPermission";

    private static final float UIBAR_VERTICAL_MARGIN_DP = 15.0f;

    private static final long[] VIBRATE_PATTERN = {0, 70, 10, 40};

    private static final int TOAST_OFFSET_Y = -75;

    private static final int DATA_ENTRY_REQUEST_ID = 10;
    private static final int PERMISSION_REQUEST_ID = 11;

    private static final int PERMISSIONS_EXTERNAL_STORAGE = 801;

    /**
     * 展示 闪光灯 和文案
     */
    private OverlayView mOverlay;
    //横屏监听
    //private OrientationEventListener orientationListener;

    // （预览）由扫描仪来访问。不是最好的做法。
    Preview mPreview;

    private CreditCard mDetectedCard;
    private Rect mGuideFrame;
    private int mLastDegrees;
    private int mFrameOrientation;
    private boolean suppressManualEntry;
    private boolean mDetectOnly;
    private LinearLayout customOverlayLayout;
    private boolean waitingForPermission;

    private RelativeLayout mUIBar;
    private boolean useApplicationTheme;

    private CardScanner mCardScanner;

    private boolean manualEntryFallbackOrForced = false;


    private int quality = 100;


    /**
     * Static variable for the decorated card image. This is ugly, but works. Parceling and
     * unparceling card image data to pass to the next {@link Activity} does not work because the image
     * data
     * is too big and causes a somewhat misleading exception. Compressing the image data yields a
     * reduction to 30% of the original size, but still gives the same exception. An alternative
     * would be to persist the image data in a file. That seems like a pretty horrible idea, as we
     * would be persisting very sensitive data on the device.
     */
    private View include_crop;
    private CropView cropView;
    private MaskView cropMaskView;
    private FrameOverlayView overlayView;


    public static void activityStart(Activity context, int guideColor, int bitmapQuality) {
        Intent intent = new Intent(context, CardIOActivity.class)
                .putExtra(CardIOActivity.EXTRA_NO_CAMERA, false)
                .putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false)
                .putExtra(CardIOActivity.EXTRA_SCAN_EXPIRY, false)
                .putExtra(CardIOActivity.EXTRA_HIDE_CARDIO_LOGO, true)
                .putExtra(CardIOActivity.EXTRA_LANGUAGE_OR_LOCALE, "zh-Hans")
                .putExtra(CardIOActivity.EXTRA_USE_PAYPAL_ACTIONBAR_ICON, false)
                .putExtra(CardIOActivity.EXTRA_KEEP_APPLICATION_THEME, true)
                .putExtra(CardIOActivity.EXTRA_GUIDE_COLOR, guideColor)
                .putExtra(CardIOActivity.EXTRA_SUPPRESS_SCAN, true)
                //设置图片质量
                .putExtra(CardIOActivity.EXTRA_CRAD_QUALITY, bitmapQuality)
                .putExtra(CardIOActivity.EXTRA_RETURN_CARD_IMAGE, true)
                .putExtra(CardIOActivity.EXTRA_UNBLUR_DIGITS, 4);
        context.startActivityForResult(intent, 100);
    }


    // ------------------------------------------------------------------------
    // ACTIVITY LIFECYCLE
    // ------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent clientData = getIntent();
        //是否控制主题
        useApplicationTheme = clientData.getBooleanExtra(CardIOActivity.EXTRA_KEEP_APPLICATION_THEME, false);
        //设置主题
        ActivityHelper.setActivityTheme(this, useApplicationTheme);
        //设置语言
        LocalizedStrings.setLanguage(clientData);

        // Validate app's manifest is correct. 这个属性是 裁剪 关闭界面
        mDetectOnly = clientData.getBooleanExtra(EXTRA_SUPPRESS_SCAN, false);

        ResolveInfo resolveInfo;
        String errorMsg;

        // Check for DataEntryActivity's portrait orientation

        // Check for CardIOActivity's orientation config in manifest
        resolveInfo = getPackageManager().resolveActivity(clientData,
                PackageManager.MATCH_DEFAULT_ONLY);
        errorMsg = Util.manifestHasConfigChange(resolveInfo, CardIOActivity.class);
        if (errorMsg != null) {
            throw new RuntimeException(errorMsg); // Throw the actual exception from this class, for
            // clarity.
        }
        //是否 从键盘上删除键盘按钮 扫描屏幕
        suppressManualEntry = clientData.getBooleanExtra(EXTRA_SUPPRESS_MANUAL_ENTRY, false);
        quality = clientData.getIntExtra(EXTRA_CRAD_QUALITY, 100);

        if (savedInstanceState != null) {
            waitingForPermission = savedInstanceState.getBoolean(BUNDLE_WAITING_FOR_PERMISSION);
        }

        if (clientData.getBooleanExtra(EXTRA_NO_CAMERA, false)) {
            manualEntryFallbackOrForced = true;
        } else if (!CardScanner.processorSupported()) {
            manualEntryFallbackOrForced = true;
        } else {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!waitingForPermission) {
                        if (checkSelfPermission(Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_DENIED) {
                            String[] permissions = {Manifest.permission.CAMERA};
                            waitingForPermission = true;
                            requestPermissions(permissions, PERMISSION_REQUEST_ID);
                            //finish();
                        } else {
                            checkCamera();
                            android23AndAboveHandleCamera();
                        }
                    }
                } else {
                    checkCamera();
                    android22AndBelowHandleCamera();
                }
            } catch (Exception e) {
                handleGeneralExceptionError(e);
            }
        }

    }

    private void android23AndAboveHandleCamera() {
        if (manualEntryFallbackOrForced) {
            finishIfSuppressManualEntry();
        } else {
            // Guaranteed to be called in API 23+
            showCameraScannerOverlay();
        }
    }


    private void android22AndBelowHandleCamera() {
        if (manualEntryFallbackOrForced) {
            finishIfSuppressManualEntry();
        } else {
            // guaranteed to be called in onCreate on API < 22, so it's ok that we're removing the window feature here
            requestWindowFeature(Window.FEATURE_NO_TITLE);

            showCameraScannerOverlay();
        }
    }

    private void finishIfSuppressManualEntry() {
        if (suppressManualEntry) {
            setResultAndFinish(RESULT_SCAN_NOT_AVAILABLE, null);
        }
    }

    private void checkCamera() {
        try {
            if (!Util.hardwareSupported()) {
                StringKey errorKey = StringKey.ERROR_NO_DEVICE_SUPPORT;
                String localizedError = LocalizedStrings.getString(errorKey);
                Log.w(Util.PUBLIC_LOG_TAG, errorKey + ": " + localizedError);
                manualEntryFallbackOrForced = true;
            }
        } catch (CameraUnavailableException e) {
            StringKey errorKey = StringKey.ERROR_CAMERA_CONNECT_FAIL;
            String localizedError = LocalizedStrings.getString(errorKey);

            Log.e(Util.PUBLIC_LOG_TAG, errorKey + ": " + localizedError);
            Toast toast = Toast.makeText(this, localizedError, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, TOAST_OFFSET_Y);
            toast.show();
            manualEntryFallbackOrForced = true;
        }
    }

    public void showCameraScannerOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            View decorView = getWindow().getDecorView();
            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            // Remember that you should never show the action bar if the
            // status bar is hidden, so hide that too if necessary.
            ActionBar actionBar = getActionBar();
            if (null != actionBar) {
                actionBar.hide();
            }
        }

        try {
            mGuideFrame = new Rect();

            mFrameOrientation = ORIENTATION_PORTRAIT;

            if (getIntent().getBooleanExtra(PRIVATE_EXTRA_CAMERA_BYPASS_TEST_MODE, false)) {
                if (!this.getPackageName().contentEquals("io.card.development")) {
                    throw new IllegalStateException("Illegal access of private extra");
                }
                // use reflection here so that the tester can be safely stripped for release builds.
                Class<?> testScannerClass = Class.forName("io.card.payment.CardScannerTester");
                Constructor<?> cons = testScannerClass.getConstructor(this.getClass(),
                        Integer.TYPE);
                mCardScanner = (CardScanner) cons.newInstance(new Object[]{this, mFrameOrientation});
            } else {
                mCardScanner = new CardScanner(this, mFrameOrientation);
            }
            mCardScanner.prepareScanner();
            //设置UI
            setMyPreviewLayout();
            //横屏监听
            /*orientationListener = new OrientationEventListener(this,
                    SensorManager.SENSOR_DELAY_UI) {
                @Override
                public void onOrientationChanged(int orientation) {
                    doOrientationChange(orientation);
                }
            };*/

        } catch (Exception e) {
            handleGeneralExceptionError(e);
        }
    }

    private void handleGeneralExceptionError(Exception e) {
        StringKey errorKey = StringKey.ERROR_CAMERA_UNEXPECTED_FAIL;
        String localizedError = LocalizedStrings.getString(errorKey);

        Log.e(Util.PUBLIC_LOG_TAG, "Unknown exception, please post the stack trace as a GitHub issue", e);
        Toast toast = Toast.makeText(this, localizedError, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, TOAST_OFFSET_Y);
        toast.show();
        manualEntryFallbackOrForced = true;
    }

    /**
     * 横竖屏
     *
     * @param orientation 角度
     */
    private void doOrientationChange(int orientation) {
        if (orientation < 0 || mCardScanner == null) {
            return;
        }

        orientation += mCardScanner.getRotationalOffset();

        // Check if we have gone too far forward with
        // rotation adjustment, keep the result between 0-360
        if (orientation > 360) {
            orientation -= 360;
        }
        int degrees;

        degrees = -1;

        if (orientation < DEGREE_DELTA || orientation > 360 - DEGREE_DELTA) {
            degrees = 0;
            mFrameOrientation = ORIENTATION_PORTRAIT;
        } else if (orientation > 90 - DEGREE_DELTA && orientation < 90 + DEGREE_DELTA) {
            degrees = 90;
            mFrameOrientation = ORIENTATION_LANDSCAPE_LEFT;
        } else if (orientation > 180 - DEGREE_DELTA && orientation < 180 + DEGREE_DELTA) {
            degrees = 180;
            mFrameOrientation = ORIENTATION_PORTRAIT_UPSIDE_DOWN;
        } else if (orientation > 270 - DEGREE_DELTA && orientation < 270 + DEGREE_DELTA) {
            degrees = 270;
            mFrameOrientation = ORIENTATION_LANDSCAPE_RIGHT;
        }
        if (degrees >= 0 && degrees != mLastDegrees) {
            mCardScanner.setDeviceOrientation(mFrameOrientation);
            setDeviceDegrees(degrees);
            if (degrees == 90) {
                rotateCustomOverlay(270);
            } else if (degrees == 270) {
                rotateCustomOverlay(90);
            } else {
                rotateCustomOverlay(degrees);
            }
        }
    }

    /**
     * Suspend/resume camera preview as part of the {@link Activity} life cycle (side note: we reuse the
     * same buffer for preview callbacks to greatly reduce the amount of required GC).
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (!waitingForPermission) {
            if (manualEntryFallbackOrForced) {
                if (suppressManualEntry) {
                    finishIfSuppressManualEntry();
                    return;
                } else {
                    nextActivity();
                    return;
                }
            }

            Util.logNativeMemoryStats();

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            ActivityHelper.setFlagSecure(this);

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            //激活横屏监听
            //orientationListener.enable();

            if (!restartPreview()) {
                StringKey error = StringKey.ERROR_CAMERA_UNEXPECTED_FAIL;
                showErrorMessage(LocalizedStrings.getString(error));
                nextActivity();
            } else {
                // Turn flash off
                setFlashOn(false);
            }

            doOrientationChange(mLastDegrees);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(BUNDLE_WAITING_FOR_PERMISSION, waitingForPermission);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //停止横屏监听
        /*if (orientationListener != null) {
            orientationListener.disable();
        }*/
        setFlashOn(false);

        if (mCardScanner != null) {
            mCardScanner.pauseScanning();
        }
    }

    @Override
    protected void onDestroy() {
        mOverlay = null;
        //停止横屏监听
        /*if (orientationListener != null) {
            orientationListener.disable();
        }*/
        setFlashOn(false);

        if (mCardScanner != null) {
            mCardScanner.endScanning();
            mCardScanner = null;
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_ID) {
            waitingForPermission = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCameraScannerOverlay();
                onResume();
            } else {
                // show manual entry - handled in onResume()
                manualEntryFallbackOrForced = true;
                Toast.makeText(this, "请设置相机使用权限", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DATA_ENTRY_REQUEST_ID && null != data) {
            //从相册回来 裁剪照片
            if (resultCode == Activity.RESULT_OK) {

                Uri uri = data.getData();
                cropView.setFilePath(getRealPathFromURI(uri));
                showCrop();
            }
        }
    }

    /**
     * This {@link Activity} overrides back button handling to handle back presses properly given the
     * various states this {@link Activity} can be in.
     * <br><br>
     * This method is called by Android, never directly by application code.
     */
    @Override
    public void onBackPressed() {
        if (!manualEntryFallbackOrForced && mOverlay.isAnimating()) {
            try {
                restartPreview();
            } catch (RuntimeException re) {
                Log.w(TAG, "*** could not return to preview: " + re);
            }
        } else if (mCardScanner != null) {
            if (null != include_crop && include_crop.getVisibility() == View.VISIBLE) {
                onResume();
                include_crop.setVisibility(View.GONE);
            } else {
                super.onBackPressed();
            }
        }
    }

    // ------------------------------------------------------------------------
    // STATIC METHODS
    // ------------------------------------------------------------------------

    /**
     * Determine if the device supports card scanning.
     * <br><br>
     * An ARM7 processor and Android SDK 8 or later are required. Additional checks for specific
     * misbehaving devices may also be added.
     *
     * @return <code>true</code> if camera is supported. <code>false</code> otherwise.
     */
    public static boolean canReadCardWithCamera() {
        try {
            return Util.hardwareSupported();
        } catch (CameraUnavailableException e) {
            return false;
        } catch (RuntimeException e) {
            Log.w(TAG, "RuntimeException accessing Util.hardwareSupported()");
            return false;
        }
    }

    /**
     * Returns the String version of this SDK.  Please include the return value of this method in any support requests.
     *
     * @return The String version of this SDK
     */
    public static String sdkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * @deprecated Always returns {@code new Date()}.
     */
    @Deprecated
    public static Date sdkBuildDate() {
        return new Date();
    }

    /**
     * Utility method for decoding card bitmap
     *
     * @param intent - intent received in {@link Activity#onActivityResult(int, int, Intent)}
     * @return decoded bitmap or null
     */
    public static Bitmap getCapturedCardImage(Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_CAPTURED_CARD_IMAGE)) {
            return null;
        }

        byte[] imageData = intent.getByteArrayExtra(EXTRA_CAPTURED_CARD_IMAGE);
        ByteArrayInputStream inStream = new ByteArrayInputStream(imageData);
        Bitmap result = BitmapFactory.decodeStream(inStream, null, new BitmapFactory.Options());
        return result;
    }

    // end static

    void onFirstFrame() {
        SurfaceView sv = mPreview.getSurfaceView();
        if (mOverlay != null) {
            mOverlay.setCameraPreviewRect(new Rect(sv.getLeft(), sv.getTop(), sv.getRight(), sv.getBottom()));
        }
        mFrameOrientation = ORIENTATION_PORTRAIT;
        setDeviceDegrees(0);

        onEdgeUpdate(new DetectionInfo());
    }

    void onEdgeUpdate(DetectionInfo dInfo) {
        mOverlay.setDetectionInfo(dInfo);
    }

    //拍照 并剪切
    void onCardDetected(Bitmap detectedBitmap, DetectionInfo dInfo) {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_PATTERN, -1);
        } catch (SecurityException e) {
            Log.e(Util.PUBLIC_LOG_TAG,
                    "Could not activate vibration feedback. Please add <uses-permission android:name=\"android.permission.VIBRATE\" /> to your application's manifest.");
        } catch (Exception e) {
            Log.w(Util.PUBLIC_LOG_TAG, "Exception while attempting to vibrate: ", e);
        }

        mCardScanner.pauseScanning();

        if (dInfo.predicted()) {
            mDetectedCard = dInfo.creditCard();
            mOverlay.setDetectedCard(mDetectedCard);
        }

        float sf;
        if (mFrameOrientation == ORIENTATION_PORTRAIT || mFrameOrientation == ORIENTATION_PORTRAIT_UPSIDE_DOWN) {
            sf = mGuideFrame.right / (float) CardScanner.CREDIT_CARD_TARGET_WIDTH * .95f;
        } else {
            sf = mGuideFrame.right / (float) CardScanner.CREDIT_CARD_TARGET_WIDTH * 1.15f;
        }

        Matrix m = new Matrix();
        m.postScale(sf, sf);

        Bitmap scaledCard = Bitmap.createBitmap(detectedBitmap, 0, 0, detectedBitmap.getWidth(), detectedBitmap.getHeight(), m, false);
        mOverlay.setBitmap(scaledCard);

        if (mDetectOnly) {
            //自动拍照 结果
            if (mOverlay != null && mOverlay.getBitmap() != null) {
                setIntent(mOverlay.getBitmap());
            } else {
                finish();
            }
        } else {
            nextActivity();
        }
    }

    void onCardDetected(Bitmap b, int scroll) {
        Bitmap rectBitmap;
        //保存图片到sdcard
        if (null != b) {
            if (scroll > 0) {
                // 定义矩阵对象
                Matrix matrix = new Matrix();
                // 缩放原图
                //matrix.postScale(3f, 3f);
                // 向左旋转45度，参数为正则向右旋转
                matrix.postRotate(scroll);
                rectBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);
                if (rectBitmap.isRecycled()) {
                    rectBitmap.recycle();
                    rectBitmap = null;
                }
            } else {
                rectBitmap = b;
            }
            //手动拍照的 结果

            Matrix m = new Matrix();
            m.postScale(3f, 3f);
            setIntent(Bitmap.createBitmap(rectBitmap, 26, 185, CardScanner.CREDIT_CARD_TARGET_WIDTH, CardScanner.CREDIT_CARD_TARGET_HEIGHT, m, false));
            if (!b.isRecycled()) {
                b.recycle();
                b = null;
            }
        } else {
            finish();
        }

    }


    //todo   去 下一个 页面使用     这个方法用不上
    private void nextActivity() {
        finish();
    }


    /**
     * Show an error message using toast.
     */
    private void showErrorMessage(final String msgStr) {
        Log.e(Util.PUBLIC_LOG_TAG, "error display: " + msgStr);
        Toast toast = Toast.makeText(CardIOActivity.this, msgStr, Toast.LENGTH_LONG);
        toast.show();
    }

    private boolean restartPreview() {
        mDetectedCard = null;
        assert mPreview != null;
        boolean success = mCardScanner.resumeScanning(mPreview.getSurfaceHolder());
        /*if (success) {
            mUIBar.setVisibility(View.VISIBLE);
        }*/

        return success;
    }

    private void setDeviceDegrees(int degrees) {
        View sv;

        sv = mPreview.getSurfaceView();

        if (sv == null) {
            return;
        }

        mGuideFrame = mCardScanner.getGuideFrame(sv.getWidth(), sv.getHeight());

        // adjust for surface view y offset 表面视图y偏移量调整    控制边框的上下偏移
        mGuideFrame.top += sv.getTop();
        mGuideFrame.bottom += sv.getTop();
        mOverlay.setGuideAndRotation(mGuideFrame, degrees);
        mLastDegrees = degrees;
    }

    // Called by OverlayView
    void toggleFlash() {
        setFlashOn(!mCardScanner.isFlashOn());
    }

    //设置 闪光的 开关
    void setFlashOn(boolean b) {
        boolean success = (mPreview != null && mOverlay != null && mCardScanner.setFlashOn(b));
        if (success) {
            mOverlay.setTorchOn(b);
        }
    }

    /**
     * 触发自动对焦
     */
    void triggerAutoFocus() {
        mCardScanner.triggerAutoFocus(true);
    }


    /**
     * 临时方法
     *
     * @param bitmap
     * @param filePath
     * @param fileName
     * @return
     */
    public String saveBitmapToGallery(Bitmap bitmap, String filePath, String fileName) {
        FileOutputStream fileOutputStream = null;
        try {
            File file = new File(filePath, fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
            fileOutputStream.flush();
            galleryAddPic(CardIOActivity.this, file.getAbsolutePath());
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 保存图片后，更新相册
     *
     * @param context           上下文
     * @param mCurrentPhotoPath 图路径
     */
    public static void galleryAddPic(Context context, String mCurrentPhotoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }


    /**
     * 自定义 主布局   ===================
     */
    private void setMyPreviewLayout() {

        // top level container  顶层容器 背景容器
        View layout_card_io_main = View.inflate(this, R.layout.layout_card_io_main, null);
        //取消按钮的点击事件
        layout_card_io_main.findViewById(R.id.tv_cancel).setOnClickListener(this);
        //相册按钮的点击事件
        layout_card_io_main.findViewById(R.id.tv_photo).setOnClickListener(this);
        //拍照按钮的点击事件
        layout_card_io_main.findViewById(R.id.take_photo_button).setOnClickListener(this);


        //相机层
        FrameLayout mMainLayout = layout_card_io_main.findViewById(R.id.fl_camera_main);
        //控制上下的背景颜色
        //相机 主界面
        FrameLayout previewFrame = new FrameLayout(this);
        previewFrame.setId(FRAME_ID);
        //相机界面
        mPreview = new Preview(this, null, mCardScanner.mPreviewWidth, mCardScanner.mPreviewHeight);
        mPreview.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.TOP));
        previewFrame.addView(mPreview);

        mOverlay = new OverlayView(this, null, Util.deviceSupportsTorch(this));
        mOverlay.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        if (getIntent() != null) {
            //设置展示logo
            boolean useCardIOLogo = getIntent().getBooleanExtra(EXTRA_USE_CARDIO_LOGO, false);
            mOverlay.setUseCardIOLogo(useCardIOLogo);

            int color = getIntent().getIntExtra(EXTRA_GUIDE_COLOR, 0);

            if (color != 0) {
                // force 100% opaque guide colors.
                int alphaRemovedColor = color | 0xFF000000;
                mOverlay.setGuideColor(alphaRemovedColor);
            } else {
                // default to greeeeeen
                mOverlay.setGuideColor(Color.GREEN);
            }
            //设置是否 隐藏logo
            boolean hideCardIOLogo = getIntent().getBooleanExtra(EXTRA_HIDE_CARDIO_LOGO, false);
            mOverlay.setHideCardIOLogo(hideCardIOLogo);
            //设置 中间提示文案
            String scanInstructions = getIntent().getStringExtra(EXTRA_SCAN_INSTRUCTIONS);
            if (scanInstructions != null) {
                mOverlay.setScanInstructions(scanInstructions);
            }
        }
        //添加 相机覆盖 图标等
        previewFrame.addView(mOverlay);
        RelativeLayout.LayoutParams previewParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        previewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        previewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        previewParams.addRule(RelativeLayout.ABOVE, UIBAR_ID);
        //添加相机
        mMainLayout.addView(previewFrame, previewParams);

        include_crop = layout_card_io_main.findViewById(R.id.card_include_crop);
        cropView = include_crop.findViewById(R.id.crop_view);
        cropMaskView = include_crop.findViewById(R.id.crop_mask_view);
        overlayView = include_crop.findViewById(R.id.overlay_view);
        include_crop.findViewById(R.id.button_rotate).setOnClickListener(this);
        include_crop.findViewById(R.id.button_confirm).setOnClickListener(this);
        include_crop.findViewById(R.id.button_cancel).setOnClickListener(this);
        this.setContentView(layout_card_io_main);
    }

    private void rotateCustomOverlay(float degrees) {
        if (customOverlayLayout != null) {
            float pivotX = customOverlayLayout.getWidth() / 2;
            float pivotY = customOverlayLayout.getHeight() / 2;

            Animation an = new RotateAnimation(0, degrees, pivotX, pivotY);
            an.setDuration(0);
            an.setRepeatCount(0);
            an.setFillAfter(true);

            customOverlayLayout.setAnimation(an);
        }
    }


    /**
     * 将结果 bitmap转换成数组
     * <p>
     * <p>
     * <p>
     * 精华
     * 继承 CardIOActivity.class
     * 重写 setIntent(Bitmap)
     * <p>
     * 获取到的 bitmap 会 传入 setIntent(bitmap)
     *
     * @param bitmap
     */
    public void setIntent(Bitmap bitmap) {
        Log.e("====", "bitmap===" + bitmap);
        if (null != bitmap) {
            setResultAndFinish(RESULT_SCAN_SUPPRESSED, new Intent().putExtra(CardIOActivity.EXTRA_CAPTURED_CARD_IMAGE, bitmapToArray(bitmap, quality)));
        } else {
            finish();
        }
    }

    /**
     * 将bitmap 转换成 byte[]
     *
     * @param bitmap   图片
     * @param qualityM 质量
     * @return
     */
    public byte[] bitmapToArray(Bitmap bitmap, int qualityM) {
        try {
        ByteArrayOutputStream scaledCardBytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, qualityM, scaledCardBytes);
        byte[] bytes = scaledCardBytes.toByteArray();
            scaledCardBytes.close();
        return bytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    /**
     * 返回结果 并关闭
     *
     * @param resultCode
     * @param data
     */
    private void setResultAndFinish(final int resultCode, final Intent data) {
        setResult(resultCode, data);
        finish();
    }

    // for torch test
    public Rect getTorchRect() {
        if (mOverlay == null) {
            return null;
        }
        return mOverlay.getTorchRect();
    }

    /**
     * 后添加的  获取 相册 图片路径的
     *
     * @param contentURI
     * @return
     */
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }


    private void showCrop() {
        onPause();
        include_crop.setVisibility(View.VISIBLE);
        cropMaskView.setVisibility(View.VISIBLE);
        overlayView.setVisibility(View.INVISIBLE);
        overlayView.setTypeWide();
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tv_cancel) {
            onBackPressed();

            //点击相册
        } else if (id == R.id.tv_photo) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ActivityCompat.requestPermissions(CardIOActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_EXTERNAL_STORAGE);
                    Toast.makeText(CardIOActivity.this, "请设置读取相册权限", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, DATA_ENTRY_REQUEST_ID);

            //点击拍照按钮
        } else if (id == R.id.take_photo_button) {
            mCardScanner.isTakePhoto = true;

        } else if (id == R.id.button_rotate) {
            cropView.rotate(90);

        } else if (id == R.id.button_confirm) {// 从图片选择直接 裁剪
            int maskType = cropMaskView.getMaskType();
            Rect rect;
            switch (maskType) {
                case MaskView.MASK_TYPE_BANK_CARD:
                    rect = cropMaskView.getFrameRect();
                    break;
                default:
                    rect = overlayView.getFrameRect();
                    Log.e("=====", rect.width() + "======" + rect.height());
                    break;
            }
            Bitmap cropBitmap = cropView.crop(rect);
            if (null != cropBitmap) {
                // 计算缩放比例.
                int scaleWidth = cropBitmap.getWidth();
                int scaleHeight = cropBitmap.getHeight();
                // 取得想要缩放的matrix参数.
                Matrix matrix = new Matrix();
                matrix.postScale(1296.0f / scaleWidth, 968.0f / scaleHeight);
                // 得到新的图片.
                setIntent(Bitmap.createBitmap(cropBitmap, 0, 0, scaleWidth, scaleHeight, matrix, true));
            } else {
                setIntent(cropBitmap);
            }

        } else if (id == R.id.button_cancel) {
            onResume();
            include_crop.setVisibility(View.GONE);

        }
    }


    /*mCardScanner.mCamera.takePicture(null, null, new android.hardware.Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
                        camera.stopPreview();
                        Bitmap b = null;
                        if (null != data) {
                            System.out.println("===========111  data==="+data.length);
                            b = BitmapFactory.decodeByteArray(data, 0, data.length);//data是字节数据，将其解析成位图

                        }
                        //保存图片到sdcard
                        if (null != b) {
                            System.out.println("==================b  w    " + b.getWidth());
                            System.out.println("==================b  h    " + b.getHeight());
                            // 定义矩阵对象
                            Matrix matrix = new Matrix();
                            // 缩放原图
                            matrix.postScale(1f, 1f);
                            // 向左旋转45度，参数为正则向右旋转
                            matrix.postRotate(90);
                            //bmp.getWidth(), 500分别表示重绘后的位图宽高  //y + height must be <= bitmap.height()
                            Bitmap rectBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);

                            //Bitmap rectBitmap = Bitmap.createBitmap(b, 40, 40, 1280, 960);
                            //todo 保存  bitmap
                            String imgUrl = saveBitmapToGallery(rectBitmap, getCacheDir().getAbsolutePath(), "/temp.jpg");

                            System.out.println("imgUrl======================" + imgUrl);
                            *//*if (!b.isRecycled()) {
                                b.recycle();
                                b = null;
                            }*//*
                            if (rectBitmap.isRecycled()) {
                                rectBitmap.recycle();
                                rectBitmap = null;
                            }
                            setResult(111);
                        }
                        finish();
                    }
                });*/
}
