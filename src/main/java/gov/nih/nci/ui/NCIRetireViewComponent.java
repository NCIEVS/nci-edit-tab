package gov.nih.nci.ui;

import java.awt.BorderLayout;

import org.protege.editor.owl.model.selection.OWLSelectionModelListener;
import org.protege.editor.owl.ui.view.cls.OWLClassAnnotationsViewComponent;
import org.semanticweb.owlapi.model.OWLClass;

import gov.nih.nci.ui.event.ComplexEditType;
import gov.nih.nci.ui.event.EditTabChangeEvent;
import gov.nih.nci.ui.event.EditTabChangeListener;

public class NCIRetireViewComponent extends OWLClassAnnotationsViewComponent implements EditTabChangeListener {

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
	}

	@Override
	public void initialiseClassView() throws Exception {
		retirePanel = new RetirePanel(getOWLEditorKit());
    	setLayout(new BorderLayout());
        add(retirePanel);
        
	}
	
	@Override
	protected OWLClass updateView(OWLClass selectedClass) {	
		if (retirePanel.getRetiringClass() != null) {
			if (!selectedClass.equals(retirePanel.getRetiringClass())) {
				// switch to edit tab to edit reference to retiring class
				if (NCIEditTab.currentTab().isRetiring()) {
					NCIEditTab.currentTab().selectClass(selectedClass);
					getOWLEditorKit().getWorkspace().getViewManager().bringViewToFront(
							"nci-edit-tab.EditView");
				}

			}
		}
		return selectedClass;

	}

	@Override
	public void disposeView() {
		retirePanel.dispose();
	}

}
