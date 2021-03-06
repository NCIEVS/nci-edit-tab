package gov.nih.nci.ui;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.ui.framelist.OWLFrameList;
import org.protege.editor.owl.ui.view.AbstractActiveOntologyViewComponent;
import org.semanticweb.owlapi.model.OWLOntology;


/*
 * Copyright (C) 2007, University of Manchester
 *
 *
 */

/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 14-Oct-2007<br><br>
 */
public class InferredAxiomsViewComponent extends AbstractActiveOntologyViewComponent {


    private InferredAxiomsFrame frame;

    private OWLFrameList<OWLOntology> frameList;

    private OWLModelManagerListener listener = event -> {
        if(event.isType(EventType.ONTOLOGY_CLASSIFIED)) {
            if(isSynchronizing() && isShowing()) {
                try {
                        updateView(getOWLModelManager().getActiveOntology());
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    };

    protected void initialiseOntologyView() throws Exception {
        add(getCenterPane(), BorderLayout.CENTER);
        // considered adding a refresh button here...
        updateHeader();
        getOWLModelManager().addListener(listener);
    }
    
    private JComponent getCenterPane() {
        setLayout(new BorderLayout());
        frame = new InferredAxiomsFrame(getOWLEditorKit());
        frameList = new OWLFrameList<>(getOWLEditorKit(), frame);
        getOWLModelManager().removeListener(frameList.getModelListener());
        frameList.setRootObject(getOWLModelManager().getActiveOntology());
        return new JScrollPane(frameList);
    }


    protected void updateView(OWLOntology activeOntology) throws Exception {    	
    	if (isSynchronizing()) {
    		frameList.setRootObject(activeOntology);
    		updateHeader();
    	}    	
    }


    private void updateHeader() {
        getView().setHeaderText("Classified using " + getOWLModelManager().getOWLReasonerManager().getCurrentReasonerName());
    }


    protected void disposeOntologyView() {
        frameList.dispose();
        getOWLModelManager().removeListener(listener);
    }
}
