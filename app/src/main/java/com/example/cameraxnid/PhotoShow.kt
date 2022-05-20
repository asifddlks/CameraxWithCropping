package com.example.cameraxnid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.cameraxnid.databinding.ActivityPhotoShowBinding

class PhotoShow : AppCompatActivity() {
   private lateinit var binding: ActivityPhotoShowBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoShowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras
        if (extras != null){
            val data = extras.getString("CropBitMapImage")
            Glide.with(this).load(data).into(binding.ImgFrontSideOfNid)
        }

        binding.cancelBtn.setOnClickListener {
            finish()
        }

    }
}