package com.example.appjimenez.ui.web

import android.app.Activity
import android.content.ClipData
import android.content.Context
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
import android.content.Intent
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.JavascriptInterface
import android.widget.Toast

import android.content.ClipboardManager

/////////////////////////////////////////////////////////////////////////////////////////////////////////////
//DECLARACION DE VARIABLES EXTERNAS
/////////////////////////////////////////////////////////////////////////////////////////////////////////////

private const val ERROR_URL = "file:///android_asset/error.html"    //Ruta hacia el html con la pantalla de error de conexion


private fun isErrorAsset(url: String?):     //El prefijo is indica que sera una funcion booleana. El input es un string que puede ser null
                                            //Esta funcion nos dira si el link lleva a un error
        Boolean = url == null               //Comprueba si url es null y da un True or False al respecto
        || url == "about:blank"             //O comprueba si es un link a una pagina en blanco
        || url.startsWith(ERROR_URL)        //O comprueba si el link es a la pantalla de error de conexion


private fun WebView.goBackSkippingErrorsAndDuplicates(      //Funcion de extension sobre el WebView (se puede meter en el WebView)
    isError: (String?) -> Boolean                           //Esta funcion recibe una funcion que recibe un String de input
): Boolean {                                                //Indica que recibe un String y devuelve un Boolean
    val list = copyBackForwardList() ?: return false        //Obtiene el historial de navegacion, si fuera nulo, devuelve False
    val curIndex = list.currentIndex                        //Obtiene el indice actual dentro del historial de navegacion
    val curUrl = list.currentItem?.url                      //Obtiene el url asociado al indice actual dentro del historial de navegacion

    var target = curIndex - 1                               //Obtiene el indice anterior al actual dentro del historial de navegacion
    while (target >= 0) {                                   //Siempre que el indice anterior al actual exista (sea 0 o superior)
        val item = list.getItemAtIndex(target)              //Se introduce en una variable llamada item la informacion de ese termino, es decir, no solo su indice, sino tambien su url
        val url = item?.url                                 //Se mete esa url en una variable

        val isDuplicateOfCurrent = (url == curUrl)          //Valor que es True si la url de la pagina anterior es igual a la de la pagina actual
        val isJunk = isError(url) || url == "about:blank"   //Valor que es True si la url de la pagina anterior es la de la pagina de error de conexion o una en blanco
                                                            //Para comprobar si es la pagina de error de conexion hace uso de la funcion input, que sea isErrorAsset

        if (!isJunk && !isDuplicateOfCurrent) break         //Si no es ni duplicada ni la de error, el bucle para, estamos ante una url valida
        target--                                            //Si es duplicada o del error, se pasa a la siguiente url de la lista
    }

    return if (target >= 0) {                               //Si se encontro un indice con una url valida
        goBackOrForward(target - curIndex)                  //Se dan los pasos para llegar al indice de esa url valida y se va a ella
        true                                                //Se devuelve True, indicando que se ha ido a una url valida
    } else {                                                //Si no se han encontrado url validas y la lista del historial ha terminado
        false                                               //Se devuelve False, indicando que no se ha encontrado una url valida en el historial
    }
}



