package com.bc.util.httputil;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.TrustManager;

import java.io.IOException;
import java.security.cert.CertificateException;


public class HttpClientUtil {
    public static String getReturnXML(String sendURL, String sendXML) {
        // 此函数是平台主动向保险公司发起的http请求,并接收保险公司的响应
        Date startDate = new Date();
        String responseStr = "";
        byte[] iXMLData = null;
        try {
            // 建立一个HttpURLConnection
            HttpURLConnection httpConnection = (HttpURLConnection) new URL(sendURL).openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setDoOutput(true);
            httpConnection.setDoInput(true);
//		System.setProperty("sun.net.client.defaultConnectTimeout", "5000");
//		System.setProperty("sun.net.client.defaultReadTimeout", "20000");
            httpConnection.setAllowUserInteraction(true);
            httpConnection.connect();
            // 发送数据给保险公司
            OutputStream outputStream = httpConnection.getOutputStream();
            // 平台生成的XML串
            byte[] buffer = sendXML.getBytes();
            outputStream.write(buffer);
            outputStream.flush();
            outputStream.close();
            // 接收保险公司返回的数据
            InputStream inputStream = httpConnection.getInputStream();
            // 输入流,用于接收请求的数据
            BufferedInputStream input = null;
            // 数据缓冲区
            buffer = new byte[1024];
            // 每个缓冲区的实际数据长度
            int count = 0;
            // 请求数据存放对象
            ByteArrayOutputStream streamXML = new ByteArrayOutputStream();
            try {
                input = new BufferedInputStream(inputStream);
                while ((count = input.read(buffer)) != -1) {
                    streamXML.write(buffer, 0, count);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (Exception f) {
                        f.printStackTrace();
                    }
                }
            }
            // 得到一个byte数组,提供给平台
            iXMLData = streamXML.toByteArray();
            httpConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();

        }
        responseStr = new String(iXMLData);
        Date endDate = new Date();
        System.out.println("请求URL为=====" + sendURL + "     响应时间差为=========" + (endDate.getTime() - startDate.getTime()));
        return responseStr;
    }

    private static RequestConfig requestConfig;
    private static PoolingHttpClientConnectionManager cm;

    /**
     * 绕过验证
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("SSLv3");

        // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
        X509TrustManager trustManager = new X509TrustManager() {
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sc.init(null, new TrustManager[]{trustManager}, null);
        return sc;
    }

    static {
        SSLContext sslcontext = null;
        try {
            sslcontext = createIgnoreVerifySSL();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        // 设置协议http和https对应的处理socket链接工厂的对象

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", new SSLConnectionSocketFactory(sslcontext))
                .build();

        cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        // 将最大连接数增加
        cm.setMaxTotal(200);
        // 将每个路由基础的连接增加
        cm.setDefaultMaxPerRoute(40);

        requestConfig = RequestConfig.custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(30000)
                .setConnectionRequestTimeout(5000)
                .build();
    }

    public static CloseableHttpClient getHttpClient() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig).build();
        return httpClient;
    }

    public static JSONObject httpPost(String url, Map<String, String> params) {
        JSONObject json = new JSONObject();
        try {
            HttpPost httppost = new HttpPost(url);
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            Iterator<?> iterator = params.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> elem = (Map.Entry<String, String>) iterator.next();
                list.add(new BasicNameValuePair(elem.getKey(), elem
                        .getValue()));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list,
                    "UTF-8");
            httppost.setEntity(entity);
            CloseableHttpResponse response;
            String str = "";
            try {
                response = getHttpClient().execute(httppost);
                HttpEntity httpentity = response.getEntity();
                if (httpentity != null) {
                    str = EntityUtils.toString(httpentity, "UTF-8");
                    json.put("result", str);
                    json.put("success", true);
                }
            } catch (ClientProtocolException e) {
                json.put("success", false);
                json.put("returnMsg", " 请求失败");
                e.printStackTrace();
            } catch (IOException e) {
                json.put("success", false);
                json.put("returnMsg", "请求失败");
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static void main(String[] args) {
        try {
            System.out.println(HttpClientUtil.getReturnXML("http://10.10.206.99:8001/pdfbpoc/sendServlet", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><PeoplesBankMessageZC>  <requesthead>    <user>0102</user>    <sender>0102</sender>    <uuid>6c5f0e82-545b-4b27-809b-1bf56abfc4c2</uuid>    <flowintime>20171207111228</flowintime>  </requesthead>  <requestbody>    <CheckType>1</CheckType>    <BasicInformation>      <InRecordLength>1965</InRecordLength>      <InRecordType>61</InRecordType>      <SegmentMark>B</SegmentMark>      <InsuranceComCode>53252201      </InsuranceComCode>      <PolicyNo>PBAU201753250000000002                                      </PolicyNo>      <PpolicyNo>                                                            </PpolicyNo>      <ContractNumber>                                                            </ContractNumber>      <InsuredType>2</InsuredType>      <InsuredName>樊雄                                                                            </InsuredName>      <IdentifyType>0</IdentifyType>      <IdentifyNumber>532522197510091815</IdentifyNumber>      <DataReportDate>20171211</DataReportDate>      <PreChar>                                                            </PreChar>    </BasicInformation>    <InsurContInformation>      <SegmentMark>D</SegmentMark>      <BusinessType>01</BusinessType>      <GuaranteeType>1</GuaranteeType>      <Amount>00000000000000106590</Amount>      <StartDate>20171211</StartDate>      <EndDate>20181210</EndDate>      <DepositRatio>000</DepositRatio>      <AntiGuaranteeMode>x</AntiGuaranteeMode>      <ContReinsurRatio>000</ContReinsurRatio>      <Rate>2.0000</Rate>      <AnnualRate>2.0000</AnnualRate>      <PreChar>                                                            </PreChar>    </InsurContInformation>    <ActualCompemResponInformation>      <SegmentMark>H</SegmentMark>      <OthFlag>1</OthFlag>      <CompenLiaRelDate>        </CompenLiaRelDate>      <PolicyBalance>00000000000000106590</PolicyBalance>      <ChaBalDate>20171211</ChaBalDate>    </ActualCompemResponInformation>  </requestbody></PeoplesBankMessageZC>"));
//	            query();
//	            result();
//	            feedback();
//	            health();
//	            memberInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
