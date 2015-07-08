package us.codecraft.webmagic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.statusful.StatusfulConnection;
import us.codecraft.webmagic.utils.Experimental;

/**
 * Object contains url to crawl.<br>
 * It contains some additional information.<br>
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.1.0
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 2062192774891352043L;

    public static final String CYCLE_TRIED_TIMES = "_cycle_tried_times";
    public static final String STATUS_CODE = "statusCode";
    //public static final String RESPONSE_STATUS_CODE = "responseStatusCode";
    public static final String STATUSFUL_CONN_CODE = "statusfulConnCode";
    public static final String PROXY = "proxy";
    public static final String STATUSFUL_CONN = "statusfulConn";
    public static final String BIZCODE = "bizcode";
    public static final String MATCH_ERR_MSG = "matchErrMsg";
    public static final String HOST = "Host";
    
    public static final int ERROR_ILLEGAL_REQUEST_URI = 10004;

    private String url;

    private String method;
    
    private Integer fieldRuleId;//新的下载请求对应的是哪条fieldrule
    
    /**
     * Store additional information in extras.
     */
    private Map<String, Object> extras;
    
    /**
     * Store inherit fields values from last request.
     */
    private Map<String, Object> inheritFields;
    
    /**
     * headers for each request, even though requests are in from same site
     */
    private Map<String, String> headers;
    
    /**
     * in some situation, to complete a page,must do more than one request,the link of requests does not have a strict hierarchy
     */
    private Request nextRequest;
    
    /**
     * Priority of the request.<br>
     * The bigger will be processed earlier. <br>
     * @see us.codecraft.webmagic.scheduler.PriorityScheduler
     */
    private long priority;

    public Request() {
    }

    public Request(String url) {
        this.url = url;
    }

    public long getPriority() {
        return priority;
    }

    /**
     * Set the priority of request for sorting.<br>
     * Need a scheduler supporting priority.<br>
     * @see us.codecraft.webmagic.scheduler.PriorityScheduler
     *
     * @param priority
     * @return this
     */
    @Experimental
    public Request setPriority(long priority) {
        this.priority = priority;
        return this;
    }

    public Object getExtra(String key) {
        if (extras == null) {
            return null;
        }
        return extras.get(key);
    }

    public Request putExtra(String key, Object value) {
        if (extras == null) {
            extras = new HashMap<String, Object>();
        }
        extras.put(key, value);
        return this;
    }
    
    public Object getInheritField(String key){
    	if(inheritFields == null){
    		return null;
    	}
    	return inheritFields.get(key);
    }
    
    public void addInheritField(String key, Object value){
    	if(inheritFields == null){
    		inheritFields = new HashMap<String, Object>();
    	}
    	inheritFields.put(key, value);
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Request request = (Request) o;

        if (!url.equals(request.url)) return false;

        return true;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    public void setExtras(Map<String, Object> extras) {
        this.extras = extras;
    }

    public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public Request getNextRequest() {
		return nextRequest;
	}

	public void setNextRequest(Request nextRequest) {
		this.nextRequest = nextRequest;
	}

	public Proxy getProxy() {
		if(getExtra(PROXY)==null){
    		return null;
    	}
    	return (Proxy)getExtra(PROXY);
	}

	public void setProxy(Proxy proxy) {
		putExtra(PROXY, proxy);
	}
	
	public void clean(){
		this.extras.remove(PROXY);
		this.extras.remove(STATUSFUL_CONN);
	}
	
	public void setStatusCode(int statCode){
		putExtra(STATUS_CODE, statCode);
	}
	
	public Integer getStatusCode(){
		if(getExtra(STATUS_CODE) == null){
			return null;
		}
		return (Integer) getExtra(STATUS_CODE);
	}
	
	public void setStatusfulConnCode(int statCode){
		putExtra(STATUSFUL_CONN_CODE, statCode);
	}
	
	public Integer getStatusfulConnCode(){
		if(getExtra(STATUSFUL_CONN_CODE) == null){
			return null;
		}
		return (Integer) getExtra(STATUSFUL_CONN_CODE);
	}
	
	public void setStatusfulConn(StatusfulConnection conn){
		putExtra(STATUSFUL_CONN, conn);
	}
	
	public StatusfulConnection getStatusfulConn(){
		if(getExtra(STATUSFUL_CONN) == null){
			return null;
		}
		return (StatusfulConnection) getExtra(STATUSFUL_CONN);
	}

	public void setUrl(String url) {
        this.url = url;
    }

    /**
     * The http method of the request. Get for default.
     * @return httpMethod
     * @see us.codecraft.webmagic.utils.HttpConstant.Method
     * @since 0.5.0
     */
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
    
    public Integer getFieldRuleId() {
		return fieldRuleId;
	}

	public void setFieldRuleId(int fieldRuleId) {
		this.fieldRuleId = fieldRuleId;
	}

	public String getBizcode(){
    	if(getExtra(BIZCODE)==null){
    		return null;
    	}
    	return (String)getExtra(BIZCODE);
    }
    
    public void setBizcode(String bizcode){
    	putExtra(BIZCODE, bizcode);
    }
    
    public String getMatchErrMsg(){
    	if(getExtra(MATCH_ERR_MSG)==null){
    		return null;
    	}
    	return (String)getExtra(MATCH_ERR_MSG);
    }
    
    //{'fieldname':'%s','fieldrule':'%s','type':'xpath'}
    public void setMatchErrMsg(String matchErrMsg){
    	putExtra(MATCH_ERR_MSG, matchErrMsg);
    }

    public Map<String, Object> getInheritFields() {
		return inheritFields;
	}

	public void setInheritFields(Map<String, Object> inheritFields) {
		this.inheritFields = inheritFields;
	}
	
	@Override
    public String toString() {
        return "Request{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", extras=" + extras +
                ", priority=" + priority +
                '}';
    }
}
