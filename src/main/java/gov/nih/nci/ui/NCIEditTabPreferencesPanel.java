package gov.nih.nci.ui;

import java.awt.BorderLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.protege.editor.core.ui.preferences.PreferencesLayoutPanel;
import org.protege.editor.owl.ui.preferences.OWLPreferencesPanel;

public class NCIEditTabPreferencesPanel extends OWLPreferencesPanel {

	private static final long serialVersionUID = 1L;

    private JLabel complexEditLabel = new JLabel("Complex Edit");

    JCheckBox cbSplit = new JCheckBox("Split");
    JCheckBox cbCopy = new JCheckBox("Copy");
    JCheckBox cbMerge = new JCheckBox("Merge");
    JCheckBox cbDualEdits = new JCheckBox("Dual Edits");
    JCheckBox cbRetire = new JCheckBox("Retire");

    @Override
    public void initialise() throws Exception {
        setLayout(new BorderLayout());

        PreferencesLayoutPanel panel = new PreferencesLayoutPanel();
        add(panel, BorderLayout.NORTH);

        panel.addGroup("ComplexEdit");
        panel.addGroupComponent(complexEditLabel);

        cbSplit.setSelected(NCIEditTabPreferences.getFnSplit());
        cbCopy.setSelected(NCIEditTabPreferences.getFnCopy());
        cbMerge.setSelected(NCIEditTabPreferences.getFnMerge());
        cbDualEdits.setSelected(NCIEditTabPreferences.getFnDualEdits());
        cbRetire.setSelected(NCIEditTabPreferences.getFnRetire());

        panel.addGroupComponent(cbSplit);
        panel.addGroupComponent(cbCopy);
        panel.addGroupComponent(cbMerge);
        panel.addGroupComponent(cbDualEdits);
        panel.addGroupComponent(cbRetire);

        panel.addVerticalPadding();

        cbSplit.addActionListener(e -> {
            NCIEditTabPreferences.setFnSplit(cbSplit.isSelected());
        });
        cbCopy.addActionListener(e -> {
        	NCIEditTabPreferences.setFnCopy(cbCopy.isSelected());
        });
        cbMerge.addActionListener(e -> {
        	NCIEditTabPreferences.setFnMerge(cbMerge.isSelected());
        });
        cbDualEdits.addActionListener(e -> {
            NCIEditTabPreferences.setFnDualEdits(cbDualEdits.isSelected());
        });
        cbRetire.addActionListener(e -> {
            NCIEditTabPreferences.setFnRetire(cbRetire.isSelected());
        });
        
    }

    @Override
    public void dispose() throws Exception {
        // NO-OP
    }

    @Override
    public void applyChanges() {
    	NCIEditTabPreferences.setFnSplit(cbSplit.isSelected());
    	NCIEditTabPreferences.setFnCopy(cbCopy.isSelected());
    	NCIEditTabPreferences.setFnMerge(cbMerge.isSelected());
    	NCIEditTabPreferences.setFnDualEdits(cbDualEdits.isSelected());
    	NCIEditTabPreferences.setFnRetire(cbRetire.isSelected());
    }
}
