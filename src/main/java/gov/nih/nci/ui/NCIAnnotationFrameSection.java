package gov.nih.nci.ui;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.entity.AnnotationPropertyComparator;
import org.protege.editor.owl.ui.editor.OWLAnnotationEditor;
import org.protege.editor.owl.ui.editor.OWLObjectEditor;
import org.protege.editor.owl.ui.frame.AbstractOWLFrameSection;
import org.protege.editor.owl.ui.frame.OWLAnnotationsFrameSectionRow;
import org.protege.editor.owl.ui.frame.OWLFrame;
import org.protege.editor.owl.ui.frame.OWLFrameSectionRow;
import org.protege.editor.owl.ui.renderer.OWLRendererPreferences;
import org.semanticweb.owlapi.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 26-Jan-2007<br><br>
 */
public class NCIAnnotationFrameSection extends AbstractOWLFrameSection<OWLAnnotationSubject, OWLAnnotationAssertionAxiom, OWLAnnotation> {

    private static final String LABEL = "Annotations";

    private static OWLAnnotationSectionRowComparator comparator;
    
    private Set<OWLAnnotationProperty> propsToExclude;
    private Set<OWLAnnotationProperty> readOnlyProps;

    public NCIAnnotationFrameSection(OWLEditorKit editorKit, OWLFrame<? extends OWLAnnotationSubject> frame, Set<OWLAnnotationProperty> props,
    		Set<OWLAnnotationProperty> read_only_props) {
        super(editorKit, LABEL, "Entity annotation", frame);
        comparator = new OWLAnnotationSectionRowComparator(editorKit.getModelManager());
        propsToExclude = props;
        readOnlyProps = read_only_props;
    }


    protected void clear() {
    	//this.getRows().clear();
    }


    protected void refill(OWLOntology ontology) {
        boolean hidden = false;
        final OWLAnnotationSubject annotationSubject = getRootObject();
        for (OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(annotationSubject)) {
        	if (!getOWLEditorKit().getWorkspace().isHiddenAnnotationURI(ax.getAnnotation().getProperty().getIRI().toURI()) &&
        			!propsToExclude.contains(ax.getProperty())) {
        		if (!readOnlyProps.contains(ax.getProperty())) {
        			addRow(new NCIOWLAnnotationsFrameSectionRow(getOWLEditorKit(), this, ontology, annotationSubject, propsToExclude, ax));
        		} else {
        			addRow(new NCIOWLAnnotationsFrameSectionRow(getOWLEditorKit(), this, ontology, annotationSubject, propsToExclude, ax, false));

        		}
        	} else {
        		hidden = true;
        	}
        }
        if (hidden) {
            setLabel(LABEL + " (some annotations are hidden)");
        } else {
            setLabel(LABEL);
        }
        
    }


    protected OWLAnnotationAssertionAxiom createAxiom(OWLAnnotation object) {
        return getOWLDataFactory().getOWLAnnotationAssertionAxiom(getRootObject(), object);
    }


    public OWLObjectEditor<OWLAnnotation> getObjectEditor() {
    	return new NCIOWLAnnotationEditor(getOWLEditorKit(), propsToExclude);    	
    }


    /**
     * Obtains a comparator which can be used to sort the rows
     * in this section.
     *
     * @return A comparator if to sort the rows in this section,
     * or <code>null</code> if the rows shouldn't be sorted.
     */
    public Comparator<OWLFrameSectionRow<OWLAnnotationSubject, OWLAnnotationAssertionAxiom, OWLAnnotation>> getRowComparator() {
        return comparator;
    }


    public boolean canAcceptDrop(List<OWLObject> objects) {
        for (OWLObject obj : objects) {
            if (!(obj instanceof OWLAnnotation)) {
                return false;
            }
        }
        return true;
    }


