package org.reactiveminds.blocnet.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.annotation.PostConstruct;

import org.reactiveminds.blocnet.utils.SerdeUtil;
import org.reactiveminds.blocnet.utils.err.FileAccessException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnBean(value = RestApi.class)
class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;
    private Path fileStorageLocation;
    
    @PostConstruct
    private void init() {
        try {
        	fileStorageLocation = Files.createDirectories(Paths.get(uploadDir).toAbsolutePath().normalize());
        } catch (Exception ex) {
            throw new BeanCreationException("Could not create the directory where the uploaded files will be stored", ex);
        }
    }

    public Path storeFile(MultipartFile file) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if(fileName.contains("..")) {
                throw new IllegalArgumentException("Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetLocation;
            
        } catch (IOException ex) {
            throw new FileAccessException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }
    static class FileContent{
    	@Override
		public String toString() {
			return "FileContent [name=" + name + ", contentType=" + contentType + ", size=" + size + "]";
		}

		protected FileContent(byte[] bytes, String name, String contentType, long size) {
			super();
			this.bytes = bytes;
			this.name = name;
			this.contentType = contentType;
			this.size = size;
		}
		final byte [] bytes;
    	final String name;
    	final String contentType;
    	final long size;
    	
    	public byte[] encode() {
    		byte[] n = name.getBytes(StandardCharsets.UTF_8);
    		byte[] c = contentType.getBytes(StandardCharsets.UTF_8);
    		ByteBuffer buff = ByteBuffer.allocate(8 + 8 + n.length + c.length + bytes.length);
    		buff.putInt(n.length);
    		buff.put(n);
    		buff.putInt(c.length);
    		buff.put(c);
    		buff.putLong(size);
    		buff.put(bytes);
    		
    		return SerdeUtil.compressBytes(buff.array(), true);
    	}
    	
    	public static FileContent decode(byte[] b) {
    		byte[] orig = SerdeUtil.decompressBytes(b);
    		
    		ByteBuffer buff = ByteBuffer.wrap(orig);
    		int n = buff.getInt();
    		byte[] c = new byte[n];
    		buff.get(c);
    		
    		String name = new String(c, StandardCharsets.UTF_8);
    		
    		n = buff.getInt();
    		c = new byte[n];
    		buff.get(c);
    		
    		String type = new String(c, StandardCharsets.UTF_8);
    		long size = buff.getLong();
    		
    		n = buff.remaining();
    		c = new byte[n];
    		buff.get(c);
    		
    		return new FileContent(c, name, type, size);
    	}
    }
    
    
    public FileContent getContent(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if(fileName.contains("..")) {
                throw new IllegalArgumentException("Filename contains invalid path sequence " + fileName);
            }
            return new FileContent(file.getBytes(), fileName, file.getContentType(), file.getSize());
            
        } catch (Exception ex) {
            throw new FileAccessException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new FileAccessException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileAccessException("File not found " + fileName, ex);
        }
    }

}
