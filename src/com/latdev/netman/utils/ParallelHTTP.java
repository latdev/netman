package com.latdev.netman.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ParallelHTTP {

    private CookieManager cookieManager;
    private int lastStatusCode = 0;
    private Exception lastError;

    private static class ParameterStringBuilder {
        public static String getParamsString(Map<String, String> params) throws UnsupportedEncodingException {
            return ParameterStringBuilder.getParamsString(params, "=", "&");
        }
        public static String getParamsString(Map<String, String> params, String endChar) throws UnsupportedEncodingException {
            return ParameterStringBuilder.getParamsString(params, "=", endChar);
        }
        public static String getParamsString(Map<String, String> params, String nameValueSeparator, String endChar) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append(nameValueSeparator);
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                result.append(endChar);
            }
            String resultString = result.toString();
            return resultString.length() > 0
                    ? resultString.substring(0, resultString.length() - endChar.length())
                    : resultString;
        }
    }

    public ParallelHTTP() {
        cookieManager = new CookieManager();
    }

    private String readConnBuffer(HttpURLConnection conn) throws IOException {
        BufferedReader dataIn = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder inputBuffer = new StringBuilder();
        while ((inputLine = dataIn.readLine()) != null) {
            inputBuffer.append((String)(inputLine + "\n"));
        }
        dataIn.close();
        return inputBuffer.toString();
    }

    private void applyCookieString(HttpURLConnection conn) {
        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        Map<String,String> cookiesHeader = new HashMap<>();
        for (HttpCookie cookie: cookies) {
            cookiesHeader.put(cookie.getName(), cookie.getValue());
        }
        this.noError(() -> {
            String str = ParameterStringBuilder.getParamsString(cookiesHeader, "; ");
            conn.addRequestProperty("Cookie", str);
            return 1;
        });
    }

    private void storeSetCookie(HttpURLConnection conn) {
        List<String> cookieHeaders = conn.getHeaderFields().get("Set-Cookie");
        if (cookieHeaders != null) {
            cookieHeaders.forEach(cookieHeader -> {
                List<HttpCookie> cookies = HttpCookie.parse(cookieHeader);
                cookies.forEach(cookie -> cookieManager.getCookieStore().add(null, cookie));
            });
        }
    }

    public String get(String url) {
        lastError = null;
        String[] result = {""};
        lastStatusCode = this.noError(() -> {
            URL cu = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) cu.openConnection();
            conn.setInstanceFollowRedirects(true);
            applyCookieString(conn);

            conn.setRequestMethod("GET");
            int statusCode = conn.getResponseCode();
            storeSetCookie(conn);
            result[0] = readConnBuffer(conn);
            conn.disconnect();
            return statusCode;
        });
        return result[0];
    }

    public String post(String url, Map<String,String> params) {
        lastError = null;
        String[] result = {""};
        lastStatusCode = this.noError(() -> {
            URL cu = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) cu.openConnection();
            conn.setInstanceFollowRedirects(true);
            applyCookieString(conn);

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            DataOutputStream dataOut = new DataOutputStream(conn.getOutputStream());
            dataOut.writeBytes(ParameterStringBuilder.getParamsString(params));
            dataOut.flush();
            dataOut.close();

            int statusCode = conn.getResponseCode();
            storeSetCookie(conn);
            result[0] = readConnBuffer(conn);
            conn.disconnect();
            return statusCode;
        });
        return result[0];
    }

    private Integer noError(Callable<Integer> func) {
        try {
            return func.call();
        } catch (Exception err) {
            lastError = err;
            return -1;
        }
    }

    public Exception getLastError() {
        return lastError;
    }

}

