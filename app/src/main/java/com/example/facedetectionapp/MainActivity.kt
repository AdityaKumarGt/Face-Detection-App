package com.example.facedetectionapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.bumptech.glide.request.target.Target


class MainActivity : AppCompatActivity(), FaceDetectionCallback  {
    private val CAMERA_PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        val button = findViewById<Button>(R.id.button)
        val laterButton = findViewById<Button>(R.id.laterBtn)
        button.setOnClickListener {
            checkCameraPermissionAndLaunchCamera()
        }
        laterButton.setOnClickListener {
            checkCameraPermissionAndLaunchCamera()
        }
    }



    private fun checkCameraPermissionAndLaunchCamera() {
        // Check if the camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, proceed with the camera operation
            startCameraIntent()
        } else {
            // Request camera permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Function to start the camera intent
    private fun startCameraIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            someActivityResultLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
        }
    }


    private val someActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val bitmap = data?.extras?.get("data") as? Bitmap

                if (bitmap != null) {
                    detectFace(bitmap,this)
                }
            }
        }


    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted, proceed with the camera operation
                    startCameraIntent()
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun detectFace(bitmap: Bitmap, callback: FaceDetectionCallback) {
        if (bitmap == null) {
            Toast.makeText(this, "Bitmap is null", Toast.LENGTH_SHORT).show()
            callback.onFaceDetectionResult("")
        } else {


            val highAccuracyOpts = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()

            val detector = FaceDetection.getClient(highAccuracyOpts)
            val image = InputImage.fromBitmap(bitmap, 0)

            val result = detector.process(image)
                .addOnSuccessListener { faces ->
                    // Task completed successfully
                   var resultText = " "
                    var i = 1

                    for (face in faces) {
                        resultText = "· Faces: $i" +
                                "\n· Smile: ${String.format("%.2f", face.smilingProbability?.times(100)!!.toFloat())}%" +
                                "\n· Left eye open: ${String.format("%.2f", face.leftEyeOpenProbability?.times(100)!!.toFloat())}%" +
                                "\n· Right eye open: ${String.format("%.2f", face.rightEyeOpenProbability?.times(100)!!.toFloat())}%" +
                                "\n· face.boundingBox ${face.boundingBox}" +
                                "\n· headEulerAngleY: ${String.format("%.2f", face.headEulerAngleY.times(100).toFloat())}" +
                                "\n· headEulerAngleZ:${String.format("%.2f", face.headEulerAngleZ.times(100).toFloat())}" +
                               "\n· Face type: ${face.getLandmark(FaceLandmark.LEFT_EAR)}" +



                        i++

                    }

                    if (faces.isEmpty()) {
                        Toast.makeText(this, "No face detected", Toast.LENGTH_SHORT).show()
                        callback.onFaceDetectionResult("")
                    } else {
                        callback.onFaceDetectionResult(resultText)
                        val resultLayout = findViewById<CardView>(R.id.resultLayout)
                        resultLayout.visibility = View.VISIBLE

                        val image = findViewById<ImageView>(R.id.image)
                        Glide.with(this)
                            .load(bitmap).into(image)
                        

                        val button = findViewById<Button>(R.id.button)
                        button.visibility = View.GONE

                        val laterButton = findViewById<Button>(R.id.laterBtn)
                        laterButton.visibility = View.VISIBLE


                    }

                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    Toast.makeText(this, "Detection failed", Toast.LENGTH_SHORT).show()
                    callback.onFaceDetectionResult("") // Notify callback with an empty result

                }

        }
    }

    override fun onFaceDetectionResult(result: String) {
        val resultText = findViewById<TextView>(R.id.textview)
        resultText.text = result
    }
}