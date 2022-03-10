package gov.nih.nci.utils;

import static gov.nih.nci.ui.NCIEditTabConstants.LABEL_PROP;
import static gov.nih.nci.ui.NCIEditTabConstants.PREF_NAME;
import static gov.nih.nci.ui.NCIEditTabConstants.PRE_MERGE_ROOT;
import static gov.nih.nci.ui.NCIEditTabConstants.PRE_RETIRE_ROOT;
import static gov.nih.nci.ui.NCIEditTabConstants.RETIRE_CONCEPTS_ROOT;
import static gov.nih.nci.ui.NCIEditTabConstants.RETIRE_ROOT;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import edu.stanford.protege.metaproject.api.ProjectId;
import gov.nih.nci.api.RuleService;
import gov.nih.nci.ui.NCIEditTab;
import gov.nih.nci.ui.NCIEditTabConstants;
import gov.nih.nci.ui.RuleServiceLoader;

public class PropertyCheckUtil {

	private RuleService ruleservice;
	private NCIEditTab tab;
	
	public PropertyCheckUtil() {
		createRuleService();
		tab = NCIEditTab.currentTab();
	}
	
	private void createRuleService() {
		try {
			ruleservice = RuleServiceLoader.instanceOf(RuleService.class);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	public boolean syncDefinition(OWLClass cls) {
    	List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    	OWLAnnotationProperty definition = tab.getDefinition();
    	
    	List<OWLAnnotationAssertionAxiom> assertions = new ArrayList<OWLAnnotationAssertionAxiom>();

    	for (OWLAnnotationAssertionAxiom ax : tab.getOntology().getAnnotationAssertionAxioms(cls.getIRI())) {
    		if (ax.getProperty().equals(definition)) {
    			if (ruleservice.isDefNCI(ax)) {
    				assertions.add(ax);
    			} 
    		}
    	}
    	
    	if ((assertions.size() > 1)) {
    		JOptionPane.showMessageDialog(tab, "Only one def source NCI is allowed in Definition.", "Warning", JOptionPane.WARNING_MESSAGE);
    		return false;
    	} else {
    		tab.getOWLModelManager().applyChanges(changes);
    		return true;
    	}
    }
	
	public String checkQualifierTypes(String prop_iri, Map<String, String> qualifiers) {

		String errors = "";
		// check qualifiers are valid properties
		for (String qs : qualifiers.keySet()) {
			if (tab.lookUpShort(qs) == null) {
				errors += "qualifier " + qs + " does not exist. \n";
			}
		}
		OWLAnnotationProperty p = tab.lookUpShort(prop_iri);
		OWLAnnotationProperty fullSyn = tab.getFullSyn();
		if (p.equals(fullSyn) && ruleservice.isNCIPtFullSyn(prop_iri, qualifiers)) {
			errors += "only one NCI/PT FULL_SYN allowed. \n";
			
		}
		List<String> req_quals = tab.getRequiredQualifiers(prop_iri);
		for (String rs : req_quals) {
			String q_val = qualifiers.get(rs);
			if (q_val != null) {
				if (tab.isReadOnlyProperty(rs)) {
					errors += "required qualifier: " + rs + " is immutable. \n";	
					
				}
				if (tab.checkType(rs, q_val)) {
					
				} else {
					errors += "value " + q_val + " of required qualifier: " + rs + " has invalid type. \n";					
				}
				
			} 
		}
		if (errors.equals("")) {
			return null;
		} else {
			return errors;
		}	
	}

	public boolean syncFullSyn(OWLClass cls) {
    	List<String> message = new ArrayList<String>();
    	boolean isSavable = false;
    
		isSavable = ruleservice.isFullSynSavable(cls, tab.getOntology(), tab.getFullSyn(), message);
    	
    	List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    	
    	if (!isSavable) {
			// editedf below for use with Gout/UAB
    		JOptionPane.showMessageDialog(tab, "One and only one PT with source UAB is allowed in Full Syn.", "Warning", JOptionPane.WARNING_MESSAGE);
    		return false;
    	} else {
    		syncPrefNameLabel(cls, message.get(0), changes);
    		tab.getOWLModelManager().applyChanges(changes);
    		//selectClass(cls);
    		return true;
    	}
    }
	
	/** Anyone can pre-retire so there is no need for a separate pre-retire step. There is just retire.
     * If the class is already a chle of the "PreRetired" root class, then the retirement is ready for
     * review and only an admin user can do so. 
     * 
     */
    public boolean canRetire(OWLClass cls) { 
    	
    	if (isUneditableRoot(cls)) {
    		return false;    				
    	}

		ProjectId projectId = tab.getClientSession().getActiveProject();
		if (projectId == null) {
			return false;
		}

    	boolean can = tab.getClientSession().getActiveClient().getConfig().canPerformProjectOperation(
		NCIEditTabConstants.RETIRE.getId(), projectId);
		if (!can) {
			return false;
		}

		if (tab.isPreRetired(cls)) {
			return tab.isWorkFlowManager();
		} else {
			return !tab.isRetired(cls);
		}
    }
    
    public boolean canMerge(OWLClass cls) {
    	if (!tab.isWorkFlowManager()) {
    		if (tab.isSubClass(cls, PRE_MERGE_ROOT)) {
    			return false;
    		}
    	}
    	return true;
    }  
    
    private boolean isUneditableRoot(OWLClass cls) {
    	if (cls.equals(PRE_RETIRE_ROOT) ||
    			cls.equals(PRE_MERGE_ROOT) ||
    			cls.equals(RETIRE_ROOT) ||
    			cls.equals(RETIRE_CONCEPTS_ROOT)) {
    		return true;    				
    	}
    	return false;	
    	
    }
    
    // make sure there is a FULL_SYN property with group PT and an rdfs:label
    // that has the same value as the preferred_name property
    // TODO: Add FULL_SYN without creating cycle
    public void syncPrefName(String preferred_name) {
    	List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    	if (tab.getCurrentlyEditing() != null) {
    		syncPrefNameLabelFullSyn(tab.getCurrentlyEditing(), preferred_name, changes);
    		tab.getOntology().getOWLOntologyManager().applyChanges(changes);
    	}
    }
    
    public void syncPrefNameLabelFullSyn(OWLClass cls, String preferred_name, List<OWLOntologyChange> changes) {
    	//retrieve rdfs:label and adjust if needed
       	if (tab.getRDFSLabel(cls).isPresent() &&
       			!tab.getRDFSLabel(cls).get().equals(preferred_name)) {
    		OWLLiteral pref_name_val = tab.getOntology().getOWLOntologyManager().getOWLDataFactory().getOWLLiteral(preferred_name, OWL2Datatype.RDF_PLAIN_LITERAL);
    		for (OWLAnnotationAssertionAxiom ax : tab.getOntology().getAnnotationAssertionAxioms(cls.getIRI())) {
    			if (ax.getProperty().equals(NCIEditTabConstants.LABEL_PROP)) {
    				changes.add(new RemoveAxiom(tab.getOntology(), ax));
    				
    				OWLAxiom ax2 = tab.getOntology().getOWLOntologyManager().getOWLDataFactory()
    						.getOWLAnnotationAssertionAxiom(LABEL_PROP, cls.getIRI(), pref_name_val);
    				changes.add(new AddAxiom(tab.getOntology(), ax2));
    			} else if (ax.getProperty().equals(NCIEditTabConstants.PREF_NAME)) {
    				changes.add(new RemoveAxiom(tab.getOntology(), ax));
    				OWLAxiom ax2 = tab.getOntology().getOWLOntologyManager().getOWLDataFactory()
    						.getOWLAnnotationAssertionAxiom(PREF_NAME, cls.getIRI(), pref_name_val);
    				changes.add(new AddAxiom(tab.getOntology(), ax2));
    				
    			} else if (ax.getProperty().equals(NCIEditTabConstants.FULL_SYN)) {
    				if (ruleservice.isQualsPTNCI(ax)) {
    					changes.add(new RemoveAxiom(tab.getOntology(), ax));
    					
    					OWLDataFactory df = tab.getOntology().getOWLOntologyManager().getOWLDataFactory();
    					
    					OWLAxiom new_axiom = 
    							df.getOWLAnnotationAssertionAxiom(NCIEditTabConstants.FULL_SYN, cls.getIRI(), pref_name_val);
    					
    					Set<OWLAnnotation> anns = ax.getAnnotations();    					
    					  					
    					OWLAxiom new_new_axiom = new_axiom.getAxiomWithoutAnnotations().getAnnotatedAxiom(anns);
    							
    					changes.add(new AddAxiom(tab.getOntology(), new_new_axiom));
    					
    					
    				}
    			}
    			
    		}   		
    	}
    	
    }
    
    public void syncPrefNameLabel(OWLClass cls, String preferred_name, List<OWLOntologyChange> changes) {
    	//retrieve rdfs:label and adjust if needed
    	if (tab.getRDFSLabel(cls).isPresent() &&
    			!tab.getRDFSLabel(cls).get().equals(preferred_name)) {
    		OWLLiteral pref_name_val = 
    				tab.getOntology().getOWLOntologyManager().getOWLDataFactory().getOWLLiteral(preferred_name, OWL2Datatype.RDF_PLAIN_LITERAL);
    		for (OWLAnnotationAssertionAxiom ax : tab.getOntology().getAnnotationAssertionAxioms(cls.getIRI())) {
    			if (ax.getProperty().equals(NCIEditTabConstants.LABEL_PROP)) {
    				changes.add(new RemoveAxiom(tab.getOntology(), ax));
    				
    				OWLAxiom ax2 = tab.getOntology().getOWLOntologyManager().getOWLDataFactory()
    						.getOWLAnnotationAssertionAxiom(LABEL_PROP, cls.getIRI(), pref_name_val);
    				changes.add(new AddAxiom(tab.getOntology(), ax2));
    			} else if (ax.getProperty().equals(NCIEditTabConstants.PREF_NAME)) {
    				changes.add(new RemoveAxiom(tab.getOntology(), ax));
    				OWLAxiom ax2 = tab.getOntology().getOWLOntologyManager().getOWLDataFactory()
    						.getOWLAnnotationAssertionAxiom(PREF_NAME, cls.getIRI(), pref_name_val);
    				changes.add(new AddAxiom(tab.getOntology(), ax2));    				
    			}    			
    		}   		
    	}    	
    }
}
