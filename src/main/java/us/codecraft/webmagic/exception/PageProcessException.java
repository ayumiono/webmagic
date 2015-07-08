package us.codecraft.webmagic.exception;

public class PageProcessException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public PageProcessException(){
		super();
	}
	
	public PageProcessException(String message){
		super(message);
	}
	
	public PageProcessException(String message,Throwable cause){
		super(message,cause);
	}

}
