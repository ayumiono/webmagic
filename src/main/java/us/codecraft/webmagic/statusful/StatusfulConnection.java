package us.codecraft.webmagic.statusful;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class StatusfulConnection implements Delayed {
	
	public static final int SUCCESS = 20000;//正常
	public static final int ACCOUNT_TOO_OFFTEN = 20001;//频率过高
	public static final int INVALID_ACCOUNT = 20002;//账号失效
	public static final int AUTH_EXPIRE = 20003;//登录过期
	public static final int PROXY_ERROR = 20004;//代理IP异常
	public static final int PROXYIP_TOO_OFFTEN = 20005;//ip段太频繁

	@Override
	public long getDelay(TimeUnit unit) {
		return 0;
	}

	@Override
	public int compareTo(Delayed o) {
		return 0;
	}

	public boolean isValid() {
		return true;
	}

}
