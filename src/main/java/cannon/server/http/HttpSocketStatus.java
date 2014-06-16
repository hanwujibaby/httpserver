package cannon.server.http;

public enum HttpSocketStatus {
	SKIP_CONTROL_CHARS,
	READ_INITIAL,//读取
	READ_HEADER,
	READ_VARIABLE_LENGTH_CONTENT,
	RUNNING,
	WRITING;
}
