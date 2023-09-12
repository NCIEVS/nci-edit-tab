package gov.nih.nci.ui;

import java.awt.BorderLayout;

import org.protege.editor.owl.model.selection.OWLSelectionModelListener;
import org.protege.editor.owl.ui.view.cls.OWLClassAnnotationsViewComponent;
import org.semanticweb.owlapi.model.OWLClass;

import gov.nih.nci.ui.event.ComplexEditType;
import gov.nih.nci.ui.event.EditTabChangeEvent;
import gov.nih.nci.ui.event.EditTabChangeListener;
import gov.nih.nci.ui.event.PreferencesChangeEvent;
import gov.nih.nci.ui.event.PreferencesChangeListener;

public class NCIRetireViewComponent extends OWLClassAnnotationsViewComponent implements EditTabChangeListener, PreferencesChangeListener {

    private static final long serialVersionUID = 1L;
	private RetirePanel retirePanel;

	@Override
	public void handleChange(EditTabChangeEvent event) {
		if (event.isType(ComplexEditType.RETIRE)) {
			
			retirePanel.setOWLClass(NCIEditTab.currentTab().getRetireClass());
			
			
			getOWLEditorKit().getWorkspace().getViewManager().bringViewToFront(
	               "nci-edit-tab.RetireView");
			
			setHeaderText(NCIEditTab.currentTab().getRetireClass().asOWLClass().getIRI().getShortForm());
		}
	}

	
	
	public NCIRetireViewComponent() {
		NCIEditTab.addListener(this);	
		NCIEditTab.addPrefListener(this);
	}

	@Override
	public void initialiseClassView() throws Exception {
		if (NCIEditTabPreferences.getFnRetire()) {
			retirePanel = new RetirePanel(getOWLEditorKit());
	    	setLayout(new BorderLayout());
	        add(retirePanel);
		}
	}
	
	@Override
	protected OWLClass updateView(OWLClass selectedClass) {	
		if(retirePanel != null) {
			if (retirePanel.getRetiringClass() != null) {
				if (!selectedClass.equals(retirePanel.getRetiringClass())) {
					// switch to edit tab to edit reference to retiring class
					if (NCIEditTab.currentTab().getCurrentOp().isRetiring()) {
						NCIEditTab.currentTab().selectClass(selectedClass);
						getOWLEditorKit().getWorkspace().getViewManager().bringViewToFront(
								"nci-edit-tab.EditView");
					}
	
				}
			} else if (NCIEditTab.currentTab().isRetired(selectedClass)) {
				this.retirePanel.setOWLClass(null);
				return null;
			}
		}
		return selectedClass;
	}

	@Override
	public void disposeView() {
		retirePanel.dispose();
	}

	@Override
	public void handleChange(PreferencesChangeEvent event) {
		try {
		if (event.isType(ComplexEditType.PREFMODIFY)) {
			if(NCIEditTabPreferences.getFnRetire()) {
				if (retirePanel == null) {
					initialiseClassView();
				} else {
					retirePanel.enableButtons();
				}
	    	} else {	
	    		remove(retirePanel);
	    		NCIEditTab.currentTab().resetState();
	    		retirePanel = null;
	    		
	    	}
		} 
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
