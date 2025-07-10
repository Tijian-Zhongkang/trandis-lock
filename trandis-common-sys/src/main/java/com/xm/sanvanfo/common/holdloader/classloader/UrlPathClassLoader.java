package com.xm.sanvanfo.common.holdloader.classloader;


import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@SuppressWarnings("WeakerAccess")
public class UrlPathClassLoader extends AbstractCustomClassLoader implements Cloneable{

    protected String basePath;
    protected ClassLoader parent;

    public UrlPathClassLoader(String path) {
        super();
        basePath = wrap(path);
    }

    public UrlPathClassLoader(String path, ClassLoader parent) {
        super(parent);
        basePath = wrap(path);
        this.parent = parent;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    private String wrap(String path) {
        try {
            path = path.replace("\\\\", "/");
            if(!path.endsWith("/")) {
                path = path + "/";
            }
            URI uri = new URI(path);
            if(null == uri.getHost()) {
                if(!path.startsWith("file:")) {
                    return "file:///" + path;
                }
            }
            return path;
        }
        catch (URISyntaxException ex) {
            return path;
        }
    }

    String getBasePath() {
        return basePath;
    }


    @Override
    protected byte[] loadClassData(String name) throws ClassNotFoundException {
        try {
            String fname = name.replaceAll("\\.", "/");
            URI url = new URI(basePath + fname + ".class");
            if(url.getHost() == null) {
                File f = new File(url);
                if(!f.exists()) {
                    throw new ClassNotFoundException("the class is not exists or address is wrong");
                }
                try(FileInputStream s = new FileInputStream(f)) {
                    long size = s.getChannel().size();
                    byte[] bytes = new byte[(int) size];
                    int len = s.read(bytes);
                    if(len <= 0) {
                        throw new ClassNotFoundException("the class length is 0");
                    }
                    return Decode(bytes);
                }

            }
            return Decode(getUrlBytes(url));
        }
        catch (URISyntaxException ex) {
            throw new ClassNotFoundException("the class is not exists or address is wrong");
        }
        catch (IOException ex) {
            throw new ClassNotFoundException("IO exception");
        }
    }

    protected byte[] Decode(byte[] body) {
        return  body;
    }

    protected byte[] getUrlBytes(URI url) {
        SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(false).setSoLinger(1).setSoReuseAddress(true).setSoTimeout(10000).setTcpNoDelay(true).build();
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultSocketConfig(socketConfig).build();
        CloseableHttpResponse response = null;

        try {
            response = doGetResponse(httpClient, url.getRawPath());
            return EntityUtils.toByteArray(response.getEntity());
        } catch (IOException e) {
           throw new RuntimeException(e);
        } finally {
            CloseResponse(httpClient, response, url.getRawPath());
        }
    }

    protected CloseableHttpResponse doGetResponse(CloseableHttpClient httpClient, String url) throws IOException {
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36";
        HttpGet httpGet = new HttpGet(url);
        HttpClientContext context = HttpClientContext.create();
        RequestConfig.Builder requestConfigB = RequestConfig.custom();
        requestConfigB.setConnectTimeout(35000).setConnectionRequestTimeout(35000).setSocketTimeout(9000);
        httpGet.setHeader("User-Agent", ua);

        httpGet.setHeader("Accept", " text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");


        RequestConfig requestConfig = requestConfigB.build();
        httpGet.setConfig(requestConfig);
        return httpClient.execute(httpGet, context);
    }

    @SuppressWarnings("unused")
    protected void CloseResponse(CloseableHttpClient httpClient, CloseableHttpResponse response, String url) {
        if (null != response) {
            try {
                EntityUtils.consume(response.getEntity());
                response.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (null != httpClient) {
            try {
                httpClient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
