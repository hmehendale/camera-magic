package com.example.cameramagic

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cameramagic.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Log.e("MAGIC/CAMERA", "No camera detected!")
            Toast.makeText(this, "No camera detected", Toast.LENGTH_LONG).show()
        }

        if (savedInstanceState == null) {
            Log.d("MAGIC/CAMERA", "Loading Camera fragment")
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_camera, CameraFragment())
                .commit()
        }
    }
}
