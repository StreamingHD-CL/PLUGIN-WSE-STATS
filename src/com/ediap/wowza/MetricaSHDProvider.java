package com.ediap.wowza;

import com.wowza.wms.http.HTTProvider2Base;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.vhost.IVHost;
import com.wowza.wms.application.IApplication;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HTTPProvider que devuelve estadísticas de visualización de todas las aplicaciones e instancias
 * cargadas en el VHost. Debe configurarse en VHost.xml escuchando en el puerto 8086 con la ruta
 * /MetricaSHD sin parámetros.
 */
public class MetricaSHDProvider extends HTTProvider2Base {

    private static final int PROTOCOL_HLS = IHTTPStreamerSession.SESSIONPROTOCOL_CUPERTINOSTREAMING;

    @Override
    public void onHTTPRequest(IVHost vhost, IHTTPRequest req, IHTTPResponse resp) {
        // Solamente atender GET sin parámetros.
        try {
            JsonObject root = new JsonObject();
            JsonArray applicationsArr = new JsonArray();

            int totalViewers = 0;

            // Recorremos todas las aplicaciones cargadas en el VHost
            List<?> appNames = vhost.getApplicationNames();
            if (appNames != null) {
                for (Object appNameObj : appNames) {
                    String appName = String.valueOf(appNameObj);
                    IApplication app = vhost.getApplication(appName);
                    if (app == null) {
                        continue;
                    }

                    JsonObject appJson = new JsonObject();
                    appJson.addProperty("application", appName);
                    JsonArray instancesArr = new JsonArray();

                    List<String> instanceNames = app.getAppInstanceNames();
                    if (instanceNames != null) {
                        for (String instName : instanceNames) {
                            IApplicationInstance appInst = app.getAppInstance(instName);
                            if (appInst == null) {
                                continue;
                            }

                            JsonObject instJson = new JsonObject();
                            instJson.addProperty("name", instName);
                            JsonArray viewersArr = new JsonArray();

                            // Obtenemos todas las sesiones HTTP (HLS)
                            List<IHTTPStreamerSession> httpSessions = appInst.getHTTPStreamerSessions();
                            int instViewerCount = 0;
                            if (httpSessions != null) {
                                for (IHTTPStreamerSession session : httpSessions) {
                                    if (session != null && session.getSessionProtocol() == PROTOCOL_HLS && session.isActive()) {
                                        instViewerCount++;
                                        JsonObject viewerJson = new JsonObject();
                                        String ip = session.getForwardedIP();
                                        if (ip == null || ip.isEmpty()) {
                                            ip = session.getIpAddress();
                                        }
                                        viewerJson.addProperty("ip", ip);
                                        viewerJson.addProperty("userAgent", session.getUserAgent());
                                        viewersArr.add(viewerJson);
                                    }
                                }
                            }

                            instJson.addProperty("viewerCount", instViewerCount);
                            instJson.add("viewers", viewersArr);
                            instancesArr.add(instJson);
                            totalViewers += instViewerCount;
                        }
                    }

                    appJson.add("instances", instancesArr);
                    applicationsArr.add(appJson);
                }
            }

            root.addProperty("totalViewerCount", totalViewers);
            root.add("applications", applicationsArr);

            byte[] outBytes = root.toString().getBytes(StandardCharsets.UTF_8);

            resp.setHeader("Content-Type", "application/json; charset=UTF-8");
            resp.setHeader("Access-Control-Allow-Origin", "*"); // CORS simple
            resp.setResponseCode(200);
            resp.setHeader("Content-Length", String.valueOf(outBytes.length));

            OutputStream out = resp.getOutputStream();
            out.write(outBytes);
        } catch (Exception e) {
            // En caso de error interno devolver 500
            try {
                String msg = "{\"error\":\"" + e.getMessage() + "\"}";
                byte[] outBytes = msg.getBytes(StandardCharsets.UTF_8);
                resp.setHeader("Content-Type", "application/json; charset=UTF-8");
                resp.setResponseCode(500);
                resp.setHeader("Content-Length", String.valueOf(outBytes.length));
                OutputStream out = resp.getOutputStream();
                out.write(outBytes);
            } catch (Exception ignored) {
            }
        }
    }
} 