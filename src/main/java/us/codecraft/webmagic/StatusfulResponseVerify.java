package us.codecraft.webmagic;



public interface StatusfulResponseVerify {

	/**
	 * 检验当前ip是否被禁用掉
	 * 
	 * @return
	 */
	public boolean validProxyIpSafe(Page page);
	
	/**
	 * 检验当前登录用户是否已失效
	 * 
	 * @return true：没有，false：已失效
	 */
	public boolean validExpire(Page page) ;
	
	/**
	 * 检验当前登录用户是否已是异常用户，账号被禁用
	 * 
	 * @return
	 */
	public boolean validUser(Page page) ;
	
	/**
	 * 检验抓取网页内容是否正常，默认值true
	 * @param content
	 * @return
	 */
	public boolean validPageContent(Page page);
	
	/**
	 * 检验当前登录用户抓取行为是否正常，默认值true
	 * @param content
	 * @return
	 */
	public boolean validUserAction(Page page);

}
