package com.example.chatty

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class MainPage: AppCompatActivity() {
    private lateinit var optionsIcon: ImageView
    private lateinit var optionsLayout: ConstraintLayout
    private lateinit var profileLayout: ConstraintLayout
    private lateinit var settingsLayout: ConstraintLayout

    // Sets visibility of options
    private var showOptions = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)

        optionsIcon = findViewById(R.id.optionsIcon)
        optionsLayout = findViewById(R.id.optionsLayout)
        profileLayout = findViewById(R.id.profileLayout)
        settingsLayout = findViewById(R.id.settingsLayout)

        // Show/hide Options
        optionsIcon.setOnClickListener{
            if(!showOptions){
                optionsLayout.visibility = View.VISIBLE
                showOptions = true
            }
            else{
                optionsLayout.visibility = View.GONE
                showOptions = false
            }
        }

        // Profile option in Options
        profileLayout.setOnClickListener{
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }

        // Settings option in Options
        settingsLayout.setOnClickListener{
            val intent = Intent(this, SettingsPage::class.java)
            startActivity(intent)
        }
    }
}