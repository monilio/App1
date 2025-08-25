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
fun WebAppScreen(url: String) {     //Toma de input un valor String que será el url o link al que se acceda al iniciar la app


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //DECLARACION DE VARIABLES
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    val activity = (LocalActivity.current as? Activity)                 //Introduce la Activity en una variable local
                                                                        //Una Activity es una pantalla de la aplicacion
                                                                        //La interrogacion permite que el valor pueda ser nulo
                                                                        //Se usara por ejemplo en el BackHandler para cerrar la pantalla

    var webViewRef by remember { mutableStateOf<WebView?>(null) }       //Guarda en una variable local una referencia nullable (que puede ser null) a la WebView principal, tambien usada por el BackHandler
                                                                        //La WebView es un objeto que tiene sus propias funciones y caracteristicas

    var popupWebView by remember { mutableStateOf<WebView?>(null) }     //Igual que la anterior, pero con una WebView hija (los popups)
    var canGoBack by remember { mutableStateOf(false) }                 //Guarda en una variable local booleana que representa si hay una pagina a la que volver





    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //MANEJO BOTON ATRAS
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    BackHandler(enabled = true) {                                           //Funcion que establece el uso de boton de atras del movil
        when {                                                              //Bucle when para que siempre este activo y atento para ver si ocurre lo de dentro de las llaves
            popupWebView != null -> {                                       //Si hay un popup abierto
                try { popupWebView?.destroy() } catch (_: Exception) {}     //Destruye el objeto WebView del popup (la ? cubre el caso de que ya fuera null)
                popupWebView = null                                         //Limpias la referencia y la dejas vacia (es decir, el WebView de popup se vuelve null)
            }
            webViewRef?.canGoBack() == true -> webViewRef?.goBack()         //Si esta WebView tiene una WebView anterior a la que volver, vuelve a ella
            else -> activity?.finish()                                      //Si no habia ni popup ni pagina anterior, cierra la actividad (la pantalla) y por tanto cierra la app
        }
    }





    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //DEFINICION DEL ANDROIDVIEW
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    AndroidView(                        //Es un composable de Jetpack Compose que permite insertar un View clasico de Android como: WebView, MapView o TextView
        modifier = Modifier             //Declara la lista de modificadores, que determinaran caracteristicas fisicas (en este caso del AndroidView)
            .fillMaxSize()              //Hace que el AndroidView ocupe toda la pantalla
            .systemBarsPadding(),       //Hace que la AndroidView pare antes de ser cubierta por las barras superior e inferior del propio movil (las de notificaciones y navegacion respectivamente)

    factory = { context ->              //Parametro obligatorio de AndroidView que debera usar el WebView


        val swipe = SwipeRefreshLayout(context).apply {     //Utiliza el context para crear un SwipeRefreshLayout
                                                            //Este es un ViewGroup que contiene al WebView y detecta el gesto de arrasstrar hacia abajo para lanzar un refresh
                                                            //Se configurara mas abajo con setOnRefreshListener

            setColorSchemeResources(                        //Configura los colores del circulo que gira al hacer refresh (opcional)
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light
            )
        }



        val web = WebView(context).apply {                  //Crea un nuevo WebView y le pasa el contexto obligatorio, a su vez lo guarda en una variable local llamada web
                                                            //Este es un objeto que usara una url o link para mostrar una pagina web en al AndroidView

                webViewRef = this                           //Guarda la referencia a la WebView en la variable local que definimos anteriormente
                overScrollMode = View.OVER_SCROLL_ALWAYS    //Muestra el efecto tipico cuando tratas de deslizar mas alla del limite de la pantalla (opcional)



                webViewClient = object : WebViewClient() {  //Propiedad de un objeto WebView que establece el controlador del navegador, en este caso sera el objeto WebViewClient
                                                            //Todas las llamadas realcionadas con la navegacion, como abrir links, pasaran por este objeto
                                                            //Es un objeto que permite controlar las cosas relacionadas con la navegacion

                    override fun shouldOverrideUrlLoading(                      //Se sobreescribe el metodo de WebViewClient si se da a un link dentro de la pagina
                        view: WebView?,                                         //La pagina que se esta intentando cargar
                        request: WebResourceRequest?                            //Detalles de la nueva pagina, como el link y otra info
                    ): Boolean {                                                //Situacion de verdadero falso
                                                                                //Verdadero es que el objeto maneje la interaccion y falso que no haga nada y se las apañe el propio WebView

                        val target = request?.url?.toString() ?: return false   //Coje la url y lo transforma en String, si fuera null, devuelve Falso (El controlador no hace nada y se debe ocupar el WebView)
                        view?.loadUrl(target)                                   //Coje el link clickado (variable view) y lo carga
                        return true                                             //Devuelve True, indicando al WebView que no haga nada, que ya se ha encargado el controlador cargando el link
                    }

                    override fun onPageStarted(                     //Metodo del WebViewClient que se ejecuta cuando el WebView empieza a cargar una pagina
                        view: WebView?,                             //WebView que esta cargando la pagina
                        url: String?,                               //Url de esta pagina
                        favicon: android.graphics.Bitmap?) {        //Icono de la pagina

                        super.onPageStarted(view, url, favicon)     //LLama al comportamiento por defecto del WebViewClient con los parametros como inputs
                        canGoBack = view?.canGoBack() == true       //Actualiza el estado canGoBack, indicando que si se ha cargado una nueva pagina, habra una a la que volver
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {     //Metodo del WebViewClient que se ejecuta cuando la pagina termina de cargarse completamente
                        super.onPageFinished(view, url)                             //Llama al comportamiento por defecto del WebViewClient con los parametros como inputs
                        canGoBack = view?.canGoBack() == true                       //Actualiza una vez mas por si acaso el historial cambia al terminar de cargarse la pagina
                        swipe.isRefreshing = false                                  //Si has llegado a esta pagina refresheando, al terminar de cargar la pagina, cambia el estado de refreshing a false para indicar que el refresh ha finalizado
                    }
                }




                webChromeClient = object : WebChromeClient() {  //Otra propiedad del WebView, parecida al webViewClient, pero con una funcion diferente
                                                                //El otro controlaba la navegacion, este controla funciones avanzadas del navegador, como popus, JavaScript o geolocalizacion
                                                                //Una vez toma como valor un objeto integro conocido como WebChromeClient

                    override fun onCreateWindow(    //Metodo que se llama al tratar de abrir una ventana nueva
                                                    //Por defecto WebView no abre ventanas nuevas
                        view: WebView?,             //WebView principal que pidio abrir la ventana
                        isDialog: Boolean,          //Boolean que indica si la ventana abierta deberia mostrarse como un dialogo modal ()??
                        isUserGesture: Boolean,     //True si fuer por accion del usuario, False si se lanzo automaticamente
                        resultMsg: Message?         //Mensaje especial que permite transportar la nueva WebView creada de vuelta al sistema, es decir, el sobre en el que va la nueva ventana al entregarla al sistema
                    ): Boolean {                    //True es ya se ha gestionado la creacion de la nueva ventana, False es no se gestiono e ingora el intento de abrirla
                        if (view == null || resultMsg == null) return false     //Si el view principal es nulo o no hay forma de entregar esta nueva ventana al sistema, toma False e ignora el intento

                        val child = WebView(view.context).apply {               //Crea un nuevo objeto WebView usando el mismo context que el padre y lo guarda en una variable llamada child (que representa el popup)
                                                                                //Es decir, fabrica un navegador independiente dentro de tu app exclusivo para la ventana emergente

                            settings.apply {                                                        //Comienza la lista de ajustes de child
                                javaScriptEnabled = true                                            //Permite que la pagina ejecute codigo JavaScript
                                domStorageEnabled = true                                            //Habilita el almacenamiento web (localStorage y sessionStorage)
                                databaseEnabled = true                                              //Permite bases de datos web (HTML5 Web SQL)
                                loadsImagesAutomatically = true                                     //Las imagenes se cargan sin intervencion extra
                                cacheMode = WebSettings.LOAD_DEFAULT                                //Usa el cache si esta disponible
                                javaScriptCanOpenWindowsAutomatically = true                        //Permite que el propio JavaScript cree popups
                                setSupportMultipleWindows(true)                                     //Habilita multiples ventanas dentro de este WebView
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE     //Permite mezclar HTTP y HTTPS
                                mediaPlaybackRequiresUserGesture = true                             //Evite el autoplay y exige la accion del usuario para reproducir medios
                                userAgentString = WebSettings.getDefaultUserAgent(context)          //Usa el User-Agent (identificacion del navegador) estandar de Android/Chrome, asi la web no te detecta como un navegador raro
                                useWideViewPort = true                                              //Interpreta el viewport como en un navegador normal, no como uno fijo, es decir, coge la forma que le diga la pagina web
                                loadWithOverviewMode = true                                         //Ajusta el contenido al ancho de la pantalla
                            }

                            setLayerType(View.LAYER_TYPE_HARDWARE, null)    //Fuerza el renderizado por GPU en lugar de por software (mejor rendimiento)

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