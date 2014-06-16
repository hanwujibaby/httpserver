package cannon.server.controller;

import java.io.IOException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

public final class XssFilterJsonSerializer extends JsonSerializer<String>{

	@Override
	public void serialize(String value, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
			JsonProcessingException {
		jgen.writeString(StringEscapeUtils.escapeHtml4(value));
	}
	
}
