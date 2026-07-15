package gov.nih.nci.utils.batch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import gov.nih.nci.ui.BatchProcessOutputPanel;
import gov.nih.nci.ui.NCIEditTab;
import gov.nih.nci.utils.CharMapper;

/**
 * @Author: Bob Dionne
 */

public class BatchLoadTask extends BatchTask {	

	
	
	List<String> codes = new ArrayList<String>();
	
	HashMap<String, String> names = new HashMap<String, String>();

	public BatchLoadTask(BatchProcessOutputPanel be, NCIEditTab tab, String infile,
			String outfile, String fileDelim) {
		super(be, tab);
		this.infile = infile;
		this.outfile = outfile;
		this.fieldDelim = fileDelim;
		setMessage("Batch Load processing in progress, please wait ...");
	}

	public static long create_time = 0;
	public static long evs_time = 0;
	public boolean processTask(int taskId) {
		try {
			String s = (String) data_vec.elementAt(taskId);
			Vector<String> w = parseTokens(s);
			
			String name = (String) w.elementAt(0);
			String sup = (String) w.elementAt(1);
			String code = codes.get(taskId);
			
			if (super.checkNoErrors(w, taskId)) {
				tab.createNewChild(tab.getClass(sup), Optional.of(name), Optional.of(code), true);
			} else {
				return false;
			}
			
			super.print("Creating:\t" + name + "\t" + code + "\t" + sup);
			// TODO: Should we also record history?

		} catch (Exception e) {
			super.print("Exception caught" + e.toString());
			return false;
		}

		return true;
	}
	
	public boolean begin() {
		codes = tab.generateCodes(max);
		return super.begin();
		
		
	}
	
	private CharMapper mapper = new CharMapper();

	public ArrayList<Vector<String>> validateData(Vector<String> v) {
		Vector<String> w = new Vector<String>();
		Vector<String> warnings = new Vector<String>();
		ArrayList<Vector<String>> err_warn = new ArrayList<Vector<String>>();
		err_warn.add(w);
		err_warn.add(warnings);
		
		try {
			
				
				String fname = mapper.fix((String) v.elementAt(0));
				
				if (!tab.validPrefName(fname)) {
					String error_msg = " -- prefered name cannot contain special chars.";
					w.add(error_msg);
					System.out.println(error_msg);
					
				}
				// TODO: Check that name doesn not already exist, need to look up by preferred name
				
				if (tab.existsPrefName(fname)) {
					String error_msg = " -- a class with this preferred name already exists.";
					w.add(error_msg);
					System.out.println(error_msg);
					
				}
				if (names.get(fname) != null) {
					String error_msg = " -- a class with this preferred name was already used in this load file.";
					w.add(error_msg);
					System.out.println(error_msg);
					
				} else {
					names.put(fname, fname);
				}
				String sup = (String) v.elementAt(1);
				
				if (tab.getClass(sup) == null) {
					String error_msg = " -- super class does not exist.";
					w.add(error_msg);
					System.out.println(error_msg);
				}
				
		} catch (Exception e) {
			super.print("Exception caught" + e.toString());
			return null;
		}

		return err_warn;

	}

	


}
