package com.example.appjimenez.ui.web

import android.R.attr.overScrollMode
import android.app.Activity
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout


@Composable
fun WebAppScreen(url: String) {

    val activity = (LocalActivity.current as? Activity)

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    // Estado para pull-to-refresh
    var isRefreshing by remember { mutableStateOf(false) }
    var atTop by remember { mutableStateOf(true) } // true cuando scrollY == 0



    // Manejo global del botón atrás
    // Back principal (gestión combinada)
    BackHandler(enabled = true) {
        when {
            popupWebView != null -> {
                try { popupWebView?.destroy() } catch (_: Exception) {}
                popupWebView = null
            }
            webViewRef?.canGoBack() == true -> webViewRef?.goBack()
            else -> activity?.finish()
        }
    }




    AndroidView(                        //Permite usar el WebView o MapView
        modifier = Modifier
            .fillMaxSize()        // ocupa toda la pantalla
            .systemBarsPadding(), // añade padding igual a status + nav bar

    factory = { context ->          //Parametro obligatorio de AndroidView

        // Contenedor nativo con Pull-to-Refresh
        val swipe = SwipeRefreshLayout(context).apply {
            // Opcional: esquema de colores del spinner
            setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light
            )
        }

        val web = WebView(context).apply {    //Crea un nuevo WebView y le pasa un contexto

                webViewRef = this // ⬅️ guardamos referencia a la WebView principal

                // (opcional, ayuda con el “glow” de overscroll)
                overScrollMode = View.OVER_SCROLL_ALWAYS
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
                        // Fin del refresco si veníamos de pull-to-refresh
                        swipe.isRefreshing = false
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

            // 🔑 Para WebView, el SwipeRefreshLayout no siempre sabe si puede “scrollear arriba”.
            // Con este callback le decimos: “bloquea el refresh si el WebView NO está en el tope”.
            swipe.setOnChildScrollUpCallback { _, _ ->
                (web.scrollY > 0)
            }

            // Acción de refresco (pull-to-refresh)
            swipe.setOnRefreshListener {
                val current = web.url ?: url
                web.loadUrl(current) // o web.reload()
            }

            // Móntalo
            swipe.addView(
                web,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            swipe
        },
        update = { view ->
            // 'it' es el SwipeRefreshLayout
            canGoBack = webViewRef?.canGoBack() == true
        }
    )
}