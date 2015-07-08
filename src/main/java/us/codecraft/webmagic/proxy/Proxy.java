package us.codecraft.webmagic.proxy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;

/**    
 * >>>> Proxy lifecycle 
 
        +----------+     +-----+
        | last use |     | new |
        +-----+----+     +---+-+
              |  +------+   |
              +->| init |<--+
                 +--+---+
                    |
                    v
                +--------+
           +--->| borrow |
           |    +---+----+
           |        |+------------------+
           |        v
           |    +--------+
           |    | in use |  Respone Time
           |    +---+----+
           |        |+------------------+
           |        v
           |    +--------+
           |    | return |
           |    +---+----+
           |        |+-------------------+
           |        v
           |    +-------+   reuse interval
           |    | delay |   (delay time)
           |    +---+---+
           |        |+-------------------+
           |        v
           |    +------+
           |    | idle |    idle time
           |    +---+--+
           |        |+-------------------+
           +--------+
 */

/**
 * Object has these status of lifecycle above.<br>
 * 
 * @author yxssfxwzy@sina.com <br>
 * @since 0.5.1
 * @see ProxyPool
 */

public class Proxy implements Delayed, Serializable {

	private static final long serialVersionUID = 228939737383625551L;
	
	public static final int SUCCESS = 200;
	public static final int ERROR_404 = 404;
	
	
	public static final int ERROR_403 = 403;
	public static final int ERROR_500 = 500;
	public static final int ERROR_BANNED = 10000;// banned by website
	public static final int ERROR_Proxy = 10001;// the proxy itself failed
	public static final int ERROR_REQUEST_TIME_OUT = 10002;// the target request time out
	public static final int ERROR_PROXY_TIME_OUT = 10003;//conn to proxyhost time out-->speed is too low
	//public static final int ERROR_ILLEGAL_REQUEST_URI = 10004;
	public static final int ERROR_SOCKET_READ_TIME_OUT = 10005;//SocketTimeoutException: Read timed out
	public static final int ERROR_DEFAULT = 10009;//other errs
	

	
	//代理IP级别的错误
	public static final Set<Integer> PROXY_ERROR_CODE_SET = new HashSet<Integer>();

	private final HttpHost httpHost;

	protected int reuseTimeInterval = 1500;// ms
	protected Long canReuseTime = 0L;
	protected Long lastBorrowTime = System.currentTimeMillis();
	protected Long responseTime = 0L;
	protected int socketTimeout = 10000;

	protected int failedNum = 0;//只记录前一周期的失败数
	protected int successNum = 0;//只记录前一周期的成功数
	protected int borrowNum = 0;//只记录前一周期的使用数

	protected List<Integer> failedErrorType = new ArrayList<Integer>();
	
	static{
		PROXY_ERROR_CODE_SET.add(ERROR_DEFAULT);
		PROXY_ERROR_CODE_SET.add(ERROR_403);
		PROXY_ERROR_CODE_SET.add(ERROR_500);
		PROXY_ERROR_CODE_SET.add(ERROR_BANNED);
		PROXY_ERROR_CODE_SET.add(ERROR_Proxy);
		PROXY_ERROR_CODE_SET.add(ERROR_PROXY_TIME_OUT);
		PROXY_ERROR_CODE_SET.add(ERROR_SOCKET_READ_TIME_OUT);
	}
	
	public Proxy(){
		this(null);
	}

	public Proxy(HttpHost httpHost) {
		this.httpHost = httpHost;
		this.canReuseTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(reuseTimeInterval, TimeUnit.MILLISECONDS);
	}

	public Proxy(HttpHost httpHost, int reuseInterval) {
		this.httpHost = httpHost;
		this.canReuseTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(reuseInterval, TimeUnit.MILLISECONDS);
	}

	public int getSuccessNum() {
		return successNum;
	}
	
	public void resetSuccessNum(){
		successNum = 0;
	}
	
	public void resetBorrowNum(){
		borrowNum = 0;
	}
	
	public void resetFailedNum(){
		failedNum = 0;
	}

	public void successNumIncrement(int increment) {
		this.successNum += increment;
	}

	public Long getLastUseTime() {
		return lastBorrowTime;
	}

	public void setLastBorrowTime(Long lastBorrowTime) {
		this.lastBorrowTime = lastBorrowTime;
	}

	public void recordResponse() {
		this.responseTime = (System.currentTimeMillis() - lastBorrowTime + responseTime) / 2;
		this.lastBorrowTime = System.currentTimeMillis();
	}

	public List<Integer> getFailedErrorType() {
		return failedErrorType;
	}

	public void setFailedErrorType(List<Integer> failedErrorType) {
		this.failedErrorType = failedErrorType;
	}

	public void fail(int failedErrorType) {
		this.failedNum++;
		this.failedErrorType.add(failedErrorType);
	}

	public void setFailedNum(int failedNum) {
		this.failedNum = failedNum;
	}

	public int getFailedNum() {
		return failedNum;
	}

	public String getFailedType() {
		String re = "";
		for (Integer i : this.failedErrorType) {
			re += i + " . ";
		}
		return re;
	}

	public HttpHost getHttpHost() {
		return httpHost;
	}

	public int getReuseTimeInterval() {
		return reuseTimeInterval;
	}
	
	public Long getCanReuseTime() {
		return canReuseTime;
	}

	public void setReuseTimeInterval(int reuseTimeInterval) {
		this.reuseTimeInterval = reuseTimeInterval;
		this.canReuseTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(reuseTimeInterval, TimeUnit.MILLISECONDS);

	}
	
	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
	
	public int getSocketTimeout(){
		return this.socketTimeout;
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(canReuseTime - System.nanoTime(), TimeUnit.NANOSECONDS);
	}

	@Override
	public int compareTo(Delayed o) {
		Proxy that = (Proxy) o;
		return canReuseTime > that.canReuseTime ? 1 : (canReuseTime < that.canReuseTime ? -1 : 0);

	}

	@Override
	public String toString() {

		String re = String.format("host: %15s >> %5dms >> success: %-3.2f%% >> borrow: %d", httpHost.getAddress().getHostAddress(), responseTime,
				successNum * 100.0 / borrowNum, borrowNum);
		return re;

	}

	public void borrowNumIncrement(int increment) {
		this.borrowNum += increment;
	}

	public int getBorrowNum() {
		return borrowNum;
	}

}
