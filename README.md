# Plugin MetricaSHD para Wowza Streaming Engine

## Descripción

`MetricaSHDProvider` es un *HTTP Provider* para **Wowza Streaming Engine 4.8.27+5** que expone un único endpoint HTTP (`/MetricaSHD`) en el puerto 8086.  
El endpoint devuelve en tiempo real las métricas de todas las aplicaciones e instancias cargadas en el _VHost_ activo, incluyendo:

* Total de visualizadores HLS conectados.
* Número de visualizadores por aplicación e instancia.
* Dirección IP y **User-Agent** de cada espectador.

La respuesta se entrega en formato **JSON** idéntico al siguiente ejemplo:

```json
{
  "totalViewerCount": 8,
  "applications": [
    {
      "application": "pipo2",
      "instances": [
        {
          "name": "_definst_",
          "status": "Online",
          "viewerCount": 5,
          "viewers": [
            {
              "ip": "203.0.113.55",
              "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X)..."
            },
            {
              "ip": "198.51.100.42",
              "userAgent": "VLC/3.0.20 LibVLC/3.0.20"
            }
          ]
        },
        {
          "name": "backup",
          "status": "Offline",
          "viewerCount": 0,
          "viewers": []
        }
      ]
    }
  ]
}
```

> **Nota:** solo se contabilizan las sesiones HLS (Cupertino Streaming).

---

## Estructura del proyecto

```
MODULE-WSE-STATS/
 ├── lib/            # JARs de Wowza y dependencias (proporcionados)
 ├── src/
 │   └── com/
 │       └── ediap/
 │           └── wowza/
 │               └── MetricaSHDProvider.java
 └── README.md
```

---

## Compilación

1. **Requisitos previos**
   * **Java 11 SDK** (Wowza 4.8.27 está compilado con versión de clase 55).  
     Comprueba con `javac -version` que obtengas ≥ 11.

2. **Compilar el plugin**

   Ejecuta desde la raíz del proyecto:

   ```bash
   mkdir -p build
   javac -classpath "lib/*" -d build $(find src -name '*.java')
   jar cfv MetricaSHD.jar -C build .
   ```

   ### macOS / Homebrew: usando un JDK 11 paralelo

   Si tu `javac` por defecto es 1.8 (habitual en macOS) y ya tienes instalado **OpenJDK 11** mediante *Homebrew*, puedes compilar indicando la ruta explícita:

   ```bash
   # Asumiendo que lo instalaste con:
   #   brew install openjdk@11

   export JAVA11="/opt/homebrew/opt/openjdk@11"
   mkdir -p build
   "$JAVA11/bin/javac" -classpath "lib/*" -d build $(find src -name '*.java')
   "$JAVA11/bin/jar" cfv MetricaSHD.jar -C build .
   ```

   Comprueba la versión:

   ```bash
   "$JAVA11/bin/java" -version  # Debe mostrar 11.x
   ```

   De esta forma evitas el error de incompatibilidad de versión de clase al usar un JDK 8.

   Se generará el archivo `MetricaSHD.jar` listo para desplegar.

---

## Instalación en Wowza

1. Copia `MetricaSHD.jar` en la carpeta `lib` de tu instalación de Wowza (por ejemplo `/usr/local/WowzaStreamingEngine/lib`).
2. Edita el archivo `conf/VHost.xml` y registra el *HTTPProvider* en la sección `<HTTPProviders>` del **HostPort** que usa el puerto 8086, añadiendo la siguiente línea **antes** de la entrada `HTTPServerVersion`:

   ```xml
   <HTTPProvider>
     <BaseClass>com.ediap.wowza.MetricaSHDProvider</BaseClass>
     <RequestFilters>*</RequestFilters>
     <RESTMethod>GET</RESTMethod>
     <AuthenticationMethod>none</AuthenticationMethod>
   </HTTPProvider>
   ```

   Si tu VHost no dispone del puerto 8086, revisa la sección `<HostPort>` correspondiente o crea uno nuevo.

3. Reinicia Wowza Streaming Engine para que cargue el nuevo proveedor:

   ```bash
   sudo service WowzaStreamingEngine restart
   # o
   sudo systemctl restart WowzaStreamingEngine
   ```

---

## Consumo de la API

```
GET http://<wowza-host>:8086/MetricaSHD
```

El endpoint **no** requiere autenticación ni parámetros.  
La cabecera `Access-Control-Allow-Origin: *` se añade automáticamente para permitir llamadas desde navegadores.

---

## Preguntas frecuentes

| Pregunta | Respuesta |
| -------- | --------- |
| ¿Qué pasa si existen aplicaciones sin instancias activas? | Se incluirán en la respuesta con `viewerCount = 0` y un array `viewers` vacío. |
| ¿Por qué solo se cuentan sesiones HLS? | El requisito indicaba HLS (Cupertino). Si deseas incluir otros protocolos, modifica la condición en `MetricaSHDProvider.java`. |
| ¿Puedo proteger el endpoint con usuario/contraseña? | Sí. Cambia el valor de `<AuthenticationMethod>` a `basic` o `digest` y configura los usuarios en `conf/Server.xml`. |

---

## Créditos

Desarrollado por **EDIAP** (2024).  
Basado en la API oficial de Wowza Streaming Engine 4.8. 