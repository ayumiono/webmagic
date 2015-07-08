package us.codecraft.webmagic;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;

import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Json;
import us.codecraft.webmagic.selector.Selectable;
import us.codecraft.webmagic.statusful.StatusfulConnection;
import us.codecraft.webmagic.utils.UrlUtils;

/**
 * Object storing extracted result and urls to fetch.<br>
 * Not thread safe.<br>
 * Main method：                                               <br>
 * {@link #getUrl()} get url of current page                   <br>
 * {@link #getHtml()}  get content of current page                 <br>
 * {@link #putField(String, Object)}  save extracted result            <br>
 * {@link #getResultItems()} get extract results to be used in {@link us.codecraft.webmagic.pipeline.Pipeline}<br>
 * {@link #addTargetRequests(java.util.List)} {@link #addTargetRequest(String)} add urls to fetch                 <br>
 *
 * @author code4crafter@gmail.com <br>
 * @see us.codecraft.webmagic.downloader.Downloader
 * @see us.codecraft.webmagic.processor.PageProcessor
 * @since 0.1.0
 */
public class Page {

    private Request request;

    private ResultItems resultItems = new ResultItems();

    private Html html;
    
    private Header[] headers;

    private Json json;

    private String rawText;
    
    private Selectable url;
    
    private String charset;

    private int statusCode;

    private boolean needCycleRetry = false;
    
    private List<Request> targetRequests;
    
    private String host;
    
    private StatusfulConnection conn;
    
    public Page() {
    }

    public Page setSkip(boolean skip) {
        resultItems.setSkip(skip);
        return this;

    }

    /**
     * store extract results
     *
     * @param key
     * @param field
     */
    public void putField(String key, Object field) {
        resultItems.put(key, field);
    }

    /**
     * get html content of page
     *
     * @return html
     */
    public Html getHtml() {
        if (html == null) {
            html = new Html(UrlUtils.fixAllRelativeHrefs(rawText, request.getUrl()));
        }
        return html;
    }

    /**
     * get json content of page
     *
     * @return json
     * @since 0.5.0
     */
    public Json getJson() {
        if (json == null) {
            json = new Json(rawText);
        }
        return json;
    }

    /**
     * @param html
     * @deprecated since 0.4.0
     * The html is parse just when first time of calling {@link #getHtml()}, so use {@link #setRawText(String)} instead.
     */
    public void setHtml(Html html) {
        this.html = html;
    }

    public List<Request> getTargetRequests() {
        return targetRequests;
    }

    /**
     * add urls to fetch
     *
     * @param requests
     */
    public void addTargetRequests(List<String> requests) {
        synchronized (this) {
        	if(targetRequests==null){
        		targetRequests = new ArrayList<Request>();
        	}
            for (String s : requests) {
                if (StringUtils.isBlank(s) || s.equals("#") || s.startsWith("javascript:")) {
                    continue;
                }
                s = UrlUtils.canonicalizeUrl(s, url.toString());
                targetRequests.add(new Request(s));
            }
        }
    }

    /**
     * add urls to fetch
     *
     * @param requests
     */
    public void addTargetRequests(List<String> requests, long priority) {
        synchronized (this) {
        	if(targetRequests==null){
        		targetRequests = new ArrayList<Request>();
        	}
            for (String s : requests) {
                if (StringUtils.isBlank(s) || s.equals("#") || s.startsWith("javascript:")) {
                    continue;
                }
                s = UrlUtils.canonicalizeUrl(s, url.toString());
                targetRequests.add(new Request(s).setPriority(priority));
            }
        }
    }

    /**
     * add url to fetch
     *
     * @param requestString
     */
    public void addTargetRequest(String requestString) {
        if (StringUtils.isBlank(requestString) || requestString.equals("#")) {
            return;
        }
        synchronized (this) {
        	if(targetRequests==null){
        		targetRequests = new ArrayList<Request>();
        	}
            requestString = UrlUtils.canonicalizeUrl(requestString, url.toString());
            targetRequests.add(new Request(requestString));
        }
    }

    /**
     * add requests to fetch
     *
     * @param request
     */
    public void addTargetRequest(Request request) {
        synchronized (this) {
        	if(targetRequests==null){
        		targetRequests = new ArrayList<Request>();
        	}
            targetRequests.add(request);
        }
    }

    /**
     * get url of current page
     *
     * @return url of current page
     */
    public Selectable getUrl() {
        return url;
    }

    public void setUrl(Selectable url) {
        this.url = url;
    }

    public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
     * get request of current page
     *
     * @return request
     */
    public Request getRequest() {
        return request;
    }

    public boolean isNeedCycleRetry() {
        return needCycleRetry;
    }

    public void setNeedCycleRetry(boolean needCycleRetry) {
        this.needCycleRetry = needCycleRetry;
    }

	public void setRequest(Request request) {
        this.request = request;
        this.resultItems.setRequest(request);
    }

    public ResultItems getResultItems() {
        return resultItems;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getRawText() {
        return rawText;
    }

    public Page setRawText(String rawText) {
        this.rawText = rawText;
        return this;
    }
    
    @Override
    public String toString() {
        return "Page{" +
                "request=" + request +
                ", resultItems=" + resultItems +
                ", rawText='" + rawText + '\'' +
                ", url=" + url +
                ", statusCode=" + statusCode +
                ", targetRequests=" + targetRequests +
                '}';
    }

	public StatusfulConnection getConn() {
		return conn;
	}

	public void setConn(StatusfulConnection conn) {
		this.conn = conn;
	}

	public Header[] getHeaders() {
		return headers;
	}

	public void setHeaders(Header[] headers) {
		this.headers = headers;
	}
	
	public String getHeader(String name){
		if(headers==null){
			return null;
		}
		for(Header header : headers){
			if(name.equals(header.getName())){
				return header.getValue();
			}
		}
		return null;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}
}
