package com.example.appjimenez
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import com.example.appjimenez.ui.theme.AppJimenezTheme


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.appjimenez.ui.web.WebAppScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) { //La interrogacion indica que ese valor puede ser null
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  //Indica el uso de toda la pantalla
        setContent {
            AppJimenezTheme {   //Aplica el tema
                WebAppScreen(url = "https://farmaciajimenez.com/")
            }
        }
    }
}


















