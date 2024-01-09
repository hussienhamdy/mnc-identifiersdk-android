package id.mncinnovation.mncidentifiersdk.kotlin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import id.mncinnovation.face_detection.MNCIdentifier
import id.mncinnovation.face_detection.SelfieWithKtpActivity
import id.mncinnovation.face_detection.analyzer.DetectionMode
import id.mncinnovation.identification.core.common.ResultErrorType
import id.mncinnovation.mncidentifiersdk.BuildConfig
import id.mncinnovation.mncidentifiersdk.databinding.ActivityMainBinding
import id.mncinnovation.ocr.ExtractDataOCRListener
import id.mncinnovation.ocr.MNCIdentifierOCR
import id.mncinnovation.ocr.ScanOCRActivity
import id.mncinnovation.ocr.model.OCRResultModel
import java.io.File
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    private var latestTmpUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val detectionModes = mutableListOf(
            DetectionMode.HOLD_STILL
        )
        var random = 0;
        for (i in 1 until 3) {
            random = Random.nextInt(4);
            Log.d("tag","random random $random");
            when (random) {
                0 -> {
                    detectionModes.add(i, DetectionMode.TURN_RIGHT)
                }
                1 -> {
                    detectionModes.add(i, DetectionMode.TURN_LEFT)
                }
                2 -> {
                    detectionModes.add(i, DetectionMode.SMILE)
                }
                else -> {
                    detectionModes.add(i, DetectionMode.BLINK)
                }
            }
        }
                MNCIdentifier.setDetectionModeSequence(
            true, detectionModes
        )
        MNCIdentifier.setLowMemoryThreshold(50) // for face detection

        with(binding) {
            btnScanKtp.setOnClickListener {
                resultLauncherOcr.launch(Intent(this@MainActivity, ScanOCRActivity::class.java))
            }

            btnCaptureKtpOwn.setOnClickListener {
                takeImage()
            }

            btnCaptureKtp.setOnClickListener {
                // keep without named param
                MNCIdentifierOCR.config(
                    true, //withFlash
                    true, //cameraOnly
                    50 // for ocr
                )
                MNCIdentifierOCR.startCapture(this@MainActivity, 102)
            }

            btnLivenessDetection.setOnClickListener {
                resultLauncherLiveness.launch(MNCIdentifier.getLivenessIntent(this@MainActivity))
            }

            btnSelfieWKtp.setOnClickListener {
                resultLauncherSelfieWithKtp.launch(
                    Intent(
                        this@MainActivity,
                        SelfieWithKtpActivity::class.java
                    )
                )
            }
        }
    }

    private val resultLauncherOwnCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                latestTmpUri?.let {
                    val uriList = mutableListOf<Uri>()
                    uriList.add(it)
                    MNCIdentifierOCR.extractDataFromUri(
                        uriList,
                        this@MainActivity,
                        object : ExtractDataOCRListener {
                            override fun onStart() {
                                Log.d("TAGAPP", "onStart Process Extract")
                            }

                            override fun onFinish(result: OCRResultModel) {
                                if (result.isSuccess) {
                                    result.getBitmapImage()?.let { bitmap ->
                                        binding.ivKtp.setImageBitmap(bitmap)
                                    }
                                    binding.tvScanKtp.text = result.toString()
                                } else {
                                    handleError(result.errorMessage, result.errorType)
                                }
                            }

                            override fun onError(message: String?, errorType: ResultErrorType?) {
                                handleError(message, errorType)
                            }
                        })
                }
            } else {
                Toast.makeText(this, "Capture image failed", Toast.LENGTH_SHORT).show()
            }
        }


    private val resultLauncherOcr =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val captureOCRResult = MNCIdentifierOCR.getOCRResult(data)
                captureOCRResult?.let { ktpResult ->
                    if (ktpResult.isSuccess) {
                        ktpResult.getBitmapImage()?.let {
                            binding.ivKtp.setImageBitmap(it)
                        }
                        binding.tvScanKtp.text = captureOCRResult.toString()
                    } else {
                        handleError(ktpResult.errorMessage, ktpResult.errorType)
                    }
                }
            }
        }

    private val resultLauncherLiveness =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val livenessResult = MNCIdentifier.getLivenessResult(data)
                livenessResult?.let {
                    if (it.isSuccess) {
                        binding.tvAttempt.apply {
                            visibility = View.VISIBLE
                            text = "Attempt: ${it.attempt}"
                        }
                        val livenessResultAdapter = LivenessResultAdapter(it)
                        binding.rvLiveness.apply {
                            layoutManager = LinearLayoutManager(
                                this@MainActivity,
                                LinearLayoutManager.HORIZONTAL,
                                false
                            )
                            adapter = livenessResultAdapter
                        }
                    } else {
                        Log.d(
                            this.javaClass.name,
                            "Error : ${it.errorMessage}, Type : ${it.errorType.toString()}"
                        )
                    }
                }
            }
        }

    private val resultLauncherSelfieWithKtp =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val selfieResult = MNCIdentifier.getSelfieResult(data)
                selfieResult?.let { selfieWithKtpResult ->
                    if(selfieWithKtpResult.isSuccess) {
                        binding.llResultSelfieWKtp.visibility = View.VISIBLE
                        selfieWithKtpResult.getBitmap(this) { message, errorType ->
                            handleError(message, errorType)
                        }?.let {
                            binding.ivSelfieWKtpOri.setImageBitmap(it)
                        }
                        selfieWithKtpResult.getListFaceBitmap(this) { message, errorType ->
                            handleError(message, errorType)
                        }
                            .forEachIndexed { index, bitmap ->
                                when (index) {
                                    0 -> {
                                        binding.ivFace1.apply {
                                            visibility = View.VISIBLE
                                            setImageBitmap(bitmap)
                                        }
                                    }

                                    1 -> binding.ivFace2.apply {
                                        visibility = View.VISIBLE
                                        setImageBitmap(bitmap)
                                    }
                                }
                            }
                    } else {
                        handleError(selfieResult.errorMessage, selfieResult.errorType)
                    }
                }
            }
        }

    private fun handleError(errorMessage: String?, errorType: ResultErrorType?) {
        Log.d(this.javaClass.name, "Error : $errorMessage, Type : ${errorType.toString()}")
        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 102) {
            if (resultCode == Activity.RESULT_OK) {
                val captureOCRResult = MNCIdentifierOCR.getOCRResult(data)
                captureOCRResult?.let { ktpResult ->
                    if (ktpResult.isSuccess) {
                        ktpResult.getBitmapImage()?.let {
                            binding.ivKtp.setImageBitmap(it)
                        }
                        binding.tvScanKtp.text = captureOCRResult.toString()
                    } else {
                        handleError(ktpResult.errorMessage, ktpResult.errorType)
                    }
            }
        }
    }
}
    private fun takeImage() {
        getTmpFileUri().let { uri ->
            latestTmpUri = uri
            resultLauncherOwnCamera.launch(uri)
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(
            applicationContext,
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }
}