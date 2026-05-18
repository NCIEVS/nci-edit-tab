package gov.nih.nci.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.hierarchy.OWLAnnotationPropertyHierarchyProvider;
import org.protege.editor.owl.ui.editor.AbstractOWLObjectEditor;
import org.protege.editor.owl.ui.editor.EnumEditor;
import org.protege.editor.owl.ui.editor.IRIFromEntityEditor;
import org.protege.editor.owl.ui.editor.IRITextEditor;
import org.protege.editor.owl.ui.editor.OWLConstantEditor;
import org.protege.editor.owl.ui.editor.OWLObjectEditor;
import org.protege.editor.owl.ui.selector.OWLAnnotationPropertySelectorPanel;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import gov.nih.nci.utils.CharMapper;

/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 10-Feb-2007<br><br>
 */
public class NCIOWLAnnotationEditor extends AbstractOWLObjectEditor<OWLAnnotation> implements VerifiedInputEditor {


    protected final OWLEditorKit owlEditorKit;

    private JTabbedPane tabbedPane;
    
    private int curr_jtab = 0;
    
    private boolean isIRI = false;

    private JPanel mainPanel;

    private OWLAnnotationPropertySelectorPanel annotationPropertySelector;

    private List<OWLObjectEditor<? extends OWLAnnotationValue>> editors;

    private OWLAnnotationProperty lastSelectedProperty;

    private List<InputVerificationStatusChangedListener> verificationListeners = new ArrayList<>();

    private boolean status = false;
    
    private static String lastEditorName = "";

    private ChangeListener changeListener = event -> verify();
    
    private CharMapper mapper = new CharMapper();
    
    private InputVerificationStatusChangedListener mergedVerificationListener = new InputVerificationStatusChangedListener() {
		
		public void verifiedStatusChanged(final boolean newState) {
			for (InputVerificationStatusChangedListener listener : verificationListeners) {
				listener.verifiedStatusChanged(newState);
			}
		}
	};

    public NCIOWLAnnotationEditor(OWLEditorKit owlEditorKit, Set<OWLAnnotationProperty> filterProps) {
        this.owlEditorKit = owlEditorKit;
        tabbedPane = new JTabbedPane();
        mainPanel = new VerifiedInputJPanel();
        mainPanel.setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainPanel.add(splitPane);

        annotationPropertySelector = createAnnotationPropertySelector(filterProps);
        JPanel listHolder = new JPanel(new BorderLayout());
        listHolder.add(annotationPropertySelector);
        listHolder.setPreferredSize(new Dimension(200, 300));

        splitPane.setLeftComponent(listHolder);
        splitPane.setRightComponent(tabbedPane);
        splitPane.setBorder(null);
        loadEditors();
        initialiseLastSelectedProperty();

        annotationPropertySelector.addSelectionListener(event -> {
            verify();
        });

        tabbedPane.addChangeListener(changeListener);
    }

