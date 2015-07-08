package us.codecraft.webmagic.downloader;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.utils.HttpConstant;
import us.codecraft.webmagic.utils.LoggerUtil;
import us.codecraft.webmagic.utils.UrlUtils;

import com.google.common.collect.Sets;


/**
 * The http downloader based on HttpClient.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 */
@ThreadSafe
public class HttpClientDownloader extends AbstractDownloader {
	
	private static final String logName = "HttpClientDownloader";

    //所有HttpClientDownloader对象共享一个httpClient池
    private final static Map<String, CloseableHttpClient> httpClients = new HashMap<String, CloseableHttpClient>();
    
    private HttpClientGenerator httpClientGenerator = new HttpClientGenerator();

    private CloseableHttpClient getHttpClient(Site site) {
    	if (site == null) {
            return httpClientGenerator.getClient(null);
        }
    	String domain = site.getDomain();
        CloseableHttpClient httpClient = httpClients.get(domain);
        if (httpClient == null) {
        	synchronized (this) {
        		httpClient = httpClients.get(domain);
                if (httpClient == null) {
                    httpClient = httpClientGenerator.getClient(site);
                    httpClients.put(domain, httpClient);
                }
            }
        }
        return httpClient;
    }

    @Override
    public Page download(Request request, Task task) {
        Site site = null;
        if (task != null) {
            site = task.getSite();
        }
        Set<Integer> acceptStatCode;
        String charset = null;
        Map<String, String> headers = null;
        if (site != null) {
            acceptStatCode = site.getAcceptStatCode();
            charset = site.getCharset();
            headers = site.getHeaders();
            //用request中的headers覆盖site中的headers
            Map<String,String> independentheaders = request.getHeaders();
            if(independentheaders!=null){
            	for(Entry<String,String> entry : independentheaders.entrySet()){
            		headers.put(entry.getKey(), entry.getValue());
            	}
            }
        } else {
            acceptStatCode = Sets.newHashSet(200);
            headers = request.getHeaders();
        }
        LoggerUtil.info(task.getUUID(),logName,"downloading page {}",new Object[]{request.getUrl()});
        CloseableHttpResponse httpResponse = null;
        int statusCode=0;
        try {
        	HttpUriRequest httpUriRequest = getHttpUriRequest(request, site, headers);
        	String host = httpUriRequest.getURI().getHost();
            httpResponse = getHttpClient(site).execute(httpUriRequest);
            statusCode = httpResponse.getStatusLine().getStatusCode();
            request.putExtra(Request.STATUS_CODE, statusCode);
            if (statusAccept(acceptStatCode, statusCode)) {
                Page page = handleResponse(request, charset, httpResponse, task);
                //添加网页有效性验证字段，为了防止statuscode=200，但网页无效（可能的原因有代理IP响应速度太慢或被封）
                String validRule = site.getValidCheck(request.getFieldRuleId() == null ? 0 :request.getFieldRuleId());
                if(!StringUtils.isEmpty(validRule)){
                	if(StringUtils.isEmpty(page.getHtml().xpath(validRule).toString()) && StringUtils.isEmpty(page.getHtml().regex(validRule).toString())){
                		LoggerUtil.info(task.getUUID(),logName,"没有通过页面正确性检测,statusCode:{},{}",new Object[]{statusCode,request.getUrl()});
                    	request.putExtra(Request.STATUS_CODE, Proxy.ERROR_BANNED);//为代理IP级ERROR_BANNED错误
                    	return addToCycleRetry(request, site);
                    }
                }
                onSuccess(request);
                LoggerUtil.info(task.getUUID(),logName,"{}: downloading page success! {}", new Object[]{Thread.currentThread().getName(),request.getUrl()});
                page.setHost(host);
                return page;	
            } else {
            	if(statusCode == Proxy.ERROR_403 || statusCode == Proxy.ERROR_500){
            		LoggerUtil.info(task.getUUID(),logName,"code error " + statusCode + "\t" + request.getUrl());
            		if(site.getHttpProxyPool()!=null && site.getHttpProxyPool().isEnable()){
                		return addToCycleRetry(request, site);
                	}else{
                		return null;
                	}
                }
            	LoggerUtil.info(task.getUUID(),logName,"code error " + statusCode + "\t" + request.getUrl());
                return null;
            }
        } catch(HttpHostConnectException e){
        	//由于是代理IP本身的原因，请求要重新放回，更换代理IP再试
        	request.putExtra(Request.STATUS_CODE, Proxy.ERROR_Proxy);
        	return addToCycleRetry(request, site);
        } catch(ConnectTimeoutException e){
        	//由于是代理IP本身的原因，请求要重新放回，更换代理IP再试
        	request.putExtra(Request.STATUS_CODE, Proxy.ERROR_PROXY_TIME_OUT);
        	return addToCycleRetry(request, site);
        } catch(ClientProtocolException e){
            request.putExtra(Request.STATUS_CODE, Request.ERROR_ILLEGAL_REQUEST_URI);
            onError(request);
            return null;
        }catch(SocketTimeoutException e){
        	request.putExtra(Request.STATUS_CODE, Proxy.ERROR_SOCKET_READ_TIME_OUT);
        	//代理IP速度太慢，导致读response超时
        	return addToCycleRetry(request, site);
        }catch(UnknownHostException e){
            request.putExtra(Request.STATUS_CODE, Proxy.ERROR_DEFAULT);
            onError(request);
            return null;
        }catch (IOException e) {
            request.putExtra(Request.STATUS_CODE, Proxy.ERROR_DEFAULT);
            onError(request);
            return addToCycleRetry(request, site);
        }finally {
            try {
                if (httpResponse != null) {
                    EntityUtils.consume(httpResponse.getEntity());
                }
            } catch (IOException e) {
            	//2015-04-01 socket在consume之前即已断开，不记录此错误日志
//            	LoggerFactory.getLogger(HttpClientDownloader.class).error("close response fail", e);
            }
        }
    }

