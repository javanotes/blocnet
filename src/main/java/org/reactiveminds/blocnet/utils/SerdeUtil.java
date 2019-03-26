package org.reactiveminds.blocnet.utils;

import java.io.IOException;
import java.util.Base64;

import org.springframework.core.serializer.support.SerializationFailedException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SerdeUtil {

	private static final ObjectMapper mapper = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			//.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
			.enable(SerializationFeature.INDENT_OUTPUT)
			//.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
			;
	
	public static <T> byte[] toBytes(T entity) {
		//TODO : will have custom serde providers
		try {
			return mapper.writerFor(entity.getClass()).writeValueAsBytes(entity);
		} catch (JsonProcessingException e) {
			throw new SerializationFailedException(e.getMessage());
		}
	}
	public static <T> String toJson(T entity) {
		//TODO : will have custom serde providers
		try {
			return mapper.writerFor(entity.getClass()).writeValueAsString(entity);
		} catch (JsonProcessingException e) {
			throw new SerializationFailedException(e.getMessage());
		}
	}
	public static <T> T fromBytes(byte[] b, Class<T> type) {
		//TODO : will have custom serde providers
		try {
			return mapper.readerFor(type).readValue(b);
		} catch (IOException e) {
			throw new SerializationFailedException(e.getMessage());
		}
	}
	
	public static String encodeBytes(byte[] b) {
		return Base64.getEncoder().encodeToString(b);
	}
	
	public static byte[] decodeBytes(String bytes) {
		return Base64.getDecoder().decode(bytes);
	}
}