    protected final void initialiseLastSelectedProperty() {
    	assert lastSelectedProperty == null; 
        lastSelectedProperty = getDefaultAnnotationProperty(); 
    }
	protected OWLAnnotationProperty getDefaultAnnotationProperty() {
        final OWLModelManager mngr = owlEditorKit.getOWLModelManager();
		return mngr.getOWLDataFactory().getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI());		
	}

    protected OWLAnnotationPropertySelectorPanel createAnnotationPropertySelector(Set<OWLAnnotationProperty> filterProps) {
        final OWLModelManager mngr = owlEditorKit.getOWLModelManager();
        final OWLAnnotationPropertyHierarchyProvider hp =
                mngr.getOWLHierarchyManager().getOWLAnnotationPropertyHierarchyProvider(filterProps);
        return new OWLAnnotationPropertySelectorPanel(owlEditorKit, true, hp);
	}


	private void loadEditors() {
        editors = createEditors();
        assert !editors.isEmpty();
        int selIndex = 0;
        int tabCount = 0;
        for (OWLObjectEditor<? extends OWLAnnotationValue> editor : editors) {
            String editorTypeName = editor.getEditorTypeName();
            tabbedPane.add(editorTypeName, editor.getEditorComponent());
            if(lastEditorName != null && editorTypeName != null && lastEditorName.equals(editorTypeName)) {
                selIndex = tabCount;
            }
            tabCount++;
        }
        tabbedPane.setSelectedIndex(selIndex);
        tabbedPane.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
		        int index = sourceTabbedPane.getSelectedIndex();
		        if (index != curr_jtab) {
		        	if (isIRI) {
		        		OWLObjectEditor<?> editor = editors.get(index);
						if ((editor instanceof IRIFromEntityEditor) ||
								(editor instanceof IRITextEditor)) {
							curr_jtab = index;
						}
		        		
		        	}
		        			        	
		        }
		        tabbedPane.setSelectedIndex(curr_jtab);
			}
        	
        });
        
    }


    protected List<OWLObjectEditor<? extends OWLAnnotationValue>> createEditors() {
        final IRIFromEntityEditor iriEditor = new IRIFromEntityEditor(owlEditorKit);
        iriEditor.addSelectionListener(changeListener);

        final OWLConstantEditor constantEditor = new OWLConstantEditor(owlEditorKit);
     
        final IRITextEditor textEditor = new IRITextEditor(owlEditorKit);
        textEditor.addStatusChangedListener(mergedVerificationListener);
        
        final EnumEditor enumEditor = new EnumEditor(owlEditorKit);
        this.annotationPropertySelector.addSelectionListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {				
				OWLAnnotationProperty prop = annotationPropertySelector.getSelectedObject();				
				if (prop != null) {
					isIRI = false;
					IRI range = getDataTypeIRI(prop);
					if (range != null) {
						if (enumEditor.isDataTypeCombobox(range)) {
							enumEditor.setProp(prop);
							for (int i = 0; i < editors.size(); i++) {
								OWLObjectEditor<?> editor = editors.get(i);
								if (editor instanceof EnumEditor) {
									curr_jtab = i;
									tabbedPane.setSelectedIndex(i);
									
								}
							}
						} else if (range.getShortForm().equals("anyURI")) {
							isIRI = true;
							for (int i = 0; i < editors.size(); i++) {
								OWLObjectEditor<?> editor = editors.get(i);
								if (editor instanceof IRIFromEntityEditor) {
									curr_jtab = i;
									tabbedPane.setSelectedIndex(i);
									
								}
							}
							
						} else if (range.getShortForm().equals("string") ||
								range.getShortForm().equals("textArea")) {
							for (int i = 0; i < editors.size(); i++) {
								OWLObjectEditor<?> editor = editors.get(i);
								if (editor instanceof OWLConstantEditor) {
									((OWLConstantEditor) editor).setDataType(getOWLDatatype(prop));
									curr_jtab = i;
									tabbedPane.setSelectedIndex(i);
									
								}
							}
							
						}
					} else {
						// default if there's no type
						for (int i = 0; i < editors.size(); i++) {
							OWLObjectEditor<?> editor = editors.get(i);
							if (editor instanceof OWLConstantEditor) {
								((OWLConstantEditor) editor).setDataType(getOWLDatatype(prop));
								curr_jtab = i;
								tabbedPane.setSelectedIndex(i);
								
							}
						}
					}
					
					
				}
			}
			
        });
        
    	
    	List<OWLObjectEditor<? extends OWLAnnotationValue>> result = new ArrayList<>();
        result.add(constantEditor);
        result.add(iriEditor);
        result.add(textEditor);
        result.add(enumEditor);
		return result;
	}
    
    private IRI getDataTypeIRI(OWLAnnotationProperty prop) {
		Set<OWLAnnotationPropertyRangeAxiom> types = 
				owlEditorKit.getOWLModelManager().getActiveOntology().getAnnotationPropertyRangeAxioms(prop);
		
		for (OWLAnnotationPropertyRangeAxiom ax : types) {
			return ax.getRange();
		}
		return null;
	}
    
    private OWLDatatype getOWLDatatype(OWLAnnotationProperty prop) {
		Set<OWLAnnotationPropertyRangeAxiom> types = 
				owlEditorKit.getOWLModelManager().getActiveOntology().getAnnotationPropertyRangeAxioms(prop);
		
		for (OWLAnnotationPropertyRangeAxiom ax : types) {
			return owlEditorKit.getOWLModelManager().getOWLDataFactory().getOWLDatatype(ax.getRange());
		}
		return null;
	}


	protected OWLObjectEditor<? extends OWLAnnotationValue> getSelectedEditor() {
        return editors.get(tabbedPane.getSelectedIndex());
    }


    @SuppressWarnings("unchecked")
	public boolean setEditedObject(OWLAnnotation annotation) {
        int tabIndex = -1;
        if (annotation != null) {
            annotationPropertySelector.setSelection(annotation.getProperty());
            for (int i = 0; i < editors.size(); i++) {
            	@SuppressWarnings("rawtypes")
				OWLObjectEditor editor = editors.get(i);
            	// because we don't know the type of the editor we need to test
            	if (editor.canEdit(annotation.getValue())) {
            		if (editor instanceof EnumEditor) {
            			EnumEditor eed = (EnumEditor) editor;
            			IRI iri = getDataTypeIRI(annotation.getProperty()); 
            			if ((iri != null) && eed.isDataTypeCombobox(iri)) {                		
            				eed.setProp(annotation.getProperty());
            				tabIndex = i;
            			}
            		}
            		editor.setEditedObject(annotation.getValue());
            		if (tabIndex == -1) {
            			tabIndex = i;
            			
            		}
            		curr_jtab = tabIndex;
            	}
            	else {
            		editor.clear();
            		editor.setEditedObject(null);
            		
            	}
            }
        }
        else {
            annotationPropertySelector.setSelection(lastSelectedProperty);
            for (int i = 0; i < editors.size(); i++) {
                OWLObjectEditor<? extends OWLAnnotationValue> editor = editors.get(i);
                editor.setEditedObject(null);
                editor.clear();
                if(lastEditorName.equals(editor.getEditorTypeName())) {
                    tabIndex = i;
                    curr_jtab = i;
                }
            }
        }
        tabbedPane.setSelectedIndex(tabIndex == -1 ? 0 : tabIndex);
        return true;
    }


    public OWLAnnotation getAnnotation() {
        OWLAnnotationProperty property = annotationPropertySelector.getSelectedObject();
        if (property != null){
            lastSelectedProperty = property;
            lastEditorName = getSelectedEditor().getEditorTypeName();

            OWLDataFactory dataFactory = owlEditorKit.getModelManager().getOWLDataFactory();

            OWLAnnotationValue obj = getSelectedEditor().getEditedObject();

            if (obj != null) {
            	if (obj instanceof OWLLiteral) {
            		String val = ((OWLLiteral) obj).getLiteral();
            		OWLAnnotationValue newobj = dataFactory.getOWLLiteral(mapper.fix(val), OWL2Datatype.RDF_PLAIN_LITERAL);
            		return dataFactory.getOWLAnnotation(property, newobj);
            	} else {
            		return dataFactory.getOWLAnnotation(property, obj);
            	}
            	
            }
        }
        return null;
    }


    public String getEditorTypeName() {
        return "OWL Annotation";
    }


    public boolean canEdit(Object object) {
        return object instanceof OWLAnnotation;
    }


    public JComponent getEditorComponent() {
        return mainPanel;
    }


    public JComponent getInlineEditorComponent() {
        return getEditorComponent();
    }


    /**
     * Gets the object that has been edited.
     * @return The edited object
     */
    public OWLAnnotation getEditedObject() {
        return getAnnotation();
    }


    public void dispose() {
        annotationPropertySelector.dispose();
        for (OWLObjectEditor<? extends OWLAnnotationValue> editor : editors) {
            editor.dispose();
        }
    }


    private void verify() {
        if (status != isValid()){
            status = isValid();
            for (InputVerificationStatusChangedListener l : verificationListeners){
                l.verifiedStatusChanged(status);
            }
        }
    }


    private boolean isValid() {
        return annotationPropertySelector.getSelectedObject() != null && getSelectedEditor().getEditedObject() != null;
    }


    public void addStatusChangedListener(InputVerificationStatusChangedListener listener) {
        verificationListeners.add(listener);
        listener.verifiedStatusChanged(isValid());
    }


    public void removeStatusChangedListener(InputVerificationStatusChangedListener listener) {
        verificationListeners.remove(listener);
    }
    
    private class VerifiedInputJPanel extends JPanel implements VerifiedInputEditor {
		private static final long serialVersionUID = -6537871629287844213L;

		public void addStatusChangedListener(InputVerificationStatusChangedListener listener) {
			NCIOWLAnnotationEditor.this.addStatusChangedListener(listener);
		}

		public void removeStatusChangedListener(InputVerificationStatusChangedListener listener) {
			NCIOWLAnnotationEditor.this.removeStatusChangedListener(listener);
		}
    	
    }
    
    protected final OWLAnnotationProperty getLastSelectedProperty() {
		return lastSelectedProperty;
	}
    
    protected final OWLAnnotationPropertySelectorPanel getAnnotationPropertySelector() {
		return annotationPropertySelector;
	}
}
