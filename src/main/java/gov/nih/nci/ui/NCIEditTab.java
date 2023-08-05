package gov.nih.nci.ui;
import static gov.nih.nci.ui.NCIEditTabConstants.*;

import static gov.nih.nci.ui.event.ComplexEditType.*;
import static org.semanticweb.owlapi.search.Searcher.annotationObjects;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import edu.stanford.protege.metaproject.api.*;

import org.protege.editor.core.ModelManager.ConnectionMode;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.LocalHttpClient;
import org.protege.editor.owl.client.LocalHttpClient.UserType;
import org.protege.editor.owl.client.SessionRecorder;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.api.exception.LoginTimeoutException;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent;
import org.protege.editor.owl.client.event.ClientSessionChangeEvent.EventCategory;
import org.protege.editor.owl.client.event.ClientSessionListener;
import org.protege.editor.owl.client.event.CommitOperationEvent;
import org.protege.editor.owl.client.event.CommitOperationListener;
import org.protege.editor.owl.client.ui.UserLoginPanel;
import org.protege.editor.owl.client.util.ClientUtils;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.find.OWLEntityFinder;
import org.protege.editor.owl.model.history.HistoryManager;
import org.protege.editor.owl.model.history.UndoManagerListener;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.http.messages.History;
import org.protege.editor.owl.server.policy.CommitBundleImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.ui.OWLWorkspaceViewsTab;
import org.protege.editor.owl.ui.prefix.PrefixUtilities;
import org.protege.editor.owl.ui.renderer.OWLEntityAnnotationValueRenderer;
import org.protege.editor.owl.ui.renderer.OWLModelManagerEntityRenderer;
import org.protege.editor.owl.ui.renderer.OWLRendererPreferences;
import org.protege.editor.search.lucene.SearchContext;
import org.protege.owlapi.inference.cls.ChildClassExtractor;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnnotationValueVisitor;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.CollectionFactory;
import org.semanticweb.owlapi.util.OWLObjectDuplicator;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import edu.stanford.protege.metaproject.impl.RoleIdImpl;
import edu.stanford.protege.search.lucene.tab.engine.BasicQuery;
import edu.stanford.protege.search.lucene.tab.engine.FilteredQuery;
import edu.stanford.protege.search.lucene.tab.engine.IndexDirMapper;
import edu.stanford.protege.search.lucene.tab.engine.QueryType;
import edu.stanford.protege.search.lucene.tab.engine.SearchTabManager;
import edu.stanford.protege.search.lucene.tab.engine.SearchTabResultHandler;
import gov.nih.nci.curator.CuratorReasonerPreferences;
import gov.nih.nci.ui.action.ComplexOperation;
import gov.nih.nci.ui.dialog.NCIClassCreationDialog;
import gov.nih.nci.ui.dialog.NoteDialog;
import gov.nih.nci.ui.event.ComplexEditType;
import gov.nih.nci.ui.event.EditTabChangeEvent;
import gov.nih.nci.ui.event.EditTabChangeListener;
import gov.nih.nci.utils.CharMapper;
import gov.nih.nci.utils.CuratorChecks;
import gov.nih.nci.utils.NCIClassSearcher;
import gov.nih.nci.utils.ParentRemover;
import gov.nih.nci.utils.PropertyCheckUtil;
import gov.nih.nci.utils.ReferenceReplace;
import gov.nih.nci.utils.RoleReplacer;

public class NCIEditTab extends OWLWorkspaceViewsTab implements ClientSessionListener, UndoManagerListener, CommitOperationListener {
	private static final Logger log = LoggerFactory.getLogger(NCIEditTab.class);
	private static final long serialVersionUID = -4896884982262745722L;
	
	private static NCIEditTab tab;	
	
	public static NCIEditTab currentTab() {
		return tab;
	}
	
	private static NCIToldOWLClassHierarchyViewComponent navTree = null;
	
	public static void setNavTree(NCIToldOWLClassHierarchyViewComponent t) { navTree = t;}
	
	public void refreshNavTree() {
		//navTree.refreshTree();
		if (navTree != null) {
		navTree.setSelectedEntity(current_op.getCurrentlyEditing());
		}
		
	}
	
	private boolean inPowerMode = false;
	public void setPowerMode(Boolean b) {inPowerMode = b;}
	
	private String classDeletedName = "";
	private String classDeletedCode = "";
	private String classDeletedLabel = "";
	
	private boolean initEagerViews = false;

	public void setClassDeleted(OWLClass c) {
		classDeletedName = c.getIRI().getShortForm();
		classDeletedCode = getCodeOrIRI(c);
		classDeletedLabel = getRDFSLabel(c).get();
	}
	
