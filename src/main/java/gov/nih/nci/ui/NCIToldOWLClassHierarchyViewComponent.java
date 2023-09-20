package gov.nih.nci.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.FocusManager;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.protege.editor.core.ui.menu.PopupMenuId;
import org.protege.editor.core.ui.view.DisposableAction;
import org.protege.editor.core.ui.workspace.TabbedWorkspace;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.protege.editor.owl.model.selection.SelectionDriver;
import org.protege.editor.owl.ui.OWLIcons;
import org.protege.editor.owl.ui.action.AbstractOWLTreeAction;
import org.protege.editor.owl.ui.action.DeleteClassAction;
import org.protege.editor.owl.ui.tree.OWLTreeDragAndDropHandler;
import org.protege.editor.owl.ui.tree.UserRendering;
import org.protege.editor.owl.ui.view.CreateNewChildTarget;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassHierarchyViewComponent;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.OWLEntitySetProvider;

import gov.nih.nci.ui.action.AddComplexTarget;
import gov.nih.nci.ui.action.CloneClassTarget;
import gov.nih.nci.ui.action.RetireClassTarget;
import gov.nih.nci.ui.action.SplitClassTarget;
import gov.nih.nci.ui.dialog.BatchProcessingDialog;
import gov.nih.nci.ui.dialog.NCIClassCreationDialog;
import gov.nih.nci.ui.event.ComplexEditType;
import gov.nih.nci.ui.event.EditTabChangeEvent;

