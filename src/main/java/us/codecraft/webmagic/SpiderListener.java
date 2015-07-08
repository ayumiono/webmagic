package us.codecraft.webmagic;

/**
 * Listener of Spider on page processing. Used for monitor and such on.
 *
 * @author code4crafer@gmail.com
 * @since 0.5.0
 */
public interface SpiderListener {

    public void onSuccess(Request request);//下载成功

    public void onError(Request request);//下载失败
    
    public void onMatchSuccess(Request request);//匹配成功
    
    public void onMatchError(Request request);//匹配失败
    
}
