package org.reactiveminds.blocnet.utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.reactiveminds.blocnet.dto.TxnRequest;
import org.reactiveminds.blocnet.model.BlockData;
import org.springframework.core.serializer.support.SerializationFailedException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SerdeUtil {

	// Pool constructor arguments: thread safe, soft references, maximum capacity
	private static Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, 8) {
	   protected Kryo create () {
	      Kryo kryo = new Kryo();
	      kryo.register(TxnRequest.class);
	      kryo.register(ArrayList.class);
	      kryo.register(byte[].class);
	      kryo.register(BlockData.class);
	      // Configure the Kryo instance.
	      return kryo;
	   }
	};

	private static final ObjectMapper mapper = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			//.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
			.enable(SerializationFeature.INDENT_OUTPUT)
			//.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
			;
	
	public static <T> byte[] toBytes(T entity) {
		Kryo kryo = kryoPool.obtain();
		try (Output o = new ByteBufferOutput(1024, -1)){
			kryo.writeObject(o, entity);
			return o.toBytes();
		} catch (Exception e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
		finally {
			kryoPool.free(kryo);
		}
	}
	public static <T> T fromBytes(byte[] b, Class<T> type) {
		Kryo kryo = kryoPool.obtain();
		try (Input in = new ByteBufferInput(b)){
			return kryo.readObject(in, type);
		} catch (Exception e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
		finally {
			kryoPool.free(kryo);
		}
	}
	public static <T> String toJson(T entity) {
		try {
			return mapper.writerFor(entity.getClass()).withDefaultPrettyPrinter().writeValueAsString(entity);
		} catch (JsonProcessingException e) {
			throw new SerializationFailedException(e.getMessage());
		}
	}
	
	/**
	 * To base64 encoded string
	 * @param b
	 * @return
	 */
	public static String encodeBytes(byte[] b) {
		return Base64.getEncoder().encodeToString(b);
	}
	/**
	 * From base64 encoded string
	 * @param bytes
	 * @return
	 */
	public static byte[] decodeBytes(String bytes) {
		return Base64.getDecoder().decode(bytes);
	}
	/**
	 * Unzip byte[]
	 * @param bytes
	 * @return
	 */
	public static byte[] decompressBytes(byte[] bytes){
        
        ByteArrayOutputStream baos = null;
        Inflater iflr = new Inflater();
        iflr.setInput(bytes);
        baos = new ByteArrayOutputStream();
        byte[] tmp = new byte[4*1024];
        try{
            while(!iflr.finished()){
                int size = iflr.inflate(tmp);
                baos.write(tmp, 0, size);
            }
        } catch (Exception ex){
             
        } finally {
            try{
                if(baos != null) baos.close();
            } catch(Exception ex){}
        }
         
        return baos.toByteArray();
    }
	/**
	 * Zip byte[]
	 * @param bytes
	 * @return
	 */
	public static byte[] compressBytes(byte[] bytes, boolean high){
        
        ByteArrayOutputStream baos = null;
        Deflater dfl = new Deflater();
        dfl.setLevel(high ? Deflater.BEST_COMPRESSION : Deflater.BEST_SPEED);
        dfl.setInput(bytes);
        dfl.finish();
        baos = new ByteArrayOutputStream();
        byte[] tmp = new byte[4*1024];
        try{
            while(!dfl.finished()){
                int size = dfl.deflate(tmp);
                baos.write(tmp, 0, size);
            }
        } catch (Exception ex){
             
        } finally {
            try{
                if(baos != null) baos.close();
            } catch(Exception ex){}
        }
         
        return baos.toByteArray();
    }
	/**
	 * 
	 * @param chainPool
	 * @return
	 */
	public static byte[] toBlockBytes(BlockData chainPool) {
		Kryo kryo = kryoPool.obtain();
		try (Output o = new ByteBufferOutput(8192, -1)){
			//o.writeString(chainPool.getChain());
			//o.writeInt(chainPool.getRequests().size());
			kryo.writeObject(o, chainPool);
			return o.toBytes();
		} catch (Exception e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
		finally {
			kryoPool.free(kryo);
		}
	}
	
}
