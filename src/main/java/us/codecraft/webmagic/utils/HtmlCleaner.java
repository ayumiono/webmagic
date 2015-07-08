package us.codecraft.webmagic.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlCleaner {
	
	public static final String regEx_html = "<[^>]+>";
	private static final String regEx_space = "\t|\r|\n";
	
	/**
	 * 去除html标签
	 * 去除空格，回车
	 * @return
	 * TODO
	 */
	public static String clean(String content){
		Pattern p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
        Matcher m_html = p_html.matcher(content);
        content = m_html.replaceAll("");
        Pattern p_space = Pattern.compile(regEx_space, Pattern.CASE_INSENSITIVE);  
        Matcher m_space = p_space.matcher(content);
        content = m_space.replaceAll("");
		return content;
	}
	
	public static void main(String[] args){
		String html = "<a class=\"ShuKeyWordLink\" target=\"_blank\" href=\"http://car.autohome.com.cn/price/brand-15.html\">宝马</a>";
		System.out.println(clean(html));
	}
}
