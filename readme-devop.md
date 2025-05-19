# Guía para Desarrolladores: API MetricaSHD

## Descripción General

La API MetricaSHD proporciona estadísticas en tiempo real sobre los espectadores conectados a streams HLS en Wowza Streaming Engine. Esta guía explica cómo consumir esta API desde aplicaciones web desarrolladas con Node.js y React.

## Características de la API

- **Endpoint**: `http://<wowza-host>:8086/MetricaSHD`
- **Método**: GET
- **Autenticación**: No requiere (por defecto)
- **CORS**: Habilitado con `Access-Control-Allow-Origin: *`
- **Formato de respuesta**: JSON

## Datos que proporciona la API

La API devuelve un objeto JSON con la siguiente estructura:

```json
{
  "totalViewerCount": 8,           // Número total de espectadores en todas las aplicaciones
  "applications": [                // Array de aplicaciones
    {
      "application": "pipo2",     // Nombre de la aplicación
      "instances": [               // Instancias dentro de la aplicación
        {
          "name": "_definst_",    // Nombre de la instancia
          "viewerCount": 5,       // Número de espectadores en esta instancia
          "viewers": [             // Información detallada de cada espectador
            {
              "ip": "203.0.113.55",
              "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X)..."
            },
            {
              "ip": "198.51.100.42",
              "userAgent": "VLC/3.0.20 LibVLC/3.0.20"
            }
          ]
        }
      ]
    }
  ]
}
```

> **Nota importante**: La API solo contabiliza sesiones HLS (Cupertino Streaming).

## Consumiendo la API con Node.js

### Usando Fetch (Node.js 18+)

```javascript
async function getMetricas() {
  try {
    const response = await fetch('http://<wowza-host>:8086/MetricaSHD');
    
    if (!response.ok) {
      throw new Error(`Error HTTP: ${response.status}`);
    }
    
    const data = await response.json();
    console.log(`Total de espectadores: ${data.totalViewerCount}`);
    return data;
  } catch (error) {
    console.error('Error al obtener métricas:', error);
  }
}
```

### Usando Axios (Versiones anteriores de Node.js)

Primero, instala Axios:

```bash
npm install axios
```

Después, úsalo para consumir la API:

```javascript
const axios = require('axios');

async function getMetricas() {
  try {
    const response = await axios.get('http://<wowza-host>:8086/MetricaSHD');
    console.log(`Total de espectadores: ${response.data.totalViewerCount}`);
    return response.data;
  } catch (error) {
    console.error('Error al obtener métricas:', error);
  }
}
```

## Implementación en React

### Componente de ejemplo para mostrar estadísticas

```jsx
import { useState, useEffect } from 'react';
import axios from 'axios';

function MetricasViewer() {
  const [metricas, setMetricas] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Función para obtener las métricas
    const fetchMetricas = async () => {
      try {
        const response = await axios.get('http://<wowza-host>:8086/MetricaSHD');
        setMetricas(response.data);
        setLoading(false);
      } catch (err) {
        setError('Error al cargar las métricas');
        setLoading(false);
        console.error(err);
      }
    };

    // Cargar datos iniciales
    fetchMetricas();
    
    // Configurar actualización periódica (cada 5 segundos)
    const interval = setInterval(fetchMetricas, 5000);
    
    // Limpiar intervalo al desmontar
    return () => clearInterval(interval);
  }, []);

  if (loading) return <div>Cargando métricas...</div>;
  if (error) return <div>{error}</div>;
  if (!metricas) return <div>No hay datos disponibles</div>;

  return (
    <div className="metricas-container">
      <h2>Estadísticas de Streaming</h2>
      <div className="total-viewers">
        <h3>Total de espectadores: {metricas.totalViewerCount}</h3>
      </div>
      
      {metricas.applications.map(app => (
        <div key={app.application} className="application-stats">
          <h4>Aplicación: {app.application}</h4>
          
          {app.instances.map(instance => (
            <div key={instance.name} className="instance-stats">
              <h5>Instancia: {instance.name}</h5>
              <p>Espectadores: {instance.viewerCount}</p>
              
              <h6>Detalles de espectadores:</h6>
              <ul className="viewers-list">
                {instance.viewers.map((viewer, index) => (
                  <li key={index}>
                    <strong>IP:</strong> {viewer.ip} | 
                    <strong>User Agent:</strong> {viewer.userAgent}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}

export default MetricasViewer;
```

## Actualizaciones en tiempo real

Para aplicaciones que requieren datos en tiempo real:

1. **Polling:** Como se muestra en el ejemplo de React, usa `setInterval` para consultar periódicamente la API
2. **Server-Sent Events (SSE):** Una alternativa más eficiente al polling, aunque requiere una implementación adicional en el servidor
3. **WebSockets:** Para casos que necesiten comunicación bidireccional

## Consideraciones Importantes

1. **Rendimiento:** Ajusta la frecuencia de actualización según la cantidad de datos y usuarios
2. **Seguridad:**
   - Considera añadir autenticación si los datos son sensibles
   - Limita el acceso por IP si la API se expone públicamente
3. **Optimización:**
   - Implementa caché del lado del cliente para reducir llamadas innecesarias
   - Considera usar una implementación de debouncing o throttling en llamadas frecuentes

## Requisitos adicionales

- **Node.js:** Versión 14 o superior (v18+ si usas fetch nativo)
- **Bibliotecas:**
  - Para Node.js: axios (si usas versiones anteriores a Node.js 18)
  - Para React: react 16.8+ (para usar hooks)
  - Opcional: react-query o SWR para manejo avanzado de datos
  
## Ejemplo de despliegue completo

Para un panel de estadísticas completo, considera:

1. **Frontend (React):** Panel con visualizaciones de datos (gráficas, tablas)
2. **Backend (Node.js):** Opcional, para agregar autenticación, caché o enriquecimiento de datos
3. **Base de datos:** Para almacenar datos históricos si se requiere análisis a largo plazo

## Solución de problemas

- **CORS:** Si experimentas problemas de CORS, verifica que el servidor de Wowza esté correctamente configurado
- **Rendimiento:** Si la API tarda en responder, considera reducir la frecuencia de consultas
- **Datos inconsistentes:** Wowza puede tener latencia en actualizar el número de espectadores, especialmente cuando se desconectan

## Recursos adicionales

- [Documentación oficial de Wowza](https://www.wowza.com/docs/)
- [Documentación de React](https://react.dev/)
- [Axios Documentation](https://axios-http.com/docs/intro)
- [SWR para fetching de datos](https://swr.vercel.app/) 