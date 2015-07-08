package us.codecraft.webmagic.exception;

public class ComponentNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public ComponentNotFoundException(){
		super();
	}
	
	public ComponentNotFoundException(String message){
		super(message);
	}
	
	public ComponentNotFoundException(String message,Throwable cause){
		super(message,cause);
	}
	
}