	public boolean wasCreatedInCurrentCycle(OWLClass c) {
		String code = this.getCodeOrIRI(c);
		try {
			return ((LocalHttpClient) clientSession.getActiveClient()).checkEvsHistoryCreate(code,
					this.clientSession.getActiveProject());
		} catch (LoginTimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthorizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public void commitPowerDelete() {
		
		List<OWLOntologyChange> changes = history.getUncommittedChanges();
		
		if (!changes.isEmpty()) {

			String comment = "(" +  classDeletedName + ") - DELETED";

			Commit commit = ClientUtils.createCommit(clientSession.getActiveClient(), comment, changes);
			DocumentRevision base = clientSession.getActiveVersionOntology().getHeadRevision();
			CommitBundle commitBundle = new CommitBundleImpl(base, commit);
			ChangeHistory hist;
			try {
				hist = clientSession.getActiveClient().commit(clientSession.getActiveProject(), commitBundle);

				clientSession.getActiveVersionOntology().update(hist);
				// submit history after the commit but before you broadcast the news
				if (!inBatchMode) 
					submitDeleteHistory();
				clientSession.fireCommitPerformedEvent(new CommitOperationEvent(
						hist.getHeadRevision(),
						hist.getMetadataForRevision(hist.getHeadRevision()),
						hist.getChangesForRevision(hist.getHeadRevision())));
				JOptionPane.showMessageDialog(this, "Class successfully deleted", "Class Delete", JOptionPane.INFORMATION_MESSAGE);

			} catch (AuthorizationException | ClientRequestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
	
	private Set<OWLAnnotationProperty> annProps = null;
	
	private boolean editInProgress = false;
	private OWLClass currentlySelected = null;
	private boolean isNew = false;
	
	
	private boolean inBatchMode = false;
	private ArrayList<OWLOntologyChange> batch_changes = new ArrayList<OWLOntologyChange>();
	
	private ArrayList<List<String>> batch_history = new ArrayList<List<String>>();
	
	private void addBatchHistory(OWLClass cls, String n, ComplexEditType typ) {
		
		String c = cls.getIRI().getShortForm().toString();
		List<String> rec = new ArrayList<String>();
		rec.add(c);
		rec.add(n);
		rec.add(typ.toString());
		rec.add("");
		
		batch_history.add(rec);
	}
	
	public void applyChanges() {
		if (!batch_changes.isEmpty()) {
			this.getOWLEditorKit().getOWLModelManager().applyChanges(batch_changes);
			this.batch_changes.clear();
			for (List<String> rec : batch_history) {
				this.putHistory(rec.get(0), rec.get(1), rec.get(2), rec.get(3));
			}
		}
	}
	
	public void enableBatchMode() { 
		inBatchMode = true;
		history.stopTalking();
	}
	
	public void disableBatchMode() { 
		inBatchMode = false;
		history.startTalking();
	}
	
	public void enableHistoryRecording() {
		history.startRecording();		
	}
	
	public void disableHistoryRecording() {
		history.stopRecording();		
	}
	
	public boolean isEditing() {
		return editInProgress;
	}
	
	public void setEditInProgress(boolean b) {
		editInProgress = b;
	}
	
	public void setCurrentlyEditing(OWLClass cls, boolean refresh) { 
		current_op.setCurrentlyEditing(cls);
		if (refresh)
			refreshNavTree();
	}
	
	public OWLClass getCurrentlyEditing() { return current_op.getCurrentlyEditing(); }
	
	public OWLClass getCurrentlySelected() { return currentlySelected; }
	
	public void setNew(boolean b) {
		isNew = b;
	}
	
	public boolean isNew() {
		return isNew;
	}
	
	private ComplexOperation current_op = new ComplexOperation();
	
	public void setOp(ComplexEditType op) {
		current_op.setType(op);
	}
	
	public boolean isFree() {
		return (!inComplexOp() && !isEditing());
	}
	
	public void cancelRetire() {
		current_op = new ComplexOperation();
		editInProgress = false;
		refreshNavTree();
	}
	
	public void cancelOp() {
		ComplexEditType t = current_op.getType();
		if (t == SPLIT || t == CLONE || t == DUAL) {
			cancelSplit();
		}
		if (t == ComplexEditType.MERGE) {
			cancelMerge();			
		}
		current_op = new ComplexOperation();		
	}
	
	public void cancelSplit() {	
		setEditInProgress(false);
		// go back to previous selected class
		OWLClass s = current_op.getSource();
		setCurrentlyEditing(s, true);
		undoChanges();
		navTree.setSelectedEntity(s);
		current_op.cancelSplit();
		
	}
	
	public void cancelMerge() {		
		
		setEditInProgress(false);
		// go back to previous selected class
		if (current_op.getSource() != null) {			
			navTree.setSelectedEntity(current_op.getSource());			
		}
		
		if (current_op.getTarget() != null) {			
			navTree.setSelectedEntity(current_op.getTarget());			
		}
		undoChanges();
		current_op.cancelMerge();
			
	}
	
	public void completeOp() {
		ComplexEditType t = current_op.getType();
		if (t == SPLIT || t == CLONE) {
			completeSplit();
		}
		if (t == ComplexEditType.MERGE) {
			completeMerge();
			
		}
		current_op = new ComplexOperation();
		
	}
	
	public void completeSplit() {
		setEditInProgress(false);
		navTree.setSelectedEntity(current_op.getSource());
		current_op.completeSplit();
			
	}
	
	public void completeMerge() {
		setEditInProgress(false);
		navTree.setSelectedEntity(current_op.getTarget());
		current_op.completeMerge();
			
	}
	
	public OWLClass getRetireClass() {
		return current_op.getRetireClass();		
	}
	
	
	public void setSource(OWLClass cls) {
		//current_op.setType(ComplexEditType.MERGE);
		current_op.setSource(cls);
	}
	
	public void setTarget(OWLClass cls) {
		//current_op.setType(ComplexEditType.MERGE);
		current_op.setTarget(cls);
	}
	
	public boolean inComplexOp() {
		return current_op.inComplexOp();
	}
		
	// use undo/redo facility
	private SessionRecorder history;
	
	private ClientSession clientSession = null;
	
	public String getUserId() {
		return clientSession.getActiveClient().getUserInfo().getId();
	}
	
	public boolean isGuest() {
		return ((LocalHttpClient) clientSession.getActiveClient()).isGuest(clientSession.getActiveProject());
	}
	
	private OWLOntology ontology;
	
	public boolean isCurrentOntology(OWLOntology ont) {
		return ont.getOntologyID() == ontology.getOntologyID();
	}
	
	public boolean isImported(OWLClass c) {
		
		String classNS = c.getIRI().getNamespace();
		if (classNS.endsWith("#") ||
				classNS.endsWith("/")) {
			classNS = classNS.substring(0, classNS.length() - 1);
		}
			
		return !(classNS.equalsIgnoreCase(
				ontology.getOntologyID().getOntologyIRI().get().getIRIString()));
	}
	
	private static ArrayList<EditTabChangeListener> event_listeners = new ArrayList<EditTabChangeListener>();
		
	public static void addListener(EditTabChangeListener l) {
		event_listeners.add(l);
	}
	
	public static void removeListener(EditTabChangeListener l) {
		event_listeners.remove(l);
	}
	
	public void fireChange(EditTabChangeEvent ev) {		
		for (EditTabChangeListener l : event_listeners) {
			l.handleChange(ev);
		}		
	}	
	
	private List<OWLAnnotationProperty> complex_properties = new ArrayList<OWLAnnotationProperty>();
	
	private Set<OWLAnnotationProperty> not_equal_props = new HashSet<OWLAnnotationProperty>();
	
	private Map<OWLAnnotationProperty, Set<OWLAnnotationProperty>> configured_annotation_dependencies =
			new HashMap<OWLAnnotationProperty, Set<OWLAnnotationProperty>>();
	
	private Map<OWLAnnotationProperty, Set<OWLAnnotationProperty>> required_annotation_dependencies =
			new HashMap<OWLAnnotationProperty, Set<OWLAnnotationProperty>>();

	private Map<OWLAnnotationProperty, Set<OWLAnnotationProperty>> optional_annotation_dependencies =
			new HashMap<OWLAnnotationProperty, Set<OWLAnnotationProperty>>();
	
	private Set<OWLAnnotationProperty> immutable_properties = new HashSet<OWLAnnotationProperty>();
	
	private Set<OWLAnnotationProperty> associations = new HashSet<OWLAnnotationProperty>();
	
	private Map<OWLAnnotationProperty, OWLAnnotationProperty> restrictedBy = 
			new HashMap<OWLAnnotationProperty, OWLAnnotationProperty>();
	
	public boolean isAssociation(OWLAnnotationProperty p) {
		return associations.contains(p);
	}				

	public NCIEditTab() {
		setToolTipText("Custom Editor for NCI");
		tab = this;
	}
	
	public boolean hasActiveClient() {
		LocalHttpClient cl = (LocalHttpClient) clientSession.getActiveClient();
		if (cl != null) {
			return true;
		}
		return false;
	}
	
	public List<String> generateCodes(int no) {
		LocalHttpClient cl = (LocalHttpClient) clientSession.getActiveClient();
		List<String> codes = new ArrayList<String>(); 
		
		try {
			codes = cl.getCodes(no, clientSession.getActiveProject());
		} catch (Exception e) {
			// will detect empty array as issue with server
		}
		return codes;
	}
	
	public List<OWLAnnotationProperty> getComplexProperties() {
		return complex_properties;
	}
	
	public Set<OWLAnnotationProperty> getImmutableProperties() {
		return immutable_properties;
	}
	
	public boolean isReadOnlyProperty(String propName) {
		
		Set<OWLAnnotationProperty> props = getImmutableProperties();
		for (OWLAnnotationProperty prop : props) {
			if (prop.getIRI().getShortForm().equalsIgnoreCase(propName)) {
				return true;
			}
		}
		return false;
		
	}
	
	public Set<OWLAnnotationProperty> getRequiredAnnotationsForAnnotation(OWLAnnotationProperty annp) {
		return required_annotation_dependencies.get(annp);		
	}
	
	public Set<OWLAnnotationProperty> getConfiguredAnnotationsForAnnotation(OWLAnnotationProperty annp) {
		return configured_annotation_dependencies.get(annp);		
	}
	
	public Set<OWLAnnotationProperty> getOptionalAnnotationsForAnnotation(OWLAnnotationProperty annp) {
		return optional_annotation_dependencies.get(annp);		
	}
	
	private CharMapper mapper = new CharMapper();
	
    @Override
	public void initialise() {    	
		super.initialise();
		log.info("NCI Edit Tab initialized");
				
		clientSession = ClientSession.getInstance(getOWLEditorKit());
		
		history = SessionRecorder.getInstance(getOWLEditorKit());
		
		addListeners();
		
		getOWLEditorKit().getOWLWorkspace().setClassSearcher(new NCIClassSearcher(getOWLEditorKit()));
		
		((SearchTabManager) getOWLEditorKit().getSearchManager()).getSearchContext().setIndexDirMapper(
				new IndexDirMapper() {

					@Override
					public String getIndexDirId(OWLOntology ont) {
						return clientSession.getActiveProject().get();
						
					}
					
				});
		
		RuleServiceLoader.init();
	}
    
   
    
    /** Anyone can pre-merge so there is no need for a separate operation. A pre-merged class exists
     * as a subclass of the pre-merged root, so we can readily distinguish a pre-merge from a merge
     * and there is no need for a separate operation
     * 
     */
    public boolean readyMerge() {
    	return current_op.readyToMerge();
    }
    
    public boolean merge() {
    	List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    	OWLDataFactory df = getOWLModelManager().getOWLDataFactory();
    	
    	OWLClass source = current_op.getSource();
    	OWLClass target = current_op.getTarget();
    	if (hasActiveClient()) {
	    	if (isPreMerged(source)) {
	    		
	    		Set<OWLAnnotationAssertionAxiom> props = ontology.getAnnotationAssertionAxioms(source.getIRI());    		
	    		for (OWLAnnotationAssertionAxiom p : props) {
	    			if (p.getProperty().equals(MERGE_TARGET)) {
	    				changes.add(new RemoveAxiom(ontology, p));
	    			}
	    			
	    		}
	    		props = ontology.getAnnotationAssertionAxioms(target.getIRI());    		
	    		for (OWLAnnotationAssertionAxiom p : props) {
	    			//Fix issue #569 - Optional project configuration properties
	    			if (MERGE_SOURCE != null && p.getProperty().equals(MERGE_SOURCE)) {
	    				changes.add(new RemoveAxiom(ontology, p));
	    			}
	    			
	    		}
	    		//Fix issue #569 - Optional project configuration properties
	    		/*if (RETIRE_ROOT != null) {
	    			changes.add(new AddAxiom(getOWLModelManager().getActiveOntology(),
						df.getOWLSubClassOfAxiom(source, RETIRE_ROOT))); 
	    		}*/
	    		addAxiomForOWLSubClass(changes, getOWLModelManager().getActiveOntology(), source, RETIRE_ROOT);
				// finalize merge
				// remove parents and roles, OLD_ROLE OLD_PARENT
				// retarget inbound roles and children
				// reuse retire logic for this
				changes.addAll(finalizeMerge());
				
				changes.add(new AddAxiom(ontology, df.getDeprecatedOWLAnnotationAssertionAxiom(source.getIRI())));
	    		
	    	} else {
	    		if (switchMergeSourceTarget()) {
	    			int result = JOptionPane.showOptionDialog(null, "Retiring Concept was created before the Surviving Concept. Do you want to switch them?", 
							"Switch Retiring and Surviving Concept", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
					if (result == JOptionPane.OK_OPTION) {
						OWLClass temp = target;
		    			setTarget(source);
		    			setSource(temp);
		    			this.fireChange(new EditTabChangeEvent(this, ComplexEditType.MERGE));
						return false;
					} 
					
	    		} 
	    		// TODO:
	    		String editornote = "Merge into " + getRDFSLabel(target).get() + "(" + target.getIRI().getShortForm() + ")";
	    		editornote += ", " + clientSession.getActiveClient().getUserInfo().getName();
	    		
	    		String designnote = "See '" + getRDFSLabel(target).get() + "(" + target.getIRI().getShortForm() + ")" + "'";
	    		
	    		List<OWLOntologyChange> dcs = addNotes(editornote, designnote, source);
	    		//Fix issue #569 - Optional project configuration properties
	    		if (dcs.isEmpty() && EDITOR_NOTE != null && DESIGN_NOTE != null) {
	    			fireChange(new EditTabChangeEvent(this, ComplexEditType.MERGE));
					return false;
	    		}
	    		
	    		changes.addAll(dcs);
	    		
	    		changes.addAll(mergeAttrs());
	
	    		// if workflow modeler, add MERGE_TARGET/SOURCE props and tree under pre-merged    		
	    		if (!isWorkFlowManager()) {
	    			//Fix issue #569 - Optional project configuration properties
		    		/*OWLAxiom ax = df.getOWLAnnotationAssertionAxiom(MERGE_TARGET, 
	    					source.getIRI(), 
	    					df.getOWLLiteral(target.getIRI().getShortForm(), OWL2Datatype.RDF_PLAIN_LITERAL));
	    			changes.add(new AddAxiom(getOWLModelManager().getActiveOntology(), ax));*/
	    			addAxiomForOWLAnnotationAssertion(changes, getOWLModelManager().getActiveOntology(), MERGE_TARGET, 
	    					source.getIRI(), df.getOWLLiteral(target.getIRI().getShortForm(), OWL2Datatype.RDF_PLAIN_LITERAL));
	
	    			/*ax = df.getOWLAnnotationAssertionAxiom(MERGE_SOURCE, 
	    					target.getIRI(), 
	    					df.getOWLLiteral(source.getIRI().getShortForm(), OWL2Datatype.RDF_PLAIN_LITERAL));
	    			changes.add(new AddAxiom(getOWLModelManager().getActiveOntology(), ax));*/
	    			addAxiomForOWLAnnotationAssertion(changes, getOWLModelManager().getActiveOntology(), MERGE_SOURCE,
	    					target.getIRI(), df.getOWLLiteral(source.getIRI().getShortForm(), OWL2Datatype.RDF_PLAIN_LITERAL));
	    			
	    			//Fix issue #569 - Optional project configuration properties
	    			/*if (PRE_MERGE_ROOT != null) {
		    			changes.add(new AddAxiom(getOWLModelManager().getActiveOntology(),
		    					df.getOWLSubClassOfAxiom(source, PRE_MERGE_ROOT))); 
	    			}*/
	    			addAxiomForOWLSubClass(changes, getOWLModelManager().getActiveOntology(), source, PRE_MERGE_ROOT);
	    		} else {
	    			//Fix issue #569 - Optional project configuration properties
	    			/*if (RETIRE_ROOT != null) {
	    				changes.add(new AddAxiom(getOWLModelManager().getActiveOntology(),
	    					df.getOWLSubClassOfAxiom(source, RETIRE_ROOT)));
	    			}*/
	    			addAxiomForOWLSubClass(changes, getOWLModelManager().getActiveOntology(), source, RETIRE_ROOT);
	    			changes.add(new AddAxiom(ontology, df.getDeprecatedOWLAnnotationAssertionAxiom(source.getIRI())));
	    			// finalize merge
	    			// remove parents and roles, OLD_ROLE OLD_PARENT
	    			// retarget inbound roles and children
	    			// reuse retire logic for this
	    			changes.addAll(finalizeMerge());
	    		}
	    	} 
    	} else {
    		// Not connect to a server
    		changes.addAll(mergeAttrs());
    		
			changes.add(new AddAxiom(ontology, df.getDeprecatedOWLAnnotationAssertionAxiom(source.getIRI())));
			// finalize merge
			// remove parents and roles, OLD_ROLE OLD_PARENT
			// retarget inbound roles and children
			// reuse retire logic for this
			// changes.addAll(finalizeMerge());
    	}
    	getOWLModelManager().applyChanges(changes);
    	changes.clear();
    	
    	// check if conversion needed
    	if (!ontology.getEquivalentClassesAxioms(target).isEmpty()) {
    		//we have a defined class, first make primitive
    		
    		for (OWLEquivalentClassesAxiom ax : ontology.getEquivalentClassesAxioms(target)) {
                changes.add(new RemoveAxiom(ontology, ax));
                for (OWLClassExpression desc : ax.getClassExpressions()) {
                    if (!desc.equals(target)) {
                        if (desc instanceof OWLObjectIntersectionOf) {
                            for (OWLClassExpression op : ((OWLObjectIntersectionOf) desc).getOperands()) {
                                changes.add(new AddAxiom(ontology, df.getOWLSubClassOfAxiom(target, op)));
                            }
                        }
                        else {
                            changes.add(new AddAxiom(ontology, df.getOWLSubClassOfAxiom(target, desc)));
                        }
                    }
                }
            }
    		
    		getOWLModelManager().applyChanges(changes);
    		changes.clear();
    		// now convert back to completed
    		Set<OWLClassExpression> operands = new HashSet<>();

    		for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSubClass(target)) {
    			changes.add(new RemoveAxiom(ontology, ax));
    			operands.add(ax.getSuperClass());
    		}

            if (operands.isEmpty()) {
                return true;
            }
            
            OWLClassExpression equCls;
            if (operands.size() == 1) {
                equCls = operands.iterator().next();
            }
            else {
                equCls = df.getOWLObjectIntersectionOf(operands);
            }
            OWLAxiom ax = df.getOWLEquivalentClassesAxiom(CollectionFactory.createSet(target, equCls));
            changes.add(new AddAxiom(getOWLModelManager().getActiveOntology(), ax));
            getOWLModelManager().applyChanges(changes);
    		
    		
    	}
    	
    	return true;
    	
    }
    
    private boolean switchMergeSourceTarget() {
    	Optional<String> mergeSourceCode = getCode(current_op.getSource());
    	Optional<String> mergeTargetCode = getCode(current_op.getTarget());
    	if (mergeSourceCode.isPresent() &&
    			mergeTargetCode.isPresent()) {
    		return ((LocalHttpClient) clientSession.getActiveClient()).codeIsLessThan(mergeSourceCode.get(), mergeTargetCode.get(),
    				clientSession.getActiveProject());
    	} else {
    		return false;
    	}
    }
    
    List<OWLOntologyChange> finalizeMerge() {
    	
    	OWLClass source = current_op.getSource();
    	OWLClass target = current_op.getTarget();
    	
    	List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    	
    	OWLDataFactory df = getOWLModelManager().getOWLDataFactory();
    	
    	changes.addAll((new ReferenceReplace(getOWLModelManager())).retargetRefs(source, target)); 
    	
    	
    	// from retire panel, refactor
    	Set<OWLSubClassOfAxiom> sub_axioms = ontology.getSubClassAxiomsForSubClass(source);
        
        for (OWLSubClassOfAxiom ax1 : sub_axioms) {
        	OWLClassExpression exp = ax1.getSuperClass();
        	changes = addParentRoleAssertions(changes, exp, source);
        	changes.add(new RemoveAxiom(ontology, ax1));        	
        }
        
        Set<OWLEquivalentClassesAxiom> equiv_axioms = ontology.getEquivalentClassesAxioms(source);
        
        for (OWLEquivalentClassesAxiom ax1 : equiv_axioms) {
        	Set<OWLClassExpression> exps = ax1.getClassExpressions();
        	for (OWLClassExpression exp : exps) {
        		changes = addParentRoleAssertions(changes, exp, source);
        	}
        	changes.add(new RemoveAxiom(ontology, ax1));        	
        }  
        
        Set<OWLAnnotationAssertionAxiom> assocs = ontology.getAnnotationAssertionAxioms((OWLAnnotationSubject) source.getIRI());
        
        for (OWLAnnotationAssertionAxiom ax1 : assocs) {
        	
        	if (isAssociation(ax1.getProperty())) {
        		String val = ax1.getProperty().getIRI().getShortForm() + "|"
        				+ ax1.getValue().asIRI().get().getShortForm();
        		OWLLiteral lit =  df.getOWLLiteral(val, OWL2Datatype.RDF_PLAIN_LITERAL);
        		OWLAxiom ax = df.getOWLAnnotationAssertionAxiom(DEP_ASSOC, source.getIRI(), lit);
        		changes.add(new AddAxiom(ontology, ax));
        		changes.add(new RemoveAxiom(ontology, ax1));
        	}
        }
        if (DEPR_CONCEPT_STATUS_PROP != null) {
    		changes.add(new AddAxiom(ontology,
    				df.getOWLAnnotationAssertionAxiom(DEPR_CONCEPT_STATUS_PROP, source.getIRI(),
    						df.getOWLLiteral(DEPR_CONCEPT_STATUS_VALUE, OWL2Datatype.RDF_PLAIN_LITERAL))));
    		
    	}
    	return changes;    	
    }
    
    public List<OWLOntologyChange> mergeAttrs() {
    	
    	OWLClass source = current_op.getSource();
    	OWLClass target = current_op.getTarget();

    	List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();

    	Map<OWLEntity, IRI> replacementIRIMap = new HashMap<>();
    	replacementIRIMap.put(source, target.getIRI());

    	OWLObjectDuplicator dup = new OWLObjectDuplicator(replacementIRIMap, getOWLModelManager().getOWLOntologyManager());            

    	changes.addAll(duplicateClassAxioms(source, dup));            
    	changes.addAll(duplicateAnnotations(source, dup));
    	
    	return changes;
    }
    
    public List<OWLOntologyChange> addNotes(String editornote, String designnote, OWLClass cls) {
    	OWLDataFactory df = getOWLModelManager().getOWLDataFactory();
    	List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    	
    	//Fix issue #569 - Optional project configuration properties
    	if (EDITOR_NOTE == null && DESIGN_NOTE == null) {
    		return changes;
    	} 
    	
    	NoteDialog dlg = new NoteDialog(currentTab(), editornote, designnote);
    	OWLLiteral val = null;
    	if (dlg.OKBtnPressed()) {
    		if (EDITOR_NOTE != null) {
    		editornote = dlg.getEditorNote();
	    		val = df.getOWLLiteral(editornote, OWL2Datatype.RDF_PLAIN_LITERAL);
	    		//Fix issue #569 - Optional project configuration properties
	    		/*OWLAxiom ax = df.getOWLAnnotationAssertionAxiom(EDITOR_NOTE, cls.getIRI(), val);
	    		changes.add(new AddAxiom(getOWLModelManager().getActiveOntology(), ax));*/
	    		addAxiomForOWLAnnotationAssertion(changes, getOWLModelManager().getActiveOntology(), EDITOR_NOTE, cls.getIRI(), val);
    		}
    		if (DESIGN_NOTE != null) {
	    		designnote = dlg.getDesignNote();
	    		val = df.getOWLLiteral(designnote, OWL2Datatype.RDF_PLAIN_LITERAL);
	    		//Fix issue #569 - Optional project configuration properties
	    		/*ax = df.getOWLAnnotationAssertionAxiom(DESIGN_NOTE, cls.getIRI(), val);
	    		changes.add(new AddAxiom(getOWLModelManager().getActiveOntology(), ax));*/
	    		addAxiomForOWLAnnotationAssertion(changes, getOWLModelManager().getActiveOntology(), DESIGN_NOTE, cls.getIRI(), val);
    		}
    	}
    	return changes;

    }
    
    public boolean completeRetire(Map<OWLAnnotationProperty, Set<String>> fixups) {
    	
    	//List<OWLClass> old_parents = new ArrayList<OWLClass>();
    	if (clientSession.hasActiveClient()) {
	    	String user = clientSession.getActiveClient().getUserInfo().getName().toString();
	    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
	    	
	    	//Fix issue #569 - Optional project configuration properties
	    	String editornote = null;
	    	if (EDITOR_NOTE != null) {
	    		editornote = "Retired on: " + timestamp + " by " + user;
	    	}
	        String designnote = null;
	        if (DESIGN_NOTE != null) {
	        	designnote = "Retired on: " + timestamp;
	        }
	        
	        
	        OWLClass class_to_retire = current_op.getRetireClass();
	        
	    	List<OWLOntologyChange> changes = addNotes(editornote, designnote, class_to_retire);
	    	
	    	//Fix issue #569 - Optional project configuration properties
	    	if (changes.isEmpty() && EDITOR_NOTE != null && DESIGN_NOTE != null) {
	    		fireChange(new EditTabChangeEvent(this, ComplexEditType.RETIRE));
	    		//current_op.setRetireParents(old_parents);
	    		return false;
	    	}
	    	
	    	OWLDataFactory df = getOWLModelManager().getOWLDataFactory();    	
	        
	        Set<OWLSubClassOfAxiom> sub_axioms = ontology.getSubClassAxiomsForSubClass(class_to_retire);
	        
	        for (OWLSubClassOfAxiom ax1 : sub_axioms) {
	        	OWLClassExpression exp = ax1.getSuperClass();
	        	
	        	changes = addParentRoleAssertions(changes, exp, class_to_retire);
	        	changes.add(new RemoveAxiom(ontology, ax1));
	        	
	        }
	        
	        Set<OWLEquivalentClassesAxiom> equiv_axioms = ontology.getEquivalentClassesAxioms(class_to_retire);
	        
	        for (OWLEquivalentClassesAxiom ax1 : equiv_axioms) {
	        	Set<OWLClassExpression> exps = ax1.getClassExpressions();
	        	for (OWLClassExpression exp : exps) {
	        		
	        		changes = addParentRoleAssertions(changes, exp, class_to_retire);
	        	}
	        	changes.add(new RemoveAxiom(ontology, ax1));
	        	
	        }
	        
	        Set<OWLAnnotationAssertionAxiom> assocs = ontology.getAnnotationAssertionAxioms((OWLAnnotationSubject) class_to_retire.getIRI());
	        
	        for (OWLAnnotationAssertionAxiom ax1 : assocs) {
	        	// TODO: check that annotation is an association
	        	System.out.println("The value of the annotation is: " + ax1.getValue());
	        	boolean found = false;
	
	        	if (ax1.getProperty().isOWLAnnotationProperty()) {
	        		Set<OWLAnnotationPropertyRangeAxiom> ranges = ontology.getAnnotationPropertyRangeAxioms(ax1.getProperty());
	        		for (OWLAnnotationPropertyRangeAxiom rax : ranges) {
	        			System.out.println("The range: " + rax.toString());
	        			if (rax.getRange().getShortForm().equals("anyURI")) {
	        				found = true;
	        			}
	        		}
	        	}
	
	
	        	if (found) {
	        		
	        		OWLAnnotationValue av = ax1.getValue();
	        		String ann_val = "";
	        		if (av.asIRI().isPresent()) {
	        			ann_val = av.asIRI().get().getShortForm();        			
	        		} else {
	        			ann_val = av.toString();
	        		}
	
	        		String val = ax1.getProperty().getIRI().getShortForm() + "|"
	        				+ ann_val;
	        		OWLLiteral lit = df.getOWLLiteral(val, OWL2Datatype.RDF_PLAIN_LITERAL);
	        		OWLAxiom ax = df.getOWLAnnotationAssertionAxiom(DEP_ASSOC, class_to_retire.getIRI(), lit);
	        		changes.add(new AddAxiom(ontology, ax));
	
	        		changes.add(new RemoveAxiom(ontology, ax1));
	        	}
	        }
	        
	        for (OWLAnnotationProperty p : fixups.keySet()) {
	        	for (String s : fixups.get(p)) {
	        		OWLLiteral val1 = df.getOWLLiteral(s, OWL2Datatype.RDF_PLAIN_LITERAL);
	        		OWLAxiom ax1 = df.getOWLAnnotationAssertionAxiom(p, class_to_retire.getIRI(), val1);
	        		changes.add(new AddAxiom(ontology, ax1));        		
	        	}        	
	        }
	        if (currentTab().isWorkFlowManager()) {
	        	//Fix issue #569 - Optional project configuration properties
	        	/*if (RETIRE_ROOT != null) {
	        		changes.add(new AddAxiom(ontology,
	        			df.getOWLSubClassOfAxiom(class_to_retire, RETIRE_ROOT)));
	        	}*/
	        	addAxiomForOWLSubClass(changes, ontology, class_to_retire, RETIRE_ROOT);
	        	changes.add(new AddAxiom(ontology, df.getDeprecatedOWLAnnotationAssertionAxiom(class_to_retire.getIRI())));
	        	if (DEPR_CONCEPT_STATUS_PROP != null) {
	        		changes.add(new AddAxiom(ontology,
	        				df.getOWLAnnotationAssertionAxiom(DEPR_CONCEPT_STATUS_PROP, class_to_retire.getIRI(),
	        						df.getOWLLiteral(DEPR_CONCEPT_STATUS_VALUE, OWL2Datatype.RDF_PLAIN_LITERAL))));
	        		
	        	}
	        } else {
	        	//Fix issue #569 - Optional project configuration properties
	        	/*if (PRE_RETIRE_ROOT != null) {
	        		changes.add(new AddAxiom(ontology,
	        			df.getOWLSubClassOfAxiom(class_to_retire, PRE_RETIRE_ROOT))); 
	        	}*/
	        	addAxiomForOWLSubClass(changes, ontology, class_to_retire, PRE_RETIRE_ROOT);
	        }
	        
	        getOWLModelManager().applyChanges(changes);
	        
	        return true;
    	} else {
    		// Not connect to a server
    		OWLClass class_to_retire = current_op.getRetireClass();
    		OWLDataFactory df = getOWLModelManager().getOWLDataFactory();    	
	        
	        //Set<OWLSubClassOfAxiom> sub_axioms = ontology.getSubClassAxiomsForSubClass(class_to_retire);
	        
	        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
	        /*for (OWLSubClassOfAxiom ax1 : sub_axioms) {
	        	OWLClassExpression exp = ax1.getSuperClass();
	        	
	        	changes = addParentRoleAssertions(changes, exp, class_to_retire);
	        	changes.add(new RemoveAxiom(ontology, ax1));
	        	
	        }*/
	        
	        /*Set<OWLEquivalentClassesAxiom> equiv_axioms = ontology.getEquivalentClassesAxioms(class_to_retire);
	        
	        for (OWLEquivalentClassesAxiom ax1 : equiv_axioms) {
	        	Set<OWLClassExpression> exps = ax1.getClassExpressions();
	        	for (OWLClassExpression exp : exps) {
	        		
	        		changes = addParentRoleAssertions(changes, exp, class_to_retire);
	        	}
	        	changes.add(new RemoveAxiom(ontology, ax1));
	        	
	        }*/
	        
	        /*Set<OWLAnnotationAssertionAxiom> assocs = ontology.getAnnotationAssertionAxioms((OWLAnnotationSubject) class_to_retire.getIRI());
	        
	        for (OWLAnnotationAssertionAxiom ax1 : assocs) {
	        	// TODO: check that annotation is an association
	        	System.out.println("The value of the annotation is: " + ax1.getValue());
	        	boolean found = false;
	
	        	if (ax1.getProperty().isOWLAnnotationProperty()) {
	        		Set<OWLAnnotationPropertyRangeAxiom> ranges = ontology.getAnnotationPropertyRangeAxioms(ax1.getProperty());
	        		for (OWLAnnotationPropertyRangeAxiom rax : ranges) {
	        			System.out.println("The range: " + rax.toString());
	        			if (rax.getRange().getShortForm().equals("anyURI")) {
	        				found = true;
	        			}
	        		}
	        	}
	
	
	        	if (found) {
	        		
	        		OWLAnnotationValue av = ax1.getValue();
	        		String ann_val = "";
	        		if (av.asIRI().isPresent()) {
	        			ann_val = av.asIRI().get().getShortForm();        			
	        		} else {
	        			ann_val = av.toString();
	        		}
	
	        		String val = ax1.getProperty().getIRI().getShortForm() + "|"
	        				+ ann_val;
	        		OWLLiteral lit = df.getOWLLiteral(val, OWL2Datatype.RDF_PLAIN_LITERAL);
	        		OWLAxiom ax = df.getOWLAnnotationAssertionAxiom(DEP_ASSOC, class_to_retire.getIRI(), lit);
	        		changes.add(new AddAxiom(ontology, ax));
	
	        		changes.add(new RemoveAxiom(ontology, ax1));
	        	}
	        }*/
	        
	        for (OWLAnnotationProperty p : fixups.keySet()) {
	        	if (p != null) {
		        	for (String s : fixups.get(p)) {
		        		OWLLiteral val1 = df.getOWLLiteral(s, OWL2Datatype.RDF_PLAIN_LITERAL);
		        		OWLAxiom ax1 = df.getOWLAnnotationAssertionAxiom(p, class_to_retire.getIRI(), val1);
		        		changes.add(new AddAxiom(ontology, ax1));        		
		        	}
	        	}
	        }
	        //if (currentTab().isWorkFlowManager()) {
	        	/*changes.add(new AddAxiom(ontology,
	        			df.getOWLSubClassOfAxiom(class_to_retire, RETIRE_ROOT)));*/
	        	changes.add(new AddAxiom(ontology, df.getDeprecatedOWLAnnotationAssertionAxiom(class_to_retire.getIRI())));
	        	/*if (DEPR_CONCEPT_STATUS_PROP != null) {
	        		changes.add(new AddAxiom(ontology,
	        				df.getOWLAnnotationAssertionAxiom(DEPR_CONCEPT_STATUS_PROP, class_to_retire.getIRI(),
	        						df.getOWLLiteral(DEPR_CONCEPT_STATUS_VALUE, OWL2Datatype.RDF_PLAIN_LITERAL))));
	        		
	        	}*/
	        /*} else {
	        	changes.add(new AddAxiom(ontology,
	        			df.getOWLSubClassOfAxiom(class_to_retire, PRE_RETIRE_ROOT))); 
	        }*/
	        
	        getOWLModelManager().applyChanges(changes);
	        
	        return true;
    	}
        //current_op.setRetireParents(old_parents);
    }
    
    //Fix issue #569 - Optional project configuration properties
    private void addAxiomForOWLSubClass(List<OWLOntologyChange> changes,  OWLOntology ontology, OWLClass subClass, OWLClass superClass) {
    	if (superClass != null) {
    		changes.add(new AddAxiom(ontology, getOWLModelManager().getOWLDataFactory().getOWLSubClassOfAxiom(subClass, superClass))); 	
    	}
    }
    
    //Fix issue #569 - Optional project configuration properties
    private void addAxiomForOWLAnnotationAssertion (List<OWLOntologyChange> changes, OWLOntology ontology, OWLAnnotationProperty prop, OWLAnnotationSubject subject, OWLAnnotationValue value) {
    	if (prop != null) {
    		OWLAxiom ax = getOWLModelManager().getOWLDataFactory().getOWLAnnotationAssertionAxiom(prop, subject, value);
    		changes.add(new AddAxiom(ontology, ax)); 
    	}
    }
    
    private List<OWLOntologyChange> addParentRoleAssertions(List<OWLOntologyChange> changes, OWLClassExpression exp, OWLClass cls) {
    	OWLModelManager mngr = getOWLModelManager();
    	OWLDataFactory df = mngr.getOWLDataFactory();
    	if (exp instanceof OWLClass) {
    		if (cls.equals(exp)) {
    			// noop
    		} else {
    			OWLClass ocl  = (OWLClass) exp;
    			//Fix issue #569 - Optional project configuration properties
    			if (PRE_MERGE_ROOT != null && !ocl.equals(PRE_MERGE_ROOT)) {
    				String name = ocl.getIRI().getShortForm();
    				OWLLiteral val = df.getOWLLiteral(name, OWL2Datatype.RDF_PLAIN_LITERAL);
    				/*OWLAxiom ax = df.getOWLAnnotationAssertionAxiom(DEP_PARENT, cls.getIRI(), val);
    				changes.add(new AddAxiom(ontology, ax));*/
    				addAxiomForOWLAnnotationAssertion(changes, ontology, DEP_PARENT, cls.getIRI(), val);
    			}
    		}
    	} else if (exp instanceof OWLQuantifiedObjectRestriction) {
    		OWLQuantifiedObjectRestriction qobj = (OWLQuantifiedObjectRestriction) exp;
    		OWLClassExpression rexp = qobj.getFiller();

    		String fval;
    		if (rexp instanceof OWLClass) {
    			fval = ((OWLClass) rexp).getIRI().getShortForm();
    		} else {
    			fval = mngr.getRendering(rexp);
    		}

    		String quant = "some";
    		if (exp instanceof OWLObjectSomeValuesFrom) {
    			quant = "some";
    		} else if (exp instanceof OWLObjectAllValuesFrom) {
    			quant = "only";
    		}
    		String val = qobj.getProperty().asOWLObjectProperty().getIRI().getShortForm() + "|"
    				+ quant + "|" + fval;
    		OWLLiteral lit = df.getOWLLiteral(val, OWL2Datatype.RDF_PLAIN_LITERAL);
    		OWLAxiom ax = df.getOWLAnnotationAssertionAxiom(DEP_ROLE, cls.getIRI(), lit);
    		changes.add(new AddAxiom(ontology, ax));
    	} else if (exp instanceof OWLObjectIntersectionOf) {
    		OWLObjectIntersectionOf oio = (OWLObjectIntersectionOf) exp;
    		Set<OWLClassExpression> conjs = oio.asConjunctSet();
    		for (OWLClassExpression c : conjs) {
    			changes = addParentRoleAssertions(changes, c, cls);
    		}
    	} else if (exp instanceof OWLObjectUnionOf) {
    		OWLObjectUnionOf oio = (OWLObjectUnionOf) exp;
    		Set<OWLClassExpression> conjs = oio.asDisjunctSet();
    		for (OWLClassExpression c : conjs) {
    			changes = addParentRoleAssertions(changes, c, cls);
    		}
    	}
    	return changes;

    }
    
    public boolean isUneditableRoot(OWLClass cls) {
    	//Fix issue #569 - Optional project configuration properties
    	if ((PRE_RETIRE_ROOT != null && cls.equals(PRE_RETIRE_ROOT)) ||
    			(PRE_MERGE_ROOT != null && cls.equals(PRE_MERGE_ROOT)) ||
    			(RETIRE_ROOT != null && cls.equals(RETIRE_ROOT)) ||
    			(RETIRE_CONCEPTS_ROOT != null && cls.equals(RETIRE_CONCEPTS_ROOT))) {
    		return true;    				
    	}
    	return false;
    	
    	
    }
    
    /** Anyone can pre-retire so there is no need for a separate pre-retire step. There is just retire.
     * If the class is already a chle of the "PreRetired" root class, then the retirement is ready for
     * review and only an admin user can do so. 
     * 
     */
    public boolean canRetire(OWLClass cls) { 
    	
    	if (isUneditableRoot(cls)) {
    		return false;    				
    	}

    	if (clientSession.hasActiveClient()) {
			ProjectId projectId = clientSession.getActiveProject();
			if (projectId == null) {
				return false;
			}
	
	    	boolean can = clientSession.getActiveClient().getConfig().canPerformProjectOperation(
	    		NCIEditTabConstants.RETIRE.getId(), projectId);
			if (!can) {
				return false;
			}

			if (isPreRetired(cls)) {
				return isWorkFlowManager();
			} else {
				return !isRetired(cls);
			}
    	} else {
    		// Not connect to a server
    		return true;
    	}
    }
    
    public void retire(OWLClass selectedClass) {
    	current_op.setRetireClass(selectedClass);
    	current_op.setType(ComplexEditType.RETIRE);
    	fireChange(new EditTabChangeEvent(this, ComplexEditType.RETIRE));
    }
    
    public void addComplex(OWLClass selectedClass) {
    	fireChange(new EditTabChangeEvent(this, ComplexEditType.ADD_PROP));    	
    }
    
    public void selectClass(OWLClass cls) {
    	if (cls != null) {
    		currentlySelected = cls;
    		fireChange(new EditTabChangeEvent(this, ComplexEditType.SELECTED)); 
    	}
    }
    
    public void classModified() {
    	fireChange(new EditTabChangeEvent(this, ComplexEditType.MODIFY));    	
    }   
    
    public void resetState() {
    	fireChange(new EditTabChangeEvent(this, ComplexEditType.RESET));
    	current_op = new ComplexOperation();
    }
   
    	
    public boolean isWorkFlowManager() { 
    	if (clientSession.getActiveClient() != null) {
    		try {
    			Role wfm = ((LocalHttpClient) clientSession.getActiveClient()).getRole(new RoleIdImpl("mp-project-manager"));
    			return clientSession.getActiveClient().getActiveRoles(clientSession.getActiveProject()).contains(wfm);
    		} catch (ClientRequestException e) {
    			e.printStackTrace();
    		}
    	}
    	return false;
    }
    
    public boolean isWorkFlowModeler() {
    	return !isSysAdmin();
    }
    
    public boolean isSysAdmin() {
    	return (((LocalHttpClient) clientSession.getActiveClient()).getClientType() == UserType.ADMIN);
    	
    }
    
    public boolean isPreRetired(OWLClass cls) {
    	return isSubClass(cls, PRE_RETIRE_ROOT);    	
    }
    
    public boolean isPreMerged(OWLClass cls) {
    	return isSubClass(cls, PRE_MERGE_ROOT);    	
    }
    
    public boolean isRetireRoot(OWLClass cls) {
    	return cls.equals(RETIRE_ROOT);    	
    }
    
    public boolean isRetireConceptsRoot(OWLClass cls) {
    	return cls.equals(RETIRE_CONCEPTS_ROOT);    	
    }
    
    public boolean isRetired(OWLClass cls) {
    	if (cls != null) {
    		OWLAnnotationProperty deprecated = this.lookUpShort("deprecated");
    		Optional<String> bool = getPropertyValue(cls, deprecated);
    		if (bool.isPresent()) {
    			return (bool.get().equals("true"));
    		}
    	}
    	return false;
    }
    
    public boolean isSubClass(OWLClass sub, OWLClass sup) {
       	//before ontology is open user may click on Thing
    	if (ontology != null) {

    		if ((sub != null) && sub.equals(sup)) {
    			return true;
    		}

    		Set<OWLSubClassOfAxiom> subs = ontology.getSubClassAxiomsForSubClass(sub);
    		for (OWLSubClassOfAxiom s : subs) {
    			if (!s.getSuperClass().isAnonymous()) {
    				if (s.getSuperClass().asOWLClass().equals(sup)) {
    					return true;
    				}
    			}    		
    		}
    	}
    	return false;

    }
    
    public void updateRetire() {    	
    	editInProgress = false;
    	navTree.setSelectedEntity(current_op.getRetireClass());
    	this.fireChange(new EditTabChangeEvent(this, ComplexEditType.RETIRE));    	
    }
    
    public void completeRetire() {
        current_op = new ComplexOperation();
    }
    
    public boolean isNotSpecialRoot(OWLClass cls) {
    	return !(isPreRetired(cls) || isRetired(cls)
    			|| isPreMerged(cls) || isRetireRoot(cls) 
    			|| isRetireConceptsRoot(cls));
    }
    
    
    private boolean clearing = false;
    
    public void undoChanges() {
    	clearing = true;
    	while (history.canUndo()) {
    		history.undo();
    	}
    	clearing = false;
    	
    }
    
    public void backOutChange() {
    	if (history.canUndo()) {
    		history.undo();
    	}
    }
    
    public boolean commitChanges(boolean notify) {
    	
    	ComplexEditType type = getCurrentOp().getType();
    	if (type == null) {
    		type = ComplexEditType.MODIFY;
    	}
    	
    	List<OWLOntologyChange> changes = history.getUncommittedChanges();

    	if (changes.size() > 0) {    		
    		try {
    			
    			doCommit(changes, type, notify);
    			    			
    			fireChange(new EditTabChangeEvent(this, ComplexEditType.COMMIT));
    			
    			return true;
    			
    		} catch (ClientRequestException e) {
    			if (e instanceof LoginTimeoutException) {
                    showErrorDialog("Commit error", e.getMessage(), e);
                    Optional<AuthToken> authToken = UserLoginPanel.showDialog(getOWLEditorKit(), getOWLEditorKit().getWorkspace());
                    if (authToken.isPresent() && authToken.get().isAuthorized()) {
                    	try {
							doCommit(changes, type, notify);
							
							return true;
						} catch (Exception e1) {
							
							showErrorDialog("Retry of commit failed", e1.getMessage(), e1);
							return false;
						}
                        
                    }
                }
                else {
                    showErrorDialog("Commit error", e.getMessage(), e);
                    return false;
                }
    			
    		} catch (AuthorizationException e) {
    			showErrorDialog("This should not occur", e.getMessage(), e);
    			return false;
			}
    	}
    	return false;        
    }
    
    private void doCommit(List<OWLOntologyChange> changes, ComplexEditType type,
    		 boolean notify) throws ClientRequestException, 
    AuthorizationException, ClientRequestException {
    	String comment = "";
    	comment = type.name();
    	if (!this.inBatchMode) {
    		OWLClass source = current_op.getSource();
        	OWLClass target = current_op.getTarget();
        	OWLClass currentlyEditing = current_op.getCurrentlyEditing();
    		// TODO: This class could be null
    		String label = "";
    		if (currentlyEditing != null) {
    			label = getRDFSLabel(currentlyEditing).get();
    		}
    		if (type == MODIFY) {
    			comment = label + "(" +
    					currentlyEditing.getIRI().getShortForm() + ") - " +
    					type.name();
    		} else if (type == SPLIT)  {
    			comment = label + "(" +
    					source.getIRI().getShortForm() + " -> " +
    					target.getIRI().getShortForm() + ") - " +
    					type.name();

    		} else if (type == ComplexEditType.MERGE)  {
    			comment = label + "(" +
    					source.getIRI().getShortForm() + " -> " +
    					target.getIRI().getShortForm() + ") - " +
    					type.name();

    		} else if (type == ComplexEditType.DUAL)  {
    			comment = label + "(" +
    					source.getIRI().getShortForm() + " & " +
    					target.getIRI().getShortForm() + ") - " +
    					type.name();

    		} else if (type == ComplexEditType.RETIRE)  {
    			comment = label + "(" +
    					current_op.getRetireClass().getIRI().getShortForm() + ") - " +
    					type.name();

    		}
    	}
    	
    	// first do last update to sync
    	clientSession.fireChangeEvent(EventCategory.UPDATE_ONTOLOGY);
    	
		Commit commit = ClientUtils.createCommit(clientSession.getActiveClient(), comment, changes);
		DocumentRevision base = clientSession.getActiveVersionOntology().getHeadRevision();
		CommitBundle commitBundle = new CommitBundleImpl(base, commit);
		ChangeHistory hist = clientSession.getActiveClient().commit(clientSession.getActiveProject(), commitBundle);
		clientSession.getActiveVersionOntology().update(hist);
		// submit history after the commit but before you broadcast the news
		if (!inBatchMode) 
			submitHistory();
		clientSession.fireCommitPerformedEvent(new CommitOperationEvent(
                hist.getHeadRevision(),
                hist.getMetadataForRevision(hist.getHeadRevision()),
                hist.getChangesForRevision(hist.getHeadRevision())));
		if (notify) {
			JOptionPane.showMessageDialog(this, "Class saved successfully", "Class Save", JOptionPane.INFORMATION_MESSAGE);
		}
		
		
		
    }
    
    private void showErrorDialog(String title, String message, Throwable t) {
        JOptionPaneEx.showConfirmDialog(getOWLEditorKit().getWorkspace(), title, new JLabel(message),
                JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, null);
    }
    
    public void putHistory(String c, String n, String op, String ref) {
    	try {
			((LocalHttpClient) clientSession.getActiveClient()).putEVSHistory(c, n, op, ref, clientSession.getActiveProject());
		} catch (ClientRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthorizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    	
    }
    
    public List<History> getEvsHistory() {
    	try {
			return ((LocalHttpClient) clientSession.getActiveClient()).getEVSHistory(clientSession.getActiveProject());
		} catch (ClientRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AuthorizationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    	
    	
    }
    
    
    
    public ComplexOperation getCurrentOp() {
    	return this.current_op;
    }
    
    public void splitClass(OWLClass newClass, OWLClass selectedClass, boolean clone_p) {    	

    	OWLModelManager mngr = getOWLModelManager();
    	OWLDataFactory df = mngr.getOWLDataFactory();
    	
    	List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    	
    	if (clone_p) {
    		
    	} 
    	//Fix issue #569 - Optional project configuration properties
    	else if (hasActiveClient() && SPLIT_FROM != null){
    		OWLLiteral fromCode = df.getOWLLiteral(selectedClass.getIRI().getShortForm(), OWL2Datatype.RDF_PLAIN_LITERAL);
    		/*OWLAxiom ax = df.getOWLAnnotationAssertionAxiom(SPLIT_FROM, newClass.getIRI(), fromCode);
    		changes.add(new AddAxiom(ontology, ax));*/
    		addAxiomForOWLAnnotationAssertion(changes, ontology, SPLIT_FROM, newClass.getIRI(), fromCode);
    	}
    	
    	Map<OWLEntity, IRI> replacementIRIMap = new HashMap<>();
    	replacementIRIMap.put(selectedClass, newClass.getIRI());

    	OWLObjectDuplicator dup = new OWLObjectDuplicator(replacementIRIMap, mngr.getOWLOntologyManager());            

    	changes.addAll(duplicateClassAxioms(selectedClass, dup));            
    	changes.addAll(duplicateAnnotations(selectedClass, dup));    	           

    	current_op.setSource(selectedClass);
    	current_op.setTarget(newClass);

    	this.fireChange(new EditTabChangeEvent(this, current_op.getType()));
    	
    	setEditInProgress(true);
		setCurrentlyEditing(current_op.getTarget(), true);
		
    	
    	mngr.applyChanges(changes);
    	refreshNavTree();
    }
    
    public OWLClass createNewChild(OWLClass selectedClass, Optional<String> prefName, Optional<String> code, boolean dontApply) {
    	Optional<String> modPrefName = prefName;
    	if (prefName.isPresent()) {
    		modPrefName = Optional.of(mapper.fix(prefName.get()));    		
    	}
    	NCIClassCreationDialog<OWLClass> dlg = new NCIClassCreationDialog<OWLClass>(getOWLEditorKit(),
    			"Please enter a class name", OWLClass.class, modPrefName, code, dontApply);

    	boolean proceed = false;

    	if (prefName.isPresent()) {
    		proceed = true;
    	} else {
    		proceed = dlg.showDialog();
    	}

    	if (proceed) {
    		setOp(ComplexEditType.CREATE);
    		OWLClass newClass = dlg.getNewClass();
    		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    		if (dontApply) {
    			changes = dlg.getOntChanges();
    		}
    		OWLModelManager mngr = getOWLModelManager();
    		OWLDataFactory df = mngr.getOWLDataFactory();

    		if (!df.getOWLThing().equals(selectedClass)){
    			OWLSubClassOfAxiom ax = df.getOWLSubClassOfAxiom(newClass, selectedClass);
    			changes.add(new AddAxiom(mngr.getActiveOntology(), ax));
    			
    			//Fix issue #569 - Optional project configuration properties
    			//if (SEMANTIC_TYPE != null) {
				Optional<OWLAnnotationValue> sem_typ = this.getSemanticType(selectedClass);
				if (sem_typ.isPresent()) {
					/*OWLAnnotationValue sem_typ_val = sem_typ.get();
					OWLAxiom sem_typ_ax = 
							df.getOWLAnnotationAssertionAxiom(SEMANTIC_TYPE, newClass.getIRI(), sem_typ_val);
					changes.add(new AddAxiom(mngr.getActiveOntology(), sem_typ_ax));*/
					addAxiomForOWLAnnotationAssertion(changes, mngr.getActiveOntology(), SEMANTIC_TYPE, newClass.getIRI(),
							sem_typ.get());
				}
    			//}
    		}
    		if (dontApply) {
    			batch_changes.addAll(changes);
    			addBatchHistory(newClass, prefName.get(), CREATE);
    		} else {
    			mngr.applyChanges(changes);
    		}

    		return newClass;
    	} else {
    		return null;
    	}

    }
        
    public boolean canClone(OWLClass cls) {
    	return isNotSpecialRoot(cls);
    }

	@Override
	public void dispose() {
		clientSession.removeListener(this);
		clientSession.removeCommitOperationListener(this);
		super.dispose();
		log.info("Disposed of NCI Edit Tab");
	}
	
	public void addListeners() {
		clientSession.addListener(this);
		clientSession.addCommitOperationListener(this);
		history.addUndoManagerListener(this);
	}
	
	public void handleChange(ClientSessionChangeEvent event) {
		
		if (event.hasCategory(EventCategory.OPEN_PROJECT) || event.hasCategory(EventCategory.SWITCH_ONTOLOGY)) {
			if (event.getSource().hasActiveClient()) {
				ontology = getOWLModelManager().getActiveOntology();
				initProperties();
				if (event.hasCategory(EventCategory.OPEN_PROJECT)) {
					if (initEagerViews) {

					} else {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								//log.info("Voila");				
								fireUpViews();					
							}
						});
						initEagerViews = true;
					}
				}
			}
		}
	}
	
	@Override
	public void operationPerformed(CommitOperationEvent event) {
		resetHistory();		
	}

    @Override
    public void stateChanged(HistoryManager source) {
    	if (!getOWLEditorKit().getModelManager().getConnectionMode().equals(ConnectionMode.CLIENTSERVER)) {
    		if (current_op.getType().name().equals("RETIRE")) {
    			setCurrentlyEditing(current_op.getCurrentlyEditing(), true);
    			classModified();
    		}
    		return;    		
    	}
    	if (this.getOWLEditorKit().getModelManager().getExplanationManager().getIsRunning()) {
    		return;
    	}
    	if (this.inPowerMode) {
    		return;
    	}
    	List<OWLOntologyChange> changes = history.getUncommittedChanges();

    	if (!changes.isEmpty() && !clearing) {
    		List<? extends OWLOntologyChange> latest_changes = history.getLatestChanges();
    		
    		boolean ok_curator = (new CuratorChecks(ontology, getOWLEditorKit())).checkOkChanges(latest_changes);
			if (!ok_curator) {
				history.stopTalking();
				backOutChange();
				history.startTalking();
				return;
			}
    		
    	}
    	List<OWLClass>  subjects = findUniqueSubjects(changes);
    	if (!subjects.isEmpty()) {
    		if (current_op.isMissingOneside()) {
    			JOptionPane.showMessageDialog(null, "You need two concepts to edit in Dual Edit panel");
    			undoChanges();
    			return;
    		}
    		if (current_op.isChangedEditFocus(subjects)) {
    			int result = JOptionPane.showOptionDialog(null, "Class already being edited. Do you want to proceed with this edit?", 
    					"Proceed or stay with existing edit?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
    			if (result == JOptionPane.CANCEL_OPTION) {
    				backOutChange(); 
    				refreshNavTree();
    			} else if (result == JOptionPane.OK_OPTION) {
    				history.stopTalking();
    				undoChanges();
    				history.startTalking();
    				resetState();
    				getOWLModelManager().applyChange(changes.get(changes.size() - 1));  			

    			}
    		} else {
    			setCurrentlyEditing(current_op.getCurrentlyEditing(), true);
    			classModified();
    			
    		}
    	} else {  
    		if (!current_op.isMissingOneside()) {
	        	setEditInProgress(false);
	        	selectClass(getCurrentlyEditing());
	        	refreshNavTree();
	        	resetState();
    		}
    	}
    	
    	
    }
    
    private List<OWLClass> findUniqueSubjects(List<OWLOntologyChange> changes) {
    	List<OWLClass> result = new ArrayList<OWLClass>();
    	for (OWLOntologyChange change : changes) {
    		if (change.isAxiomChange()) {
    			
    			OWLAxiom ax = change.getAxiom();
    			OWLClass subj = null;
    			
    			if (ax instanceof OWLAnnotationAssertionAxiom) {
    				IRI iri = (IRI) ((OWLAnnotationAssertionAxiom) ax).getSubject();
    				Set<OWLEntity> classes = ontology.getEntitiesInSignature(iri);
    				for (OWLEntity ent : classes) {
    					if (ent.isOWLClass()) {
    						subj = ent.asOWLClass();
    						// take the first, it should be unique
    						break;
    					}
    				}    				
    			} else if (ax instanceof OWLSubClassOfAxiom) {
    				subj = ((OWLSubClassOfAxiom) ax).getSubClass().asOWLClass();
    			} else if (ax instanceof OWLEquivalentClassesAxiom) {
    				Set<OWLClassExpression> exps = ((OWLEquivalentClassesAxiom) ax).getClassExpressions();
    				OWLClass tmp = null;
    				boolean found_existing_subject = false;
    				for (OWLClassExpression exp : exps) {
    					if (exp instanceof OWLClass) {
    						if (result.contains(exp.asOWLClass())) {
    							found_existing_subject = true;
    							break;    							
    						} else {
    							tmp = exp.asOWLClass();
    						}
    					}
    				}
    				if (!found_existing_subject) {
    					subj = tmp;
    				}
    			}
    			if (subj != null) {
    				if (result.contains(subj)) {
    				} else {
    					result.add(subj);
    				}
    			}
    		}
    	}
    	return result;
    }
	
	public void resetHistory() {
		history.reset();		
	}
	
	public boolean isRestricted(OWLClass cls, OWLAnnotationProperty prop) {
		if (restrictedBy.containsKey(prop)) {
			OWLAnnotationProperty resProp = restrictedBy.get(prop);
			if (getPropertyValue(cls, resProp).isPresent()) {
				String val = getPropertyValue(cls, resProp).get();
				IRI valIri = IRI.create(CODE_PROP.getIRI().getNamespace() + val);
				List<String> enums = getEnumValues(valIri);
				
				if (enums.contains(clientSession.getActiveClient().getUserInfo().getId().toString())) {
					return false;
				}
	
				return true;
				
			} else {
				return false;
			}			
		} else {
			return false;
		}
		
	}
	
	
	private synchronized void initProperties() {
		
		//getOWLEditorKit().getSearchManager().disableIncrementalIndexing();
		
		
		LocalHttpClient lhc = (LocalHttpClient) clientSession.getActiveClient();
		if (lhc != null) {
			Project project = null;
			
			ProjectId pid = clientSession.getActiveProject();
			if (pid == null) {
				Optional<ProjectId> projid = lhc.findProjectId(ontology.getOntologyID().getOntologyIRI().get());
				if (projid.isPresent()) {
					pid = projid.get();
				}
			}
			if (pid == null) return;
			project = lhc.findProject(pid);

			if (project != null) {
				// get all annotations from ontology to use for lookup
				annProps = ontology.getAnnotationPropertiesInSignature();
				
				OWLAnnotationProperty restricted_by = lookUpShort("restricted_by");
				
				// populate associations
				for (OWLAnnotationProperty p : annProps) {
					Set<OWLAnnotationPropertyRangeAxiom> ranges = ontology.getAnnotationPropertyRangeAxioms(p);
	        		for (OWLAnnotationPropertyRangeAxiom rax : ranges) {
	        			//System.out.println("The range: " + rax.toString());
	        			if (rax.getRange().getShortForm().equals("anyURI")) {
	        				associations.add(p);
	        			}
	        		}
	        		
	        		Set<OWLAnnotationAssertionAxiom> panns =
	        				ontology.getAnnotationAssertionAxioms(p.getIRI());
	        		
	        		for (OWLAnnotationAssertionAxiom ax : panns) {
	        			if (ax.getProperty().equals(restricted_by)) {
	        				Optional<IRI> iri = ax.getValue().asIRI();
	        				OWLAnnotationProperty res_prop = lookUpIri(iri.get());
	        				restrictedBy.put(p, res_prop);
	        				
	        			}
	        			
	        		}
			
				}

				com.google.common.base.Optional<ProjectOptions> options = project.getOptions();
				
				Set<String> not_found_props = new HashSet<String>();

				if (options.isPresent()) {
					ProjectOptions opts = options.get();
					Set<String> complex_props = opts.getValues(COMPLEX_PROPS);
					complex_properties = new ArrayList<OWLAnnotationProperty>();
					if (!complex_props.isEmpty()) {						
						for (String cp : complex_props) {
							OWLAnnotationProperty p = lookUp(cp);
							if (p != null) {
								complex_properties.add(p);
							} else {
								not_found_props.add(cp);
							}
							
							// now get dependencies
							// cprops are what is in the config file
							Set<OWLAnnotationProperty> cprops = new HashSet<OWLAnnotationProperty>();
							// dprops are required, they have the required annotation set to true
							Set<OWLAnnotationProperty> dprops = new HashSet<OWLAnnotationProperty>();
							// oprops are options, they do not have the rquired annotation or they have it set to false
							Set<OWLAnnotationProperty> oprops = new HashSet<OWLAnnotationProperty>();
							Set<String> dependents = opts.getValues(cp);
							if (!dependents.isEmpty()) {
								for (String dp : dependents) {
									OWLAnnotationProperty dpProp = lookUp(dp);
									if (dpProp != null) {
										// always add to cprops
										cprops.add(dpProp);
										if (is_required(dpProp)) {
											dprops.add(dpProp);
										} else {
											oprops.add(dpProp);
										}
									} else {
										not_found_props.add(dp);
									}								
								}
								configured_annotation_dependencies.put(p, cprops);
								required_annotation_dependencies.put(p, dprops);
								optional_annotation_dependencies.put(p, oprops);
							}							
						}
						if (not_found_props.size() > 0) {
							String msg = "Missing Properties: \n";
							for (String prop : not_found_props) {
								msg += prop + "\n";
							}
							JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
						}
					}
					Set<String> imm_props = opts.getValues(IMMUTABLE_PROPS);
					if (!imm_props.isEmpty()) {
						for (String ip : imm_props) {
							OWLAnnotationProperty p = lookUp(ip);
							if (p != null) {
								immutable_properties.add(p);
							}
							
						}
					}
					
					// set constants for split/merge/retirement
					MERGE_SOURCE = getSingleProperty("merge_source", opts);
					MERGE_TARGET = getSingleProperty("merge_target", opts);
					SPLIT_FROM = getSingleProperty("split_from", opts);
					
					DEP_PARENT = getSingleProperty("deprecated_parent", opts);
					DEP_CHILD = getSingleProperty("deprecated_child", opts);
					DEP_ROLE = getSingleProperty("deprecated_role", opts);
					DEP_IN_ROLE = getSingleProperty("deprecated_in_role", opts);
					DEP_ASSOC = getSingleProperty("deprecated_assoc", opts);
					DEP_IN_ASSOC = getSingleProperty("deprecated_in_assoc", opts);
					
					PRE_MERGE_ROOT = findOWLClass("premerged_root", opts);
					PRE_RETIRE_ROOT = findOWLClass("preretired_root", opts);
					RETIRE_ROOT  = findOWLClass("retired_root", opts);
					RETIRE_CONCEPTS_ROOT  = findOWLClass("retired_concepts_root", opts);
					
					DESIGN_NOTE = getSingleProperty("design_note", opts);
					EDITOR_NOTE = getSingleProperty("editor_note", opts);	
					
					SYN_TYPE = getSingleProperty("syn_type", opts);
					SYN_SOURCE = getSingleProperty("syn_source", opts);
					
					DEF_SOURCE = getSingleProperty("def_source", opts);
					
					REVIEWER_NAME = getSingleProperty("reviewer_name", opts);
					REVIEW_DATE = getSingleProperty("review_date", opts);
					
					CODE_PROP = getSingleProperty("code_prop", opts);
					LABEL_PROP = getSingleProperty("label_prop", opts);
					FULL_SYN = getSingleProperty("fully_qualified_syn", opts);
					immutable_properties.add(LABEL_PROP);
					PREF_NAME = getSingleProperty("pref_name", opts);
					DEFINITION = getSingleProperty("definition", opts);
					
					SEMANTIC_TYPE = getSingleProperty("semantic_type", opts); 
					
					Set<String> depr_con = opts.getValues("deprecated_concept_status");
					if (!depr_con.isEmpty()) {
						for (String s : depr_con) {
							if (s.startsWith("http")) {
								DEPR_CONCEPT_STATUS_PROP = lookUp(s);								
							} else {
								DEPR_CONCEPT_STATUS_VALUE = s;
							}
						}
					}
					
					
				}

			}
			try {

				for (Operation op : clientSession.getActiveClient().getActiveOperations(clientSession.getActiveProject())) {
					//System.out.println(op.toString());

				}

			} catch (ClientRequestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.fireChange(new EditTabChangeEvent(this, ComplexEditType.INIT_PROPS));
		} else {
			annProps = ontology.getAnnotationPropertiesInSignature();
		}
	}
	
	private boolean is_required(OWLAnnotationProperty prop) {
		OWLAnnotationProperty p = this.lookUpShort("required");
		Optional<String> b_val = getPropertyValue(prop, p);
		if (b_val.isPresent()) {
			return Boolean.parseBoolean(b_val.get());
		}
		return false;
	}
	
	public String getDefault(OWLDatatype prop, String source) {
		if (prop == null) {return null;}
		OWLAnnotationProperty p = this.lookUpShort(NCIEditTabConstants.DEFAULT);
		if (source != null) {
			if (source.equals(NCIEditTabConstants.DEFAULT_SOURCE_NEW_CLASS)) {
				p = this.lookUpShort(NCIEditTabConstants.DEFAULT_ON_CREATE_CLASS);
			} else if (source.equals(NCIEditTabConstants.DEFAULT_SOURCE_NEW_PROPERTY)) {
				p = this.lookUpShort(NCIEditTabConstants.DEFAULT_ON_EDIT_CLASS);
			}
		}
		Optional<String> val = getPropertyValue(prop, p);
		if (val.isPresent()) {
			return val.get();
		}
		return null;
	}
	
	OWLClass findOWLClass(String opt, ProjectOptions opts) {
		OWLClass cls = null;
		Set<String> ss = opts.getValues(opt);
		if (!ss.isEmpty()) {
			IRI iri = IRI.create((String) ss.toArray()[0]);
			Set<OWLEntity> classes = ontology.getEntitiesInSignature(iri);
			for (OWLEntity et : classes) {
				cls = et.asOWLClass();
			}
		}
		return cls;
	}
	
	OWLAnnotationProperty getSingleProperty(String ps, ProjectOptions opts) {
		OWLAnnotationProperty prop = null;
		Set<String> ss = opts.getValues(ps);
		if (!ss.isEmpty()) {
			prop = lookUp((String) ss.toArray()[0]);
		}
		return prop;
	}
	
	OWLAnnotationProperty lookUp(String iri) {
		IRI cpIRI = IRI.create(iri);
		return lookUpIri(cpIRI);
	}
	
	OWLAnnotationProperty lookUpIri(IRI cpIRI) {
		
		if (annProps == null) {
			ontology = getOWLModelManager().getActiveOntology();
			initProperties();
		}
		for (OWLAnnotationProperty ap : annProps) {
			if (ap.getIRI().equals(cpIRI)) {
				IRI dt = getDataType(ap);
				if (dt != null) {
					//System.out.println(cpIRI + " it's type: " + dt);
				} else {
					//System.out.println(cpIRI);

				}
				return ap;	
			}
		}
		return null;
	}
	
	OWLDatatype lookUpDataType(String iri) {
		IRI cpIRI = IRI.create(iri);
		Set<OWLDatatype> d_types = ontology.getDatatypesInSignature();
		
		for (OWLDatatype d_t : d_types) {
			if (d_t.getIRI().getShortForm().equals(iri)) {
				return d_t;
			}
		}
		return null;
		
	}
	
	public OWLAnnotationProperty lookUpShort(String shortName) {
		
		if (annProps == null) {
			ontology = getOWLModelManager().getActiveOntology();
			initProperties();
		}
		// check again and log if annProps not initialized
		if (annProps != null) {
			for (OWLAnnotationProperty ap : annProps) {
				if (ap.getIRI().getShortForm().equals(shortName)) {				
					return ap;	
				}
			}
		} else {
			log.error("can't look up " + shortName +
					" because annProps aren't initialized");
		}
		return null;
		
	}
	
	public boolean supportsProperty(String shortPropName) {
		return lookUpShort(shortPropName) != null;
	}
	
	public boolean hasPropertyValue(OWLClass cls, String propName, String value) {
		OWLAnnotationProperty prop = lookUpShort(propName);
		List<String> values = getPropertyValues(cls, prop);
		return values.contains(value);		
	}
	
	public boolean checkHasType(String propName) {
		OWLAnnotationProperty prop = lookUpShort(propName);
		return (getDataType(prop) != null);
	}
	
	public boolean checkType(String propName, String value) {
		OWLAnnotationProperty prop = lookUpShort(propName);
		IRI type = getDataType(prop);
		if (type != null) {
			if (type.toString().endsWith("-enum")) {
				List<String> vals = getEnumValues(type);
				return vals.contains(value);			
			}
		}
		// TODO: flesh out with more types as requirements come in
		
		return true;		
	}
	
	public boolean checkAnyURIValue(String propName, String value) {
		OWLAnnotationProperty prop = lookUpShort(propName);
		IRI type = getDataType(prop);
		if (type != null) {
			
			if (type.getShortForm().equals("anyURI")) {
				IRI val = null;
				if (value.startsWith("http:")) {
					val = IRI.create(value);				
				} else {
					val = IRI.create(CODE_PROP.getIRI().getNamespace() + value);
				}
				return !ontology.getEntitiesInSignature(val).isEmpty();
							
			}
		}
		return true;		
	}
	
	public boolean isLastParent(OWLClass cls, OWLClass par) {
		Set<OWLClass> parents = getOWLEditorKit().getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider().getParents(cls);
		if (parents.size() == 1) {
			Iterator<OWLClass> itr = parents.iterator();
			OWLClass parent = (OWLClass)itr.next();
			if (parent.getIRI().equals(par.getIRI())) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	public boolean hasParent(OWLClass cls, OWLClass par_cls) {
		

		Set<OWLSubClassOfAxiom> sub_axioms = ontology.getSubClassAxiomsForSubClass(cls);

		for (OWLSubClassOfAxiom ax1 : sub_axioms) {
			OWLClassExpression exp = ax1.getSuperClass();

			if (getParent(exp, par_cls) != null) {
				return true;
			} 
		}

		// didn't find primitive parent, check defined

		Set<OWLEquivalentClassesAxiom> equiv_axioms = ontology.getEquivalentClassesAxioms(cls);

		for (OWLEquivalentClassesAxiom ax1 : equiv_axioms) {
			Set<OWLClassExpression> exps = ax1.getClassExpressions();
			for (OWLClassExpression exp : exps) {
				if (getParent(exp, par_cls) != null) {
					return true;
				}
			}
		}
		return false;	
	}
	
	private OWLClass getParent(OWLClassExpression exp, OWLClass par_cls) {
		
		OWLClass result = null;
		
    	if (exp instanceof OWLClass) {
    		if (exp.asOWLClass().equals(par_cls)) {
    			result = par_cls;
    		}
    		
    	} else if (exp instanceof OWLQuantifiedObjectRestriction) {
    		    		
    	} else if (exp instanceof OWLObjectIntersectionOf) {
    		OWLObjectIntersectionOf oio = (OWLObjectIntersectionOf) exp;
    		Set<OWLClassExpression> conjs = oio.asConjunctSet();
    		for (OWLClassExpression c : conjs) {
    			result = getParent(c, par_cls);
    		}
    	} else if (exp instanceof OWLObjectUnionOf) {
    		OWLObjectUnionOf oio = (OWLObjectUnionOf) exp;
    		Set<OWLClassExpression> conjs = oio.asDisjunctSet();
    		for (OWLClassExpression c : conjs) {
    			result = getParent(c, par_cls);
    		}
    	}
    	return result;
		
	}
	
	
	public boolean hasRole(OWLClass cls, String roleName, String mod, String filler) {

		Set<OWLSubClassOfAxiom> sub_axioms = ontology.getSubClassAxiomsForSubClass(cls);

		for (OWLSubClassOfAxiom ax1 : sub_axioms) {
			OWLClassExpression exp = ax1.getSuperClass();

			if (getRole(exp, roleName, mod, filler) != null) {
				return true;
			}        	
		}
		Set<OWLEquivalentClassesAxiom> equiv_axioms = ontology.getEquivalentClassesAxioms(cls);

		for (OWLEquivalentClassesAxiom ax1 : equiv_axioms) {
			Set<OWLClassExpression> exps = ax1.getClassExpressions();
			for (OWLClassExpression exp : exps) {
				if (getRole(exp, roleName, mod, filler) != null) {
					return true;
				}

			}
		}
		return false;	

	}
	
	private OWLQuantifiedObjectRestriction getRole(OWLClassExpression exp, String roleName,
			String modifier, String filler) {
		OWLQuantifiedObjectRestriction result = null;
    	if (exp instanceof OWLClass) {
    		
    	} else if (exp instanceof OWLQuantifiedObjectRestriction) {
    		
    		
    		OWLQuantifiedObjectRestriction qobj = (OWLQuantifiedObjectRestriction) exp;
    		
    		if (!qobj.getProperty().asOWLObjectProperty().getIRI().getShortForm().equalsIgnoreCase(roleName)) {
    			return null;
    		};
    		
    		OWLClassExpression rexp = qobj.getFiller();

    		if (rexp instanceof OWLClass) {
    			if (!((OWLClass) rexp).getIRI().getShortForm().equals(filler)) {
    				return null;
    			};
    		} else {
    			return null;
    		}

    		if ((exp instanceof OWLObjectSomeValuesFrom) &&
    				!modifier.equalsIgnoreCase("some")) {
    			return null;
    		} else if ((exp instanceof OWLObjectAllValuesFrom) &&
    				!modifier.equalsIgnoreCase("only")) {
    			return null;
    		}
    		
    		result = qobj;
    		
    	} else if (exp instanceof OWLObjectIntersectionOf) {
    		OWLObjectIntersectionOf oio = (OWLObjectIntersectionOf) exp;
    		Set<OWLClassExpression> conjs = oio.asConjunctSet();
    		for (OWLClassExpression c : conjs) {
    			result = getRole(c, roleName, modifier, filler);
    		}
    	} else if (exp instanceof OWLObjectUnionOf) {
    		OWLObjectUnionOf oio = (OWLObjectUnionOf) exp;
    		Set<OWLClassExpression> conjs = oio.asDisjunctSet();
    		for (OWLClassExpression c : conjs) {
    			result = getRole(c, roleName, modifier, filler);
    		}
    	}
    	return result;
		
	}
	
	public void removeParent(OWLClass cls, OWLClass par_cls) {
		ParentRemover par_rem = new ParentRemover(getOWLEditorKit().getModelManager());
		List<OWLOntologyChange> changes = par_rem.removeParent(cls, par_cls);
		
		if (inBatchMode) {
			batch_changes.addAll(changes);
			addBatchHistory(cls, this.getRDFSLabel(cls).get(), MODIFY);
		} else {
			getOWLEditorKit().getModelManager().applyChanges(changes);
		}

	}

	public void addParent(OWLClass cls, OWLClass par_cls) {
		OWLDataFactory df = getOWLEditorKit().getOWLModelManager().getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		
		OWLAxiom ax = df.getOWLSubClassOfAxiom(cls, par_cls);	
				
		changes.add(new AddAxiom(ontology, ax));
		
		if (inBatchMode) {
			batch_changes.addAll(changes);
			addBatchHistory(cls, this.getRDFSLabel(cls).get(), MODIFY);
		} else {
			getOWLEditorKit().getModelManager().applyChanges(changes);
		}

	}
	
	public void removeRole(OWLClass cls, String roleName, String modifier, String filler) {
		RoleReplacer role_rep = new RoleReplacer(getOWLEditorKit().getModelManager());
		List<OWLOntologyChange> changes = role_rep.removeRole(cls, roleName, modifier, filler);
		
		if (inBatchMode) {
			batch_changes.addAll(changes);
			addBatchHistory(cls, this.getRDFSLabel(cls).get(), MODIFY);
		} else {
			getOWLEditorKit().getModelManager().applyChanges(changes);
		}

	}

	public void addRole(OWLClass cls, String roleName, String modifier, String filler) {
		
		OWLDataFactory df = getOWLEditorKit().getOWLModelManager().getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		
		OWLObjectProperty o_p = getRoleShort(roleName);
		OWLClass fill_cls = getClass(filler);
		
		OWLClassExpression exp = null;
		
		if (modifier.equals("some")) {
			exp = df.getOWLObjectSomeValuesFrom(o_p, fill_cls);			
		} else {
			exp = df.getOWLObjectAllValuesFrom(o_p, fill_cls);			
		}
		
		OWLAxiom ax = df.getOWLSubClassOfAxiom(cls, exp);
		
		changes.add(new AddAxiom(ontology, ax));
		

		if (inBatchMode) {
			batch_changes.addAll(changes);
			addBatchHistory(cls, this.getRDFSLabel(cls).get(), MODIFY);
		} else {
			getOWLEditorKit().getModelManager().applyChanges(changes);
		}

	}
	
	public void modifyRole(OWLClass cls, String roleName, String modifier, String filler,
			String new_modifier, String new_filler) {
		
			// no type change
			RoleReplacer role_rep = new RoleReplacer(getOWLModelManager());
			List<OWLOntologyChange> changes = role_rep.modifyRole(cls, roleName, modifier, filler, 
					new_modifier, new_filler);
			
			if (inBatchMode) {
				batch_changes.addAll(changes);
				addBatchHistory(cls, this.getRDFSLabel(cls).get(), MODIFY);
			} else {
				getOWLEditorKit().getModelManager().applyChanges(changes);
			}
	}
	
	public void removeComplexAnnotationProperty(OWLClass cls, String propName, 
			String value, Map<String, String> annotations) {
		
		OWLDataFactory df = getOWLEditorKit().getOWLModelManager().getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		
		OWLAnnotationAssertionAxiom ann_ax = getComplexPropertyValueAssertion(cls, propName, value, annotations);
		
        changes.add(new RemoveAxiom(ontology, ann_ax));
		
		getOWLModelManager().applyChanges(changes);
		
	}
	
	public void addComplexAnnotationProperty(OWLClass cls, String propName, 
			String value, Map<String, String> annotations) {
		
		OWLDataFactory df = getOWLEditorKit().getOWLModelManager().getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		
		OWLAnnotationProperty complex_prop = lookUpShort(propName);
		
		OWLAxiom new_axiom = df.getOWLAnnotationAssertionAxiom(complex_prop, cls.getIRI(), 
				df.getOWLLiteral(value, OWL2Datatype.RDF_PLAIN_LITERAL));

		Set<OWLAnnotation> anns = new HashSet<OWLAnnotation>();
		Set<OWLAnnotationProperty> req_props = getConfiguredAnnotationsForAnnotation(complex_prop);

		Set<OWLAnnotationProperty> opt_props = new HashSet<OWLAnnotationProperty>();
		
		for (String as : annotations.keySet()) {
			OWLAnnotationProperty ap = this.lookUpShort(as);
			if (!req_props.contains(ap)) {
				opt_props.add(ap);
			}
			
		}
		for (OWLAnnotationProperty prop : req_props) {
			String val = annotations.get(prop.getIRI().getShortForm());
			if (val != null) {
				OWLAnnotation new_ann = df.getOWLAnnotation(prop, 
						df.getOWLLiteral(val, OWL2Datatype.RDF_PLAIN_LITERAL));
				anns.add(new_ann);
			} else if (is_required(prop)) {
				String def_val = getDefaultValue(getDataType(prop), "");
				OWLAnnotation new_ann = df.getOWLAnnotation(prop, 
						df.getOWLLiteral(def_val, OWL2Datatype.RDF_PLAIN_LITERAL));
				anns.add(new_ann);				
			}
		}
		
		for (OWLAnnotationProperty prop : opt_props) {
			String val = annotations.get(prop.getIRI().getShortForm());
			if (val != null) {
				OWLAnnotation new_ann = df.getOWLAnnotation(prop, 
						df.getOWLLiteral(val, OWL2Datatype.RDF_PLAIN_LITERAL));
				anns.add(new_ann);
			}
		}

		OWLAxiom new_new_axiom = new_axiom.getAxiomWithoutAnnotations().getAnnotatedAxiom(anns);

		changes.add(new AddAxiom(ontology, new_new_axiom));
		
		getOWLModelManager().applyChanges(changes);

	}
	
	public List<String> getRequiredQualifiers(String prop_iri) {
		OWLAnnotationProperty prop = this.lookUpShort(prop_iri);
		Set<OWLAnnotationProperty> req_props = getRequiredAnnotationsForAnnotation(prop);
		List<String> res = new ArrayList<String>();
		for (OWLAnnotationProperty p : req_props) {
			if (isRequired(p)) {
				res.add(p.getIRI().getShortForm());
			}
		}
		return res;
	}
	
	private boolean isRequired(OWLAnnotationProperty prop) {
		OWLAnnotationProperty defawlt = this.lookUpShort("required");
		Optional<String> def_prop = this.getPropertyValue(prop, defawlt);
		if (def_prop.isPresent()) {
			return (def_prop.get().equals("true"));

		}
		return false;
	}
	
	public boolean hasComplexPropertyValue(OWLClass cls, String propName, String value, Map<String, String> annotations) {
		
		return (getComplexPropertyValueAssertion(cls, propName, 
				value, annotations) != null);
		
	}
	
	public OWLAnnotationAssertionAxiom getComplexPropertyValueAssertion(OWLClass cls, String propName, String value, Map<String, String> annotations) {
		OWLAnnotationProperty prop = lookUpShort(propName);
		boolean found = false;
		for (OWLAnnotationAssertionAxiom ann_ax : ontology.getAnnotationAssertionAxioms(cls.getIRI())) {
			OWLAnnotation ann = ann_ax.getAnnotation();
			if (ann.getProperty().equals(prop)) {
				OWLAnnotationValue av = ann.getValue();
				Optional<OWLLiteral> ol = av.asLiteral();
				if (ol.isPresent()) {
					if (ol.get().getLiteral().equals(value)) {
						// we have a possible
						found = true;
						Set<OWLAnnotation> quals = ann_ax.getAnnotations();
						for (OWLAnnotation q_a : quals) {
							String name = q_a.getProperty().getIRI().getShortForm();
							String val = q_a.getValue().asLiteral().get().getLiteral();
							if (annotations.containsKey(name)) {
								if (val.equals(annotations.get(name))) {
									// ok
								} else {
									found = false;
								}
							}
						}
						if (found) {
							return ann_ax;
						}
					}
				}
			}
		}
		return null;
	}
		
	
	private boolean topOrBot(OWLNamedObject obj) {
		if (getOWLEditorKit().getOWLModelManager().getOWLDataFactory().getOWLThing().equals(obj) ||
				getOWLEditorKit().getOWLModelManager().getOWLDataFactory().getOWLNothing().equals(obj)) {
			return true;
		}
		return false;
	}
	
	public Optional<String> getRDFSLabel(OWLNamedObject oobj) {
		// TODO: fall back to IRI if no label
		if (oobj == null) {
			return Optional.empty();
		}
		if (topOrBot(oobj)) {
			return Optional.of(oobj.getIRI().getShortForm());			
		}
		if (ontology != null) {
			Set<OWLAnnotation> axiomanns = annotationObjects(ontology.annotationAssertionAxioms(oobj.getIRI()),
					ontology.getOWLOntologyManager().getOWLDataFactory()
					.getRDFSLabel()).
					collect(Collectors.toSet());
			if (axiomanns.isEmpty()) {
				return Optional.of("");
			}
			for (OWLAnnotation annotation : axiomanns) {
				OWLAnnotationValue av = annotation.getValue();
				Optional<OWLLiteral> ol = av.asLiteral();
				if (ol.isPresent()) {
					return Optional.of(ol.get().getLiteral());
				}
			}
		}
        
		if (!topOrBot(oobj)) {

			JOptionPane.showMessageDialog(this, oobj.getIRI().getShortForm() + " requires an rdfs:label, using IRI short form instead",
					"Warning", JOptionPane.WARNING_MESSAGE);
		}
		
		return Optional.of(oobj.getIRI().getShortForm());


	}

	public Optional<String> getCode(OWLNamedObject oobj) {
		// TODO: fall back to IRI if no label
		if (oobj != null) {
			Set<OWLAnnotationAssertionAxiom> axioms = ontology.annotationAssertionAxioms(oobj.getIRI()).
					collect(Collectors.toSet());
			if (axioms.isEmpty()) {
				return Optional.of("");
			}
			for (OWLAnnotation annotation : annotationObjects(ontology.annotationAssertionAxioms(oobj.getIRI()), NCIEditTabConstants.CODE_PROP).
					collect(Collectors.toSet())) {
				OWLAnnotationValue av = annotation.getValue();
				Optional<OWLLiteral> ol = av.asLiteral();
				if (ol.isPresent()) {
					return Optional.of(ol.get().getLiteral());
				}
			}

			if (!topOrBot(oobj)) {

				JOptionPane.showMessageDialog(this, oobj.getIRI().getShortForm() + " should have a code property, using IRI short form instead",
						"Warning", JOptionPane.WARNING_MESSAGE);
			}
			return Optional.of(oobj.getIRI().getShortForm());
		} else {
			return Optional.empty();
		}
	}
	
	public Optional<String> getCode(OWLNamedObject oobj, OWLOntology ont) {
		// TODO: fall back to IRI if no label
		if (oobj != null) {
			Set<OWLAnnotationAssertionAxiom> axioms = ont.annotationAssertionAxioms(oobj.getIRI()).
					collect(Collectors.toSet());
			if (axioms.isEmpty()) {
				return Optional.of("");
			}
			for (OWLAnnotation annotation : annotationObjects(ont.annotationAssertionAxioms(oobj.getIRI()), NCIEditTabConstants.CODE_PROP).
					collect(Collectors.toSet())) {
				OWLAnnotationValue av = annotation.getValue();
				Optional<OWLLiteral> ol = av.asLiteral();
				if (ol.isPresent()) {
					return Optional.of(ol.get().getLiteral());
				}
			}

			if (!topOrBot(oobj)) {

				JOptionPane.showMessageDialog(this, oobj.getIRI().getShortForm() + " should have a code property, using IRI short form instead",
						"Warning", JOptionPane.WARNING_MESSAGE);
			}
			return Optional.of(oobj.getIRI().getShortForm());
		} else {
			return Optional.empty();
		}
	}
	
	public Optional<OWLAnnotationValue> getSemanticType(OWLClass cls) {
		
		for (OWLAnnotation annotation : annotationObjects(ontology.annotationAssertionAxioms(cls.getIRI()), SEMANTIC_TYPE).
				collect(Collectors.toSet())) {
			OWLAnnotationValue av = annotation.getValue();
			return Optional.of(av);
			
		}
		return Optional.empty();		

	}
	
	public OWLAnnotation createAnnotation(String name, String value) {
		
		OWLDataFactory df = getOWLModelManager().getOWLDataFactory();		
        return df.getOWLAnnotation(tab.lookUpShort(name), df.getOWLLiteral(value));
		
	}
	
	public Set<OWLAnnotation> getAnnotations(OWLClass cls) {
		Set<OWLAnnotation> res = new HashSet<OWLAnnotation>();

		Set<OWLAnnotationAssertionAxiom> strs = EntitySearcher.getAnnotationAssertionAxioms(cls, ontology).
				collect(Collectors.toSet());
		
		for (OWLAnnotationAssertionAxiom ax : strs) {
			res.add(ax.getAnnotation());
		}
		
		return res;			
	}
	
	public Set<OWLAnnotation> getDependentAnnotations(OWLClass cls, OWLAnnotationProperty prop) {
		
		Set<OWLAnnotationAssertionAxiom> strs = EntitySearcher.getAnnotationAssertionAxioms(cls, ontology).
				collect(Collectors.toSet());
		
		for (OWLAnnotationAssertionAxiom ax : strs) {
			OWLAnnotation annot = ax.getAnnotation();
			if (annot.getProperty().equals(prop)) {
				return ax.getAnnotations();				
			}
		}
		return new HashSet<OWLAnnotation>();
	}
	
	// TODO: Need an all props here
	
	public List<String> getPropertyValues(OWLNamedObject oobj, OWLAnnotationProperty prop) {
		
		List<String> values = new ArrayList<String>();
		
		Set<OWLAnnotation> sowl = annotationObjects(ontology.annotationAssertionAxioms(oobj.getIRI()), prop).
				collect(Collectors.toSet());
		  
		for (OWLAnnotation annotation : sowl) {
			OWLAnnotationValue av = annotation.getValue();
			if (av instanceof IRI) {
				values.add(av.toString());
			} else {
				Optional<OWLLiteral> ol = av.asLiteral();
				if (ol.isPresent()) {
					values.add(ol.get().getLiteral());
				} 
			}
		}
		
		return values;		  
		  
	}
	
	public OWLLiteral getPropertyValueLiteral(OWLNamedObject oobj, OWLAnnotationProperty prop, String value) {
		 
		for (OWLAnnotationAssertionAxiom ax : ontology.getAnnotationAssertionAxioms(oobj.getIRI())) {
			if (ax.getProperty().equals(prop)) {
				Optional<OWLLiteral> ol = ax.getAnnotation().getValue().asLiteral();
				if (ol.isPresent()) {
					if (ol.get().getLiteral().equals(value)) {
						return ol.get();
					}
				}
				
			}
		}
		return null;		  
	}
	
	public Optional<String> getPropertyValue(OWLNamedObject oobj, OWLAnnotationProperty prop) {
		  
		for (OWLAnnotation annotation : annotationObjects(ontology.annotationAssertionAxioms(oobj.getIRI()), prop).
				collect(Collectors.toSet())) {
			OWLAnnotationValue av = annotation.getValue();
		    Optional<OWLLiteral> ol = av.asLiteral();
		    if (ol.isPresent()) {
		     return Optional.of(ol.get().getLiteral());
		     
		    }   
		}
		
		return Optional.empty();		  
		  
	}
	
	public void addAnnotationToClass(OWLClass ocl, OWLAnnotationProperty prop, String value) {
		
		value = mapper.fix(value);
		
		OWLDataFactory df = getOWLEditorKit().getOWLModelManager().getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		
		OWLAxiom ax;
		
		IRI type = this.getDataType(prop);
		if ((type != null) && type.getShortForm().equals("anyURI")) {
			IRI val;
			if (value.startsWith("http:")) {
				val = IRI.create(value);				
			} else {
				val = IRI.create(CODE_PROP.getIRI().getNamespace() + value);
			}
			
			ax = df.getOWLAnnotationAssertionAxiom(prop, ocl.getIRI(), val);
		} else {
			ax = df.getOWLAnnotationAssertionAxiom(prop, ocl.getIRI(), df.getOWLLiteral(value,
					OWL2Datatype.RDF_PLAIN_LITERAL));
			
		} 
		
		changes.add(new AddAxiom(ontology, ax));
		
		if (prop.equals(NCIEditTabConstants.PREF_NAME)) {
			PropertyCheckUtil propCheckUtil = new PropertyCheckUtil();
			propCheckUtil.syncPrefNameLabelFullSyn(ocl, value, changes);
		}
		
		if (inBatchMode) {
			batch_changes.addAll(changes);
			this.addBatchHistory(ocl, this.getRDFSLabel(ocl).get(), MODIFY);
		} else {
			getOWLEditorKit().getModelManager().applyChanges(changes);
		}
		
	}
	
	public void removeAnnotationFromClass(OWLClass ocl, OWLAnnotationProperty prop, String value) {
		
		OWLDataFactory df = getOWLEditorKit().getOWLModelManager().getOWLDataFactory();
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		
	
		
		OWLAxiom ax = null;
		
		IRI dtyp = this.getDataType(prop);
		if (dtyp.getShortForm().equals("anyURI")) {
			ax = df.getOWLAnnotationAssertionAxiom(prop, ocl.getIRI(), IRI.create(value));			
		} else {
			OWLLiteral ol = getPropertyValueLiteral(ocl, prop, value);
			if (ol != null) {
				ax = df.getOWLAnnotationAssertionAxiom(prop, ocl.getIRI(), ol);
			} else {
				ax = df.getOWLAnnotationAssertionAxiom(prop, ocl.getIRI(),
						df.getOWLLiteral(value, OWL2Datatype.RDF_PLAIN_LITERAL));
				
			}
		}
		
		changes.add(new RemoveAxiom(ontology, ax));
		
		if (inBatchMode) {
			batch_changes.addAll(changes);
			addBatchHistory(ocl, this.getRDFSLabel(ocl).get(), MODIFY);
		} else {
			getOWLEditorKit().getModelManager().applyChanges(changes);
		}
		
	}
	
	public OWLClass getIRIClass(String code) {

		if (OWLRendererPreferences.getInstance().useIriInBatchEdits()) {
			PrefixDocumentFormat prefixManager = PrefixUtilities.getPrefixOWLOntologyFormat(ontology);

			for (Map.Entry<String, String> prefixName2PrefixEntry : prefixManager.getPrefixName2PrefixMap()
					.entrySet()) {

				String prefix = prefixName2PrefixEntry.getValue();

				IRI check = IRI.create(prefix);
				System.out.println(check);

			}
		}

		OWLClass cls = null;

		IRI iri;
		if (code.startsWith("http:")) {
			iri = IRI.create(code);
		} else {
			iri = IRI.create(CODE_PROP.getIRI().getNamespace() + code);
		}

		Set<OWLEntity> classes = ontology.getEntitiesInSignature(iri);
		for (OWLEntity et : classes) {
			cls = et.asOWLClass();
		}

		return cls;
	}
	
	public OWLClass getClass(String code) {
		
		OWLClass cls = null;
		
		IRI iri;
		if (code.startsWith("http:")) {
			iri = IRI.create(code);			
		} else {
			iri = IRI.create(CODE_PROP.getIRI().getNamespace() + code);			
		}
		
		Set<OWLEntity> classes = ontology.getEntitiesInSignature(iri);
		for (OWLEntity et : classes) {
			cls = et.asOWLClass();
		}
		
		return cls;		
	}
	
	public IRI getDataType(OWLAnnotationProperty prop) {
		Set<OWLAnnotationPropertyRangeAxiom> types = ontology.getAnnotationPropertyRangeAxioms(prop);
		
		for (OWLAnnotationPropertyRangeAxiom ax : types) {
			return ax.getRange();
		}
		return null;
	}
	
	public String getDefaultValue(IRI iri, String source) {
		String type = iri.getShortForm();
		if (type.equalsIgnoreCase("date-time-system")) {
			return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
		} else if (type.equalsIgnoreCase("user-system")) {
			return clientSession.getActiveClient().getUserInfo().getName().toString();
		} else if (type.endsWith("enum")) {
			return getDefault(lookUpDataType(type), source);
		}
		return "";
	}
	
	public List<String> getEnumValues(IRI enumtype) {

		List<String> results = new ArrayList<String>();
		OWLDatatype dt = getOWLModelManager().getOWLDataFactory().getOWLDatatype(enumtype);		
		Set<OWLDatatypeDefinitionAxiom> dda = getOWLModelManager().getActiveOntology().getDatatypeDefinitions(dt);

		for (OWLDatatypeDefinitionAxiom ax : dda) {
			OWLDataOneOf done = (OWLDataOneOf) ax.getDataRange();
			Set<OWLLiteral> vals = done.getValues();
			for (OWLLiteral lit : vals) {
				results.add(lit.getLiteral());
			}

		}
		return results;
	}
	
	private List<OWLOntologyChange> duplicateClassAxioms(OWLClass selectedClass, OWLObjectDuplicator dup) {
		List<OWLOntologyChange> changes = new ArrayList<>();

		OWLOntology ont = getOWLModelManager().getActiveOntology();

		for (OWLAxiom ax : ont.getAxioms(selectedClass)) {
			if (ax.isLogicalAxiom() && !(ax instanceof OWLDisjointClassesAxiom)) {
				OWLAxiom duplicatedAxiom = dup.duplicateObject(ax);
				if (duplicatedAxiom instanceof OWLSubClassOfAxiom) {
					OWLSubClassOfAxiom osa = (OWLSubClassOfAxiom) duplicatedAxiom;
					if (osa.getSubClass().equals(osa.getSuperClass())) {
						// don't add class as a parent of self
					} else {
						changes.add(new AddAxiom(ont, duplicatedAxiom));
					}
				} else {
					changes.add(new AddAxiom(ont, duplicatedAxiom));
				}
			}
		}

		return changes;
	}

    private List<OWLOntologyChange> duplicateAnnotations(OWLClass selectedClass, OWLObjectDuplicator dup) {
        List<OWLOntologyChange> changes = new ArrayList<>();
        OWLModelManagerEntityRenderer ren = getOWLModelManager().getOWLEntityRenderer();
        List<IRI> annotIRIs = null;
        String selectedClassName = null;
        LiteralExtractor literalExtractor = new LiteralExtractor();
        if (ren instanceof OWLEntityAnnotationValueRenderer){
            selectedClassName = getOWLModelManager().getRendering(selectedClass);
            annotIRIs = OWLRendererPreferences.getInstance().getAnnotationIRIs();
        }

        

        OWLOntology ont = getOWLModelManager().getActiveOntology();
        
        for (OWLAnnotationAssertionAxiom ax : EntitySearcher.getAnnotationAssertionAxioms(selectedClass, ont).
        		collect(Collectors.toSet())) {
        	final OWLAnnotation annot = ax.getAnnotation();
        	if (annotIRIs == null || !annotIRIs.contains(annot.getProperty().getIRI())) {
        		if (okToCopy(annot.getProperty())) {
        			String label = literalExtractor.getLiteral(annot.getValue());
        			if (label == null || !label.equals(selectedClassName)){
        				OWLAxiom duplicatedAxiom = dup.duplicateObject(ax);
        				changes.add(new AddAxiom(ont, duplicatedAxiom));
        			} else if (label.equals(selectedClassName) &&
        					annot.getProperty().equals(this.getFullSyn())) {
        				
        			} else {
        				OWLAxiom duplicatedAxiom = dup.duplicateObject(ax);
        				changes.add(new AddAxiom(ont, duplicatedAxiom));
        			}
        		}
        	}
        }
        
        return changes;
    }
    
    private boolean okToCopy(OWLAnnotationProperty prop) {
    	return !(prop.equals(CODE_PROP) ||
    			 prop.equals(LABEL_PROP) ||
    			 prop.equals(PREF_NAME));
    }


    class LiteralExtractor implements OWLAnnotationValueVisitor {

        private String label;

        public String getLiteral(OWLAnnotationValue value){
            label = null;
            value.accept(this);
            return label;
        }

        public void visit(IRI iri) {
            // do nothing
        }


        public void visit(OWLAnonymousIndividual owlAnonymousIndividual) {
            // do nothing
        }


        public void visit(OWLLiteral literal) {
            label = literal.getLiteral();
        }
    }


	
    public boolean complexPropOp(String operation, OWLClass cls, OWLAnnotationProperty complex_prop, 
    		OWLAnnotationAssertionAxiom old_axiom, HashMap<String, String> ann_vals) {
    	
    	if (isRestricted(cls, complex_prop)) {
			JOptionPane.showMessageDialog(this, "Property " + complex_prop.getIRI().getShortForm() +
					 " is restricted to certain editors", "Warning", JOptionPane.WARNING_MESSAGE);
			return false; 
		}
    	List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
    	OWLDataFactory df = getOWLModelManager().getOWLDataFactory();
    	if (operation.equalsIgnoreCase(NCIEditTabConstants.EDIT) ||
    			operation.equalsIgnoreCase(NCIEditTabConstants.ADD)) {
    		if (complex_prop.equals(getFullSyn()) &&
    				!NCIEditTab.currentTab().validPrefName(ann_vals.get("Value"))) {
    			JOptionPane.showMessageDialog(this, "Preferred name cannot contain special characters, ! or ?", "Warning", JOptionPane.WARNING_MESSAGE);
    			return false; 
    		}
    		
    	}

    	if (operation.equalsIgnoreCase(NCIEditTabConstants.EDIT)) {

    		Set<OWLAnnotation> anns = old_axiom.getAnnotations();
    		Set<OWLAnnotation> new_anns = new HashSet<OWLAnnotation>(); 


    		for (OWLAnnotation annax : anns) {
    			String cv = annax.getProperty().getIRI().getShortForm();
    			String new_val = ann_vals.get(cv);
    			if (new_val != null && !new_val.isEmpty()) {
    				
    				if (containsAsciiLessThan32(new_val)) {
    					JOptionPane.showMessageDialog(this, "Value cannot contain special characters", "Warning", JOptionPane.WARNING_MESSAGE);
    					return false; 
    				}

    				OWLAnnotation new_ann = df.getOWLAnnotation(annax.getProperty(), 
    						df.getOWLLiteral(mapper.fix(new_val), OWL2Datatype.RDF_PLAIN_LITERAL));
    				new_anns.add(new_ann); 
    			} 
    		}


    		Set<OWLAnnotationProperty> req_props = this.getConfiguredAnnotationsForAnnotation(complex_prop);


    		if(req_props != null) {
	    		for (OWLAnnotationProperty prop : req_props) {
	    			String new_val = ann_vals.get(prop.getIRI().getShortForm());
	
	    			if (new_val != null && !new_val.isEmpty()) {
	    				if (containsAsciiLessThan32(new_val)) {
	    					JOptionPane.showMessageDialog(this, "Value cannot contain special characters", "Warning", JOptionPane.WARNING_MESSAGE);
	    					return false; 
	    				}
	    				OWLAnnotation new_ann = df.getOWLAnnotation(prop, 
	    						df.getOWLLiteral(mapper.fix(new_val),OWL2Datatype.RDF_PLAIN_LITERAL));
	    				new_anns.add(new_ann);
	
	    			} else {
	    				if (is_required(prop)) {
	    					JOptionPane.showMessageDialog(this, "Complex property missing required qualifier " +
	    							prop.getIRI().getShortForm(), "Warning", JOptionPane.WARNING_MESSAGE);
	    					return false; 
	    				}
	    			}
	    		}


    		}
    		
    		if (containsAsciiLessThan32(ann_vals.get("Value"))) {
				JOptionPane.showMessageDialog(this, "Value cannot contain special characters", "Warning", JOptionPane.WARNING_MESSAGE);
				return false; 
			}

    		OWLAxiom new_axiom = df.getOWLAnnotationAssertionAxiom(old_axiom.getProperty(), cls.getIRI(),
    				df.getOWLLiteral(mapper.fix(ann_vals.get("Value")), OWL2Datatype.RDF_PLAIN_LITERAL), new_anns);


    		changes.add(new RemoveAxiom(ontology, old_axiom));
    		changes.add(new AddAxiom(ontology, new_axiom));
    	} else if (operation.equalsIgnoreCase(NCIEditTabConstants.DELETE)) {
    		changes.add(new RemoveAxiom(ontology, old_axiom));

    	} else if (operation.equalsIgnoreCase(NCIEditTabConstants.ADD)) {
    		if (containsAsciiLessThan32(ann_vals.get("Value"))) {
				JOptionPane.showMessageDialog(this, "Value cannot contain special characters", "Warning", JOptionPane.WARNING_MESSAGE);
				return false; 
			}
    		OWLAxiom new_axiom = df.getOWLAnnotationAssertionAxiom(complex_prop, cls.getIRI(), 
    				df.getOWLLiteral(mapper.fix(ann_vals.get("Value")), OWL2Datatype.RDF_PLAIN_LITERAL));

    		Set<OWLAnnotation> anns = new HashSet<OWLAnnotation>();
    		Set<OWLAnnotationProperty> req_props = getConfiguredAnnotationsForAnnotation(complex_prop);

    		for (OWLAnnotationProperty prop : req_props) {
    			String val = ann_vals.get(prop.getIRI().getShortForm());
    			if (val != null && !val.isEmpty()) {
    				if (containsAsciiLessThan32(val)) {
    					JOptionPane.showMessageDialog(this, "Value cannot contain special characters", "Warning", JOptionPane.WARNING_MESSAGE);
    					return false; 
    				}
    				OWLAnnotation new_ann = df.getOWLAnnotation(prop, 
    						df.getOWLLiteral(mapper.fix(val), OWL2Datatype.RDF_PLAIN_LITERAL));
    				anns.add(new_ann);

    			}  else {
    				if (is_required(prop)) {
    					JOptionPane.showMessageDialog(this, "Complex property missing required qualifier " +
    							prop.getIRI().getShortForm(), "Warning", JOptionPane.WARNING_MESSAGE);
    					return false; 
    				}
    			}
    		}

    		OWLAxiom new_new_axiom = new_axiom.getAxiomWithoutAnnotations().getAnnotatedAxiom(anns);


    		changes.add(new AddAxiom(ontology, new_new_axiom));

    	}
    	getOWLModelManager().applyChanges(changes);
    	
    	
    	return true;
    	
    	
    }
	
	// Methods needed by BatchEditTask
	
	
	public Set<String> getSupportedRoles() {
		Set<OWLObjectProperty> o_props = ontology.getObjectPropertiesInSignature();
		Set<String> res = new HashSet<String>();
		for (OWLObjectProperty op : o_props) {
			res.add(op.getIRI().getShortForm());
		}
		
		return res;
	}
	
	public OWLObjectProperty getRoleShort(String roleName) {
		Set<OWLObjectProperty> o_props = ontology.getObjectPropertiesInSignature();
		
		for (OWLObjectProperty op : o_props) {
			if (op.getIRI().getShortForm().equals(roleName)) {
				return op;
			}
		}
		
		return null;
	}
	
	public Set<String> getSupportedAssociations() {
		return null;
	}
	
	public List<OWLClass> getDirectSubClasses(OWLClass cls) {
		ChildClassExtractor childClassExtractor = new ChildClassExtractor();
		
		childClassExtractor.setCurrentParentClass(cls);
		for (OWLAxiom ax : ontology.getReferencingAxioms(cls)) {
            if (ax.isLogicalAxiom()) {
                ax.accept(childClassExtractor);
            }
        }
        Set<OWLClass> cset = childClassExtractor.getResult();
       
		List<OWLClass> res = new ArrayList<OWLClass>();
		
		for (OWLClass c : cset) {
			res.add(c);
		}
		
		return res;
	}
	
	public List<OWLClass> getDirectSuperClasses(OWLClass cls) {
		Set<OWLSubClassOfAxiom> sups = ontology.getSubClassAxiomsForSubClass(cls);
		List<OWLClass> res = new ArrayList<OWLClass>();
		for (OWLSubClassOfAxiom ax : sups) {
			if (!ax.getSuperClass().isAnonymous()) {
				res.add(ax.getSuperClass().asOWLClass());
			}
		}
		return res;
	}
	
	
	Set<String> getLogicalRes(OWLClass cls, String type) {
		Set<String> res = new HashSet<String>();
		Set<OWLSubClassOfAxiom> sub_axioms = ontology.getSubClassAxiomsForSubClass(cls);

		for (OWLSubClassOfAxiom ax1 : sub_axioms) {
			OWLClassExpression exp = ax1.getSuperClass();
			res = getParentRoleExps(res, exp, type, false, cls);

		}

		Set<OWLEquivalentClassesAxiom> equiv_axioms = ontology.getEquivalentClassesAxioms(cls);

		for (OWLEquivalentClassesAxiom ax1 : equiv_axioms) {
			Set<OWLClassExpression> exps = ax1.getClassExpressions();
			for (OWLClassExpression exp : exps) {
				res = getParentRoleExps(res, exp, type, true, cls);
			}
		}
		return res;

	}
	
	private boolean isParentType(String type) {
		return type.equalsIgnoreCase("parents");
	}
	
	private boolean isRoleType(String type) {
		return type.equalsIgnoreCase("roles");
	}
	
    
    private Set<String> getParentRoleExps(Set<String> res, OWLClassExpression exp, String type,
    		boolean defined, OWLClass cls) {
    	
    	if ((exp instanceof OWLClass) && (isParentType(type)) &&
    			!(exp.asOWLClass().equals(cls))) {
    		Optional<String> label = getRDFSLabel(exp.asOWLClass());
    		if (label.isPresent()) {
    			res.add(label.get());
    		} else {
    			res.add(exp.asOWLClass().getIRI().getShortForm());
    		}
    	} else if ((exp instanceof OWLQuantifiedObjectRestriction) &&
    			(isRoleType(type))) {

    		OWLQuantifiedObjectRestriction qobj = (OWLQuantifiedObjectRestriction) exp;
    		OWLClassExpression rexp = qobj.getFiller();

    		String fval;
    		if (rexp instanceof OWLClass) {
    			fval = ((OWLClass) rexp).getIRI().getShortForm();
    		} else {
    			fval = rexp.toString();
    		}

    		String quant = "some";
    		if (exp instanceof OWLObjectSomeValuesFrom) {
    			quant = "some";
    		} else if (exp instanceof OWLObjectAllValuesFrom) {
    			quant = "only";
    		}
    		String val = quant + " " + qobj.getProperty().asOWLObjectProperty().getIRI().getShortForm() + " "
    				+ fval;
    		
    		if (defined) {
    			val += " [defined]";
    		}

    		res.add(val);

    	} else if (exp instanceof OWLObjectIntersectionOf) {
    		OWLObjectIntersectionOf oio = (OWLObjectIntersectionOf) exp;
    		Set<OWLClassExpression> conjs = oio.asConjunctSet();
    		for (OWLClassExpression c : conjs) {
    			res = getParentRoleExps(res, c, type, defined, cls);
    		}
    	} else if (exp instanceof OWLObjectUnionOf) {
    		OWLObjectUnionOf oio = (OWLObjectUnionOf) exp;
    		Set<OWLClassExpression> conjs = oio.asDisjunctSet();
    		for (OWLClassExpression c : conjs) {
    			res = getParentRoleExps(res, c, type, defined, cls);
    		}
    	}
    	return res;

    }
    
    String getAnnotationValue(OWLAnnotationAssertionAxiom axiom, String annProp) {
    	Set<OWLAnnotation> anns = axiom.getAnnotations();
    	for (OWLAnnotation ann : anns) {
    		if (ann.getProperty().getIRI().getShortForm().equalsIgnoreCase(annProp)) {
    			return ann.getValue().asLiteral().get().getLiteral();
    		}
    	}
    	return "None";   	
    	
    }
    
    
    
    public OWLAnnotationProperty getFullSyn() {
    	
		return FULL_SYN;
    	
    }
    
    public OWLAnnotationProperty getDefinition() {
    	
		return DEFINITION;
    	
    }

    public OWLAnnotationProperty getPreferredName() {
    	
		return PREF_NAME;
    	
    }
    
    public boolean containsAsciiLessThan32(String name) {
    	for (int index = 0; index < name.length(); index++)
		{
			int ch = (int) name.charAt(index);
			
			if (ch < 32) {
				return true;
			}			
		}
    	return false;    
    }
    
    public boolean validPrefName(String name) {
    	if (containsAsciiLessThan32(name)) {
    		return false;
    	}
    	if (name.contains("?") || name.contains("!")) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    public boolean existsPrefName(String name) {
    	SearchTabManager searchManager = (SearchTabManager) getOWLEditorKit().getSearchManager();
        
        BasicQuery.Factory queryFactory = new BasicQuery.Factory(new SearchContext(getOWLEditorKit()), searchManager);
        
        
        OWLProperty property = NCIEditTabConstants.LABEL_PROP;
        QueryType queryType = QueryType.EXACT_MATCH_STRING;
        String value = name;
        
        BasicQuery basicQuery = queryFactory.createQuery(property, queryType, value);
        
        FilteredQuery.Builder builder = new FilteredQuery.Builder();
        builder.add(basicQuery);
        FilteredQuery userQuery = builder.build(true);
        
        class MySearchTabResultHandler implements SearchTabResultHandler {
        	private boolean exists = false;
        	
        	private boolean ready = false;
        	
        	private OWLModelManager mgr;
        	
        	private String searchString;
        	
        	public MySearchTabResultHandler(OWLEditorKit kit, String str) {
        		mgr = kit.getModelManager();
        		searchString = str;
        	}

			public void searchFinished(Collection<OWLEntity> searchResults) {
				OWLEntityFinder finder = mgr.getOWLEntityFinder();
	        	Set<OWLEntity> foundEntities = new HashSet<OWLEntity>();
				Set<OWLEntity> ents = finder.getMatchingOWLEntities(searchString);
        		for (OWLEntity ent : ents) {
        			String cs = mgr.getRendering(ent);
        			String ucs = unescape(cs);
        			if (ucs.toLowerCase().equals(searchString.toLowerCase())) {
        				foundEntities.add(ent);
        			}
        		}

				exists = !foundEntities.isEmpty();
				ready = true;
			}
			
			public boolean exists() { return exists; }
			public boolean ready() { return ready; }
        	
        };
        
        MySearchTabResultHandler srh = new MySearchTabResultHandler(getOWLEditorKit(), name);
        
        searchManager.performSearch(userQuery, srh);
        
        while (!srh.ready()) {
        	try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        
    	return srh.exists(); 
    }
    
    private String unescape(String s) {
    	if (s.startsWith("'") &&
    			s.endsWith("'")) {
    		return s.substring(1, s.length() - 1);
    	}
    	return s;
    }
    
    public void submitHistory() {
    	if ((current_op.isRetiring() || current_op.isMerging()) &&
    			(isWorkFlowModeler() && !isWorkFlowManager())) {
    		// don't record merges and retirements until approved
    		return;
    	}
    	if (current_op.isRetiring()) {
    		submitRetireHistory();
    	} else if (current_op.isCloning() || current_op.isSplitting() ||
    			current_op.isMerging() || current_op.isDual()) {
    		submitComplexHistory();
    	} else {
    		submitEditHistory();
    	}
    }
    
    
    public void submitComplexHistory() {
    	
    	OWLClass cls = getCurrentOp().getSource();
		OWLClass ref_cls = getCurrentOp().getTarget();
    	
    	String c = getCodeOrIRI(cls);
    	
    	String n = getRDFSLabel(cls).get();
    	String op = getCurrentOp().toString();
    	
    	String ref = getCodeOrIRI(ref_cls);
    	
    	String ref_n = getRDFSLabel(ref_cls).get();
    	if (current_op.isSplitting()) {
    		putHistory(c, n, op, c);
    		putHistory(c, n, op, ref);
    		putHistory(ref, ref_n, ComplexEditType.CREATE.toString(), "");    		
    	} else if (current_op.isCloning()) {
    		putHistory(ref, ref_n, ComplexEditType.CREATE.toString(), "");    		
    	} else if (current_op.isMerging()) {
    		putHistory(ref, ref_n, op, ref);
    		putHistory(c, n, op, ref);
    		putHistory(c, n, ComplexEditType.RETIRE.toString(), "");    		
    	} else if (current_op.isDual()) {
    		putHistory(c, n, ComplexEditType.MODIFY.toString(), "");
    		putHistory(ref, ref_n, ComplexEditType.MODIFY.toString(), "");    		
    	}
    	
    }
    
    List<String> getRetiredParents(OWLClass cls) {
    	return this.getPropertyValues(cls, DEP_PARENT);
    }
    
    public void submitRetireHistory() {
    	OWLClass cls = current_op.getRetireClass();

    	String c = getCodeOrIRI(cls);

    	String n = getRDFSLabel(cls).get();
    	String op = getCurrentOp().toString();
    	
    	if (!getRetiredParents(cls).isEmpty()) {
    		for (String s : getRetiredParents(cls)) {
    			String ref = getCodeOrIRI(getClass(s));
    			putHistory(c, n, op, ref);
    		}
    	} else {
    		putHistory(c, n, op, "ROOT");
    	}
    }
    
    
    public void submitEditHistory() {
    	OWLClass cls = current_op.getCurrentlyEditing();
    	
    	String c = getCodeOrIRI(cls);
    	
    	String n = getRDFSLabel(cls).get();
    	String op = ComplexEditType.MODIFY.toString();
    	if (isNew()) {
    		op = ComplexEditType.CREATE.toString();
    	}
    	String ref = "";
    	putHistory(c, n, op, ref);
    	setNew(false);
    }
    
    public void submitDeleteHistory() {
    	
    	String c = classDeletedCode;
    	
    	String n = classDeletedLabel;
    	String op = ComplexEditType.DELETE.toString();
    	
    	String ref = "";
    	putHistory(c, n, op, ref);
    }
    
    private String getCodeOrIRI(OWLClass cls) {
    	String c;
    	Optional<String> cs = getCode(cls);
    	if (cs.isPresent()) {
    		c = cs.get();    		
    	} else {
    	  c = cls.getIRI().getShortForm();
    	}
    	return c;
    }
    
    public OWLOntology getOntology() {
		return ontology;
	}

	public ClientSession getClientSession() {
		return clientSession;
	}
	
	public boolean isLogicallyCorrect() {
		if (ontology.getEquivalentClassesAxioms(currentlySelected).isEmpty()) {
			return true;
		} else {
			if (!ontology.getSubClassAxiomsForSubClass(currentlySelected).isEmpty()) {
				if (CuratorReasonerPreferences.getInstance().
						isEnabled(CuratorReasonerPreferences.OptionalEditChecksTask
								.CHECK_SUBCLASS_FULL_DEFINED)) {
					JOptionPane.showMessageDialog(tab, 
							"A Defined class cannot have subclass axioms.", 
							"Warning", JOptionPane.WARNING_MESSAGE);
					return false;
				}
				
			} else {
				Set<OWLEquivalentClassesAxiom> eqs = ontology.getEquivalentClassesAxioms(currentlySelected);
				
				if (eqs.size() > 1) {
					return true;
				} else {
					
					Set<OWLClassExpression> exs = eqs.iterator().next().getClassExpressions();
					
					for (OWLClassExpression exp : exs) {
						if (exp.isOWLClass() && exp.equals(currentlySelected)) {
							
						} else {
							if (exp instanceof OWLObjectIntersectionOf) {
								return true;
							}
						}
					}
					
					JOptionPane.showMessageDialog(tab, 
							"A Defined class needs to have two or more conditions.", 
							"Warning", JOptionPane.WARNING_MESSAGE);
					return false;
					
					
				}
				
				
			}
			
		}
		return true;
	}
	
}
