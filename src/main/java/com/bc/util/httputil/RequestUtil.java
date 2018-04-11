package com.bc.util.httputil;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2017/9/19.
 */
public class RequestUtil {
    /*
     * 处理https GET/POST请求
	 * 请求地址、请求方法、参数
	 * */
    public static String HttpsRequest(String requestUrl, String requestMethod, String outputStr) {
        StringBuffer buffer = null;
        try {
            //创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManager[] tm = {new MyX509TrustManager()};
            //初始化
            sslContext.init(null, tm, new java.security.SecureRandom());
            //获取SSLSocketFactory对象
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            URL url = new URL(requestUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod(requestMethod);
            //设置当前实例使用的SSLSoctetFactory
            conn.setSSLSocketFactory(ssf);
            conn.connect();
            //往服务器端写内容
            if (null != outputStr) {
                OutputStream os = conn.getOutputStream();
                os.write(outputStr.getBytes("utf-8"));
                os.close();
            }

            //读取服务器端返回的内容
            Map<String, List<String>> headMap = conn.getHeaderFields();
//            Map<String,List<String>> requestMap = conn.getRequestProperties();
            for (String key : headMap.keySet()) {
                System.out.println("headkey===" + key);
                for (String value : headMap.get(key)) {
                    System.out.println("value===" + value);
                }
            }
//            for (String key:requestMap.keySet()){
//                System.out.println("requestkey==="+key);
//                for (String value:requestMap.get(key)) {
//                    System.out.println("value==="+value);
//                }
//            }
            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            BufferedReader br = new BufferedReader(isr);
            buffer = new StringBuffer();
            String line = null;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public static Object[] HttpRequest(String requestUrl, String requestMethod, String outputStr) {
        StringBuffer buffer = null;
        Object[] response = null;
        try {
            response = new Object[2];
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod(requestMethod);
            conn.connect();
            //往服务器端写内容 也就是发起http请求需要带的参数
            if (null != outputStr) {
                OutputStream os = conn.getOutputStream();
                os.write(outputStr.getBytes("utf-8"));
                os.close();
            }

            //读取服务器端返回的内容
            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            BufferedReader br = new BufferedReader(isr);
            buffer = new StringBuffer();
            String line = null;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
            }
            response[0] = buffer.toString();
            response[1] = conn.getHeaderFields();
        } catch (Exception e) {
            e.printStackTrace();
            response = null;
        }
        return response;
    }

    public static void main(String[] args) {
        String str = HttpsRequest("https://kyfw.12306.cn/otn/login/init", "GET", "");
        System.out.println(str);
    }
}
