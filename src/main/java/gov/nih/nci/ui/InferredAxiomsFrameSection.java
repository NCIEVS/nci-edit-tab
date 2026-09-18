package gov.nih.nci.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
/*
 * Copyright (C) 2007, University of Manchester
 *
 *
 */

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.triplestore.InferredDiff;
import org.protege.editor.owl.ui.editor.OWLObjectEditor;
import org.protege.editor.owl.ui.frame.AbstractOWLFrameSection;
import org.protege.editor.owl.ui.frame.InferredAxiomsFrameSectionRow;
import org.protege.editor.owl.ui.frame.OWLFrame;
import org.protege.editor.owl.ui.frame.OWLFrameSectionRow;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.BatchInferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;

import gov.nih.nci.utils.CuratorChecks;



/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 14-Oct-2007<br><br>
 */
public class InferredAxiomsFrameSection extends AbstractOWLFrameSection<OWLOntology, OWLAxiom, OWLAxiom>{
	
	private CuratorChecks cur_checks = null;

    public InferredAxiomsFrameSection(OWLEditorKit editorKit, OWLFrame<? extends OWLOntology> frame) {
        super(editorKit, "Inferred axioms", "Inferred axiom", frame);
    }


    protected void clear() {

    }


    protected OWLAxiom createAxiom(OWLAxiom object) {
        return object;
    }


    public OWLObjectEditor<OWLAxiom> getObjectEditor() {
    	return null;
        
    }


    protected void refill(OWLOntology ontology) {
    }


    @SuppressWarnings("rawtypes")
	protected void refillInferred() {
    	try {
    		if (this.getOWLModelManager().getExplanationManager().getIsRunning()) {
    			return;
    		}
    		
    		cur_checks = new CuratorChecks(this.getOWLModelManager().getActiveOntology(), getOWLEditorKit());

    		Set<OWLAxiom> inferred;
    		// Lazy read model: the diff comes from the server's inferred graph (inferred minus asserted)
    		// in Virtuoso, not the in-client reasoner.
    		boolean serverDiff = Boolean.getBoolean("nci.lazyHierarchy");
    		if (serverDiff) {
    			inferred = InferredDiff.compute(getOWLModelManager().getOWLDataFactory());
    		} else {
    			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
    			OWLOntology inferredOnt = man.createOntology(IRI.create("http://another.com/ontology" + System.currentTimeMillis()));
    			InferredOntologyGenerator ontGen = new InferredOntologyGenerator(getOWLModelManager().getReasoner(), new ArrayList<>());
    			ontGen.addGenerator(new BatchInferredSubClassAxiomGenerator());
    			ontGen.addGenerator(new InferredEquivalentClassAxiomGenerator());
    			ontGen.fillOntology(man.getOWLDataFactory(), inferredOnt);
    			inferred = inferredOnt.getAxioms();
    		}

    		for (OWLAxiom ax : new TreeSet<>(inferred)) {
    			boolean add = true;
    			// The server diff already excludes asserted edges; only re-check for the legacy path.
    			if (!serverDiff && getOWLModelManager().getActiveOntology().containsAxiom(ax)) {
    				add = false;
    			}

    			if (this.isVacuousOrRootAxiom(ax)) {
    				add = false;
    			}

    			if (add) {
    				doctorAndAdd(new InferredAxiomsFrameSectionRow(getOWLEditorKit(), this, null, getRootObject(), ax));
    			}
    		}
    	}
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void doctorAndAdd(InferredAxiomsFrameSectionRow row) {
    	if (isInconsistent(row.getAxiom())) {
    		row.setEditingHint(" - needs_repair");
    		addInferredRowIfNontrivial(row);
    	} else {
    		row.setEditingHint(" - Add_Axiom");
    		addInferredRowIfNontrivial(row);
    		// now possibly make a new remove axiom for each redudant link
    		List<OWLAxiom> supsToRemove = findCommonParents(row.getAxiom());
    		for (OWLAxiom ax : supsToRemove) {
    			InferredAxiomsFrameSectionRow remRow = 
    					new InferredAxiomsFrameSectionRow(getOWLEditorKit(), this, null, getRootObject(), ax);
    			remRow.setEditingHint(" - Remove_Axiom");
    			addInferredRowIfNontrivial(remRow);
    		}
    	}
    }
    
    private boolean isInconsistent(OWLAxiom ax) {
    	if (ax.isOfType(AxiomType.SUBCLASS_OF)) {
    		OWLSubClassOfAxiom subax = (OWLSubClassOfAxiom) ax;
    		return subax.getSuperClass().isOWLNothing();    
    	}
    	return false;
    }
    
    private boolean isVacuousOrRootAxiom(OWLAxiom ax) {
    	if (ax.isOfType(AxiomType.SUBCLASS_OF)) {
    		OWLSubClassOfAxiom subax = (OWLSubClassOfAxiom) ax;
    		if (subax.getSuperClass().isOWLThing() ||
    				subax.getSubClass().isOWLNothing()) {
    			return true;
    		}
    	}
    	return false;
    }
    
   
    
    private List<OWLAxiom> findCommonParents(OWLAxiom newAxiom) {
    	List<OWLAxiom> results = new ArrayList<OWLAxiom>();
    	if (newAxiom.isOfType(AxiomType.SUBCLASS_OF)) {
    		OWLSubClassOfAxiom subax = (OWLSubClassOfAxiom) newAxiom;
    		OWLClass cls = subax.getSubClass().asOWLClass();
    		if (!subax.getSuperClass().isAnonymous()) {
    			OWLClass newParent = subax.getSuperClass().asOWLClass();
    			Set<OWLClass> assertedParents = cur_checks.getStatedParents(cls);
    			Set<OWLClass> newParentAssertedParents = cur_checks.getStatedParents(newParent);
    			for (OWLClass ap : assertedParents) {
    				if (newParentAssertedParents.contains(ap)) {
    					OWLAxiom newAx = 
    							getOWLModelManager().getOWLDataFactory().getOWLSubClassOfAxiom(cls, ap);
    					results.add(newAx);
    				}
    			}    			
    		}
    	}
    	return results;
    }

    @Override
    protected boolean isResettingChange(OWLOntologyChange change) {
        return false;
    }


    public Comparator<OWLFrameSectionRow<OWLOntology, OWLAxiom, OWLAxiom>> getRowComparator() {
        return (o1, o2) -> {

            int diff = o1.getAxiom().compareTo(o2.getAxiom());
            if(diff != 0) {
                return diff;
            }
            else if (o1.getOntology() == null  && o2.getOntology() == null) {
                return 0;
            }
            else if (o1.getOntology() == null) {
                return -1;
            }
            else if (o2.getOntology() == null) {
                return +1;
            }
            else {
                return o1.getOntology().compareTo(o2.getOntology());
            }
        };
    }
}
