package gov.nih.nci.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionListener;

import org.protege.editor.core.ui.list.MListButton;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.frame.OWLFrame;
import org.protege.editor.owl.ui.frame.OWLFrameSectionRow;
import org.protege.editor.owl.ui.frame.cls.OWLSubClassAxiomFrameSectionRow;
import org.protege.editor.owl.ui.framelist.OWLFrameList;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.ui.dialog.PropertyEditingDialog;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplNoCompression;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplString;

public class NCIOWLFrameList<R> extends OWLFrameList {

	private static final long serialVersionUID = 1L;
	
	private final Logger logger = LoggerFactory.getLogger(NCIOWLFrameList.class);
	
	private boolean read_only = false;
	
	public void setReadOnly(boolean b) {
		//super.setReadOnly(b);
		read_only = b;
	}
	
	private OWLEditorKit editorKit;
	
	private List<OWLAnnotationProperty> annotationProps;
	
	private List<OWLAnnotation> annotations;
	
	OWLAnnotationAssertionAxiom axiom;
	
	private MListIconButton addButton;
    
    private MListIconButton editButton;
    
    private MListIconButton deleteButton;
    
    private List<MListButton> buttons = new ArrayList<MListButton>();
    
    private ListSelectionListener selListener = event -> handleSelectionEvent(event);
    
	public NCIOWLFrameList(OWLEditorKit editorKit, OWLFrame<R> frame, boolean read_only) {
		this(editorKit, frame);
		this.read_only = read_only;
	}
	
	public NCIOWLFrameList(OWLEditorKit editorKit, OWLFrame<R> frame) {
		super(editorKit, frame);
		this.editorKit = editorKit;
		//this.frame = frame;
		//setupFrameListener();
		addListSelectionListener(selListener);
	}
	
	protected List<MListButton> getButtons(Object value) {
		List<MListButton> buttons = new ArrayList<MListButton>();
		if (read_only) {

    	} else if (isComplexProperty(value)) {
    		buttons = new ArrayList<>(super.getButtons(value));
    		if (value instanceof OWLFrameSectionRow) {
    			if (((OWLFrameSectionRow) value).isEditable()) {
    				buttons = createButtons();
    				if (value != null && !isComplexProperty(value) && buttons.size() == 3) {
    					buttons.remove(2);
    				}
    				
    			}
    		}
    	} else {
    		buttons = super.getButtons(value);
    	}
		return buttons;
	}
	
	private List<MListButton> createButtons() {
		List<MListButton> buttons = new ArrayList<MListButton>();
    	addButton = new MListIconButton(NCIEditTabConstants.ADD, "ButtonAddIcon.png", NCIEditTabConstants.ADD, event -> handleAdd());
    	editButton = new MListIconButton(NCIEditTabConstants.EDIT, "ButtonEditIcon.png", NCIEditTabConstants.EDIT, event -> handleEdit());
    	deleteButton = new MListIconButton(NCIEditTabConstants.DELETE, "ButtonDeleteIcon.png", NCIEditTabConstants.DELETE, event -> handleDelete());
		
    	buttons.add(deleteButton);
    	buttons.add(editButton);
    	buttons.add(addButton);
    	return buttons;
    }
	
	public void handleAdd() {
		if(isComplexProperty(getSelectedValue())) {
			loadAnnotationsAndProperties();
			Set<OWLAnnotationProperty> configuredAnnotations = NCIEditTab.currentTab().getConfiguredAnnotationsForAnnotation(axiom.getProperty());
			List<OWLAnnotationProperty> requiredAnnotationsList = new ArrayList<OWLAnnotationProperty>(configuredAnnotations);
			PropertyEditingDialog add = new	PropertyEditingDialog(NCIEditTabConstants.ADD, 
					PropertyUtil.getSelectedPropertyType(requiredAnnotationsList), 
					PropertyUtil.getDefaultPropertyValues(requiredAnnotationsList), 
					PropertyUtil.getSelectedPropertyOptions(annotationProps), 
					PropertyUtil.getDefaultSelectedPropertyLabel(requiredAnnotationsList));
			
			boolean done = false;
			while (!done) {

				HashMap<String, String> data = 	add.showDialog(this.editorKit, "Adding Properties");

				OWLAnnotationAssertionAxiom axiom = ((NCIOWLAnnotationsFrameSectionRow)getSelectedValue()).getAxiom();


				String codeStr = ((IRI)axiom.getSubject()).getShortForm();
				OWLClass cls = NCIEditTab.currentTab().getClass(codeStr);

				if (data != null) {
					if (NCIEditTab.currentTab().complexPropOp(NCIEditTabConstants.ADD, cls,
							axiom.getProperty(), axiom, data)) {
						done = true;					
					}			

				} else {
					done = true;
				}
			}
		} else {
			super.handleEdit();
		}
		
	}
	