public class NCIToldOWLClassHierarchyViewComponent extends AbstractOWLClassHierarchyViewComponent
implements CreateNewChildTarget, SplitClassTarget, CloneClassTarget,
RetireClassTarget, AddComplexTarget, SelectionDriver {
	
	private static final Icon ADD_SUB_ICON = OWLIcons.getIcon("class.add.sub.png");
	private static final JButton batchbutton = new JButton("Batch Load/Edit");
	private static final JButton searchbutton = new JButton("Search");
	private static final BatchProcessOutputPanel batchProcessPanel = new BatchProcessOutputPanel();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private class InternalOWLEntitySetProvider implements OWLEntitySetProvider<OWLClass> {

        public Set<OWLClass> getEntities() {
            return new HashSet<>(getTree().getSelectedOWLObjects());
        }

		@Override
		public Stream<OWLClass> entities() {
			return getEntities().stream();
		}
    }

	public NCIToldOWLClassHierarchyViewComponent() {}	

	public void performExtraInitialisation() throws Exception {

		batchProcessPanel.setVisible(false);
		add(batchProcessPanel, BorderLayout.SOUTH);
		
		addAction(new AbstractOWLTreeAction<OWLClass>("Add subclass", ADD_SUB_ICON, getTree().getSelectionModel()) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent event) {
				createNewChild();
			}

			protected boolean canPerform(OWLClass cls) {
				return canCreateNewChild();
			}
		}, "A", "A");
		
		DeleteClassAction deleteClassAction =
                new DeleteClassAction(getOWLEditorKit(),
                                      new InternalOWLEntitySetProvider()) {
                    @Override
                    public void updateState() {
                        super.updateState();
                        if (isEnabled()) {
                        	NCIEditTab tab = NCIEditTab.currentTab();
                            setEnabled(isInAssertedMode() &&
                            		tab.isRetired(getSelectedEntity()) &&
                            		!(tab.isRetireRoot(getSelectedEntity()) ||
                            				tab.isRetireConceptsRoot(getSelectedEntity())) &&
                            		NCIEditTab.currentTab().isWorkFlowManager());
                        }
                    }
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                    	NCIEditTab tab = NCIEditTab.currentTab();
                    	if (tab.isEditing()) {
                    		JOptionPane.showMessageDialog(null,"Other Edits in Progress", "Class Delete", JOptionPane.INFORMATION_MESSAGE);
                    		return;
                    	}
                    	if (tab.wasCreatedInCurrentCycle(getSelectedEntity())) {


                    		NCIEditTab.currentTab().setPowerMode(true);
                    		tab.setClassDeleted(getSelectedEntity());
                    		super.actionPerformed(e);                        
                    		NCIEditTab.currentTab().commitPowerDelete();
                    		NCIEditTab.currentTab().setPowerMode(false);
                    	} else {
                    		JOptionPane.showMessageDialog(null,"Class was not created in current cycle, can't delete", "Class Delete", JOptionPane.INFORMATION_MESSAGE);
                    		return;

                    	}
                        
                        
                    }
                };

        addAction(deleteClassAction, "B", "B");
		
		addAction(new DisposableAction("Batch Load/Edit", batchbutton.getIcon()) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent event) {
				if (!NCIEditTab.currentTab().isEditing()) {
					batchProcessPanel.setVisible(true);
					BatchProcessingDialog dl = new BatchProcessingDialog(batchProcessPanel, NCIEditTab.currentTab());
				} else {
					warn("Can't perform batch load/edit while editing in progress.");
					
				}
			}

			@Override
			public void dispose() {
				// TODO Auto-generated method stub
				
			}
			
			
		}, "C", "C");
		
		addAction(new DisposableAction("Search", searchbutton.getIcon()) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent event) {
				
				Component focusOwner = FocusManager.getCurrentManager().getFocusOwner();
		        if(focusOwner == null) {
		            return;
		        }
		        
		        OWLClass cls = getOWLWorkspace().searchForClass(focusOwner);
		        if (cls != null) {
				setSelectedEntity(cls);
		        }			
				
			}

			public void dispose() {
				
			}
		}, "C", "C");
		
		getTree().setDragAndDropHandler(new OWLTreeDragAndDropHandler<OWLClass>() {
            public boolean canDrop(Object child, Object parent) {
                return false;
            }
            public void move(OWLClass child, OWLClass fromParent, OWLClass toParent) {}
            public void add(OWLClass child, OWLClass parent) {}
        });
        getAssertedTree().setPopupMenuId(new PopupMenuId("[NCIAssertedClassHierarchy]")); 
        
        
        
        
        getAssertedTree().addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (NCIEditTab.currentTab().isRetired(getTree().getSelectedOWLObject()) &&
						!NCIEditTab.currentTab().getCurrentOp().isRetiring()) {
					NCIEditTab.currentTab().fireChange(new EditTabChangeEvent(NCIEditTab.currentTab(), ComplexEditType.READ));
				} else {
					NCIEditTab.currentTab().selectClass(getTree().getSelectedOWLObject());
				}
						
			}        	
        });
        
        if (getInferredTree() != null) {
        	getInferredTree().addTreeSelectionListener(new TreeSelectionListener() {

    			@Override
    			public void valueChanged(TreeSelectionEvent e) {
    				if (getInferredTree().isVisible()) {
    					if (NCIEditTab.currentTab().isRetired(getTree().getSelectedOWLObject())) {
    						NCIEditTab.currentTab().fireChange(new EditTabChangeEvent(NCIEditTab.currentTab(), ComplexEditType.READ));
    					} else {
    						NCIEditTab.currentTab().selectClass(getTree().getSelectedOWLObject());
    					}
    				}
    				
    			}        	
            });
        	
        }
        
        
        
        NCIEditTab.setNavTree(this);               
    }
	
	private void warn(String msg) {
		JOptionPane.showMessageDialog(this, 
				msg, "Warning", JOptionPane.WARNING_MESSAGE);
	}
	
	@Override
	protected OWLClass updateView(OWLClass selectedClass) {
		if (NCIEditTab.currentTab().inComplexOp()) {
			
		} else {
			setSelectedEntity(selectedClass);
		}   
		
        return selectedClass;
	}
	
	@Override
	public void setSelectedEntity(OWLClass entity) {
		if (entity != null) {
			getTree().setSelectedOWLObject(entity);			
		}

	} 
	
	private boolean isFree() {
		return NCIEditTab.currentTab().isFree();
		
	}
	 
	@Override
	public boolean canRetireClass() {
		return (getSelectedEntities().size() == 1 &&
				NCIEditTab.currentTab().canRetire(getSelectedEntity()) &&
				isFree());
	}

	@Override
	public void retireClass() {
		OWLClass selectedClass = getSelectedEntity();
		NCIEditTab.currentTab().retire(selectedClass);
		NCIEditTab.currentTab().refreshNavTree();
		
	}
	
	public void refreshTree() {
		this.getTree().refreshComponent();	
	}


	@Override
	public boolean canCloneClass() {
		return (getSelectedEntities().size() == 1 &&
				NCIEditTab.currentTab().canClone(getSelectedEntity()) &&
				isFree());
	}

	@Override
	public void cloneClass() {
		splitOrCloneClass(true);		
	}

	@Override
	public boolean canSplitClass() {
		return (isInAssertedMode() &&
				getSelectedEntities().size() == 1 &&
				NCIEditTab.currentTab().isNotSpecialRoot(getSelectedEntity()) &&
				isFree());
	}

	@Override
	public void splitClass() {
		splitOrCloneClass(false);
		
	}
	
	private void splitOrCloneClass(boolean clone_p) {
		if (clone_p) {
			NCIEditTab.currentTab().setOp(ComplexEditType.CLONE);
		} else {
			NCIEditTab.currentTab().setOp(ComplexEditType.SPLIT);
		}
		OWLClass from_cls = getSelectedEntity();
		NCIClassCreationDialog<OWLClass> dlg = new NCIClassCreationDialog<OWLClass>(getOWLEditorKit(),
				"Please enter a class name", OWLClass.class, Optional.empty(), Optional.empty());
		if (dlg.showDialog()) {
			NCIEditTab.currentTab().splitClass(dlg.getNewClass(), from_cls, clone_p);
			
		} else {
			NCIEditTab.currentTab().setOp(null);			
		}
	}
	
	private boolean isRestricted(OWLClass cls) {
		
		if (cls.equals(NCIEditTabConstants.RETIRE_ROOT) ||
				cls.equals(NCIEditTabConstants.PRE_MERGE_ROOT) ||
				cls.equals(NCIEditTabConstants.PRE_RETIRE_ROOT) ||
				NCIEditTab.currentTab().isRetired(cls) ||
				cls.getIRI().getShortForm().equals("Thing") ||
				cls.getIRI().getShortForm().equals("Retired_Concepts")) {
			return true;
		} else {
			return false;
		}		
	}
	
	@Override
	public boolean canDelete() {
		if (((TabbedWorkspace) getWorkspace()).isReadOnly(this.getView().getPlugin())) {
			return false;

    	} else {
    		return super.canDelete();
    	}
	}

	@Override
	public boolean canCreateNewChild() {
		
		if (NCIEditTab.currentTab().isEditing()) {
			return false;
		}
		if (getSelectedEntities().size() == 1) {
			if (NCIEditTab.currentTab().isWorkFlowManager()) {
				return true;
			} else {
				// modeler can't create at top
				if (isRestricted(getSelectedEntity())) {
					return false;
				} else {
					return true;
				}
			}
			
		}
		return false;
	}

	@Override
	public void createNewChild() {	

		OWLClass selectedClass = getSelectedEntity();
		if (NCIEditTab.currentTab().isEditing()) {
			JOptionPane.showMessageDialog(this, 
					"Can't create new class while edit in progress.", "Warning", JOptionPane.WARNING_MESSAGE);
			return;
		}
		NCIEditTab.currentTab().enableBatchMode();
		OWLClass newCls = NCIEditTab.currentTab().createNewChild(selectedClass, Optional.empty(), Optional.empty(), false);
		NCIEditTab.currentTab().disableBatchMode();
		if (newCls != null) {
			getTree().setSelectedOWLObject(newCls);
			if (NCIEditTab.currentTab().hasActiveClient()) {
				NCIEditTab.currentTab().setCurrentlyEditing(newCls, true);
				NCIEditTab.currentTab().setNew(true);
				NCIEditTab.currentTab().classModified();
			}
		}

	}
	
	 protected OWLObjectHierarchyProvider<OWLClass> getHierarchyProvider() {
	        return getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider();
	    }

	    
	 @Override
	 protected Optional<OWLObjectHierarchyProvider<OWLClass>> getInferredHierarchyProvider() {
		 return Optional.of(getOWLModelManager().getOWLHierarchyManager().getInferredOWLClassHierarchyProvider());
	 }
	    

		@Override
		public boolean canAddComplex() {
			return (getSelectedEntities().size() == 1 &&
					NCIEditTab.currentTab().isNotSpecialRoot(getSelectedEntity()) &&
					!NCIEditTab.currentTab().isRetired(getSelectedEntity()));
					
		}

		@Override
		public void addComplex() {
			OWLClass selectedClass = getSelectedEntity();
			NCIEditTab.currentTab().addComplex(selectedClass);
		}

		
		@Override
		protected UserRendering getUserRenderer() {
			return new UserRendering() {

				@Override
				public String render(Object object, String in) {
					if (NCIEditTab.currentTab().getCurrentOp().isRetiring()) {
						OWLClass cls = NCIEditTab.currentTab().getRetireClass();
						if (cls != null) {
							if (cls.equals(object)) {
								return in + "(retiring...)";
							}
						}						
					};

					if (NCIEditTab.currentTab().isEditing()) {						
						OWLClass cls = NCIEditTab.currentTab().getCurrentlyEditing(); 
						if (cls != null) {
							if (cls.equals(object)) {
								return in + "(editing...)";
							}
						}
					}
					if (NCIEditTab.currentTab().getCurrentOp().isSplitting()) {
						OWLClass cls = NCIEditTab.currentTab().getCurrentOp().getSource(); 
						if (cls != null) {
							if (cls.equals(object)) {
								return in + "(splitting...)";								
							}
						}				
					}
					return in;
				}
			};
		}
		

		@Override
		public Component asComponent() {
			return this;
		}

		    @Override
		    public Optional<OWLObject> getSelection() {
		        return Optional.ofNullable(getSelectedEntity());
		    }

			

		

	
}
