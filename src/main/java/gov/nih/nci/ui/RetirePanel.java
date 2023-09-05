package gov.nih.nci.ui;

import static gov.nih.nci.ui.NCIEditTabConstants.DEPR_CONCEPT_STATUS_PROP;
import static gov.nih.nci.ui.NCIEditTabConstants.DEPR_CONCEPT_STATUS_VALUE;
import static gov.nih.nci.ui.NCIEditTabConstants.PRE_RETIRE_ROOT;
import static gov.nih.nci.ui.NCIEditTabConstants.RETIRE_ROOT;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.ui.framelist.OWLFrameList;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import gov.nih.nci.ui.event.ComplexEditType;
import gov.nih.nci.ui.event.EditTabChangeEvent;
import gov.nih.nci.ui.transferhandler.RetireTransferHandler;
import gov.nih.nci.utils.ReferenceFinder;



public class RetirePanel extends JPanel {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private OWLEditorKit owlEditorKit;	

    private OWLModelManager mngr; 
    private OWLDataFactory df;    	
	private OWLOntology ont;	
	private OWLClass classToRetire = null; 
	
	public OWLClass getRetiringClass() { return classToRetire; }
	
    private Map<OWLAnnotationProperty, Set<String>> fixups;
    
    private OWLFrameList<OWLAnnotationSubject> upperPanelList;
    
    private UsagePanel usage_panel = null;
    
    private JPanel buttonPanel;
    
    private JButton retireButton;
    
    private JButton cancelButton;
    
    public RetirePanel(OWLEditorKit editorKit) {
        this.owlEditorKit = editorKit;
        mngr = owlEditorKit.getOWLModelManager();
        df = mngr.getOWLDataFactory();    	
    	ont = mngr.getActiveOntology();
        
        upperPanelList = new OWLFrameList<OWLAnnotationSubject>(owlEditorKit, 
        		new FilteredAnnotationsFrame(owlEditorKit, new HashSet<>(), 
        				NCIEditTab.currentTab().getImmutableProperties()));
        
        usage_panel = new UsagePanel(owlEditorKit);
        createUI();
        this.setOWLClass(null);
    }


    private void createUI() {
        setLayout(new BorderLayout());
        
        JPanel upperPanel = new JPanel(new BorderLayout());
        JPanel lowerPanel = new JPanel(new BorderLayout());
        
        JScrollPane upperComp = new JScrollPane(upperPanelList);
        upperComp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        upperPanel.add(upperComp);
        upperPanel.setTransferHandler(new RetireTransferHandler(this));
        
        JScrollPane lowerComp = new JScrollPane(usage_panel);
        lowerComp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        lowerPanel.add(lowerComp);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upperPanel, lowerPanel);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(280);