	public void handleEdit() {
		if(isComplexProperty(getSelectedValue())) {
			loadAnnotationsAndProperties();
			if(annotationProps == null) return;
			
			OWLAnnotationAssertionAxiom axiom = ((NCIOWLAnnotationsFrameSectionRow)getSelectedValue()).getAxiom();
			Set<OWLAnnotationProperty> configuredAnnotations = NCIEditTab.currentTab().getConfiguredAnnotationsForAnnotation(axiom.getProperty());
			List<OWLAnnotationProperty> requiredAnnotationsList = new ArrayList<OWLAnnotationProperty>(configuredAnnotations);
			
			PropertyEditingDialog edit = new	PropertyEditingDialog(NCIEditTabConstants.EDIT, 
					PropertyUtil.getSelectedPropertyType(requiredAnnotationsList),
					getPropertyValues(configuredAnnotations),
					PropertyUtil.getSelectedPropertyOptions(annotationProps), 
					PropertyUtil.getDefaultSelectedPropertyLabel(requiredAnnotationsList));
			boolean done = false;
			while (!done) {
				HashMap<String, String> data = edit.showDialog(this.editorKit, "Editing Properties");

				String codeStr = ((IRI) axiom.getSubject()).getShortForm();
				OWLClass cls = NCIEditTab.currentTab().getClass(codeStr);
				if (data != null) {
					if (NCIEditTab.currentTab().complexPropOp(NCIEditTabConstants.EDIT, cls, axiom.getProperty(), axiom,
							data)) {
						done = true;
					}
				} else {
					done = true;
				}
			}
		} else {
			super.handleEdit();
		}
	}
	
	public void handleDelete() {
		if (isOWLClass(getSelectedValue())) {
			OWLClass cls = NCIEditTab.currentTab().getCurrentlySelected();
			Set<OWLClass> parents = editorKit.getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider().getParents(cls);
			if (parents == null || parents.isEmpty() ) {
				//JOptionPane.showMessageDialog(null,"Can not delete the last super class", "Class Delete", JOptionPane.INFORMATION_MESSAGE);
        		logger.warn(cls.toString() + "doesn't have super class defined.");
			} else if (parents.size() == 1) {
				Iterator itr = parents.iterator();
				OWLClass parent = (OWLClass)itr.next();
				if (parent.getIRI().equals(getSuperClass(getSelectedValue()).getIRI())) {
					JOptionPane.showMessageDialog(null,"Can not delete the last super class", "Class Delete", JOptionPane.INFORMATION_MESSAGE);
				} else {
					super.handleDelete();
				}
			} else {
				super.handleDelete();
			}
		} else if (isComplexProperty(getSelectedValue())) {			
			
			OWLAnnotationAssertionAxiom axiom = ((NCIOWLAnnotationsFrameSectionRow)getSelectedValue()).getAxiom();
						
			String codeStr = ((IRI) axiom.getSubject()).getShortForm();
			OWLClass cls = NCIEditTab.currentTab().getClass(codeStr);
			NCIEditTab.currentTab().complexPropOp(NCIEditTabConstants.DELETE, cls, 
					axiom.getProperty(), axiom, null);
			
		} else {
			super.handleDelete();
		}
	}

	private Map<String, String> getPropertyValues(Set<OWLAnnotationProperty> configuredAnnotations) {
		Map<String, String> propertyValues = PropertyUtil.getSelectedPropertyValues(annotations);
		//Check if configured annotation is not in propertyValues, add it into propertyValues with null value
		for (OWLAnnotationProperty annotProp : configuredAnnotations) {
			if (!propertyValues.containsKey(annotProp.getIRI().getShortForm())) {
				propertyValues.put(annotProp.getIRI().getShortForm(), null);	
			}
		}
		
		if (axiom.getValue() instanceof OWLLiteralImplString) {
			propertyValues.put("Value", ((OWLLiteralImplString)axiom.getValue()).getLiteral());
		} else if (axiom.getValue() instanceof OWLLiteralImplNoCompression) {
			propertyValues.put("Value", ((OWLLiteralImplNoCompression)axiom.getValue()).getLiteral());
		}
		return propertyValues;
	}
	
	private boolean isComplexProperty(Object val) {
		if (val instanceof NCIOWLAnnotationsFrameSectionRow) {
			axiom = ((NCIOWLAnnotationsFrameSectionRow)val).getAxiom();
			OWLAnnotationProperty annoProp = axiom.getProperty();
			List<OWLAnnotationProperty> complexProps = NCIEditTab.currentTab().getComplexProperties();
			if(complexProps.contains(annoProp)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isOWLClass(Object val) {
		if (val instanceof OWLSubClassAxiomFrameSectionRow) {
			return ((OWLSubClassAxiomFrameSectionRow) val).getAxiom().getSuperClass().isOWLClass();
		}
		return false;
	}
	
	private OWLClass getSuperClass(Object val) {
		if (val instanceof OWLSubClassAxiomFrameSectionRow) {
			return (OWLClass) ((OWLSubClassAxiomFrameSectionRow) val).getAxiom().getSuperClass();
		}
		return null;
		
	}
	
	private void loadAnnotationsAndProperties() {
		final Object val = getSelectedValue();
		if (val instanceof NCIOWLAnnotationsFrameSectionRow) {
			axiom = ((NCIOWLAnnotationsFrameSectionRow)val).getAxiom();
			annotations = new ArrayList<OWLAnnotation>(axiom.getAnnotations());
			annotationProps = new ArrayList<OWLAnnotationProperty>();
			for(OWLAnnotation annotation : annotations) {
				annotationProps.add(annotation.getProperty());
			}
		}
	}
	
	public void dispose() {
		
		removeListSelectionListener(selListener);
		super.dispose();
	}
}
