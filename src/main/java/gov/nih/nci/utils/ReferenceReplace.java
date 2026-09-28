package gov.nih.nci.utils;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.protege.editor.owl.model.OWLModelManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;

public class ReferenceReplace implements OWLClassExpressionVisitor {

	private OWLClass source;
	
	private OWLClass target;
	
	private OWLModelManager owlModelManager;
	
	private OWLDataFactory dataFact;

	private OWLOntology ont;
	
	
	public ReferenceReplace(OWLModelManager man) {
		owlModelManager = man;
		dataFact = man.getOWLDataFactory();
	}
	
	public List<OWLOntologyChange> retargetRefs(OWLClass from, OWLClass to) { 
		// TODO: finish !! same idea as retirement but modify rather than annotate

		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		

		source = from;
		target = to;

		ont = owlModelManager.getActiveOntology();
		
		

		Set<OWLAxiom> axioms = ont.getReferencingAxioms(from);
		for (OWLAxiom ax : axioms) {
			
			
			if (ax instanceof OWLEquivalentClassesAxiom) {
				changes.add(new RemoveAxiom(ont, ax));
				Set<OWLClassExpression> newExps = new HashSet<OWLClassExpression>();
				for (OWLClassExpression desc : ((OWLEquivalentClassesAxiom) ax).getClassExpressions()) {
					desc.accept(this);
					newExps.add(getNewExpression());
				}
				OWLEquivalentClassesAxiom newAx = dataFact.getOWLEquivalentClassesAxiom(newExps);
				changes.add(new AddAxiom(ont, newAx));
				
			} else if (ax instanceof OWLSubClassOfAxiom) {

				OWLClassExpression par = ((OWLSubClassOfAxiom) ax).getSuperClass();
				OWLClassExpression child = ((OWLSubClassOfAxiom) ax).getSubClass();

				OWLClassExpression newPar = null;

				if (par.isAnonymous()) {
					par.accept(this);
					newPar = this.getNewExpression();

				} else if (par.asOWLClass().equals(source)) {
					newPar = target;

					String val = child.asOWLClass().getIRI().getShortForm();					
					System.out.println("OLD_CHILD:" + val );
				}
				if (newPar != null) {
					changes.add(new RemoveAxiom(ont, ax));
					if (!child.equals(newPar)) {
						OWLSubClassOfAxiom newAx = dataFact.getOWLSubClassOfAxiom(child, newPar);
						changes.add(new AddAxiom(ont, newAx));
					}
				}
				
			}
		}
		
		for (OWLAnnotationAssertionAxiom ax : ont.getAxioms(AxiomType.ANNOTATION_ASSERTION)) {
            Optional<IRI> valueIRI = ax.getValue().asIRI();
            if (valueIRI.isPresent()) {
                if (valueIRI.get().equals(from.getIRI())) {
                	changes.add(new RemoveAxiom(ont, ax));
                	OWLAnnotationAssertionAxiom newax = dataFact.getOWLAnnotationAssertionAxiom(ax.getProperty(), ax.getSubject(),
                			to.getIRI());
                	changes.add(new AddAxiom(ont, newax));
                	
                }
            }
        }
		
		

		return changes;

	}
	
	private OWLClassExpression newExpression;
	
	private OWLClassExpression getNewExpression() {
		return newExpression;
	}

	
	@Override
	public void visit(OWLClass ce) {
		newExpression = ce;
		
	}
	
	@Override
	public void visit(OWLObjectIntersectionOf ce) {
		Set<OWLClassExpression> newConjs = new HashSet<OWLClassExpression>();
		
		Set<OWLClassExpression> conjs = ce.asConjunctSet();
		for (OWLClassExpression c : conjs) {
			c.accept(this);
			newConjs.add(getNewExpression());
		}
		newExpression = dataFact.getOWLObjectIntersectionOf(newConjs);
	}

	@Override
	public void visit(OWLObjectUnionOf ce) {
		Set<OWLClassExpression> newDisjs = new HashSet<OWLClassExpression>();
		
		Set<OWLClassExpression> disjs = ce.asDisjunctSet();
		for (OWLClassExpression c : disjs) {
			c.accept(this);
			newDisjs.add(getNewExpression());
		}
		newExpression = dataFact.getOWLObjectUnionOf(newDisjs);
		
	}

	@Override
	public void visit(OWLObjectComplementOf ce) {
		OWLClassExpression op = ce.getOperand();
		if (!op.isAnonymous() && op.asOWLClass().equals(source)) {
			newExpression = dataFact.getOWLObjectComplementOf(target);
		} else {
			newExpression = ce;
		}
	}

	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		if (!ce.getFiller().isAnonymous() && ce.getFiller().asOWLClass().equals(source)) {
			newExpression = dataFact.getOWLObjectSomeValuesFrom(ce.getProperty(), target);
		} else {
			newExpression = ce;
		}
	}

	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
		if (!ce.getFiller().isAnonymous() && ce.getFiller().asOWLClass().equals(source)) {
			newExpression = dataFact.getOWLObjectAllValuesFrom(ce.getProperty(), target);
		} else {
			newExpression = ce;
		}
	}



	@Override
	public void visit(OWLObjectHasValue ce) {
		newExpression = ce;
	}



	@Override
	public void visit(OWLObjectMinCardinality ce) {
		if (!ce.getFiller().isAnonymous() && ce.getFiller().asOWLClass().equals(source)) {
			newExpression = dataFact.getOWLObjectMinCardinality(ce.getCardinality(), ce.getProperty(), target);
		} else {
			newExpression = ce;
		}
	}



	@Override
	public void visit(OWLObjectExactCardinality ce) {
		if (!ce.getFiller().isAnonymous() && ce.getFiller().asOWLClass().equals(source)) {
			newExpression = dataFact.getOWLObjectExactCardinality(ce.getCardinality(), ce.getProperty(), target);
		} else {
			newExpression = ce;
		}
	}



	@Override
	public void visit(OWLObjectMaxCardinality ce) {
		if (!ce.getFiller().isAnonymous() && ce.getFiller().asOWLClass().equals(source)) {
			newExpression = dataFact.getOWLObjectMaxCardinality(ce.getCardinality(), ce.getProperty(), target);
		} else {
			newExpression = ce;
		}
	}



	@Override
	public void visit(OWLObjectHasSelf ce) {
		newExpression = ce;
	}



	@Override
	public void visit(OWLObjectOneOf ce) {
		newExpression = ce;
	}



	@Override
	public void visit(OWLDataSomeValuesFrom ce) {
		newExpression = ce;
	}



	@Override
	public void visit(OWLDataAllValuesFrom ce) {
		newExpression = ce;
	}



	@Override
	public void visit(OWLDataHasValue ce) {
		newExpression = ce;
	}



	@Override
	public void visit(OWLDataMinCardinality ce) {
		newExpression = ce;
	}



	@Override
	public void visit(OWLDataExactCardinality ce) {
		newExpression = ce;
	}



	@Override
	public void visit(OWLDataMaxCardinality ce) {
		newExpression = ce;
	}



	
}


