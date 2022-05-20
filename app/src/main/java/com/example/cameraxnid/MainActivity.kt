package com.example.cameraxnid
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface.ROTATION_0
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.example.cameraxnid.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null
    private lateinit var imgCaptureExecutor: ExecutorService
    private lateinit var outputDirectory: File

   private  var flashMode: Int = FLASH_MODE_AUTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cameraPermissionResult =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
                if (permissionGranted) {
                    startCamera()
                } else {
                    Snackbar.make(
                        binding.root,
                        "The camera permission is necessary",
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                }
            }

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProvider = cameraProviderFuture.get()
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        outputDirectory = getOutputDirectory()
        imgCaptureExecutor = Executors.newSingleThreadExecutor()
        cameraPermissionResult.launch(android.Manifest.permission.CAMERA)

        binding.btnTakePicture.setOnClickListener {
            takePhoto()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                animateFlash()
            }
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({

            val viewFinder = binding.preview
            val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
            val aspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            val rotation = viewFinder.display?.rotation

            val currentFlashModeState = getFlashMode()

            preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation!!).build()
                .also {
                    it.setSurfaceProvider(binding.preview.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation)
                .setFlashMode(currentFlashModeState)
                .build()

            imageAnalysis = ImageAnalysis.Builder().apply {
                setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            }.build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.d(TAG, "Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }



    private fun getFlashMode(): Int {
        binding.btnFlashOnOf.setOnClickListener {
            when (flashMode) {
                ImageCapture.FLASH_MODE_OFF ->{
                    flashMode = ImageCapture.FLASH_MODE_ON
                    binding.btnFlashOnOf.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_flash_on))
                }
                ImageCapture.FLASH_MODE_ON ->{
                    flashMode = ImageCapture.FLASH_MODE_AUTO
                    binding.btnFlashOnOf.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_flash_auto))
                }
                FLASH_MODE_AUTO ->{
                    flashMode = ImageCapture.FLASH_MODE_OFF
                    binding.btnFlashOnOf.setImageDrawable(resources.getDrawable(R.drawable.ic_flash_off))
                }

            }
            imageCapture?.flashMode = flashMode
        }

        return flashMode
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                @RequiresApi(Build.VERSION_CODES.R)
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)

                    val inputData = contentResolver.openInputStream(savedUri)?.readBytes()
                    val capturedImageBitmap =
                        BitmapFactory.decodeByteArray(inputData, 0, inputData!!.size)

                    val capturedImageCroppedBitmap: Bitmap?
                    val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay

                    val imageRotation = getRotation(photoFile)

                    if (display.rotation == ROTATION_0) {
                        val matrix = Matrix()
                        matrix.postRotate(imageRotation)

                        val capturedImageRotatedBitmap = Bitmap.createBitmap(
                            capturedImageBitmap,
                            0,
                            0,
                            capturedImageBitmap.width,
                            capturedImageBitmap.height,
                            matrix,
                            true
                        )

                        //calculate aspect ratio
                        val width: Int = capturedImageRotatedBitmap.width
                        val height: Int = capturedImageRotatedBitmap.height

                        val ratioConstraint: Int = width / 36

                        val widthRatio = 16 * ratioConstraint
                        val heightRatio = 9 * ratioConstraint

                        val leftX1: Int = width / 2 - widthRatio
                        val rightX2: Int = width / 2 + widthRatio
                        val topY1: Int = height / 2 - heightRatio
                        val bottomY2: Int = height / 2 + heightRatio

                        val cropWidth = (rightX2 - leftX1)
                        val cropHeight = (bottomY2 - topY1)

                        //calculate position and size for cropping
                        val cropStartX = leftX1
                        val cropStartY = topY1
                        val cropWidthX = cropWidth
                        val cropHeightY = cropHeight

                        //check limits and make crop
                        capturedImageCroppedBitmap =
                            if (cropStartX + cropWidthX <= capturedImageRotatedBitmap.width && cropStartY + cropHeightY <= capturedImageRotatedBitmap.height) {
                                Bitmap.createBitmap(
                                    capturedImageRotatedBitmap,
                                    cropStartX,
                                    cropStartY,
                                    cropWidthX,
                                    cropHeightY
                                )
                            } else {
                                null
                            }

                        loadImage(capturedImageCroppedBitmap)
                    }
                }
            })
    }

    private fun loadImage(bitmap: Bitmap?) {
        Glide.with(this@MainActivity).load(bitmap).into(binding.ImgFrontSideOfNid)
        binding.constraint.visibility = View.GONE
        binding.constraintCapture.visibility = View.VISIBLE
        cameraProvider.unbind(preview)
        binding.cancelBtn.setOnClickListener {
            binding.constraint.visibility = View.VISIBLE
            binding.constraintCapture.visibility = View.GONE
            startCamera()
        }
    }

    private fun getRotation(photoFile: File): Float {
        var orientation = 0

        val exif = ExifInterface(photoFile)

        val exifOrientation: Int = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_270 -> orientation = 270
            ExifInterface.ORIENTATION_ROTATE_180 -> orientation = 180
            ExifInterface.ORIENTATION_ROTATE_90 -> orientation = 90
            ExifInterface.ORIENTATION_NORMAL -> orientation = 0
            else -> {
            }
        }
        return orientation.toFloat()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull().let {
            File(
                it,
                "Camerax"
                // resources.getString(R.string.app_name)
            ).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    companion object {
        private const val TAG = "CameraXDemo"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0 // aspect ratio 4x3
        private const val RATIO_16_9_VALUE = 16.0 / 9.0 // aspect ratio 16x9
    }


}