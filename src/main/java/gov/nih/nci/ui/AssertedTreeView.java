package gov.nih.nci.ui;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.tree.TreePath;

import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.protege.editor.owl.ui.framelist.OWLFrameList;
import org.protege.editor.owl.ui.tree.UserRendering;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassHierarchyViewComponent;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

public class AssertedTreeView extends AbstractOWLClassHierarchyViewComponent {

    /**
	 * 
	 */
	private static final long serialVersionUID = 3417030023003101000L;
	
	
	public AssertedTreeView() {}

    @Override
    public void disposeView() {
		//treePanel.dispose();
    }

	@Override
	protected void performExtraInitialisation() throws Exception {
		//this.getTree().setBackground(OWLFrameList.INFERRED_BG_COLOR);
		
	}
	
	@Override
	protected OWLClass updateView(OWLClass selectedClass) {
		getTree().setSelectedOWLObject(selectedClass);
        return selectedClass;
	}
	
	@Override
	public void setSelectedEntity(OWLClass entity) {
		getTree().setSelectedOWLObject(entity);	
		

	} 
	

        
	@Override
	protected UserRendering getUserRenderer() {
		return null;
	}

	@Override
	protected OWLObjectHierarchyProvider<OWLClass> getHierarchyProvider() {
		//return getOWLModelManager().getOWLHierarchyManager().getInferredOWLClassHierarchyProvider();
		//return Optional.empty();
        return getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider();
    }

	@Override
	protected Optional<OWLObjectHierarchyProvider<OWLClass>> getInferredHierarchyProvider() {
		return Optional.empty();
		//return Optional.of(getOWLModelManager().getOWLHierarchyManager().getInferredOWLClassHierarchyProvider());
	}
}

