package gov.nih.nci.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.frame.cls.OWLClassDescriptionFrame;
import org.protege.editor.owl.ui.framelist.OWLFrameList;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyChange;

import gov.nih.nci.ui.dialog.ComplexPropChooser;
import gov.nih.nci.ui.event.ComplexEditType;

public class EditPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private OWLEditorKit owlEditorKit;
	
	private Set<OWLAnnotationProperty> complexProps;
	
	private Set<OWLAnnotationProperty> propsToExclude;
	
	private Set<OWLAnnotationProperty> readOnlyProperties;
	
	private OWLFrameList<OWLClass> list;
	
	private OWLFrameList<OWLAnnotationSubject> gen_props;
	
	private OWLClass currentClass = null;
	
	JPanel genPropPanel = null;
    
    private List<PropertyTablePanel> tablePanelList = new ArrayList<PropertyTablePanel>();
    
    JTabbedPane tabbedPane;
    
    JScrollPane descrPane;
    
    private JTextField prefNameText;
    private String origPref = "";
    private String newPref = "";
    private JTextField iri;
    private JTextField codeText;
	
	private JPanel buttonPanel;
	
	private JButton saveButton, cancelButton;
    
    private DocumentListener doc_listen = null;
    
    private boolean read_only = false;
    
    public EditPanel(OWLEditorKit editorKit) {
    	this(editorKit, false);    	
    }
    
    public EditPanel(OWLEditorKit editorKit, boolean ro) {
    	read_only = ro;
    	
        this.owlEditorKit = editorKit;
        
        complexProps = NCIEditTab.currentTab().getComplexProperties();
        propsToExclude = new HashSet<OWLAnnotationProperty>();
        propsToExclude.addAll(complexProps);
        propsToExclude.add(NCIEditTabConstants.PREF_NAME);
        propsToExclude.add(NCIEditTabConstants.CODE_PROP);
        
        
        readOnlyProperties = NCIEditTab.currentTab().getImmutableProperties();
        
        createUI();
        
    }
    
    private JPanel complexPropertyPanel;
    
    private void createUI() {
    	setLayout(new BorderLayout());
        
        tabbedPane = new JTabbedPane();
        
        complexPropertyPanel = new JPanel();
        complexPropertyPanel.setLayout(new BoxLayout(complexPropertyPanel, BoxLayout.Y_AXIS));
        JScrollPane compPropSP = new JScrollPane(complexPropertyPanel);
       
        tabbedPane.addTab("Complex Properties", compPropSP);
        
        genPropPanel = new JPanel();
        genPropPanel.setLayout(new BorderLayout());
        
        JLabel prefNamLabel = new JLabel("Preferred Name");        
        prefNameText = new JTextField("preferred name");
        prefNameText.setVisible(true);
        
        doc_listen = new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				if (prefNameText.getText().equals(origPref)) {
					if (!NCIEditTab.currentTab().isEditing()) {
						disableButtons();
					}
					newPref = "";

				} else {
					if (!NCIEditTab.currentTab().isEditing()) {
						NCIEditTab.currentTab().setEditInProgress(true);
						NCIEditTab.currentTab().setCurrentlyEditing(currentClass, false);						
						enableButtons();
					}
					newPref = prefNameText.getText();
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				if (prefNameText.getText().equals(origPref)) {
					if (!NCIEditTab.currentTab().isEditing()) {
						disableButtons();
					}
					newPref = "";

				} else {
					if (!NCIEditTab.currentTab().isEditing()) {
						NCIEditTab.currentTab().setEditInProgress(true);
						NCIEditTab.currentTab().setCurrentlyEditing(currentClass, false);
						enableButtons();
					}
					newPref = prefNameText.getText();
				}

			}

			@Override
			public void changedUpdate(DocumentEvent e) {}
        	
        };
        
        prefNameText.getDocument().addDocumentListener(doc_listen);
        
        JPanel topHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        topHeader.add(prefNamLabel);
        topHeader.add(prefNameText);
        
        JLabel iriLabel = new JLabel("IRI fragment:");
        iri = new JTextField("iri-short-form");
        iri.setVisible(true);
        iri.setEditable(false);
       
        topHeader.add(iriLabel);
        topHeader.add(iri);
        
        JLabel classCode = new JLabel("Code");
        codeText = new JTextField("code");
        codeText.setVisible(true);
        codeText.setEditable(false);
               
        topHeader.add(classCode);
        topHeader.add(codeText);
      
        gen_props = new OWLFrameList<OWLAnnotationSubject>(owlEditorKit, 
        		new FilteredAnnotationsFrame(owlEditorKit, propsToExclude, readOnlyProperties), read_only) {
        	
        	public void handleDelete() {
        		super.handleDelete();
        		//NCIEditTab.currentTab().classModified();        		
        	}
        	
        	public void handleEdit() {
        		super.handleEdit();
        	    //NCIEditTab.currentTab().classModified();
        	}        	
        };
          
        JScrollPane generalSP = new JScrollPane(gen_props);//will add tree or list to it
        generalSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        genPropPanel.add(generalSP, BorderLayout.CENTER);
        
        tabbedPane.addTab("General", genPropPanel);
        
        Iterator<OWLAnnotationProperty> it = complexProps.iterator();
        while(it.hasNext()) {
        	createComplexPropertyTable((OWLAnnotationProperty) it.next());
        }        
        
        
        list = new OWLFrameList<OWLClass>(owlEditorKit, new NCIOWLClassDescriptionFrame(owlEditorKit)) {
        //list = new OWLFrameList<OWLClass>(owlEditorKit, new OWLClassDescriptionFrame(owlEditorKit)) {
        	public void handleDelete() {
        		super.handleDelete();
        	    //NCIEditTab.currentTab().classModified();
        	}
        	
        	public void handleEdit() {
        		super.handleEdit();
        	    //NCIEditTab.currentTab().classModified();
        	}        	
        };
        
        descrPane = new JScrollPane(list);// will add description list to it
        descrPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                
        tabbedPane.addTab("Description", descrPane);
        
        add(topHeader, BorderLayout.NORTH);
                
        add(tabbedPane, BorderLayout.CENTER);
		
		add(createJButtonPanel(), BorderLayout.SOUTH);
		
		setVisible(true);
    }
    
    public void setSelectedClass(OWLClass cls) {
    	
    	prefNameText.getDocument().removeDocumentListener(doc_listen);    	
    	if (cls != null && !NCIEditTab.currentTab().isUneditableRoot(cls)) {   		

    		currentClass = cls;

    		Optional<String> ps = NCIEditTab.currentTab().getRDFSLabel(cls);

    		if (ps.isPresent()) {
    			origPref = ps.get();
    			newPref = "";
    			prefNameText.setText(ps.get());    	     
    		} else {
    			prefNameText.setText("Missing");
    		}

    		Optional<String> cps = NCIEditTab.currentTab().getCode(cls);

    		if (cps.isPresent()) {
    			codeText.setText(cps.get());
    		} else {
    			codeText.setText("nocode");
    		}
    		
    		iri.setText(cls.getIRI().getShortForm());

    		List<PropertyTablePanel> tablePanelList = getPropertyTablePanelList();
    		for (PropertyTablePanel tablePanel : tablePanelList) {
    			tablePanel.setSelectedCls(cls);
    			if (tablePanel.isViewable()) {
    				complexPropertyPanel.add(tablePanel);    				
    			} else {
    				this.complexPropertyPanel.remove(tablePanel);
    			}
    		}
    		complexPropertyPanel.repaint();
    		
    		list.setRootObject(cls);
    		if (cls != null) {
    			gen_props.setRootObject(cls.getIRI());
    		}

    		if (NCIEditTab.currentTab().getCurrentOp().isRetiring()) {
    			tabbedPane.setSelectedComponent(descrPane);

    		}
    	} else {
    		// null out panels
    		List<PropertyTablePanel> tablePanelList = getPropertyTablePanelList();
    		for (PropertyTablePanel tablePanel : tablePanelList) {
    			tablePanel.setSelectedCls(null);
    			if (tablePanel.isViewable()) {
    				complexPropertyPanel.add(tablePanel);    				
    			} else {
    				this.complexPropertyPanel.remove(tablePanel);
    			}
    		}
    		complexPropertyPanel.repaint();
    		list.setRootObject(null);
    		gen_props.setRootObject(null);
    		prefNameText.setText(null);
    		codeText.setText("nocode");
    		genPropPanel.repaint();
    		currentClass = null;
    	}
    	prefNameText.getDocument().addDocumentListener(doc_listen);
    }
    
    public OWLClass getSelectedClass() {
    	return this.currentClass;
    }
    
    public void addNewComplexProp() {
    	ComplexPropChooser chooser = new ComplexPropChooser(NCIEditTab.currentTab().getComplexProperties());
    	OWLAnnotationProperty c_prop = chooser.showDialog(owlEditorKit, "Choosing Complex Property");
    	if (c_prop != null) {
    		for (PropertyTablePanel panel : tablePanelList) {
    			if (panel.getComplexProp().equals(c_prop)) {
    				panel.createNewProp();
    			}
    		}
    		setSelectedClass(NCIEditTab.currentTab().getCurrentlyEditing());
    	}
    }
    
    private void createComplexPropertyTable(OWLAnnotationProperty complexProperty) {
    	Optional<String> tableName = NCIEditTab.currentTab().getRDFSLabel(complexProperty);
    	PropertyTablePanel tablePanel = new PropertyTablePanel(this.owlEditorKit, complexProperty, tableName.get());
    	tablePanelList.add(tablePanel);
    }
    
    public OWLEditorKit getEditorKit() {
    	return owlEditorKit;
    }
    
    public List<PropertyTablePanel> getPropertyTablePanelList() {
    	return tablePanelList;
    }
    
    private JPanel createJButtonPanel() {
		buttonPanel = new JPanel();
		saveButton = new JButton("Save");
		saveButton.setEnabled(false);
		
		saveButton.addActionListener(new ActionListener() {
			 
            public void actionPerformed(ActionEvent e)
            {
            	// Do the save
            	if (shouldSave()) {
            		if (NCIEditTab.currentTab().syncFullSyn(NCIEditTab.currentTab().getCurrentlyEditing())) {

            			if (!newPref.equals("")) {
            				NCIEditTab.currentTab().syncPrefName(newPref);                		
            			}

            			if (NCIEditTab.currentTab().commitChanges()) {
            				origPref = prefNameText.getText();
            				NCIEditTab.currentTab().refreshNavTree();
            			}
            		}
            		//disableButtons();
            	} else {
                	saveButton.setEnabled(false);
                }
            	
            }
        });     
		
		cancelButton = new JButton("Clear");
		cancelButton.setEnabled(false);
		
		cancelButton.addActionListener(new ActionListener() {
			 
            public void actionPerformed(ActionEvent e)
            {
            	NCIEditTab.currentTab().undoChanges();            	
            	NCIEditTab.currentTab().setEditInProgress(false);
            	//NCIEditTab.currentTab().selectClass(NCIEditTab.currentTab().getCurrentlySelected());
            	NCIEditTab.currentTab().refreshNavTree();
            	disableButtons();
            	
            }
        });     
		
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);
		return buttonPanel;
	}
    
    public boolean shouldSave() {
    	if (NCIEditTab.currentTab().getCurrentOp().isRetiring()) {
    		NCIEditTab.currentTab().updateRetire();
    		return false;
    	} else {
    		return true;
    	}
    }
    
    
    
    
    
    public void disposeView() {
    	list.dispose();
    	gen_props.dispose();
    	for(PropertyTablePanel propTable : tablePanelList) {
    		propTable.dispose();
    	}
    }
    
    public void enableButtons() {
    	saveButton.setEnabled(true);
    	cancelButton.setEnabled(true);    	
    }
    
    public void disableButtons() {
    	saveButton.setEnabled(false);
    	cancelButton.setEnabled(false);    	
    }
}