		add(splitPane, BorderLayout.CENTER);
		add(createJButtonPanel(), BorderLayout.SOUTH);
        
    }
    
    public void dispose() {
		upperPanelList.dispose();		
	}
    
    
    public void setOWLClass(OWLClass inst) {
    	
    	// always update the usages panel
    	usage_panel.setOWLEntity(inst, false);
    	if (inst == null) {
    		return;
    	}
    	
    	if ((classToRetire != null) &&
    			(inst.equals(classToRetire))) {
    		
    	} else {
    		// either first time in or a new class
    		classToRetire = inst;
			upperPanelList.setRootObject(inst.getIRI()); 
			fixups = (new ReferenceFinder(owlEditorKit.getModelManager())).computeAnnotations(inst);
			if (NCIEditTab.currentTab().isPreRetired(classToRetire)) {
				retireButton.setText("Approve");
			} else {
				retireButton.setText("Retire");
			}
			this.enableButtons();
    		
    	}
    }  
    
    private JPanel createJButtonPanel() {
    	buttonPanel = new JPanel();
    	retireButton = new JButton("Retire");
    	retireButton.setEnabled(false);

    	retireButton.addActionListener(new ActionListener() {

    		public void actionPerformed(ActionEvent e)
    		{
    			if (usage_panel.getCount() > 1) {
    				warnUsages();
    			} else if (retireButton.getText().equals("Save")) {
    				// TODO: refactor and move type check to edit tab
    				//YX - 20230828: If it is in file mode, do not commitChanges
    				if (NCIEditTab.currentTab().hasActiveClient()) {
	    				if (NCIEditTab.currentTab().commitChanges(true)) {
	    					upperPanelList.setRootObject(null);
	    					usage_panel.setOWLEntity(null, false);
	    					disableButtons();
	    					NCIEditTab.currentTab().selectClass(classToRetire);
	    					NCIEditTab.currentTab().refreshNavTree();
	    					NCIEditTab.currentTab().completeRetire();
	    					NCIEditTab.currentTab().fireChange(new EditTabChangeEvent(NCIEditTab.currentTab(), 
	    							ComplexEditType.READ));
	    				}
    				} else {
						upperPanelList.setRootObject(null);
    					usage_panel.setOWLEntity(null, false);
    					disableButtons();
    					NCIEditTab.currentTab().selectClass(classToRetire);
    					NCIEditTab.currentTab().refreshNavTree();
    					NCIEditTab.currentTab().completeRetire();
    					NCIEditTab.currentTab().fireChange(new EditTabChangeEvent(NCIEditTab.currentTab(), 
    							ComplexEditType.READ));
    				}
    			} else if (retireButton.getText().equals("Approve")) {
    				approveRetire();
    				
    				
    			} else {
    				// proceed to retire
    				if (NCIEditTab.currentTab().completeRetire(fixups)) {
    					NCIEditTab.currentTab().refreshNavTree();
    					retireButton.setText("Save");
    				}			
    			}


    		}
    	});     

    	cancelButton = new JButton("Cancel");
    	cancelButton.setEnabled(false);

    	cancelButton.addActionListener(new ActionListener() {

    		public void actionPerformed(ActionEvent e)
    		{
    			upperPanelList.setRootObject(null);
    			usage_panel.setOWLEntity(null, false);
    		    classToRetire = null;
    			NCIEditTab.currentTab().undoChanges();
    			NCIEditTab.currentTab().cancelRetire();
    			disableButtons();
    		}
    	});     

    	buttonPanel.add(retireButton);
    	buttonPanel.add(cancelButton);
    	return buttonPanel;
    }
    
    public void warnUsages() {
    	JOptionPane.showMessageDialog(this, "Can't retire until all usages are repaired", "Warning", JOptionPane.WARNING_MESSAGE);    	
    }
    
    
    
    
    public void approveRetire() {
    	
    	ont = NCIEditTab.currentTab().getOWLModelManager().getActiveOntology();
    	
    	List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    	
    	Set<OWLSubClassOfAxiom> subs = ont.getSubClassAxiomsForSubClass(classToRetire);
    	for (OWLSubClassOfAxiom s : subs) {
    		if (s.getSuperClass().asOWLClass().equals(PRE_RETIRE_ROOT)) {
    			changes.add(new RemoveAxiom(ont, s));
    			//old_parents.add(s.getSuperClass().asOWLClass());
    		}
    	}
    	changes.add(new AddAxiom(mngr.getActiveOntology(),
    			df.getOWLSubClassOfAxiom(classToRetire, RETIRE_ROOT)));
    	changes.add(new AddAxiom(mngr.getActiveOntology(), df.getDeprecatedOWLAnnotationAssertionAxiom(classToRetire.getIRI())));
    	if (DEPR_CONCEPT_STATUS_PROP != null) {
    		changes.add(new AddAxiom(mngr.getActiveOntology(),
    				df.getOWLAnnotationAssertionAxiom(DEPR_CONCEPT_STATUS_PROP, classToRetire.getIRI(),
    						df.getOWLLiteral(DEPR_CONCEPT_STATUS_VALUE, OWL2Datatype.RDF_PLAIN_LITERAL))));
    		
    	}
    	
    	mngr.applyChanges(changes);
        
        retireButton.setText("Save");
    	
    	
    }
    
    public void enableButtons() {
    	retireButton.setEnabled(true);
    	cancelButton.setEnabled(true);
    	
    }
    
    public void disableButtons() {
    	retireButton.setEnabled(false);
    	cancelButton.setEnabled(false);
    	
    }
    
    
    
}
