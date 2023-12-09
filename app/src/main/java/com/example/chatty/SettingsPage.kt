package com.example.chatty

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout

class SettingsPage: AppCompatActivity() {

    private lateinit var themeSettings: ConstraintLayout
    private lateinit var changePasswordSettings: ConstraintLayout
    private lateinit var deleteAccountSettings: ConstraintLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_page)

        val toolbar = findViewById<Toolbar>(R.id.toolbar3)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        themeSettings = findViewById(R.id.themeSettings)
        changePasswordSettings = findViewById(R.id.changePasswordSettings)
        deleteAccountSettings = findViewById(R.id.deleteAccountSettings)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            // Handle other menu items if needed
        }
        return super.onOptionsItemSelected(item)
    }
}