package com.example.zavrsni_mojaboja;

//ovo su svi importi koji su potrebni za rad ovog activity-a
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class SecondActivity extends AppCompatActivity implements ImageAnalysis.Analyzer {

    //inicijalizacija varijabli koje koristimo za spremanje odnosno pamćenje boja
    private static final String PREFS_NAME = "MyAppPrefs";
    private static final String KEY_ORIGINAL_COLOR = "originalColor";
    private static final String KEY_REPLACEMENT_COLOR = "replacementColor";

    //inicijalizacija potrebnih varijabli
    private ListenableFuture<ProcessCameraProvider> camera_provider_future;
    private PreviewView preview_view;
    private ImageView processed_image_view;
    private Button btn_original_color, btn_replacement_color;
    private Switch switch_camera;
    private Spinner spinner_resolution;

    //kod prvog pokretanja aplikacije postavljamo originalnu boju na crvenu i zamjensku na plavu
    private int original_color = Color.RED;
    private int replacement_color = Color.BLUE;


    //koristimo 3 vrste rezolucija: VGA, HD, FHD
    private final Size[] supported_resolutions = {new Size(640, 480), new Size(1280, 720), new Size(1920, 1080)};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        //povezivanje inicijaliziranih varijabli sa xml activity-ma
        preview_view = findViewById(R.id.previewView);
        processed_image_view = findViewById(R.id.processedImageView);
        btn_original_color = findViewById(R.id.btnOriginalColor);
        btn_replacement_color = findViewById(R.id.btnReplacementColor);
        switch_camera = findViewById(R.id.switchCamera);
        spinner_resolution = findViewById(R.id.spinnerResolution);

        //dohvaćamo zadnje spremljene postavke za boje
        load_color_settings();

        //postavljamo odabranu boju kao pozadinsku na botunima
        btn_original_color.setBackgroundColor(original_color);
        btn_replacement_color.setBackgroundColor(replacement_color);

        //pritiskom na botun pozivamo funkciju za otvaranje xml file za odabir boje
        btn_original_color.setOnClickListener(v -> open_color_config_file(true));
        btn_replacement_color.setOnClickListener(v -> open_color_config_file(false));


        //provjera jesmo li uspješno dodali openCv u naš projekt
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV inicijalizacijska pogreška");
        } else {
            Log.d("OpenCV", "OpenCV inicijalizacija uspješna");
        }

        //pozivamo funkcije za odabir rezolucije i za kameru
        setup_resolution_spinner();
        setup_camera();


        //funkcionalnosti switch buttona
        switch_camera.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //prikaz obrađene slike preko cijelog ekrana
            if (isChecked) {
                preview_view.setVisibility(View.GONE);
                btn_original_color.setVisibility(View.VISIBLE);
                btn_replacement_color.setVisibility(View.VISIBLE);
                spinner_resolution.setVisibility(View.GONE);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) processed_image_view.getLayoutParams();
                params.weight = 1;
                processed_image_view.setLayoutParams(params);
            }
            //normalni mod
            else {
                preview_view.setVisibility(View.VISIBLE);
                btn_original_color.setVisibility(View.VISIBLE);
                btn_replacement_color.setVisibility(View.VISIBLE);
                spinner_resolution.setVisibility(View.VISIBLE);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) processed_image_view.getLayoutParams();
                params.weight = 2;
                processed_image_view.setLayoutParams(params);
            }
        });

    }

    //funkcija koja integrira hsvColorWheel u SecondActivity
    private void open_color_config_file(boolean isOriginal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialog_view = LayoutInflater.from(this).inflate(R.layout.hsv_color_wheel, null);
        builder.setView(dialog_view);
        HSVColorWheel HSV_color_wheel = dialog_view.findViewById(R.id.hsvColorWheel);
        Button btn_confirm_color = dialog_view.findViewById(R.id.btnConfirmColor);
        AlertDialog dialog = builder.create();

        HSV_color_wheel.setOnColorSelectedListener(color -> {
            if (isOriginal) {
                original_color = color;
                btn_original_color.setBackgroundColor(original_color);
            }
            else {
                replacement_color = color;
                btn_replacement_color.setBackgroundColor(replacement_color);
            }
            btn_confirm_color.setBackgroundColor(color);
        });

        btn_confirm_color.setOnClickListener(v -> {
            save_color_settings();
            dialog.dismiss();
        });
        dialog.show();
    }


    //funkcija za spremanje boja, odnosno pamćenje zadnjih odabranih boja
    private void save_color_settings() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_ORIGINAL_COLOR, original_color);
        editor.putInt(KEY_REPLACEMENT_COLOR, replacement_color);
        editor.apply();
    }

    //funkcija koja dohvaća zadnje odabrane (spremljene) boje
    private void load_color_settings() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        original_color = preferences.getInt(KEY_ORIGINAL_COLOR, Color.RED);
        replacement_color = preferences.getInt(KEY_REPLACEMENT_COLOR, Color.BLUE);
        btn_original_color.setBackgroundColor(original_color);
        btn_replacement_color.setBackgroundColor(replacement_color);
    }

    //funkcija za odabir rezolucije
    private void setup_resolution_spinner() {
        List<String> resolutionOptions = new ArrayList<>();
        for (Size resolution : supported_resolutions) {
            resolutionOptions.add(resolution.getWidth() + "x" + resolution.getHeight());
        }

        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, resolutionOptions); spinner_resolution.setAdapter(resolutionAdapter);

        spinner_resolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                restart_camera_resolution(supported_resolutions[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    //funkcija koja osvježava rezoluciju kamere nakon odabira rezolucije
    private void restart_camera_resolution(Size resolution) {
        camera_provider_future.addListener(() -> {
            try {
                ProcessCameraProvider camera_provider = camera_provider_future.get();
                camera_provider.unbindAll();
                start_cameraX(camera_provider, resolution);
            }
            catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Pogreška u promjeni rezolucije obrade", e);
            }
        }, getExecutor());
    }

    //funkcija u kojoj postavljamo postavke kamere
    private void setup_camera() {
        camera_provider_future = ProcessCameraProvider.getInstance(this);
        camera_provider_future.addListener(() -> {
            try {
                ProcessCameraProvider camera_provider = camera_provider_future.get();
                start_cameraX(camera_provider);
            }
            catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Pogreška u inicijalizaciji CameraX", e);
            }
        }, getExecutor());
    }


    //funkcije za pokretanje camereX
    private void start_cameraX(ProcessCameraProvider cameraProvider) {
        start_cameraX(cameraProvider, supported_resolutions[0]);
    }

    private void start_cameraX(ProcessCameraProvider cameraProvider, Size resolution) {
        cameraProvider.unbindAll();
        CameraSelector camera_selector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        Preview preview = new Preview.Builder().setTargetResolution(resolution).build();
        preview.setSurfaceProvider(preview_view.getSurfaceProvider());
        ImageAnalysis image_analysis = new ImageAnalysis.Builder().setTargetResolution(resolution).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        image_analysis.setAnalyzer(getExecutor(), this);
        cameraProvider.bindToLifecycle((LifecycleOwner) this, camera_selector, preview, image_analysis);
    }

    //ova funkcija uzme trenutnu sliku, smanji je za /2 radi brže obrade, pošalje sliku na obradu
    @Override
    public void analyze(@NonNull ImageProxy image) {
        Bitmap bitmap = preview_view.getBitmap();
        image.close();
        if (bitmap == null) return;
        Bitmap scaled_bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true);
        Bitmap processed_bitmap = color_replace_function(scaled_bitmap, original_color, replacement_color);
        runOnUiThread(() -> processed_image_view.setImageBitmap(processed_bitmap));
    }

    //funkcija u kojoj mjenjamo boje i u kojoj postavljamo postavke za prepoznavanje boje
    private Bitmap color_replace_function(Bitmap original, int target_color, int new_color) {
        Mat srcMat = new Mat();
        Utils.bitmapToMat(original, srcMat);

        // konvertiramo u hsv spektar
        // (OpenCV koristi H: 0-180, S: 0-255, V: 0-255)
        Mat hsvMat = new Mat();
        Imgproc.cvtColor(srcMat, hsvMat, Imgproc.COLOR_RGB2HSV);

        // prikupljamo HSV vrijednosti odabrane boje
        // (android koristi H: 0-360, S: 0-1, V: 0-1)
        float[] target_hsv_android = new float[3];
        Color.RGBToHSV(Color.red(target_color), Color.green(target_color), Color.blue(target_color), target_hsv_android);

        //konvertiramo hsv spektar u openCV
        Scalar target_hsv_opencv = new Scalar(target_hsv_android[0] / 2, target_hsv_android[1] * 255,target_hsv_android[2] * 255);

        //tolerancije
        double hueTolerance = 10;
        double minSaturation = 0.35 * 255;
        double minValue = 0.35 * 255;

        // kreiramo donju granicu za detekciju
        Scalar lower = new Scalar(Math.max(target_hsv_opencv.val[0] - hueTolerance, 0),minSaturation,minValue);
        // kreiramo gornju granicu za detekciju
        Scalar upper = new Scalar(Math.min(target_hsv_opencv.val[0] + hueTolerance, 180),255,255);

        // crvenu boju treba posebno dohvaćati jer se ona nalazi na rubu 2 spektra
        Mat mask = new Mat();
        //ispod su granice za crvenu boju
        if (target_hsv_android[0] < 15 || target_hsv_android[0] > 345) {
            Mat mask1 = new Mat();
            Mat mask2 = new Mat();
            // donji dio kruga crvene (0-15° u OpenCV)
            Core.inRange(hsvMat, new Scalar(0, minSaturation, minValue),new Scalar(15, 255, 255), mask1);
            // gornji dio kruga crvene (165-180° u OpenCV)
            Core.inRange(hsvMat, new Scalar(165, minSaturation, minValue), new Scalar(180, 255, 255), mask2);
            Core.bitwise_or(mask1, mask2, mask);
        }
        else {
            // uobicajna obrada za sve ostale boje
            Core.inRange(hsvMat, lower, upper, mask);
        }

        return applyColorChange(srcMat, mask, new_color);
    }


    //ova funkcija boja prepoznatu originalnu boju u zamjensku
    private Bitmap applyColorChange(Mat srcMat, Mat mask, int new_color) {
        Mat hsv = new Mat();
        Imgproc.cvtColor(srcMat, hsv, Imgproc.COLOR_RGB2HSV);
        // dijelimo na H, S, V kanale
        List<Mat> hsvChannels = new ArrayList<>(3);
        Core.split(hsv, hsvChannels);
        float[] newHSV = new float[3];
        Color.RGBToHSV(Color.red(new_color), Color.green(new_color), Color.blue(new_color), newHSV);
        double newHue = newHSV[0] / 2.0; // OpenCV hue: 0–180
        Mat newHueMat = new Mat(hsv.size(), CvType.CV_8UC1, new Scalar(newHue));
        // Primijeni novu boju samo na maskiranim dijelovima i spajamo H, S, V
        newHueMat.copyTo(hsvChannels.get(0), mask);
        Core.merge(hsvChannels, hsv);
        // opet konvertiramo u RGB i pretvaramo u bitmap objekt
        Mat resultMat = new Mat();
        Imgproc.cvtColor(hsv, resultMat, Imgproc.COLOR_HSV2RGB);
        Bitmap result = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultMat, result);
        // oslobađamo memoriju
        srcMat.release();
        hsv.release();
        resultMat.release();
        newHueMat.release();
        for (Mat m : hsvChannels) m.release();
        mask.release();
        return result;
    }


    //funkcija koju koristimo da se funkcionalnosti izvršavaju odmah na ekranu, a ne u pozadini
    @NonNull
    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }
}