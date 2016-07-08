package gov.nih.nci.ui.transferhandler;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.TransferHandler;

import org.protege.editor.owl.model.entity.OWLEntityCreationSet;
import org.protege.editor.owl.ui.framelist.OWLFrameList;
import org.protege.editor.owl.ui.transfer.OWLObjectDataFlavor;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;

import gov.nih.nci.ui.ComplexEditPanel;
import gov.nih.nci.ui.NCIEditTab;
import gov.nih.nci.ui.dialog.NCIClassCreationDialog;

public class ListTransferHandler extends TransferHandler {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3809002388495775198L;
	private ComplexEditPanel complexEditPanel;
	
	public ListTransferHandler(ComplexEditPanel complexEditPanel) {
		this.complexEditPanel = complexEditPanel;
	}

	public boolean canImport(TransferSupport support) {
		if(!support.isDrop()) {
			return false;
		}
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public boolean importData(TransferSupport support) {
		if( !canImport(support)) {
			return false;
		}
		
		List<OWLClass> data; 
		try {
			data = (List<OWLClass>) support.getTransferable().getTransferData(OWLObjectDataFlavor.OWL_OBJECT_DATA_FLAVOR);
			
			
		} catch (UnsupportedFlavorException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		
		if (complexEditPanel.isSplitBtnSelected() || complexEditPanel.isCloneBtnSelected()) {
			if (NCIEditTab.currentTab().canSplit(data.get(0))) {
				// if you can split you can clone
				OWLEntityCreationSet<OWLClass> set = NCIClassCreationDialog.showDialog(complexEditPanel.getEditorKit(),
						"Please enter a class name", OWLClass.class);

				if (set != null) {

					NCIEditTab.currentTab().splitClass(set, data.get(0));
					complexEditPanel.setEnableUnselectedRadioButtons(false);
					return true;
				}
				return false;
			} else {
				JOptionPane.showMessageDialog(complexEditPanel, 
						"Can't split or clone a retired class.", "Warning", JOptionPane.WARNING_MESSAGE);
				return false;
			}
		} else if (complexEditPanel.isMergeBtnSelected()) {	
			// TODO: need check for canMerge here
			complexEditPanel.dropOnComp(support.getComponent(), data.get(0));
			complexEditPanel.setEnableUnselectedRadioButtons(false);
			
		} else {
			JOptionPane.showMessageDialog(complexEditPanel, "One editing option has to be selected.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
		
		return true;
		
	}

}