/////////////////////////////////////////////////////////////////////////////////////////////////////////////
//COMPOSABLE
/////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Composable
fun WebAppScreen(url: String) {     //Toma de input un valor String que será el url o link al que se acceda al iniciar la app


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //declaracion de variables internas

    val activity = (LocalActivity.current as? Activity)                 //Introduce la Activity en una variable local
                                                                        //Una Activity es una pantalla de la aplicacion
                                                                        //La interrogacion permite que el valor pueda ser nulo
                                                                        //Se usara por ejemplo en el BackHandler para cerrar la pantalla

    var webViewRef by remember { mutableStateOf<WebView?>(null) }       //Guarda en una variable local una referencia nullable (que puede ser null) a la WebView principal, tambien usada por el BackHandler
                                                                        //La WebView es un objeto que tiene sus propias funciones y caracteristicas

    var popupWebView by remember { mutableStateOf<WebView?>(null) }     //Igual que la anterior, pero con una WebView hija (los popups)
    var canGoBack by remember { mutableStateOf(false) }                 //Guarda en una variable local booleana que representa si hay una pagina a la que volver

    var lastFailedUrlLocal: String? = null      //Variable local en la que se guarda un String
                                                //Sera en la que se guarde la url a la que se trataba de llegar cuando fallo la conexion


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////// 40
    //manejo boton atras

    BackHandler(enabled = true) {                                           //Funcion que establece el uso de boton de atras del movil
        when {                                                              //Bucle when para que siempre este activo y atento para ver si ocurre lo de dentro de las llaves
            popupWebView != null -> {                                       //Si hay un popup abierto
                try { popupWebView?.destroy() } catch (_: Exception) {}     //Destruye el objeto WebView del popup (la ? cubre el caso de que ya fuera null)
                popupWebView = null                                         //Limpias la referencia y la dejas vacia (es decir, el WebView de popup se vuelve null)
            }
            webViewRef?.canGoBack() == true -> {                            //Si la pagina actual (no popup) tiene una pagina anterior a la que volver
                val jumped = webViewRef!!.                                  //Mete en una constante booleana si se ha hecho algun salto de url, es decir, si ha podido volver bien a una pagina anterior valida
                    goBackSkippingErrorsAndDuplicates(::isErrorAsset)       //Llama a la funcion de busqueda de url valida
                if (!jumped) webViewRef!!.                                  //Si no ha podido encontrar una url valida
                    goBack()                                                //Simplemente fuerza el ir a atras 1 posicion (esto no deberia darse nunca creo)
                                                                            //La !! indica que si el valor es nulo, la app crashea para no liarla
            }
            else -> activity?.finish()                                      //Si no hay popup y no hay una pagina anterior a la que ir, sale de la aplicacion
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////// 52
    //definicion del AndroidView

    AndroidView(                        //Es un composable de Jetpack Compose que permite insertar un View clasico de Android como: WebView, MapView o TextView
        modifier = Modifier             //Declara la lista de modificadores, que determinaran caracteristicas fisicas (en este caso del AndroidView)
            .fillMaxSize()              //Hace que el AndroidView ocupe toda la pantalla
            .systemBarsPadding(),       //Hace que la AndroidView pare antes de ser cubierta por las barras superior e inferior del propio movil (las de notificaciones y navegacion respectivamente)

    factory = { context ->              //Parametro obligatorio de AndroidView que debera usar el WebView


        val swipe = SwipeRefreshLayout(context).apply {     //Utiliza el context para crear un SwipeRefreshLayout
            //Este es un ViewGroup (contenedor de vistas) que detecta el gesto de arrasstrar hacia abajo para lanzar un refresh
            //Lo de contenedor es porque es un marco que esta sobre el WebView principal aparte de tener su propio visual
            //Se configurara mas abajo con setOnRefreshListener

            setColorSchemeResources(                        //Configura los colores del circulo que gira al hacer refresh (opcional)
                android.R.color.holo_orange_dark,           //Colores usados
                android.R.color.holo_red_light,
                android.R.color.holo_orange_light
            )
        }

        val web = WebView(context).apply {      //Crea un nuevo WebView y le pasa el contexto obligatorio, a su vez lo guarda en una variable local llamada web
                                                //Este es un objeto que usara una url o link para mostrar una pagina web en el AndroidView  65

            val initialUrl = url                        //Guarda en una constante local la url principal/inicial
            val wv = this                               //Guarda en una constante local el propio objeto WebView actual, como referencia
            webViewRef = this                           //Guarda la referencia en la WebView en la variable local que definimos anteriormente (esta si es variable)
            overScrollMode = View.OVER_SCROLL_ALWAYS    //Muestra el efecto tipico cuando tratas de deslizar mas alla del limite de la pantalla (opcional)


            setOnLongClickListener { v ->                                                                       //Funcion para copiar links manteniendo pulsado
                val hitTestResult = (v as WebView).hitTestResult
                if (hitTestResult.type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                    val linkUrl = hitTestResult.extra
                    // Aquí ya tienes el enlace que el usuario mantuvo pulsado
                    // Ejemplo: copiar al portapapeles
                    val clipboard = v.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("URL", linkUrl))
                    Toast.makeText(v.context, "Enlace copiado", Toast.LENGTH_SHORT).show()
                    true
                } else {
                    false
                }
            }

            webViewClient = object : WebViewClient() {  //Propiedad de un objeto WebView que establece el controlador del navegador, en este caso sera el objeto WebViewClient
                                                        //Todas las llamadas realcionadas con la navegacion, como abrir links, pasaran por este objeto
                                                        //Es un objeto que permite controlar las cosas relacionadas con la navegacion. Es un controlador

                override fun shouldOverrideUrlLoading(                      //Se sobreescribe el metodo de WebViewClient si se da a un link dentro de la pagina
                    view: WebView?,                                         //La pagina que se esta intentando cargar
                    request: WebResourceRequest?                            //Detalles de la nueva pagina, como el link y otra info
                ): Boolean {                                                //Situacion de verdadero falso
                                                                            //Verdadero es que el objeto maneje la interaccion y falso que no haga nada y se las arregle el propio WebView

                    val uri = request?.url ?: return false                  //Trata de obtener la url, si no la consigue, devuelve False
                    val scheme = uri.scheme ?: return false                 //Extrae la info de url

                    if (scheme == "http" || scheme == "https") return false //Si es una web normal directamente que se ocupe el WebView porque lo tiene facil

                    return try {                                            //Si es un link especial, ya lo manjea el controlador
                        val intent = Intent(Intent.ACTION_VIEW, uri)        //Declara la intencion de abrirla
                        view?.context?.startActivity(intent)                //Se inicia la intencion de abrirla
                        true                                                //Si funciona se devuelve True
                    } catch (_: Exception) {                                //Si falla
                        false                                               //Devuelve False
                    }
                }

                override fun onPageStarted(                     //Metodo del WebViewClient que se ejecuta cuando el WebView empieza a cargar una pagina
                    view: WebView?,                             //WebView que esta cargando la pagina
                    url: String?,                               //Url de esta pagina
                    favicon: android.graphics.Bitmap?) {        //Icono de la pagina

                    super.onPageStarted(view, url, favicon)     //LLama al comportamiento por defecto del WebViewClient con los parametros como inputs

                    if (url != null && !isErrorAsset(url)) {    //Si la url no es nula y no es la de la pagina de error de conexion o una en blanco
                        lastFailedUrlLocal = url                //Esta url se convierte en la ultima url valida (la cual servira guardarla si hay error de conexion)
                    }

                    canGoBack = view?.canGoBack() == true       //Actualiza el estado canGoBack, indicando que si se ha cargado una nueva pagina, habra una a la que volver
                }

                override fun onPageFinished(view: WebView?, url: String?) {     //Metodo del WebViewClient que se ejecuta cuando la pagina termina de cargarse completamente
                    super.onPageFinished(view, url)                             //Llama al comportamiento por defecto del WebViewClient con los parametros como inputs

                    canGoBack = view?.canGoBack() == true                       //Actualiza una vez mas por si acaso el historial cambia al terminar de cargarse la pagina
                    swipe.isRefreshing = false                                  //Si has llegado a esta pagina refresheando, al terminar de cargar la pagina, cambia el estado de refreshing a false para indicar que el refresh ha finalizado
                    CookieManager.getInstance().flush()                         //Fuerza a escribir las cookies en el disco para que no te las vuelva a pedir
                }

                override fun onReceivedError(                               //Sobreescribe en el WebViewClient si se ha encontrado un error (falta de conexion)
                    view: WebView?,                                         //Input de objeto WebView en el que ha ocurrido el error
                    request: WebResourceRequest?,                           //Input de la url fallida
                    error: WebResourceError?                                //Input del tipo de error
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   //Si estas >API 23
                        if (request?.isForMainFrame == true) {              //Si el error es de la pagina principal que se trata de ver (no de cosas como anuncios)
                            lastFailedUrlLocal  = request.url?.toString()   //Guarda localmente la url a la que se trataba de acceder cuando ocurrió el error
                            view?.loadUrl(ERROR_URL)                        //Carga la pagina de error de conexion
                        }
                    } else {                                                //Si el error no es de la pagina principal (no deberia darse este caso)
                        view?.loadUrl(ERROR_URL)                            //Se pone la pantalla de error de conexion igualmente
                    }
                }

                override fun onReceivedHttpError(                                   //Funcion que salta cuando la respuesta HTTP del WebViewClient llega con un error (404/500/etc.)
                                                                                    //Es decir, esta trata errores, pero no de conexion, sino de otro tipo

                    view: WebView?,                                                 //Input de objeto WebView en el que ha ocurrido el error
                    request: WebResourceRequest?,                                   //Input de la url fallida
                    errorResponse: android.webkit.WebResourceResponse?              //Input de los metadatos de la respuesta al error
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {    //Si estas >API 21
                        if (request?.isForMainFrame == true) {                      //Si el error es de la pagina principal que se trata de ver (no de cosas como anuncios)
                            lastFailedUrlLocal = request.url?.toString()            //Guarda localmente la url a la que se trataba de acceder cuando ocurrió el error
                            view?.loadUrl(ERROR_URL)                                //Carga la pagina de error de conexion (podria hacerse una personalizada)
                        }
                    }
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
                            offscreenPreRaster = true
                            //offscreenPreRaster = true
                        }

                        setLayerType(View.LAYER_TYPE_HARDWARE, null)    //Fuerza el renderizado por GPU en lugar de por software (mejor rendimiento)

                        webViewClient = object : WebViewClient() {              //Este es el cliente de naveegacion pero para el child (al igual que se hizo con el WebView padre)

                            override fun onPageStarted(                     //Metodo del WebViewClient que se ejecuta cuando el WebView empieza a cargar una pagina
                                v: WebView?,                                //WebView que esta cargando la pagina
                                url: String?,                               //Url de esta pagina
                                favicon: android.graphics.Bitmap?) {        //Icono de la pagina
                                if (url != null) view.loadUrl(url)          //Redirige la URL del popup a la WebView principal
                                try { destroy() } catch (_: Exception) {}   //Limpia el child para evitar fugas y estados raros
                                popupWebView = null                         //Pasa el valor de popupWebView de vuelta a null
                            }

                            override fun onPageFinished(v: WebView?, u: String?) {  //Se ejecuta cuando termina de cargar la pagina

                                super.onPageFinished(v, u)                          //Se llama a la implementacion por defecto

                                canGoBack = v?.canGoBack() == true                  //Actualiza una vez mas por si acaso el historial cambia al terminar de cargarse la pagina
                                swipe.isRefreshing = false                          //Si has llegado a esta pagina refresheando, al terminar de cargar la pagina, cambia el estado de refreshing a false para indicar que el refresh ha finalizado


                            }

                        }

                        webChromeClient = object : WebChromeClient() {                      //Funciones avanzadas del navegador pero para el child

                            override fun onCloseWindow(window: WebView?) {                  //Cuando se cierra esta ventana
                                try { popupWebView?.destroy() } catch (_: Exception) {}     //Se destruye el popupWebView para liberar memoria
                                popupWebView = null                                         //Pasa el valor de popupWebView a null al ya no haber nada
                            }
                        }
                    }


                    val transport = resultMsg.obj as WebView.WebViewTransport   //Dentro del sobre vacio que era el resultMsg se introduce el objeto a transportar
                    transport.webView = child                                   //Este objeto es el child
                    resultMsg.sendToTarget()                                    //El sobre se envia al WebView padre

                    return true                                                 //Se devuelve True, indicando que ya se ha gestionado esta ventana
                }

                override fun onGeolocationPermissionsShowPrompt(    //Habilita la geolocalización (opcional), ocurre cuando el WebView pide acceso a la localización usando JavaScript
                    origin: String?,                                //Dominio que solicita el permiso
                    callback: GeolocationPermissions.Callback?      //Funcion que debes invocar para conceder/denegar el permiso
                ) {
                    callback?.invoke(origin, true, false)           //Si se ha pedido la ubicacion, se invoca la funcion que la permite
                                                                    //Los inputs son el dominio que la pidio, True que indica que se concede y False que indica que no se recuerde esta decision si se cierra y vuelve a abrir la app
                }
            }


            settings.apply {    //El apply permite ponerlas todas una debajo de otra
                javaScriptEnabled = true                                        //Permite que la pagina ejecute codigo JavaScript
                domStorageEnabled = true                                        //Habilita el almacenamiento web (localStorage y sessionStorage)
                databaseEnabled = true                                          //Permite bases de datos web (HTML5 Web SQL)
                loadsImagesAutomatically = true                                 //Permite que las imágenes se carguen automaticamente
                cacheMode = WebSettings.LOAD_DEFAULT                            //Establece como manejar el cache. LOAD_DEFAULT = usa el cache si esta disponible
                javaScriptCanOpenWindowsAutomatically = true                    //Permite que JavaScript abra ventanas emergentes
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE //Permite cargar contenido mixto (HTTP dentro de HTTPS), en modo de compatibilidad
                mediaPlaybackRequiresUserGesture = true                         //Permite abrir reproductores u otros medios solo mediante accion de usuario
                setSupportMultipleWindows(true)                                 //Permite los popups
                useWideViewPort = true                                          //Interpreta el viewport como en un navegador normal, no como uno fijo, es decir, coge la forma que le diga la pagina web
                loadWithOverviewMode = true                                     //Ajusta el contenido al ancho de la pantalla
                userAgentString = WebSettings.getDefaultUserAgent(context)          //Usa el User-Agent (identificacion del navegador) estandar de Android/Chrome, asi la web no te detecta como un navegador raro
                offscreenPreRaster = true
            }


            CookieManager.getInstance().setAcceptCookie(true)                   //Habilita las cookies en general
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)  //Habilita cookies de terceros en el WebView


            setLayerType(View.LAYER_TYPE_HARDWARE, null)    //Fuerza el renderizado a traves de la GPU para aumentar el rendimiento de este


            addJavascriptInterface(object {                         //Funcion que va a importar el html y que funciona de forma simial a la de swiperefresh
                @JavascriptInterface                                //Indica que esta funcion puede ser llamada desde un HTML con JavaScript
                fun retry() {                                       //Nombre de la funcion que servira para hacer refresh pulsando un boton que la invoca

                    wv.post {                                       //Es la referencia local al WebView, lo de post es manejo de hilos por venir las cosas de JavaScript
                        val current = wv.url                        //Obtiene la url de dicho WebView
                        val target = if (isErrorAsset(current)) {   //Si se esta en la pantalla de error de conexion
                            lastFailedUrlLocal                      //target adquiere el valor de la ultima url valida que se intento cargar
                                ?: wv.originalUrl                   //Si es nulo coge el original
                                ?: current                          //Si no existe, el current
                                ?: initialUrl                       //Si no existe, la inicial
                        } else {                                    //Si no se esta en la pantalla de error de conexion
                            current ?: initialUrl                   //Coge el valor de la actual, a menos que sea nula, que encontces coge la inicial
                                                                    //Como este boton solo aparece en la pantalla de error, esto nunca se va a dar, siempre se va a estar en la pantalla de error de conexion
                        }
                        if (target != null) {                       //Si el target no es nulo (siempre sera asi)
                            wv.loadUrl(target)                      //Carga otra vez la url que este guardada en el target
                        } else {                                    //Si el target es nulo
                            wv.reload()                             //Fuerza el reload de la pagina
                        }
                    }
                }
            }, "Android")

            loadUrl(url)    //Carga el url (Esta es la funcion principal)
        }


        swipe.setOnChildScrollUpCallback { _, _ ->  //Si el child todavia puede desplazarse hacia arriba, no hace el refresh
            (web.scrollY > 0)                       //True si el WebView no esta en el tope (Swiperefresh bloqueado), False si si esta en el tope
        }

        swipe.setOnRefreshListener {                    //Listener que se ejecuta cuando el usuario hacer lo de arrastar hacia abajo
            val current = web.url                       //Guarda en una constante local la url de la que se parte
            val target = if (isErrorAsset(current)) {   //Si se esta en la pantalla de error de conexion
                lastFailedUrlLocal                      //target adquiere el valor de la url de la ultima pagina valida que fue guardado
                    ?: web.originalUrl                  //Si no hay ninguno, coge el de la pagina princial
                    ?: url                              //Lo mismo que la lina anterior
            } else {                                    //Si no estas en la pantalla de error de conexion
                current ?: url                          //Adquiere el valor de la url actual
            }

            if (target != null) {                       //Si el target no es nulo (siempre sera asi)
                web.loadUrl(target)                     //Carga otra vez la url que este guardada en el target
            } else {                                    //Si el target es nulo
                web.reload()                            //Fuerza el reload de la pagina
            }
        }

        swipe.addView(                                  //Swipe es un ViewGroup y con addView incluye un hijo al contenedor
            web,                                        //web es el WebView hijo incluido
            ViewGroup.LayoutParams(                     //Se pasan los parametros de forma
                ViewGroup.LayoutParams.MATCH_PARENT,    //En el ancho, ocupa tod0 el contenedor
                ViewGroup.LayoutParams.MATCH_PARENT     //En el alto, ocupa tod0 el contenedor
            )
        )


        swipe   //Valor de retorno que devuelve un View
                //Es el nucelo de tu factory, es el view que se va a mostrar

        },
        update = { view ->                                  //Mientras que factory solo se ejecuta una vez, update se ejecuta cada vez que se recompone el AndroidView
            canGoBack = webViewRef?.canGoBack() == true     //Asegura que en cada recomposicion al avanzar se cambi el canGoBack a True para indicar que puede volver
        }
    )
}
