package gov.nih.nci.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.protege.editor.core.ProtegeManager;
import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.ui.renderer.OWLRendererPreferences;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;

import gov.nih.nci.ui.dialog.PropertyEditingDialog;


public class PropertyTablePanel extends JPanel implements ActionListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private OWLEditorKit owlEditorKit;

    private JPopupMenu popupMenu;
    
    private PropertyTableModel tableModel;
    
    private JTable propertyTable;
    
    private OWLAnnotationProperty complexProp;
    
    public OWLAnnotationProperty getComplexProp() {
    	return complexProp;
    }
    
    private String tableName;
    
    private JScrollPane sp;
    
    private JLabel tableNameLabel;
    
    private JPanel tableHeaderPanel;
    
    private JButton addButton;
    
    private JButton editButton;
    
    private JButton deleteButton;

    private Preferences prefs;
    
    public PropertyTablePanel(OWLEditorKit editorKit) {
        this.owlEditorKit = editorKit;
        initialiseOWLView();
        createPopupMenu();
    }
    
    public PropertyTablePanel(OWLEditorKit editorKit, OWLAnnotationProperty complexProperty, String tableName) {
        this.owlEditorKit = editorKit;
        this.complexProp = complexProperty;
        this.tableName = tableName;
        		
        initialiseOWLView();
        createPopupMenu();
    }


    private void createPopupMenu() {
        popupMenu = new JPopupMenu();
        popupMenu.add(new AbstractAction("Show axioms") {
            public void actionPerformed(ActionEvent e) {
                

            }
        });
    }

    protected void initialiseOWLView() {
        tableModel = new PropertyTableModel(owlEditorKit, complexProp);
        createButtons(this);
        createUI();
        prefs = tableModel.getPrefs();
    }

    public static void updateRowHeights(int column, int width, JTable table){
        for (int row = 0; row < table.getRowCount(); row++) {
            int rowHeight = table.getRowHeight();
            Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
            Dimension d = comp.getPreferredSize();
            comp.setSize(new Dimension(width, d.height));
            d = comp.getPreferredSize();
            rowHeight = Math.max(rowHeight, d.height);
            table.setRowHeight(row, rowHeight);
        }
    }

    abstract class ColumnListener extends MouseAdapter implements TableColumnModelListener {

        private int oldIndex = -1;
        private int newIndex = -1;
        private boolean dragging = false;

        private boolean resizing = false;
        private int resizingColumn = -1;
        private int oldWidth = -1;

        @Override
        public void mousePressed(MouseEvent e) {
            // capture start of resize
            if(e.getSource() instanceof JTableHeader) {
                JTableHeader header = (JTableHeader)e.getSource();
                TableColumn tc = header.getResizingColumn();
                if(tc != null) {
                    resizing = true;
                    JTable table = header.getTable();
                    resizingColumn = table.convertColumnIndexToView( tc.getModelIndex());
                    oldWidth = tc.getPreferredWidth();
                } else {
                    resizingColumn = -1;
                    oldWidth = -1;
                }
            }   
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // column moved
            if(dragging && oldIndex != newIndex) {
                columnMoved(oldIndex, newIndex);
            }
            dragging = false;
            oldIndex = -1;
            newIndex = -1;

            // column resized
            if(resizing) {
                if(e.getSource() instanceof JTableHeader) {
                    JTableHeader header = (JTableHeader)e.getSource();
                    TableColumn tc = header.getColumnModel().getColumn(resizingColumn);
                    if(tc != null) {
                        int newWidth = tc.getPreferredWidth();
                        if(newWidth != oldWidth) {
                            columnResized(resizingColumn, newWidth);
                        }
                    }
                }   
            }
            resizing = false;
            resizingColumn = -1;
            oldWidth = -1;
        }

        @Override
        public void columnAdded(TableColumnModelEvent e) {      
        }

        @Override
        public void columnRemoved(TableColumnModelEvent e) { 
        	
        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {
            // capture dragging
            dragging = true;
            if(oldIndex == -1){
                oldIndex = e.getFromIndex();
            }

            newIndex = e.getToIndex(); 
                 
        }

        @Override
        public void columnMarginChanged(ChangeEvent e) {
        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {
        }

        public abstract void columnMoved(int oldLocation, int newLocation);
        public abstract void columnResized(int column, int newWidth);
    }

    private void saveColumnNameOrder() {
    	int colCount = propertyTable.getColumnCount();
        List<String> colNames1 = new ArrayList<String>();
        
        for (int i = 0; i < colCount; i++) {
        	String columnName1 = propertyTable.getColumnModel().getColumn(i).getHeaderValue().toString();
        	colNames1.add(columnName1);
        }
        
        String propName = NCIEditTab.currentTab().getRDFSLabel(tableModel.getComplexProp()).get();  
        prefs.putStringList(propName, colNames1);
    }
    
    private void createUI() {
    	setLayout(new BorderLayout());
    	
        propertyTable = new JTable(tableModel);
        
        propertyTable.setGridColor(Color.LIGHT_GRAY);
        propertyTable.setRowHeight(propertyTable.getRowHeight() + 4);
        propertyTable.setShowGrid(true);       
        propertyTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        propertyTable.getTableHeader().setReorderingAllowed(true);
        propertyTable.setFillsViewportHeight(true);   
        propertyTable.setAutoCreateRowSorter(true);
        tableModel.setPropertyTable(propertyTable);
        
        propertyTable.setTransferHandler(new TransferHandler() {
        	@Override
            public int getSourceActions(JComponent c) {
                return COPY; // Supports copying to clipboard
            }
        	
        	 @Override
        	    protected Transferable createTransferable(JComponent c) {
        	        // Return whatever data is currently "selected" in your component
        	        JTable tab = (JTable) c;
        	        String res = "";
        	        int[] rows = tab.getSelectedRows();
        	        for (int i : rows) {
        	        	Object obj = tab.getModel().getValueAt(i, 0);
        	        	res += (String) obj;
        	        	res += "\n";
        	        	
        	        }
        		 
        		 return new StringSelection(res);
        	    }
        		
        	
        	
        	
        });
        
        
        
        ColumnListener cl = new ColumnListener(){

            @Override
            public void columnMoved(int oldLocation, int newLocation) {
            	
            }

            @Override
            public void columnResized(int column, int newWidth) {
            	TableColumn c = propertyTable.getColumnModel().getColumn(column);
                c.setCellRenderer(new RowHeightCellRenderer());
                updateRowHeights(column, c.getWidth(), propertyTable);
            }

        };

        propertyTable.getColumnModel().addColumnModelListener(cl);
        propertyTable.getTableHeader().addMouseListener(cl);

        DefaultTableCellRenderer alignRenderer = new DefaultTableCellRenderer();
        alignRenderer.setVerticalAlignment(SwingConstants.TOP);
        alignRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        int ccount = propertyTable.getColumnModel().getColumnCount();
        for (int i=0; i<ccount; i++) {
        	propertyTable.getColumnModel().getColumn(i).setCellRenderer(alignRenderer);
        }
        
                
        sp = new JScrollPane(propertyTable);
        createLabelHeader(tableName, addButton, editButton, deleteButton);
        add(tableHeaderPanel, BorderLayout.NORTH);
        tableHeaderPanel.setVisible(false);        
        sp.setVisible(false);
        sp.setPreferredSize(new Dimension(180, 150));
        add(sp, BorderLayout.CENTER);
    }
    
    public boolean isViewable() {
    	return tableModel.hasAnnotation();
    }

    private TableColumn getValueColumn() {
    	TableColumnModel tcm = propertyTable.getColumnModel();
    	int colCount = tcm.getColumnCount();
    	for( int i = 0; i < colCount; i++ ) {
    		TableColumn tc = tcm.getColumn(i);
    		if (tc.getHeaderValue().equals(NCIEditTabConstants.PROPTABLE_VALUE_COLUMN)) {
    			return tc;
    		}
    	}
    	return null;
    }
    public void setSelectedCls(OWLClass cls) {
    	tableModel.setSelection(cls);
    	
    	if (tableModel.hasAnnotation()) {
    		tableHeaderPanel.setVisible(true);
    		sp.setVisible(true);
    		tableModel.fireTableDataChanged(); 
    		
    		Font font = OWLRendererPreferences.getInstance().getFont();
    		propertyTable.setFont(font);
    		propertyTable.getTableHeader().setFont(font);
    		tableNameLabel.setFont(font);
    		
    		TableColumn c = getValueColumn();
    		if(c != null) {
    			c.setCellRenderer(new RowHeightCellRenderer());
    			updateRowHeights(c.getModelIndex(), c.getWidth(), propertyTable);
    		}
    		 
    	} else {
    		tableHeaderPanel.setVisible(false);
    		sp.setVisible(false);
    	}
    	
    }
    
    class RowHeightCellRenderer extends JTextArea  implements TableCellRenderer {
    	
        public Component getTableCellRendererComponent (JTable table,
                                                        Object value,
                                                        boolean isSelected,
                                                        boolean hasFocus,
                                                        int row,
                                                        int column ) {
            
        	setLineWrap(true);
            setWrapStyleWord(true);
            
            if(isSelected) {
                this.setBackground(table.getSelectionBackground());
                this.setForeground(table.getSelectionForeground());
            } else {
                this.setBackground(table.getBackground());
                this.setForeground(table.getForeground());
            }
            
            setText((String) value);
            
            return this;
        }
    }

    private void createLabelHeader(String labeltext, JButton b1, JButton b2, JButton b3){
    	
    	tableHeaderPanel = new JPanel();
    	tableHeaderPanel.setLayout(new BorderLayout());
        
    	tableNameLabel = new JLabel(labeltext);
        tableNameLabel.setPreferredSize(new Dimension(100, 25));
        
        JPanel panel2 = new JPanel();
        
        panel2.setPreferredSize(new Dimension(80,25));
        panel2.add(b1);
        panel2.add(b2);
        panel2.add(b3);
        
        tableHeaderPanel.add(tableNameLabel, BorderLayout.WEST);
        tableHeaderPanel.add(panel2, BorderLayout.EAST);
        
    }
    
    private void createButtons(ActionListener actionListener) {
    	addButton = new IconButton(NCIEditTabConstants.ADD, "ButtonAddIcon.png", NCIEditTabConstants.ADD, actionListener);
    	editButton = new IconButton(NCIEditTabConstants.EDIT, "ButtonEditIcon.png", NCIEditTabConstants.EDIT, actionListener);
    	deleteButton = new IconButton(NCIEditTabConstants.DELETE, "ButtonDeleteIcon.png", NCIEditTabConstants.DELETE, actionListener);
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() instanceof IconButton){
			IconButton button = (IconButton)e.getSource();

			if(button.getType() == NCIEditTabConstants.ADD){
				PropertyEditingDialog addedit = new	PropertyEditingDialog(NCIEditTabConstants.ADD, tableModel.getSelectedPropertyType(), 
						tableModel.getDefaultPropertyValues(), tableModel.getSelectedPropertyOptions(), tableModel.getDefaultSelectedPropertyLabel());
				
				this.showAndValidate(addedit, NCIEditTabConstants.ADD, null);
								
			}		
			else if(button.getType() == NCIEditTabConstants.EDIT){

				int viewRow = propertyTable.getSelectedRow();
				if (viewRow != -1) {
					int modelRow = propertyTable.convertRowIndexToModel(viewRow);
					
					PropertyEditingDialog addedit = new	PropertyEditingDialog(NCIEditTabConstants.EDIT, tableModel.getSelectedPropertyType(), tableModel.getSelectedPropertyValues(modelRow), 
							tableModel.getSelectedPropertyOptions(), tableModel.getDefaultSelectedPropertyLabel());
					
					this.showAndValidate(addedit, NCIEditTabConstants.EDIT, tableModel.getAssertion(modelRow));					
				}

				//update view
			}
			else if(button.getType() == NCIEditTabConstants.DELETE){
				int viewRow = propertyTable.getSelectedRow();
				if (viewRow != -1) {
					int modelRow = propertyTable.convertRowIndexToModel(viewRow);
					if (modelRow >= 0) {
						int ret = JOptionPane.showConfirmDialog(this, "Please confirm if you want to delete the selected property!", "Delete Confirmation", JOptionPane.OK_CANCEL_OPTION);
						if (ret == JOptionPane.OK_OPTION) {
							NCIEditTab.currentTab().complexPropOp(NCIEditTabConstants.DELETE, tableModel.getSelection(), 
									tableModel.getComplexProp(), tableModel.getAssertion(modelRow), null);

							setSelectedCls(tableModel.getSelection());

						}
					}
				}
				//todo - delete seleted table row from table, update view

			}
		}
	}
	
	public void createNewProp() {
		
		PropertyEditingDialog addedit = new	PropertyEditingDialog(NCIEditTabConstants.ADD, tableModel.getSelectedPropertyType(), tableModel.getDefaultPropertyValues(), 
				tableModel.getSelectedPropertyOptions(), tableModel.getDefaultSelectedPropertyLabel());
		
		this.showAndValidate(addedit, NCIEditTabConstants.ADD, null);
		
		
		
	}
	
	private void showAndValidate(PropertyEditingDialog addedit, String type, OWLAnnotationAssertionAxiom axiom) {

		boolean done = false;
		while(!done) {
			HashMap<String, String> data = null;
			if (type.equalsIgnoreCase(NCIEditTabConstants.ADD)) {
				data = 	addedit.showDialog(owlEditorKit, "Adding Properties");
			} else {
				data = 	addedit.showDialog(owlEditorKit, "Editing Properties");
			}
			if (data != null) {
				if (NCIEditTab.currentTab().complexPropOp(type, tableModel.getSelection(),
						tableModel.getComplexProp(), axiom, data)) {
					this.setSelectedCls(tableModel.getSelection());
					done = true;
					//NCIEditTab.currentTab().classModified();
				}				

			} else {
				done = true;
			}
		}
	}
	
	public void dispose() {
		//saveColumnNameOrder();
	}
 
}
