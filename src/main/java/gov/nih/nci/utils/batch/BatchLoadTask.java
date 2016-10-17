package gov.nih.nci.utils.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import gov.nih.nci.ui.BatchProcessOutputPanel;
import gov.nih.nci.ui.NCIEditTab;

/**
 * @Author: Bob Dionne
 */

public class BatchLoadTask extends BatchTask {	

	NCIEditTab tab = null;
	
	List<String> codes = new ArrayList<String>();

	public BatchLoadTask(BatchProcessOutputPanel be, NCIEditTab tab, String infile,
			String outfile) {
		super(be);
		this.tab = tab;
		this.infile = infile;
		this.outfile = outfile;
		
		data_vec = getData(infile);
		setMax(data_vec.size());
		setMessage("Batch Load processing in progress, please wait ...");
	}

	public static long create_time = 0;
	public static long evs_time = 0;
	public boolean processTask(int taskId) {
		try {
			String s = (String) data_vec.elementAt(taskId);
			Vector<String> w = getTokenStr(s, 2);
			
			String name = (String) w.elementAt(0);
			//String pt = (String) w.elementAt(1);
			String sup = (String) w.elementAt(1);
			
			
			
			/**
			String sem_type = "";
			if (w.size() > 3) {
				sem_type = (String) w.elementAt(3);
			}
			*/
			//
			

			// TODO: Batch these all up and do a single commit
			if (super.checkNoErrors(w, taskId)) {
				tab.createNewChild(tab.getClass(sup), Optional.of(name), Optional.of(codes.get(taskId)));
				//Thread.sleep(1);
				//tab.commitChanges();
			} else {
				return false;
			}
			
			
			
			
			
			

			// owlModel.beginTransaction("BatchLoad. Creating " + name);
			super.print("Creating " + name);

			/**
			if (!NCIEditFilter.checkXMLNCNameCompliance(name)) {
				super.print("\t" + name + " is invalid, unable to create. \n");
				return false;

			} else {
				long beg = System.currentTimeMillis();
				owlModel.beginTransaction("Batchload. Processing " + s);
				OWLNamedClass cls = wrapper.createCls(name, pt, sup);
				create_time += System.currentTimeMillis() - beg;
				

				if (cls != null) {
					beg = System.currentTimeMillis();
					if (sem_type.equalsIgnoreCase("NA")) {
						// do nothing
					} else {
						if (this.tab.getFilter().checkSemanticTypeValue(sem_type)) {
							wrapper.addAnnotationProperty(name, SemanticTypeUtil.SEMANTICTYPE,
									sem_type);
						}
					}
					
				    tab.recordHistory(NCIEditTab.EVSHistoryAction.CREATE, cls, "");
				    evs_time += System.currentTimeMillis() - beg;
					super.print("\t" + name + " created. \n");
				} else {
					super.print("\t" + "Unable to create class " + name + "\n");
					return false;
				}
				owlModel.commitTransaction();
			}
			*/

		} catch (Exception e) {
			super.print("Exception caught" + e.toString());
			return false;
		}

		return true;
	}
	
	public boolean complete() {
		tab.commitChanges();
		tab.disableBatchMode();
		return true;
	}
	
	public void cancelTask() {
		super.cancelTask();
		tab.undoChanges();
		tab.disableBatchMode();
	}
	
	public boolean begin() {
		codes = tab.generateCodes(max);
		tab.enableBatchMode();
		return true;
	}

	
	/*
	 * Olfactory_Cistern_sub_1 Olfactory_Cistern_pt_1 Olfactory_Cistern
	 * Olfactory_Cistern_sub_2 Olfactory_Cistern_pt_2 Olfactory_Cistern
	 * Olfactory_Cistern_sub_3 Olfactory_Cistern_pt_3 Olfactory_Cistern
	 * Olfactory_Cistern_sub_4 Olfactory_Cistern_pt_4 Olfactory_Cistern
	 * Olfactory_Cistern_sub_5 Olfactory_Cistern_pt_5 Olfactory_Cistern
	 * Olfactory_Cistern_sub_6 Olfactory_Cistern_pt_6 Olfactory_Cistern
	 * Olfactory_Cistern_sub_7 Olfactory_Cistern_pt_7 Olfactory_Cistern
	 * Olfactory_Cistern_sub_8 Olfactory_Cistern_pt_8 Olfactory_Cistern
	 */

	public Vector<String> validateData(Vector<String> v) {
		Vector<String> w = new Vector<String>();
		try {
			
				
				String name = (String) v.elementAt(0);
				String sup = (String) v.elementAt(1);
				
				if (tab.getClass(sup) == null) {
					String error_msg = " -- super class does not exist.";
					w.add(error_msg);
					System.out.println(error_msg);
				}
				
				/**
				String sem_type = "";
				if (v.size() > 3) {
					sem_type = (String) v.elementAt(3);
				}
				
				if (sem_type.equalsIgnoreCase("NA")) {
					// do nothing
				} else {
					/**
					if (this.tab.getFilter().checkSemanticTypeValue(sem_type)) {
						//ok						
					} else {
						String error_msg = " -- semantic type " + sem_type + " is invalid.";
						w.add(error_msg);
						System.out.println(error_msg);
						
					}
					
					
				}
				*/
				
				/**

				if (!NCIEditFilter.checkXMLNCNameCompliance(name)) {
					String error_msg = " -- concept name " + name + " is invalid.";
					w.add(error_msg);
					System.out.println(error_msg);
				}

				if (owlModel.getRDFSNamedClass(name) != null) {
					String error_msg = " -- concept " + name + " already exists.";
					w.add(error_msg);
					System.out.println(error_msg);
				}

				if (owlModel.getRDFSNamedClass(sup) == null) {
					String error_msg = " -- superconcept does not exist.";
					w.add(error_msg);
					System.out.println(error_msg);
				} else if (wrapper.isRetired(wrapper.getOWLNamedClass(sup))) {
	                String error_msg = " -- cannot edit retired concept";
	                w.add(error_msg);
	                System.out.println(error_msg);
	            }
	            */
				
				
			

		} catch (Exception e) {
			super.print("Exception caught" + e.toString());
			return null;
		}

		return w;

	}

	


}
