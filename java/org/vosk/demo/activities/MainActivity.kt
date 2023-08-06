package org.vosk.demo.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.vosk.demo.databinding.MainScreenBinding


class MainActivity: AppCompatActivity() {
    private lateinit var binding: MainScreenBinding
    private lateinit var callback: ActivityResultLauncher<Intent>

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = MainScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

         //Callback for get video from gallery
        callback = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result: ActivityResult ->
            if (result.resultCode == RESULT_OK){
                val videoPath = result.data?.dataString

                val intent = Intent(this, RecognizeActivity::class.java)
                intent.putExtra("videoUri", videoPath)
                startActivity(intent)
            }
        }

        binding.button.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "video/mp4" //pick only video in .mp4 format
            callback.launch(intent)
        }
    }



}