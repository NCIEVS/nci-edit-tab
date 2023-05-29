package gov.nih.nci.ui.transferhandler;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.TransferHandler;

import org.protege.editor.owl.ui.transfer.OWLObjectDataFlavor;
import org.semanticweb.owlapi.model.OWLClass;

import gov.nih.nci.ui.ComplexEditPanel;
import gov.nih.nci.ui.NCIEditTab;
import gov.nih.nci.ui.dialog.NCIClassCreationDialog;
import gov.nih.nci.ui.event.ComplexEditType;
import gov.nih.nci.utils.PropertyCheckUtil;

public class ComplexEditTransferHandler extends TransferHandler {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3809002388495775198L;
	private ComplexEditPanel complexEditPanel;
	
	public ComplexEditTransferHandler(ComplexEditPanel complexEditPanel) {
		this.complexEditPanel = complexEditPanel;
	}

	public boolean canImport(TransferSupport support) {
		if(!support.isDrop()) {			
			return false;
		}
		if (NCIEditTab.currentTab().getCurrentOp().isRetiring() ||
				NCIEditTab.currentTab().isEditing()) {
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
			boolean clone_p = complexEditPanel.isCloneBtnSelected(); 
			if (NCIEditTab.currentTab().isNotSpecialRoot(data.get(0))) {
				// if you can split you can clone
				if (clone_p) {
					NCIEditTab.currentTab().setOp(ComplexEditType.CLONE);
				} else {
					NCIEditTab.currentTab().setOp(ComplexEditType.SPLIT);

				}
				NCIClassCreationDialog<OWLClass> dlg = new NCIClassCreationDialog<OWLClass>(complexEditPanel.getEditorKit(),
						"Please enter a class name", OWLClass.class, Optional.empty(), Optional.empty());

				if (dlg.showDialog()) {
					NCIEditTab.currentTab().splitClass(dlg.getNewClass(), data.get(0), clone_p);
					//if (NCIEditTab.currentTab().hasActiveClient()) {
						complexEditPanel.setEnableUnselectedRadioButtons(false);
					//}
					return true;
				} else {
					NCIEditTab.currentTab().cancelOp();					
					return false;					
				}
				
			} else {
				JOptionPane.showMessageDialog(complexEditPanel, 
						"Can't split or clone a retired class.", "Warning", JOptionPane.WARNING_MESSAGE);
				return false;
			}
		} else if (complexEditPanel.isMergeBtnSelected() || complexEditPanel.isDualBtnSelected()) {	
			PropertyCheckUtil propCheckUtil = new PropertyCheckUtil();
			if (complexEditPanel.isMergeBtnSelected() && !propCheckUtil.canMerge(data.get(0))) {
				JOptionPane.showMessageDialog(complexEditPanel, 
						"Can't merge, only a manager can do this.", "Warning", JOptionPane.WARNING_MESSAGE);
				return false;
				
			}
			complexEditPanel.dropOnComp(support.getComponent(), data.get(0));
			complexEditPanel.setEnableUnselectedRadioButtons(false);
			
		} else {
			JOptionPane.showMessageDialog(complexEditPanel, "One editing option has to be selected.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
		
		return true;
		
	}

}
