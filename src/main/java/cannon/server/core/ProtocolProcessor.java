package cannon.server.core;


public interface ProtocolProcessor {
	void process()throws Throwable;
	void close();
}
