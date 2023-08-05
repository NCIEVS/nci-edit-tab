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

	boolean btnPressed;

// prefix:
//    premerge: premerge_annotation
//    preretire: preretire_annotation

	public NoteDialog(NCIEditTab tab, String editornote, String designnote){
		super((JFrame)tab.getTopLevelAncestor(), "Enter Notes", true);
		//Fix issue #569 - Optional project configuration properties
		if (editornote != null) {
			this.editornote = editornote;
		} 
		if (designnote != null) {
			this.designnote = designnote;
		} 
		this.tab = tab;
		init();
	}

	public void init()
	{
		Container contain = this.getContentPane();
		setLocation(360,300);
		setSize(new Dimension(400,200));
		contain.setLayout(new GridLayout(3,1));

		//Fix issue #569 - Optional project configuration properties
		if (this.editornote != null) {
			JPanel editorPanel = new JPanel();
			JLabel editorLabel = new JLabel("Editor's Note: ");
			fEditorNote = new JTextField(30);
			fEditorNote.setText(editornote);
			editorPanel.add(editorLabel);
			editorPanel.add(fEditorNote);
			
			contain.add(editorPanel);
		}

		//Fix issue #569 - Optional project configuration properties
		if (this.designnote != null) {
			JPanel designPanel = new JPanel();
			JLabel designLabel = new JLabel("Design Note: ");
			fDesignNote = new JTextField(30);
			fDesignNote.setText(designnote);
			designPanel.add(designLabel);
			designPanel.add(fDesignNote);
			
			contain.add(designPanel);
		}

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
			//Fix issue #569 - Optional project configuration properties
			if (fEditorNote != null) {
				editornote = fEditorNote.getText();
			}
			if (fDesignNote != null) {
				designnote = fDesignNote.getText();
			}
			if ((editornote != null && editornote.trim().equals("")) || (designnote != null && designnote.trim().equals("")))
			{
				JOptionPane.showMessageDialog(this, "Editor's Note and Design Note are required.", "Warning", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (fEditorNote != null) {
				editornote = (new Date()).toString() + " - " + fEditorNote.getText().trim();
			}
			if (fDesignNote != null) {
				designnote = (new Date()).toString() + " - " + fDesignNote.getText().trim();
			}

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