    @Override
    public void setThread(int thread) {
        httpClientGenerator.setPoolSize(thread);
    }

    protected boolean statusAccept(Set<Integer> acceptStatCode, int statusCode) {
        return acceptStatCode.contains(statusCode);
    }

    protected HttpUriRequest getHttpUriRequest(Request request, Site site, Map<String, String> headers) {
        RequestBuilder requestBuilder = selectRequestMethod(request).setUri(request.getUrl());
        if (headers != null) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectionRequestTimeout(site.getTimeOut())
                .setSocketTimeout(site.getTimeOut())
                .setConnectTimeout(site.getTimeOut())
                .setCookieSpec(CookieSpecs.BEST_MATCH);
        if (site.getHttpProxyPool() != null && site.getHttpProxyPool().isEnable()) {
        	if(request.getProxy() == null){
        		Proxy proxy = site.getHttpProxyFromPool();
    			requestConfigBuilder.setProxy(proxy.getHttpHost());
    			requestConfigBuilder.setConnectionRequestTimeout(proxy.getSocketTimeout());
                requestConfigBuilder.setSocketTimeout(proxy.getSocketTimeout());
                requestConfigBuilder.setConnectTimeout(proxy.getSocketTimeout());
    			request.setProxy(proxy);
        	}else{
        		Proxy proxy = request.getProxy();
        		requestConfigBuilder.setProxy(proxy.getHttpHost());
    			requestConfigBuilder.setConnectionRequestTimeout(proxy.getSocketTimeout());
                requestConfigBuilder.setSocketTimeout(proxy.getSocketTimeout());
                requestConfigBuilder.setConnectTimeout(proxy.getSocketTimeout());
        	}
		}
        requestBuilder.setConfig(requestConfigBuilder.build());
        return requestBuilder.build();
    }

