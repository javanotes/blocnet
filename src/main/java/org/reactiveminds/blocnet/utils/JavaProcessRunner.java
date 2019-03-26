package org.reactiveminds.blocnet.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * A simple java class runner that executes the class as a separate jvm process. Assuming there is no external class/library dependency, other than
	that already set for the invoker process. The classpath and java home will be resolved as system property from the running jvm.
 * @author Sutanu_Dalui
 *
 */
public class JavaProcessRunner implements Runnable {

	private final Class<?> mainClass;
	/**
	 * Create new runner for the given mainClass. 
	 * @param mainClass a class with public static void main()
	 */
	public JavaProcessRunner(Class<?> mainClass) {
		this.mainClass = mainClass;
	}
	private ProcessBuilder processBuilder;
	/**
	 * Initialize with program arguments. 
	 * @param args
	 */
	public void initialize(Object...args) {
		String separator = System.getProperty("file.separator");
	    String classpath = System.getProperty("java.class.path");
	    String path = System.getProperty("java.home")
	                + separator + "bin" + separator + "java";
	    
	    List<String> command = new ArrayList<>(Arrays.asList(path, "-cp", classpath, mainClass.getName()));
	    for(Object o : args)
	    	command.add(o.toString());
	    
	    processBuilder = new ProcessBuilder(command);
	    processBuilder.redirectErrorStream(true);
	}

	private StringBuilder result = new StringBuilder();
	private int exitCode;
	public String getOutputString() {
		return result.toString();
	}
	@Override
	public void run() {
		Process process = null;
		try {
			process = processBuilder.start();
			setExitCode(process.waitFor());
			try(BufferedReader b = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))){
				String line = null;
				while((line=b.readLine()) != null) {
					result.append(line);
				}
			}
			
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			process.destroy();
		}
	}

	public static void main(String[] args) {
		JavaProcessRunner runner = new JavaProcessRunner(Crypto.class);
		runner.initialize("$genesis", "0000000000000000000000000000000000000000000000000000000000000000", -1);
		runner.run();
		System.out.println("result: "+runner.getOutputString());
		System.out.println("Exit code: "+runner.getExitCode());
	}
	public int getExitCode() {
		return exitCode;
	}
	private void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}
}
