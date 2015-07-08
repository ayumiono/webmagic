package us.codecraft.webmagic.statusful;

public interface StatusfulConnectionPool {
	public StatusfulConnection getConn();
	public void returnConn(StatusfulConnection conn, int statCode);
}