    protected RequestBuilder selectRequestMethod(Request request) {
        String method = request.getMethod();
        if (method == null || method.equalsIgnoreCase(HttpConstant.Method.GET)) {
            //default get
            return RequestBuilder.get();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.POST)) {
            RequestBuilder requestBuilder = RequestBuilder.post();
            NameValuePair[] nameValuePair = (NameValuePair[]) request.getExtra("nameValuePair");
            if (nameValuePair.length > 0) {
                requestBuilder.addParameters(nameValuePair);
            }
            return requestBuilder;
        } else if (method.equalsIgnoreCase(HttpConstant.Method.HEAD)) {
            return RequestBuilder.head();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.PUT)) {
            return RequestBuilder.put();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.DELETE)) {
            return RequestBuilder.delete();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.TRACE)) {
            return RequestBuilder.trace();
        }
        throw new IllegalArgumentException("Illegal HTTP Method " + method);
    }

    protected Page handleResponse(Request request, String charset, HttpResponse httpResponse, Task task) throws IOException{
        try{
        	String content = getContent(charset, httpResponse);
            Page page = new Page();
            //将从上个request请求中继承下来的fields保存到page中
            Map<String,Object> inheritFields = request.getInheritFields();
            if(inheritFields!=null){
            	for(Entry<String,Object> entry : inheritFields.entrySet()){
                	page.putField(entry.getKey(), entry.getValue());
                }
            }
            page.setRawText(content);
            page.setUrl(new PlainText(request.getUrl()));
            page.setRequest(request);
            page.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            Header[] response_headers = httpResponse.getAllHeaders();
            page.setHeaders(response_headers);
            return page;
        }catch(IOException e){
        	LoggerUtil.info(task.getUUID(),logName,request.getUrl() + " getContent error!"+e.getMessage());
        	throw e;
        }
    }

    protected String getContent(String charset, HttpResponse httpResponse) throws IOException {
        if (StringUtils.isBlank(charset)) {
            byte[] contentBytes = IOUtils.toByteArray(httpResponse.getEntity().getContent());
            try{
            	String htmlCharset = getHtmlCharset(httpResponse, contentBytes);
            	if (StringUtils.isNotBlank(htmlCharset)) {
                    return new String(contentBytes, htmlCharset);
                } else {
                    return new String(contentBytes);
                }
            }catch(Exception e){
            	return new String(contentBytes);
            }
        } else {
            return IOUtils.toString(httpResponse.getEntity().getContent(), charset);
        }
    }

    protected String getHtmlCharset(HttpResponse httpResponse, byte[] contentBytes) throws IOException {
        String charset;
        // charset
        // 1、encoding in http header Content-Type
        String value = httpResponse.getEntity().getContentType().getValue();
        charset = UrlUtils.getCharset(value);
        if (StringUtils.isNotBlank(charset)) {
            return charset;
        }
        // use default charset to decode first time
        Charset defaultCharset = Charset.defaultCharset();
        String content = new String(contentBytes, defaultCharset.name());
        // 2、charset in meta
        if (StringUtils.isNotEmpty(content)) {
            Document document = Jsoup.parse(content);
            Elements links = document.select("meta");
            for (Element link : links) {
                // 2.1、html4.01 <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                String metaContent = link.attr("content");
                String metaCharset = link.attr("charset");
                if (metaContent.indexOf("charset") != -1) {
                    metaContent = metaContent.substring(metaContent.indexOf("charset"), metaContent.length());
                    charset = metaContent.split("=")[1];
                    break;
                }
                // 2.2、html5 <meta charset="UTF-8" />
                else if (StringUtils.isNotEmpty(metaCharset)) {
                    charset = metaCharset;
                    break;
                }
            }
        }
        // 3、todo use tools as cpdetector for content decode
        return charset;
    }
    
    private Page addToCycleRetry(Request request, Site site) {
    	Page page = new Page();
        Object cycleTriedTimesObject = request.getExtra(Request.CYCLE_TRIED_TIMES);//存储的次数仅对site级别的错误有效，因为代理IP级别的错误是无限次重试的
    	
        //根据request的statusCode来确定处理流程
    	Integer statusCode = (Integer)request.getExtra(Request.STATUS_CODE);
    	
    	//IP级别的错误，如果使用代理IP无限重试，直到正常或不是代理IP级别错误,如果不使用代理IP,则重试site.cycleRetryTimes次
    	if(Proxy.PROXY_ERROR_CODE_SET.contains(statusCode)){
    		if (site.getHttpProxyPool() != null && site.getHttpProxyPool().isEnable()) {
    			if (cycleTriedTimesObject == null) {
                    page.addTargetRequest(request.setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, 1));
                } else {
                    page.addTargetRequest(request.setPriority(0));
                }
    		}else{
    			if (cycleTriedTimesObject == null) {
                    page.addTargetRequest(request.setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, 1));
                } else {
                    int cycleTriedTimes = (Integer) cycleTriedTimesObject;
                    cycleTriedTimes++;
                    if(site.getCycleRetryTimes() == 0){
                		page.addTargetRequest(request.setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, cycleTriedTimes));
                	}else{
                		if (cycleTriedTimes >= site.getCycleRetryTimes()) {
                            return null;
                        }
                	}
                }
    		}
    	}
    	
    	//如果是site级别的可接受的statusCode，则重试site.cycleRetryTimes次
    	if(site.getAcceptStatCode().contains(statusCode)){
    		if (cycleTriedTimesObject == null) {
                page.addTargetRequest(request.setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, 1));
            } else {
                int cycleTriedTimes = (Integer) cycleTriedTimesObject;
                cycleTriedTimes++;
                if(site.getCycleRetryTimes() == 0){
            		page.addTargetRequest(request.setPriority(0).putExtra(Request.CYCLE_TRIED_TIMES, cycleTriedTimes));
            	}else{
            		if (cycleTriedTimes >= site.getCycleRetryTimes()) {
                        return null;
                    }
            	}
            }
    	}
    	
        page.setNeedCycleRetry(true);
        return page;
    }
}
