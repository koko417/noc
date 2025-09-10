package tanabu.noc.configuration;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

//TODO Change to abstract class from "RootConfig"
public class ManageConfig {
	ObjectMapper mapper;
	File file;
	
	public ManageConfig(String file) throws IOException {
		  this.file = new File(file);
		  if (!this.file.exists()) {
			  this.file.createNewFile();
		  }
		  this.mapper = new ObjectMapper(new YAMLFactory());
		  
	}

	public RootConfig load() throws StreamReadException, DatabindException, IOException {
		return mapper.readValue(file, RootConfig.class);
	}

}
