package com.msds.vision.ui.stillimage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;

import com.msds.vision.R;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.isseiaoki.simplecropview.CropImageView;
import com.isseiaoki.simplecropview.callback.CropCallback;
import com.isseiaoki.simplecropview.callback.LoadCallback;
import com.isseiaoki.simplecropview.callback.SaveCallback;
import com.isseiaoki.simplecropview.util.Logger;
import com.isseiaoki.simplecropview.util.Utils;
import com.msds.vision.ui.classification.tflite.Classifier;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static android.content.Context.MODE_PRIVATE;

@RuntimePermissions
public class StillFragment extends Fragment {
    private static final String TAG = StillFragment.class.getSimpleName();

    private static final int REQUEST_IMAGE_CAPTURE = 10010;
    private static final int REQUEST_PICK_IMAGE = 10011;
    private static final int REQUEST_SAF_PICK_IMAGE = 10012;
    private static final String PROGRESS_DIALOG = "ProgressDialog";
    private static final String KEY_FRAME_RECT = "FrameRect";
    private static final String KEY_SOURCE_URI = "SourceUri";

    private CropImageView mCropView;
    private Bitmap.CompressFormat mCompressFormat = Bitmap.CompressFormat.JPEG;
    private RectF mFrameRect = null;
    private Uri mSourceUri = null;

    private BottomSheetBehavior sheetBehavior;
    private LinearLayout bottomSheet;
    private ImageView cameraBtn;
    private ImageView deleteBtn;
    private ImageView imageHolder;
    private ImageButton buttonCrop;
    private ImageButton buttonDone;
    private ImageButton buttonRight;
    private ImageButton buttonLeft;
    private ImageButton buttonPredict;
    private View viewDivider;
    private HorizontalScrollView scrollView;

    private Spinner modelSpinner, deviceSpinner;

    private TextView detected1, detected1Value, detected2, detected2Value, detected3, detected3Value;

    private Classifier classifier;
    private Classifier.Model model = Classifier.Model.QUANTIZED_EFFICIENTNET;
    private Classifier.Device device = Classifier.Device.CPU;
    /** Input image size of the model along x axis. */
    private int imageSizeX;
    /** Input image size of the model along y axis. */
    private int imageSizeY;
    private Integer sensorOrientation;

    // obtain shared preferences
    private SharedPreferences preferences;

    // Note: only the system can call this constructor by reflection.
    public StillFragment() {
    }

