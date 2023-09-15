package gov.nih.nci.ui;

import java.awt.BorderLayout;

import org.protege.editor.owl.model.selection.OWLSelectionModelListener;
import org.protege.editor.owl.ui.view.cls.OWLClassAnnotationsViewComponent;
import org.semanticweb.owlapi.model.OWLClass;

import gov.nih.nci.ui.event.ComplexEditType;
import gov.nih.nci.ui.event.EditTabChangeEvent;
import gov.nih.nci.ui.event.EditTabChangeListener;


public class NCIEditViewComponent extends OWLClassAnnotationsViewComponent implements EditTabChangeListener {
	private static final long serialVersionUID = 1L;
	private EditPanel editPanel;
	
	
	
    public void initialiseClassView() throws Exception {
    	
    	editPanel = new EditPanel(getOWLEditorKit());
    	
        setLayout(new BorderLayout());
        add(editPanel);
        NCIEditTab.addListener(this);
        
    }

    @Override
	protected OWLClass updateView(OWLClass selectedClass) {
    	if (selectedClass != null) {
    		if (!NCIEditTab.currentTab().isRetired(selectedClass)) {
    			return selectedClass;
        	}    		
    	} else {
    		editPanel.setSelectedClass(null);
    	}    	
        return null;
	}

	@Override
	public void disposeView() {
		editPanel.disposeView();
	}

	
	@Override
	public void handleChange(EditTabChangeEvent event) {
		if (event.isType(ComplexEditType.ADD_PROP)) {
			editPanel.addNewComplexProp();
		} else if (event.isType(ComplexEditType.INIT_PROPS)) {
			editPanel.disposeView();
			this.remove(editPanel);			
			editPanel = new EditPanel(getOWLEditorKit());
	    	
	        setLayout(new BorderLayout());
	        add(editPanel);
			
		} else if (event.isType(ComplexEditType.MODIFY)) { 
			if (NCIEditTab.currentTab().getCurrentOp().isSplitting() ||
					NCIEditTab.currentTab().getCurrentOp().isMerging() ||
					NCIEditTab.currentTab().getCurrentOp().isCloning()) {
			} else {
				if (!(NCIEditTab.currentTab().isRetired(NCIEditTab.currentTab().getCurrentOp().getCurrentlyEditing()) ||
						NCIEditTab.currentTab().isPreRetired(NCIEditTab.currentTab().getCurrentOp().getCurrentlyEditing())))
				{
					if (NCIEditTab.currentTab().hasActiveClient()) {
						editPanel.enableButtons();
						NCIEditTab.currentTab().setEditInProgress(true);
						NCIEditTab.currentTab().setCurrentlyEditing(editPanel.getSelectedClass(), true);
					} else if (NCIEditTab.currentTab().getCurrentOp().isRetiring()) {
						editPanel.enableButtons();
					} else {
						editPanel.enableCancelButtonOnly();
					}
					//NCIEditTab.currentTab().setEditInProgress(true);
					//NCIEditTab.currentTab().setCurrentlyEditing(editPanel.getSelectedClass(), true);
				} else {
					getOWLEditorKit().getWorkspace().getViewManager().bringViewToFront(
							"nci-edit-tab.RetireView");
					
				}
			}

		} else if (event.isType(ComplexEditType.SELECTED)) {
			if (NCIEditTab.currentTab().isRetired(NCIEditTab.currentTab().getCurrentlySelected())) {

			} else {
				editPanel.setSelectedClass(NCIEditTab.currentTab().getCurrentlySelected());
				if (!NCIEditTab.currentTab().inComplexOp()) {
					getOWLEditorKit().getWorkspace().getViewManager().bringViewToFront(
							"nci-edit-tab.EditView");
				}
			}

		} else if (event.isType(ComplexEditType.COMMIT) ||
				event.isType(ComplexEditType.RESET)) { 
			editPanel.disableButtons();
			
		}		
	}

}
