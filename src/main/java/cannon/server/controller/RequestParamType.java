package cannon.server.controller;

public final class RequestParamType {
	private Type type;
	private boolean required = true;
	private String defaultValue = null;
	private String name = null;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	void setType(Type type) {
		this.type = type;
	}


	void setRequired(boolean required) {
		this.required = required;
	}


	void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}


	public Type getType() {
		return type;
	}


	public boolean isRequired() {
		return required;
	}


	public String getDefaultValue() {
		return defaultValue;
	}


	public enum Type{
		HTTP_REQUEST,
		HTTP_RESPONSE,
		STRING,
		LIST,
		MAP,
		SET,
		ARRARY,
		BOOLEAN,
		SHORT,
		INTEGER,
		LONG,
		FLOAT,
		DOUBLE,
		CHAR,
		BYTE
	}

}
