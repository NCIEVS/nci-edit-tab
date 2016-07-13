package gov.nih.nci.ui.dialog;



import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import gov.nih.nci.ui.NCIEditTab;

import java.util.*;

/**
 *@Author: NGIT, Kim Ong; Iris Guo
 */

public class NoteDialog extends JDialog implements ActionListener
{
    private static final long serialVersionUID = 123456032L;
    JButton okButton, cancelButton;
	JTextField fEditorNote, fDesignNote;
	String editornote, designnote;
	NCIEditTab tab;
	String prefix;

	boolean btnPressed;

// prefix:
//    premerge: premerge_annotation
//    preretire: preretire_annotation

	public NoteDialog(NCIEditTab tab, String editornote, String designnote, String prefix){
		super((JFrame)tab.getTopLevelAncestor(), "Enter Notes", true);
		this.editornote = editornote;
		this.designnote = designnote;
		this.tab = tab;
		this.prefix = prefix;
		init();
	}

	public void init()
	{
		Container contain = this.getContentPane();
		setLocation(360,300);
		setSize(new Dimension(400,200));
		contain.setLayout(new GridLayout(3,1));

		JPanel editorPanel = new JPanel();
		JLabel editorLabel = new JLabel("Editor's Note: ");
		fEditorNote = new JTextField(30);
		fEditorNote.setText(editornote);
		editorPanel.add(editorLabel);
		editorPanel.add(fEditorNote);
		
		contain.add(editorPanel);

		JPanel designPanel = new JPanel();
		JLabel designLabel = new JLabel("Design Note: ");
		fDesignNote = new JTextField(30);
		fDesignNote.setText(designnote);
		designPanel.add(designLabel);
		designPanel.add(fDesignNote);
		
		contain.add(designPanel);

		JPanel buttonPanel = new JPanel();
		okButton = new JButton("OK");
		okButton.addActionListener(this);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		contain.add(buttonPanel);

		this.setVisible(true);
   	}

   	public String getEditorNote()
   	{
		return editornote;
	}

   	public String getDesignNote()
   	{
		return designnote;
	}

	public boolean OKBtnPressed()
	{
		return btnPressed;
	}

	public void actionPerformed(ActionEvent event)
	{
		Object action = event.getSource();
		if (action == okButton){
			editornote = fEditorNote.getText();
			designnote = fDesignNote.getText();
			if (editornote.trim().equals("") || designnote.trim().equals(""))
			{
				JOptionPane.showMessageDialog(this, "Editor's Note and Design Note are required.", "Warning", JOptionPane.WARNING_MESSAGE);
				return;
			}
			editornote = prefix + "|" + (new Date()).toString() + " - " + fEditorNote.getText().trim();
			designnote = prefix + "|" + (new Date()).toString() + " - " + fDesignNote.getText().trim();

			btnPressed = true;
			dispose();

		} else if (action == cancelButton){
			editornote = "";
			designnote = "";

			btnPressed = false;
			dispose();
		}
	}
}

//
//
//
//
//import java.awt.BorderLayout;
//import java.awt.Container;
//import java.awt.Frame;
//import java.awt.GridBagConstraints;
//import java.awt.GridBagLayout;
//import java.awt.Insets;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.util.Date;
//
//import javax.swing.JButton;
//import javax.swing.JDialog;
//import javax.swing.JLabel;
//import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//import javax.swing.JSeparator;
//import javax.swing.JTextField;
//
//import gov.nih.nci.ui.NCIEditTab;
//
///**
// *@Author: NGIT, Kim Ong; Iris Guo
// */
//
//public class RetireNoteDialog extends JDialog implements ActionListener
//{
//    private static final long serialVersionUID = 123456032L;
//    JButton okButton, cancelButton;
//	JTextField fEditorNote, fDesignNote;
//	String editornote, designnote;
//	String prefix;
//
//	boolean btnPressed;
//
//// prefix:
////    premerge: premerge_annotation
////    preretire: preretire_annotation
//
//	public RetireNoteDialog(NCIEditTab tab, String editornote, String designnote, String prefix){
//		super(((Frame) tab.getTopLevelAncestor(), "Enter Notes", true);
//		//super(getTopLevelAncestor(), "Enter Notes", true);
//		this.editornote = editornote;
//		this.designnote = designnote;
//		this.prefix = prefix;
//		init();
//	}
//
//	public void init()
//	{
//		Container contain = this.getContentPane();
//		contain.setLayout(new BorderLayout());
//        JPanel holder = new JPanel(new GridBagLayout());
//        contain.add(holder);
//        Insets insets = new Insets(0, 0, 2, 2);
//
//        int rowIndex = 0;
//        
//        fDesignNote = new JTextField("Degign Note");
//        fEditorNote = new JTextField("Editor Note");
//        
//        
//        holder.add(new JLabel("Design Note:"), new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
//        holder.add(fDesignNote, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
//        rowIndex++;
//        holder.add(new JSeparator(), new GridBagConstraints(0, rowIndex, 2, 1, 100.0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 2, 5, 2), 0, 0));
//        rowIndex++;
//        holder.add(new JLabel("Editor Note:"), new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
//        holder.add(fEditorNote, new GridBagConstraints(1, rowIndex, 1, 1, 100.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
//        rowIndex++;
//
//
//		JPanel buttonPanel = new JPanel();
//		okButton = new JButton("OK");
//		okButton.addActionListener(this);
//		cancelButton = new JButton("Cancel");
//		cancelButton.addActionListener(this);
//		buttonPanel.add(okButton);
//		buttonPanel.add(cancelButton);
//		
//		contain.add(buttonPanel);
//		
//		//contain.setVisible(true);
//		
//		this.setVisible(true);
//		
//   	}
//
//   	public String getEditorNote()
//   	{
//		return editornote;
//	}
//
//   	public String getDesignNote()
//   	{
//		return designnote;
//	}
//
//	public boolean OKBtnPressed()
//	{
//		return btnPressed;
//	}
//
//	public void actionPerformed(ActionEvent event)
//	{
//		Object action = event.getSource();
//		if (action == okButton){
//			editornote = fEditorNote.getText();
//			designnote = fDesignNote.getText();
//			if (editornote.trim().equals("") || designnote.trim().equals(""))
//			{
//				JOptionPane.showMessageDialog(this, "Editor's Note and Design Note are required.", "Warning", JOptionPane.WARNING_MESSAGE);
//                
//				return;
//			}
//			editornote = prefix + "|" + (new Date()).toString() + " - " + fEditorNote.getText().trim();
//			designnote = prefix + "|" + (new Date()).toString() + " - " + fDesignNote.getText().trim();
//
//			btnPressed = true;
//			dispose();
//
//		}else if (action == cancelButton){
//			editornote = "";
//			designnote = "";
//
//			btnPressed = false;
//			dispose();
//		}
//	}
//}
//
//
