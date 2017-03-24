package com.hisign.constants;

import java.io.File;
import java.io.IOException;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;

import com.hisign.exception.ConfigException;

public class Loader {
    
    public final static String HBVE_SECTTION = "hbve";
    
    public static String hbveMasterIp;
    public static int hbveMasterPort;
    public static int hbveWorkerCount;
    
    public void load(String path) throws InvalidFileFormatException, IOException, ConfigException {
        
        Ini ini = new Ini();
        ini.load(new File(path));
        
        Section section = ini.get(HBVE_SECTTION);
        
        if (section.containsKey("HBVE_master_ip"))
            hbveMasterIp = section.get("HBVE_master_ip");
        else throw new ConfigException("HBVE_master_ip error");
        
        if (section.containsKey("HBVE_master_port"))
            hbveMasterPort = Integer.parseInt(section.get("HBVE_master_port"));
        else throw new ConfigException("HBVE_master_port error");
        
        if (section.containsKey("HBVE_worker_thread_count"))
            hbveWorkerCount = Integer.parseInt(section.get("HBVE_worker_thread_count"));
        else throw new ConfigException("HBVE_worker_thread_count error");
    }
}