    public boolean dropObjects(List<OWLObject> objects) {
        List<OWLOntologyChange> changes = new ArrayList<>();
        OWLAnnotation annot = (OWLAnnotation) objects.get(0);
        OWLAxiom ax = getOWLDataFactory().getOWLAnnotationAssertionAxiom(getRootObject(), annot);
        
        List<OWLObject> quals = objects.subList(1, objects.size());
        
        Set<OWLAnnotation> q_as = new HashSet<OWLAnnotation>();
        for (OWLObject o : quals) {
        	q_as.add((OWLAnnotation) o);
        	
        } 
        
        OWLAxiom new_ax = ax.getAxiomWithoutAnnotations().getAnnotatedAxiom(q_as);
        
        changes.add(new AddAxiom(getOWLModelManager().getActiveOntology(), new_ax));
       
        getOWLModelManager().applyChanges(changes);
        return true;
    }

    @Override
    protected boolean isResettingChange(OWLOntologyChange change) {
        return change.isAxiomChange() &&
                change.getAxiom() instanceof OWLAnnotationAssertionAxiom &&
                ((OWLAnnotationAssertionAxiom) change.getAxiom()).getSubject().equals(getRootObject());
    }


    private static class OWLAnnotationSectionRowComparator implements Comparator<OWLFrameSectionRow<OWLAnnotationSubject, OWLAnnotationAssertionAxiom, OWLAnnotation>> {

        private Comparator<OWLObject> owlObjectComparator;

        public OWLAnnotationSectionRowComparator(OWLModelManager owlModelManager) {
            owlObjectComparator = owlModelManager.getOWLObjectComparator();
        }

        public int compare(OWLFrameSectionRow<OWLAnnotationSubject, OWLAnnotationAssertionAxiom, OWLAnnotation> o1,
                           OWLFrameSectionRow<OWLAnnotationSubject, OWLAnnotationAssertionAxiom, OWLAnnotation> o2) {
            OWLAnnotation annotation1 = o1.getAxiom().getAnnotation();
            OWLAnnotation annotation2 = o2.getAxiom().getAnnotation();
            return getAnnotationDifference(annotation1, annotation2);
        }

        private int getAnnotationDifference(OWLAnnotation annotation1, OWLAnnotation annotation2) {
            int diff = getAnnotationPropertyDifference(annotation1.getProperty(), annotation2.getProperty());
            if (diff == 0) {
                diff = getAnnotationLanguageDifference(annotation1.getValue(), annotation2.getValue());
            }
            if (diff == 0) {
                diff = getAnnotationValueDifference(annotation1.getValue(), annotation2.getValue());
            }
            return diff;
        }


        private int getAnnotationPropertyDifference(OWLAnnotationProperty property1, OWLAnnotationProperty property2) {
            OWLRendererPreferences preferences = OWLRendererPreferences.getInstance();
            List<IRI> annotationIRIs = preferences.getAnnotationIRIs();
            int index1 = annotationIRIs.indexOf(property1.getIRI());
            int index2 = annotationIRIs.indexOf(property2.getIRI());
            int diff = 0;
            if (index1 != -1 && index2 != -1) {
                diff = index1 - index2;
            }
            if (diff != 0) {
                return diff;
            }
            AnnotationPropertyComparator comparator = AnnotationPropertyComparator.withDefaultOrdering(owlObjectComparator);
            return comparator.compare(property1, property2);
        }

        private int getAnnotationLanguageDifference(OWLAnnotationValue value1, OWLAnnotationValue value2) {
            int diff = 0;
            if (value1 instanceof OWLLiteral && value2 instanceof OWLLiteral) {
                OWLLiteral lit1 = (OWLLiteral) value1;
                String lang1 = lit1.getLang();
                OWLLiteral lit2 = (OWLLiteral) value2;
                String lang2 = lit2.getLang();
                List<String> langs = OWLRendererPreferences.getInstance().getAnnotationLangs();
                int langIndex1 = langs.indexOf(lang1);
                int langIndex2 = langs.indexOf(lang2);
                if (langIndex1 == -1) {
                    diff = 1;
                } else if (langIndex2 == -1) {
                    diff = -1;
                } else {
                    diff = langIndex1 - langIndex2;
                }

            }
            return diff;
        }

        private int getAnnotationValueDifference(OWLAnnotationValue value1, OWLAnnotationValue value2) {
            return owlObjectComparator.compare(value1, value2);
        }
    }

}
