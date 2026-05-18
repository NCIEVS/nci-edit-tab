package gov.nih.nci.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class CharMapper {
	
	private final String mapfile_name = "/symbolmap.data";
	private final String delim = "\\|";
	
	private Map<Integer, String> map = new HashMap<Integer, String>();
	
	public String fix(String val) {
		StringBuilder buf = new StringBuilder();
		for (int index = 0; index < val.length(); index++)
		{
			char ch = val.charAt(index);
			
			String re_val = map.get(val.codePointAt(index));
			if (re_val != null) {
				buf.append(re_val);
			} else {       	
				buf.append(ch);
			}
			
		}
		return buf.toString().trim();
	}
	
	public CharMapper() {
		
        
        BufferedReader inFile = null;
        String s;
		try {
			
			
			InputStream is = getClass().getResourceAsStream(mapfile_name); 
			inFile = new BufferedReader(new InputStreamReader(is));			
			
			while ((s = inFile.readLine()) != null) {
				s = s.trim();
				if (s.length() > 0) {
					if (s.startsWith("#")) {
						// ignore comment lines
					} else {
						String[] toks = s.split(delim);
						String src = toks[0];
						String tar = toks[1];
						map.put(Integer.parseInt(src.substring(2), 16), tar);						
					}
				}
			}
			inFile.close();
		} catch (Exception e) {
			System.err.println(e);
		}
		
	}
	
	

}
