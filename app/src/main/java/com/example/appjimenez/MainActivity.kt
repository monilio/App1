package com.example.appjimenez
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import com.example.appjimenez.ui.theme.AppJimenezTheme


import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.os.Message
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.viewinterop.AndroidView

import android.app.Activity
import androidx.activity.compose.LocalActivity


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



@Composable
fun WebAppScreen(url: String) {

    val activity = (LocalActivity.current as? Activity)

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    // Manejo global del botón atrás
    BackHandler(enabled = true) {
        when {
            popupWebView != null -> {
                try { popupWebView?.destroy() } catch (_: Exception) {}
                popupWebView = null
            }
            webViewRef?.canGoBack() == true -> {
                webViewRef?.goBack()
            }
            else -> {
                activity?.finish()
            }
        }
    }


    AndroidView(                        //Permite usar el WebView o MapView
        modifier = Modifier
            .fillMaxSize()        // ocupa toda la pantalla
            .systemBarsPadding(), // añade padding igual a status + nav bar
        factory = { context ->          //Parametro obligatorio de AndroidView

            WebView(context).apply {    //Crea un nuevo WebView y le pasa un contexto

                webViewRef = this // ⬅️ guardamos referencia a la WebView principal

                /////////////////////////////////////////////////////////////////////////////////////////////////////////////
                //NAVEGACION
                /////////////////////////////////////////////////////////////////////////////////////////////////////////////

                // Asegura que la navegación suceda dentro del WebView
                webViewClient = object : WebViewClient() {  //Propiedad que controla como el WebView maneja la navegacion y crea una clase anonima que extiende WebViewClient (permite sobreescribir metodos de navegacion)
                    override fun shouldOverrideUrlLoading(  //Se sobreescribe el metodo de WebViewClient
                        view: WebView?,
                        request: WebResourceRequest?
                        //Para clickar cosas dentro de la pagina
                    ): Boolean {
                        val target = request?.url?.toString() ?: return false
                        view?.loadUrl(target)   //Si el click devuelve una URL, pasa a esta
                        return true
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        canGoBack = view?.canGoBack() == true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        canGoBack = view?.canGoBack() == true
                    }
                }

                /////////////////////////////////////////////////////////////////////////////////////////////////////////////
                //POPUPS
                /////////////////////////////////////////////////////////////////////////////////////////////////////////////

                webChromeClient = object : WebChromeClient() {

                    // ✅ Ahora mostramos el popup real en un Dialog con WebView hija
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?
                    ): Boolean {
                        if (view == null || resultMsg == null) return false

                        val child = WebView(view.context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadsImagesAutomatically = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                javaScriptCanOpenWindowsAutomatically = true
                                setSupportMultipleWindows(true)
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                mediaPlaybackRequiresUserGesture = true
                                userAgentString = WebSettings.getDefaultUserAgent(context)
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    v: WebView?,
                                    req: WebResourceRequest?
                                ): Boolean {
                                    //val t = req?.url?.toString() ?: return false
                                    //v?.loadUrl(t)
                                    return true
                                }

                                override fun onPageFinished(v: WebView?, u: String?) {
                                    super.onPageFinished(v, u)
                                    // nada
                                }


                            }

                            webChromeClient = object : WebChromeClient() {
                                // Permite que el sitio cierre el popup (window.close)
                                override fun onCloseWindow(window: WebView?) {
                                    try { popupWebView?.destroy() } catch (_: Exception) {}
                                    popupWebView = null
                                }
                            }
                        }

                        val transport = resultMsg.obj as WebView.WebViewTransport
                        transport.webView = child
                        resultMsg.sendToTarget()

                        popupWebView = child // activar el Dialog
                        return true
                    }


                    // (Opcional) Si la web usa geolocalización
                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,                                //Dominio que solicita el permiso
                        callback: GeolocationPermissions.Callback?      //Funcion que debes invocar para conceder/denegar
                    ) {
                        // Concedemos permiso solo para esta sesión (ajusta a tus necesidades/manifest)
                        callback?.invoke(origin, true, false)
                    }
                }






                /////////////////////////////////////////////////////////////////////////////////////////////////////////////
                //AJUSTES
                /////////////////////////////////////////////////////////////////////////////////////////////////////////////

                settings.apply {    //El apply permite ponerlas todas una debajo de otra
                    javaScriptEnabled = true                    //Habilita JavaScript
                    domStorageEnabled = true                    //Habilita el DOM Storage API (localStorage/sessionStorage), usado por webs modernas
                    databaseEnabled = true                      //Permite el uso de bases de datos web (HTML5 Web SQL)
                    loadsImagesAutomatically = true             //Permite que las imágenes se carguen automaticamente
                    cacheMode = WebSettings.LOAD_DEFAULT        //Establece como manejar el cache. LOAD_DEFAULT = usa el cache si esta disponible
                    javaScriptCanOpenWindowsAutomatically = true    //Permite que JavaScript abra ventanas emergentes
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE //Permite cargar contenido mixto (HTTP dentro de HTTPS), en modo de compatibilidad
                    mediaPlaybackRequiresUserGesture = true     //Permite abrir reproductores u otros medios
                    setSupportMultipleWindows(true)     //Permite los popups
                    // (Opcional) user agent más “móvil” estándar si la web filtra WebViews
                    // userAgentString = WebSettings.getDefaultUserAgent(context)
                }






                /////////////////////////////////////////////////////////////////////////////////////////////////////////////
                //COOKIES
                /////////////////////////////////////////////////////////////////////////////////////////////////////////////

                // Habilita cookies (algunas webs las requieren)
                CookieManager.getInstance().setAcceptCookie(true)                   //Acepta las cookies automaticamente
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)  //Habilita cookies de terceros en el WebView





                /////////////////////////////////////////////////////////////////////////////////////////////////////////////
                //RENDER
                /////////////////////////////////////////////////////////////////////////////////////////////////////////////

                // Render por HW (suele estar ya activo)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)    //Cambia el modo de renderizado de la vista, fuerza renderizado por GPU en lugar de software






                /////////////////////////////////////////////////////////////////////////////////////////////////////////////
                //LOAD
                /////////////////////////////////////////////////////////////////////////////////////////////////////////////

                loadUrl(url)        //Carga el url (Esta es la funcion principal)
            }


        },
        update = { view ->
            canGoBack = view.canGoBack()
        }
    )


}

























/*
// ⬇️ Dialog que muestra la WebView del popup cuando exista
val popup = popupWebView
if (popup != null) {
    Dialog(
        onDismissRequest = {
            try { popup.stopLoading() } catch (_: Exception) {}
            try { popup.destroy() } catch (_: Exception) {}
            popupWebView = null
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true // ✅ usa el ancho por defecto (centrado)
        )
    ) {
        // Capa full-screen solo para centrar el “card” del popup
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            // “Card” centrada con límites de tamaño (se ve como modal)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)         // hasta 90% del ancho
                    .fillMaxHeight(0.8f)        // hasta 80% del alto
                    .sizeIn(maxWidth = 600.dp,  // y como máximo 600x700dp
                        maxHeight = 700.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { popup }
                )
            }
        }
    }
}

 */