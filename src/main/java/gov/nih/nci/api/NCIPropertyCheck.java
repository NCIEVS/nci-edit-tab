package gov.nih.nci.api;

import static gov.nih.nci.ui.NCIEditTabConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;


public class NCIPropertyCheck implements RuleService {

	@Override
	public boolean isFullSynSavable(OWLClass cls, OWLOntology ontology, OWLAnnotationProperty full_syn, List message) {
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    	List<OWLAnnotationAssertionAxiom> assertions = new ArrayList<OWLAnnotationAssertionAxiom>();

    	for (OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(cls.getIRI())) {
    		if (ax.getProperty().equals(full_syn)) {
    			if (isQualsPTNCI(ax)) {
    				assertions.add(ax);
    			} 
    		}
    	}
    	if ((assertions.size() != 1)) {
    		message.add("Warning");
    		message.add("One and only one PT with source UAB is allowed in Full Syn.");		// edited for UAB
    		return false;
    	} 
    	message.add(assertions.get(0).getValue().asLiteral().get().getLiteral());
    	return true;
	}
	
	@Override
	public boolean isQualsPTNCI(OWLAnnotationAssertionAxiom ax) {
		return ((getAnnotationValue(ax, SYN_TYPE).equals("PT") ||
				getAnnotationValue(ax, SYN_TYPE).equals("AQ") ||
				getAnnotationValue(ax, SYN_TYPE).equals("HD")) &&
				getAnnotationValue(ax, SYN_SOURCE).equals("UAB"));							// edited for UAB
	}

	@Override
	public boolean isDefNCI(OWLAnnotationAssertionAxiom ax) {
		String ann_val = getAnnotationValue(ax, DEF_SOURCE);    	
    	return ann_val.equalsIgnoreCase("UAB");												// edited for UAB
	}
	
	private String getAnnotationValue(OWLAnnotationAssertionAxiom axiom, OWLAnnotationProperty annProp) {
    	Set<OWLAnnotation> anns = axiom.getAnnotations();
    	for (OWLAnnotation ann : anns) {
    		if (ann.getProperty().equals(annProp)) {
    			return ann.getValue().asLiteral().get().getLiteral();
    		}
    	}
    	return "None";   	
    	
    }

	@Override
	public boolean isNCIPtFullSyn(String prop_iri, Map<String, String> qualifiers) {
		//OWLAnnotationProperty p = tab.lookUpShort(prop_iri);
    	boolean isIt = false;
    	//if (prop.equals(fullSyn)) {
		String tg = qualifiers.get(SYN_TYPE.getIRI().getShortForm());
		String ts = qualifiers.get(SYN_SOURCE.getIRI().getShortForm());
		if (tg != null &&
				ts != null &&
				(tg.equals("PT") ||
						tg.equals("AQ") ||
						tg.equals("HD"))
						&&
				ts.equals("UAB")) {														// edited for UAB
			isIt = true;
		}
    	return isIt;
	}

}