    public static StillFragment newInstance() {
        StillFragment fragment = new StillFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tfe_still_fragment, null, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // bind Views
        bindViews(view);

        preferences = getActivity().getSharedPreferences("classifier", MODE_PRIVATE);

        mCropView.setDebug(false);

        if (savedInstanceState != null) {
            // restore data
            mFrameRect = savedInstanceState.getParcelable(KEY_FRAME_RECT);
            mSourceUri = savedInstanceState.getParcelable(KEY_SOURCE_URI);
        }

        if (mSourceUri == null) {
            // default data
            mSourceUri = getUriFromDrawableResId(getContext(), R.drawable.placeholder);
            Log.e("aoki", "mSourceUri = "+mSourceUri);
        }

        // load image
        mCropView.load(mSourceUri)
                .initialFrameRect(mFrameRect)
                .useThumbnail(true)
                .execute(mLoadCallback);

        imageHolder.setImageURI(mSourceUri);
        imageHolder.setVisibility(View.VISIBLE);
        mCropView.setVisibility(View.INVISIBLE);
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
            mSourceUri = createNewUri(getContext(), Bitmap.CompressFormat.JPEG);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mSourceUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save data
        outState.putParcelable(KEY_FRAME_RECT, mCropView.getActualCropRect());
        outState.putParcelable(KEY_SOURCE_URI, mCropView.getSourceUri());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        if (resultCode == Activity.RESULT_OK) {
            // reset frame rect
            mFrameRect = null;
            switch (requestCode) {
                case REQUEST_PICK_IMAGE:
                    mSourceUri = result.getData();
                    mCropView.load(mSourceUri)
                            .initialFrameRect(mFrameRect)
                            .useThumbnail(true)
                            .execute(mLoadCallback);
                    break;
                case REQUEST_SAF_PICK_IMAGE:
                    mSourceUri = Utils.ensureUriPermission(getContext(), result);
                    mCropView.setVisibility(View.INVISIBLE);
                    imageHolder.setVisibility(View.VISIBLE);
                    imageHolder.setImageURI(mSourceUri);

                    mCropView.load(mSourceUri)
                            .initialFrameRect(mFrameRect)
                            .useThumbnail(true)
                            .execute(mLoadCallback);
                    break;
                case REQUEST_IMAGE_CAPTURE:
                    mCropView.setVisibility(View.INVISIBLE);
                    imageHolder.setVisibility(View.VISIBLE);
                    imageHolder.setImageURI(mSourceUri);

                    mCropView.load(mSourceUri)
                            .initialFrameRect(mFrameRect)
                            .useThumbnail(true)
                            .execute(mLoadCallback);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        StillFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    private void bindViews(View view) {
        mCropView = view.findViewById(R.id.cropImageView);
        view.findViewById(R.id.buttonCrop).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonFitImage).setOnClickListener(btnListener);
        view.findViewById(R.id.button1_1).setOnClickListener(btnListener);
        view.findViewById(R.id.button3_4).setOnClickListener(btnListener);
        view.findViewById(R.id.button4_3).setOnClickListener(btnListener);
        view.findViewById(R.id.button9_16).setOnClickListener(btnListener);
        view.findViewById(R.id.button16_9).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonFree).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonPickImage).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonRotateLeft).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonRotateRight).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonCustom).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonCircle).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonDone).setOnClickListener(btnListener);
        view.findViewById(R.id.buttonShowCircleButCropAsSquare).setOnClickListener(btnListener);

        bottomSheet = view.findViewById(R.id.still_bottom_sheet_layout);

        cameraBtn = view.findViewById(R.id.buttonPhoto);
        deleteBtn = view.findViewById(R.id.buttonDelete);
        modelSpinner = view.findViewById(R.id.still_model_spinner);
        deviceSpinner = view.findViewById(R.id.still_device_spinner);
        detected1 = view.findViewById(R.id.detected_item);
        detected2 = view.findViewById(R.id.detected_item1);
        detected3 = view.findViewById(R.id.detected_item2);
        detected1Value = view.findViewById(R.id.detected_item_value);
        detected2Value = view.findViewById(R.id.detected_item1_value);
        detected3Value = view.findViewById(R.id.detected_item2_value);
        imageHolder = view.findViewById(R.id.imageHolder);
        buttonCrop = view.findViewById(R.id.buttonCrop);
        buttonDone = view.findViewById(R.id.buttonDone);
        scrollView = view.findViewById(R.id.tab_bar);
        viewDivider = view.findViewById(R.id.viewDivider);
        buttonLeft = view.findViewById(R.id.buttonRotateLeft);
        buttonRight = view.findViewById(R.id.buttonRotateRight);
        buttonPredict = view.findViewById(R.id.buttonPredict);

        sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setPeekHeight(245);
        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_DRAGGING:
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                    case BottomSheetBehavior.STATE_COLLAPSED:
                    case BottomSheetBehavior.STATE_SETTLING:
                    case BottomSheetBehavior.STATE_EXPANDED:
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN: {

                    }
                    break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        buttonCrop.setOnClickListener(v -> {

            buttonCrop.setVisibility(View.INVISIBLE);
            buttonDone.setVisibility(View.VISIBLE);

            imageHolder.setVisibility(View.INVISIBLE);
            //mCropView.setImageURI(mSourceUri);
            mCropView.setVisibility(View.VISIBLE);

            buttonPredict.setVisibility(View.INVISIBLE);
            scrollView.setVisibility(View.VISIBLE);
            viewDivider.setVisibility(View.VISIBLE);
            buttonLeft.setVisibility(View.VISIBLE);
            buttonRight.setVisibility(View.VISIBLE);

            sheetBehavior.setPeekHeight(350);

        });

        buttonDone.setOnClickListener(v -> {
            buttonCrop.setVisibility(View.VISIBLE);
            buttonDone.setVisibility(View.INVISIBLE);

            buttonPredict.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
            viewDivider.setVisibility(View.GONE);
            buttonLeft.setVisibility(View.INVISIBLE);
            buttonRight.setVisibility(View.INVISIBLE);

            sheetBehavior.setPeekHeight(245);

            StillFragmentPermissionsDispatcher.cropImageWithPermissionCheck(StillFragment.this);
        });


        cameraBtn.setOnClickListener(v -> {
            StillFragmentPermissionsDispatcher.dispatchTakePictureIntentWithPermissionCheck(StillFragment.this);
        });

        deleteBtn.setOnClickListener(v -> {
            buttonDone.setVisibility(View.INVISIBLE);
            buttonCrop.setVisibility(View.VISIBLE);

            mSourceUri = getUriFromDrawableResId(getContext(), R.drawable.placeholder);
            mCropView.load(mSourceUri)
                    .initialFrameRect(mFrameRect)
                    .useThumbnail(true)
                    .execute(mLoadCallback);

            buttonPredict.setVisibility(View.VISIBLE);
            buttonLeft.setVisibility(View.INVISIBLE);
            buttonRight.setVisibility(View.INVISIBLE);
            sheetBehavior.setPeekHeight(245);
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            mCropView.setVisibility(View.INVISIBLE);
            scrollView.setVisibility(View.GONE);
            viewDivider.setVisibility(View.GONE);
            imageHolder.setImageURI(mSourceUri);
            imageHolder.setVisibility(View.VISIBLE);
        });

        buttonPredict.setOnClickListener(v -> {
            int sensorOrientation = getScreenOrientation();
            Bitmap rgbFrameBitmap = null;

            try {
                classifier = Classifier.create(getActivity(), model, device, 1);
                rgbFrameBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), mSourceUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            final List<Classifier.Recognition> results = classifier.recognizeImage(rgbFrameBitmap, sensorOrientation);
            System.out.println(results);
            showResultsInBottomSheet(results);
            classifier.close();

            sheetBehavior.setPeekHeight(480);
        });


        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                model = Classifier.Model.valueOf(modelSpinner.getSelectedItem().toString().toUpperCase());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        deviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                device = Classifier.Device.valueOf(deviceSpinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        model = Classifier.Model.valueOf(modelSpinner.getSelectedItem().toString().toUpperCase());
        device = Classifier.Device.valueOf(deviceSpinner.getSelectedItem().toString());
    }

    @UiThread
    protected void showResultsInBottomSheet(List<Classifier.Recognition> results) {
        if (results != null && results.size() >= 3) {
            Classifier.Recognition recognition = results.get(0);
            if (recognition != null) {
                if (recognition.getTitle() != null) detected1.setText(recognition.getTitle());
                if (recognition.getConfidence() != null)
                    detected1Value.setText(
                            String.format("%.2f", (100 * recognition.getConfidence())) + "%");
            }

            Classifier.Recognition recognition1 = results.get(1);
            if (recognition1 != null) {
                if (recognition1.getTitle() != null) detected2.setText(recognition1.getTitle());
                if (recognition1.getConfidence() != null)
                    detected2Value.setText(
                            String.format("%.2f", (100 * recognition1.getConfidence())) + "%");
            }

            Classifier.Recognition recognition2 = results.get(2);
            if (recognition2 != null) {
                if (recognition2.getTitle() != null) detected3.setText(recognition2.getTitle());
                if (recognition2.getConfidence() != null)
                    detected3Value.setText(
                            String.format("%.2f", (100 * recognition2.getConfidence())) + "%");
            }
        }
    }

    private int getScreenOrientation() {
        switch (getActivity().getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    private void setPrefs() {
        mFrameRect = mCropView.getActualCropRect();
        mSourceUri = mCropView.getSourceUri();

        SharedPreferences.Editor prefsEditor = preferences.edit();
        prefsEditor.putFloat("bottom", mFrameRect.bottom);
        prefsEditor.putFloat("top", mFrameRect.top);
        prefsEditor.putFloat("right", mFrameRect.right);
        prefsEditor.putFloat("left", mFrameRect.left);
        prefsEditor.putString("uri", mSourceUri.toString());
        prefsEditor.apply();
    }

    private void getPrefs() {
        mFrameRect.bottom = preferences.getFloat("bottom", 0);
        mFrameRect.top = preferences.getFloat("top", 0);
        mFrameRect.right = preferences.getFloat("right", 0);
        mFrameRect.left = preferences.getFloat("left", 0);
        mSourceUri = Uri.parse(preferences.getString("uri", null));
        mCropView.load(mSourceUri)
                .initialFrameRect(mFrameRect)
                .execute(mLoadCallback);
    }


    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SAF_PICK_IMAGE);
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void cropImage() {
        showProgress();
        mCropView.crop(mSourceUri).execute(mCropCallback);
    }

    public void showProgress() {
        ProgressDialogFragment f = ProgressDialogFragment.getInstance();
        getFragmentManager().beginTransaction().add(f, PROGRESS_DIALOG).commitAllowingStateLoss();
    }

    public void dismissProgress() {
        if (!isResumed()) return;
        FragmentManager manager = getFragmentManager();
        if (manager == null) return;
        ProgressDialogFragment f = (ProgressDialogFragment) manager.findFragmentByTag(PROGRESS_DIALOG);
        if (f != null) {
            getFragmentManager().beginTransaction().remove(f).commitAllowingStateLoss();
        }
    }

    public Uri createSaveUri() {
        return createNewUri(getContext(), mCompressFormat);
    }

    public static String getDirPath() {
        String dirPath = "";
        File imageDir = null;
        File extStorageDir = Environment.getExternalStorageDirectory();
        if (extStorageDir.canWrite()) {
            imageDir = new File(extStorageDir.getPath() + "/simplecropview");
        }
        if (imageDir != null) {
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }
            if (imageDir.canWrite()) {
                dirPath = imageDir.getPath();
            }
        }
        return dirPath;
    }

    public static Uri getUriFromDrawableResId(Context context, int drawableResId) {
        StringBuilder builder = new StringBuilder().append(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .append("://")
                .append(context.getResources().getResourcePackageName(drawableResId))
                .append("/")
                .append(context.getResources().getResourceTypeName(drawableResId))
                .append("/")
                .append(context.getResources().getResourceEntryName(drawableResId));
        return Uri.parse(builder.toString());
    }

    public static Uri createNewUri(Context context, Bitmap.CompressFormat format) {
        long currentTimeMillis = System.currentTimeMillis();
        Date today = new Date(currentTimeMillis);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String title = dateFormat.format(today);
        String dirPath = getDirPath();
        String fileName = "scv" + title + "." + getMimeType(format);
        String path = dirPath + "/" + fileName;
        File file = new File(path);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + getMimeType(format));
        values.put(MediaStore.Images.Media.DATA, path);
        long time = currentTimeMillis / 1000;
        values.put(MediaStore.MediaColumns.DATE_ADDED, time);
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, time);
        if (file.exists()) {
            values.put(MediaStore.Images.Media.SIZE, file.length());
        }

        ContentResolver resolver = context.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Logger.i("SaveUri = " + uri);
        return uri;
    }

    public static String getMimeType(Bitmap.CompressFormat format) {
        Logger.i("getMimeType CompressFormat = " + format);
        switch (format) {
            case JPEG:
                return "jpeg";
            case PNG:
                return "png";
        }
        return "png";
    }

    public static Uri createTempUri(Context context) {
        return Uri.fromFile(new File(context.getCacheDir(), "cropped"));
    }

    private final View.OnClickListener btnListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            switch (v.getId()) {
                case R.id.buttonFitImage:
                    mCropView.setCropMode(CropImageView.CropMode.FIT_IMAGE);
                    break;
                case R.id.button1_1:
                    mCropView.setCropMode(CropImageView.CropMode.SQUARE);
                    break;
                case R.id.button3_4:
                    mCropView.setCropMode(CropImageView.CropMode.RATIO_3_4);
                    break;
                case R.id.button4_3:
                    mCropView.setCropMode(CropImageView.CropMode.RATIO_4_3);
                    break;
                case R.id.button9_16:
                    mCropView.setCropMode(CropImageView.CropMode.RATIO_9_16);
                    break;
                case R.id.button16_9:
                    mCropView.setCropMode(CropImageView.CropMode.RATIO_16_9);
                    break;
                case R.id.buttonCustom:
                    mCropView.setCustomRatio(7, 5);
                    break;
                case R.id.buttonFree:
                    mCropView.setCropMode(CropImageView.CropMode.FREE);
                    break;
                case R.id.buttonCircle:
                    mCropView.setCropMode(CropImageView.CropMode.CIRCLE);
                    break;
                case R.id.buttonShowCircleButCropAsSquare:
                    mCropView.setCropMode(CropImageView.CropMode.CIRCLE_SQUARE);
                    break;
                case R.id.buttonRotateLeft:
                    mCropView.rotateImage(CropImageView.RotateDegrees.ROTATE_M90D);
                    break;
                case R.id.buttonRotateRight:
                    mCropView.rotateImage(CropImageView.RotateDegrees.ROTATE_90D);
                    break;
                case R.id.buttonPickImage:
                    StillFragmentPermissionsDispatcher.pickImageWithPermissionCheck(StillFragment.this);
                    break;
            }
        }
    };

    private final LoadCallback mLoadCallback = new LoadCallback() {
        @Override public void onSuccess() {

        }

        @Override public void onError(Throwable e) {
        }
    };

    private final CropCallback mCropCallback = new CropCallback() {
        @Override public void onSuccess(Bitmap cropped) {
            mCropView.save(cropped)
                    .compressFormat(mCompressFormat)
                    .execute(createSaveUri(), mSaveCallback);
        }

        @Override public void onError(Throwable e) {
        }
    };

    private final SaveCallback mSaveCallback = new SaveCallback() {
        @Override public void onSuccess(Uri outputUri) {
            dismissProgress();
            //((StillActivity) getActivity()).startResultActivity(outputUri);
            mSourceUri = outputUri;
            mCropView.load(outputUri)
                    .initialFrameRect(mFrameRect)
                    .useThumbnail(true)
                    .execute(mLoadCallback);

            mCropView.setVisibility(View.INVISIBLE);
            imageHolder.setVisibility(View.VISIBLE);
            imageHolder.setImageURI(mSourceUri);
        }

        @Override public void onError(Throwable e) {
            dismissProgress();
        }
    };
}
