// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006-2013 Talend – www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.designer.esb.runcontainer.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.collections.BidiMap;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.IProcess2;
import org.talend.core.model.process.ISubjobContainer;
import org.talend.core.model.process.ReplaceNodesInProcessProvider;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.prefs.ITalendCorePrefConstants;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.ui.CoreUIPlugin;
import org.talend.core.ui.branding.IBrandingService;
import org.talend.core.ui.properties.tab.IDynamicProperty;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.IMultiPageTalendEditor;
import org.talend.designer.core.debug.JobLaunchShortcutManager;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.runprocess.Processor;
import org.talend.designer.core.ui.editor.connections.Connection;
import org.talend.designer.core.ui.editor.connections.ConnectionTrace;
import org.talend.designer.core.ui.editor.jobletcontainer.JobletContainer;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.core.ui.editor.subjobcontainer.sparkstreaming.SparkStreamingSubjobContainer;
import org.talend.designer.core.ui.preferences.TalendDesignerPrefConstants;
import org.talend.designer.core.ui.views.problems.Problems;
import org.talend.designer.esb.runcontainer.preferences.ESBRunContainerPreferencePage;
import org.talend.designer.esb.runcontainer.ui.actions.JavaCamelJobScriptsExportWSForRuntimeAction;
import org.talend.designer.runprocess.IProcessMessage;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.JobErrorsChecker;
import org.talend.designer.runprocess.ProcessMessage;
import org.talend.designer.runprocess.ProcessMessage.MsgType;
import org.talend.designer.runprocess.ProcessMessageManager;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.designer.runprocess.RunProcessContext;
import org.talend.designer.runprocess.RunProcessPlugin;
import org.talend.designer.runprocess.RunprocessConstants;
import org.talend.designer.runprocess.i18n.Messages;
import org.talend.designer.runprocess.ui.ERunprocessImages;
import org.talend.designer.runprocess.ui.FindDialog;
import org.talend.designer.runprocess.ui.IDebugPreferenceConstants;
import org.talend.designer.runprocess.ui.JobVMArgumentsComposite;
import org.talend.designer.runprocess.ui.ProcessComposite;
import org.talend.designer.runprocess.ui.ProcessContextComposite;
import org.talend.designer.runprocess.ui.ProcessErrorUtil;
import org.talend.designer.runprocess.ui.ProcessManager;
import org.talend.designer.runprocess.ui.actions.ClearPerformanceAction;
import org.talend.designer.runprocess.ui.actions.ClearTraceAction;
import org.talend.designer.runprocess.ui.actions.SaveJobBeforeRunAction;
import org.talend.designer.runprocess.ui.views.IProcessViewHelper;
import org.talend.designer.runprocess.ui.views.ProcessView;
import org.talend.repository.model.IRepositoryNode.ENodeType;
import org.talend.repository.model.RepositoryNode;

/**
 * DOC yyan class global comment. Detailled comment
 */
public class RunESBContainerComposite extends ScrolledComposite implements IDynamicProperty {

    private static Logger log = Logger.getLogger(ProcessComposite.class);

    private static final int BUTTON_SIZE = 100;

    private static final int H_WEIGHT = 15;

    private static final int MINIMUM_HEIGHT = 65;

    private static final int MINIMUM_WIDTH = 530;

    private IProcessViewHelper processViewHelper;

    private static RunProcessContext processContext;

    /** Context composite. */
    private ProcessContextComposite contextComposite;

    protected Composite argumentsComposite;

    private JobVMArgumentsComposite argumentsViewer;

    protected EComponentCategory section;

    private final Composite composite;

    /** Performance button. */
    // private Button perfBtn;

    /** Trace button. */
    // private Button traceBtn;

    /** Clear trace & performance button. */
    private Button clearTracePerfBtn;

    // private Button saveJobBeforeRunButton;

    /** Clear log button. */
    // private Button clearLogBtn;
    // private Button clearBeforeExec;

    /** Show time button. */
    // private Button watchBtn;

    /** Kill button. */
    private Button killBtn;

    /** Move Button */
    // private Button moveButton;

    /** Execution console. */
    private StyledText consoleText;

    /** RunProcessContext property change listener. */
    private PropertyChangeListener pcl;

    private IStreamListener streamListener;

    private boolean isAddedStreamListener;

    private boolean hideConsoleLine = false;

    private Button enableLineLimitButton;

    private Text lineLimitText;

    private Button wrapButton;

    // private SashForm sash;
    private Button run;

    // private ToolBar toolBar;

    private Label processNameLab;

    // private ToolItem itemDropDown;

    private Double extend = new Double(0);

    public HashMap<String, IProcessMessage> errorMessMap = new HashMap<String, IProcessMessage>();

    private ProcessManager processManager;

    private ProcessView viewPart;

    private Button statusBtn;

    private Button updateBtn;

    /**
     * DOC yyi RunContainerComposite constructor comment.
     * 
     * @param parent
     * @param style
     */
    public RunESBContainerComposite(ProcessView viewPart, Composite parent, RunProcessContext processContext, int style) {
        super(parent, style);
        initGraphicComponents(parent);
        processManager = ProcessManager.getInstance();
        this.composite = parent;
        this.processContext = processContext;
        // CSS
        CoreUIPlugin.setCSSClass(this, this.getClass().getSimpleName());
    }

    /**
     * DOC yyi Comment method "init".
     * 
     * @param parent
     */
    private void init(Composite parent) {
        // TODO Auto-generated method stub

    }

    /**
     * DOC qli Comment method "initGraphicComponents".
     * 
     * @param text
     * 
     */
    private Point computeSize(String text) {
        GC gc = new GC(run.getDisplay());
        final Point p = gc.textExtent(text);
        gc.dispose();
        return p;
    }

    /**
     * DOC amaumont Comment method "initGraphicComponents".
     * 
     * @param parent
     */
    private void initGraphicComponents(Composite parent) {

        setExpandHorizontal(true);
        setExpandVertical(true);
        this.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));

        FormData layouData = new FormData();
        layouData.left = new FormAttachment(0, 0);
        layouData.right = new FormAttachment(100, 0);
        layouData.top = new FormAttachment(0, 0);
        layouData.bottom = new FormAttachment(100, 0);
        setLayoutData(layouData);

        this.setLayout(new FormLayout());
        final Composite panel = new Composite(this, SWT.NONE);
        setContent(panel);
        // panel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));

        FormLayout layout2 = new FormLayout();
        layout2.marginWidth = 5 + 2;
        layout2.marginHeight = 4;
        layout2.spacing = 6 + 1;
        panel.setLayout(layout2);

        GridData data;
        GridLayout layout = new GridLayout();

        TabFolder tabFolder = new TabFolder(panel, SWT.BOTTOM);
        tabFolder.setLayoutData(new FormData());

        FormData layouDataf = new FormData();
        layouDataf.left = new FormAttachment(0, 0);
        layouDataf.right = new FormAttachment(100, 0);
        layouDataf.top = new FormAttachment(0, 0);
        layouDataf.bottom = new FormAttachment(100, 0);
        tabFolder.setLayoutData(layouDataf);

        TabItem runItem = new TabItem(tabFolder, SWT.NONE);
        runItem.setText("Console");

        // panel.setLayout(layout);

        // Splitter
        // sash = new SashForm(this, SWT.HORIZONTAL | SWT.SMOOTH);
        // sash.setLayoutData(new GridData(GridData.FILL_BOTH));
        //
        // layout = new GridLayout();
        // sash.setLayout(layout);
        //
        // // group Button
        // // qli,see the feature 6366.
        //
        // Composite buttonComposite = new Composite(sash, SWT.ERROR);
        // buttonComposite.setLayout(new GridLayout());
        //
        // moveButton = new Button(buttonComposite, SWT.PUSH);
        //        moveButton.setText("<<"); //$NON-NLS-1$
        //        moveButton.setToolTipText(Messages.getString("ProcessComposite.hideContext")); //$NON-NLS-1$
        //
        // final GridData layoutData = new GridData();
        // layoutData.verticalAlignment = GridData.CENTER;
        // layoutData.horizontalAlignment = GridData.CENTER;
        // layoutData.grabExcessHorizontalSpace = true;
        // layoutData.grabExcessVerticalSpace = true;
        // moveButton.setLayoutData(layoutData);

        // Group execution
        Composite execPanel = new Composite(tabFolder, SWT.NONE);
        execPanel.setLayout(new FormLayout());

        Group execGroup = new Group(execPanel, SWT.NONE);
        FormData fd_execGroup = new FormData();
        fd_execGroup.bottom = new FormAttachment(0);
        fd_execGroup.right = new FormAttachment(0);
        fd_execGroup.top = new FormAttachment(0);
        fd_execGroup.left = new FormAttachment(0);
        execGroup.setLayoutData(fd_execGroup);
        execGroup.setText(Messages.getString("ProcessComposite.execGroup")); //$NON-NLS-1$
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        execGroup.setLayout(layout);

        runItem.setControl(execPanel);

        FormData layouDatag = new FormData();
        layouDatag.left = new FormAttachment(0, 0);
        layouDatag.right = new FormAttachment(100, 0);
        layouDatag.top = new FormAttachment(0, 0);
        layouDatag.bottom = new FormAttachment(100, 0);
        execGroup.setLayoutData(layouDatag);

        // leftTabFolder = new CTabFolder(this, SWT.BORDER);
        // leftTabFolder.setSimple(false);
        // //
        // leftTabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        // //
        // // // Group context
        // //
        // CTabItem contextTabItem = new CTabItem(leftTabFolder, SWT.BORDER);
        //        contextTabItem.setText(Messages.getString("ProcessComposite.contextTab")); //$NON-NLS-1$
        // // contextComposite = new ProcessContextComposite(this, SWT.NONE);
        // // contextComposite.setBackground(leftTabFolder.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        // // contextTabItem.setControl(contextComposite);
        // //
        // Composite targetExecutionComposite = createTargetExecutionComposite(leftTabFolder);
        // targetExecutionComposite.setBackground(leftTabFolder.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        // //
        // targetExecutionTabItem = new CTabItem(leftTabFolder, SWT.BORDER);
        //        targetExecutionTabItem.setText(Messages.getString("ProcessComposite.targetExecutionTab")); //$NON-NLS-1$
        // targetExecutionTabItem.setToolTipText(Messages.getString("ProcessComposite.targetExecutionTabTooltipAvailable"));
        // targetExecutionTabItem.setControl(targetExecutionComposite);
        // //
        // // // Job Run VM Arguments Tab if language is java.
        // if (LanguageManager.getCurrentLanguage() == ECodeLanguage.JAVA) {
        // jobVMTabItem = new CTabItem(leftTabFolder, SWT.BORDER);
        //            jobVMTabItem.setText(Messages.getString("ProcessComposite.JVMTab")); //$NON-NLS-1$
        // argumentsComposite = new Composite(leftTabFolder, SWT.NONE);
        // argumentsComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        // GridLayout gridLayoutArguments = new GridLayout(1, false);
        // argumentsComposite.setLayout(gridLayoutArguments);
        // argumentsViewer = new JobVMArgumentsComposite("vmarguments", Messages
        //                    .getString("RunProcessPreferencePage.vmArgument"), //$NON-NLS-1$
        // argumentsComposite);
        // // argumentsViewer.setEnabled(false, argumentsComposite);
        // jobVMTabItem.setControl(argumentsComposite);
        // }

        ScrolledComposite execScroll = new ScrolledComposite(execGroup, SWT.V_SCROLL | SWT.H_SCROLL);
        execScroll.setExpandHorizontal(true);
        execScroll.setExpandVertical(true);
        execScroll.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite execContent = new Composite(execScroll, SWT.NONE);
        layout = new GridLayout();
        execContent.setLayout(new FormLayout());
        execScroll.setContent(execContent);

        Composite execHeader = new Composite(execContent, SWT.NONE);
        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = 7;
        formLayout.marginHeight = 4;
        formLayout.spacing = 7;
        execHeader.setLayout(formLayout);
        FormData layoutData = new FormData();
        layoutData.left = new FormAttachment(0, 0);
        layoutData.right = new FormAttachment(100, 0);
        layoutData.top = new FormAttachment(0, 0);
        layoutData.bottom = new FormAttachment(0, 50);
        execHeader.setLayoutData(layoutData);// new GridData(GridData.FILL_HORIZONTAL)
        // qli
        // see the feature 6366
        run = new Button(execHeader, SWT.PUSH);

        // itemDropDown = new ToolItem(toolBar, SWT.ARROW);
        run.setText(" " + Messages.getString("ProcessComposite.exec"));//$NON-NLS-1$//$NON-NLS-2$
        run.setData(ProcessView.EXEC_ID);
        run.setToolTipText(Messages.getString("ProcessComposite.execHint"));//$NON-NLS-1$
        run.setImage(ImageProvider.getImage(ERunprocessImages.RUN_PROCESS_ACTION));

        // final Menu menu = new Menu(execHeader);
        run.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent event) {
                execRun();
            }
        });

        // Run
        // final MenuItem menuItem1 = new MenuItem(menu, SWT.PUSH);
        //        menuItem1.setText(" " + Messages.getString("ProcessComposite.exec"));//$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        // menuItem1.setImage(ImageProvider.getImage(ERunprocessImages.RUN_PROCESS_ACTION));
        // menuItem1.setData(ProcessView.EXEC_ID);
        // menuItem1.addSelectionListener(new SelectionAdapter() {
        //
        // public void widgetSelected(SelectionEvent event) {
        // if (!itemDropDown.getData().equals(ProcessView.PAUSE_ID) &&
        // !itemDropDown.getData().equals(ProcessView.RESUME_ID)) {
        // itemDropDown.setText(menuItem1.getText());
        // itemDropDown.setData(ProcessView.EXEC_ID);
        // itemDropDown.setImage(ImageProvider.getImage(ERunprocessImages.RUN_PROCESS_ACTION));
        //                    itemDropDown.setToolTipText(Messages.getString("ProcessComposite.execHint"));//$NON-NLS-1$
        // toolBar.getParent().layout();
        // }
        // }
        // });
        IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                IBrandingService.class);
        // if (brandingService.getBrandingConfiguration().isAllowDebugMode()) {
        // // Debug
        // debugMenuItem = new MenuItem(menu, SWT.PUSH);
        //            debugMenuItem.setText(" " + Messages.getString("ProcessDebugDialog.debugBtn")); //$NON-NLS-1$//$NON-NLS-2$
        // debugMenuItem.setData(ProcessView.DEBUG_ID);
        // debugMenuItem.setImage(ImageProvider.getImage(ERunprocessImages.DEBUG_PROCESS_ACTION));
        // debugMenuItem.addSelectionListener(new SelectionAdapter() {
        //
        // public void widgetSelected(SelectionEvent event) {
        // if (!itemDropDown.getData().equals(ProcessView.PAUSE_ID)
        // && !itemDropDown.getData().equals(ProcessView.RESUME_ID)) {
        // itemDropDown.setText(debugMenuItem.getText());
        // itemDropDown.setData(ProcessView.DEBUG_ID);
        // itemDropDown.setImage(ImageProvider.getImage(ERunprocessImages.DEBUG_PROCESS_ACTION));
        //                        itemDropDown.setToolTipText(Messages.getString("ProcessComposite.debugHint"));//$NON-NLS-1$
        // toolBar.getParent().layout();
        // }
        //
        // }
        // });
        // }
        if (processContext == null) {
            run.setEnabled(false);
        }
        // toolBar.setEnabled(false);
        FormData formData = new FormData();
        // see the feature 6366,qli comment.
        // make a judge when the text change in diffrent languages.

        Point debugSize = null;
        Point execSize = null;
        formData.left = new FormAttachment(0);
        // if (brandingService.getBrandingConfiguration().isAllowDebugMode()) {
        // // set debug text to judge size
        // itemDropDown.setText(debugMenuItem.getText());
        // debugSize = computeSize(itemDropDown.getText());
        //
        // // set exec text to judge size
        // itemDropDown.setText(menuItem1.getText());
        // execSize = computeSize(itemDropDown.getText());
        // if (debugSize.x > execSize.x) {
        // formData.right = new FormAttachment(0, debugSize.x + 70);
        // } else {
        // formData.right = new FormAttachment(0, execSize.x + 70);
        // }
        // } else {
        // set exec text to judge size

        execSize = computeSize(run.getText());
        formData.right = new FormAttachment(0, execSize.x + 70);
        formData.height = 30;
        // }
        run.setLayoutData(formData);

        killBtn = new Button(execHeader, SWT.PUSH);
        killBtn.setText(Messages.getString("ProcessComposite.kill")); //$NON-NLS-1$
        killBtn.setToolTipText(Messages.getString("ProcessComposite.killHint")); //$NON-NLS-1$
        killBtn.setImage(ImageProvider.getImage(ERunprocessImages.KILL_PROCESS_ACTION));
        setButtonLayoutData(killBtn);
        killBtn.setEnabled(false);
        formData = new FormData();
        formData.top = new FormAttachment(run, 0, SWT.TOP);
        formData.left = new FormAttachment(run, 0, SWT.RIGHT);
        // qli modified to fix the bug "7302".
        Point killSize = computeSize(killBtn.getText());
        // if (brandingService.getBrandingConfiguration().isAllowDebugMode()) {
        // if ((killSize.x > debugSize.x) && (killSize.x > execSize.x)) {
        // formData.right = new FormAttachment(toolBar, killSize.x + 70, SWT.RIGHT);
        // } else if (debugSize.x > execSize.x) {
        // formData.right = new FormAttachment(toolBar, debugSize.x + 70, SWT.RIGHT);
        // } else {
        // formData.right = new FormAttachment(toolBar, execSize.x + 70, SWT.RIGHT);
        // }
        // } else {
        // if (killSize.x > execSize.x) {
        // formData.right = new FormAttachment(toolBar, killSize.x + 70, SWT.RIGHT);
        // } else {
        // formData.right = new FormAttachment(toolBar, execSize.x + 70, SWT.RIGHT);
        // }
        // }
        formData.right = new FormAttachment(run, 30 + 70, SWT.RIGHT);
        formData.height = 30;
        killBtn.setLayoutData(formData);

        // saveJobBeforeRunButton = new Button(execHeader, SWT.CHECK);
        //        saveJobBeforeRunButton.setText(Messages.getString("ProcessComposite.saveBeforeRun")); //$NON-NLS-1$
        //        saveJobBeforeRunButton.setToolTipText(Messages.getString("ProcessComposite.saveBeforeRunHint")); //$NON-NLS-1$
        // // saveJobBeforeRunButton.setEnabled(false);
        // saveJobBeforeRunButton.setSelection(RunProcessPlugin.getDefault().getPreferenceStore().getBoolean(
        // RunProcessPrefsConstants.ISSAVEBEFORERUN));
        // data = new GridData();
        // data.horizontalSpan = 2;
        // data.horizontalAlignment = SWT.END;
        // saveJobBeforeRunButton.setLayoutData(data);
        // formData = new FormData();
        // formData.top = new FormAttachment(toolBar, 0, SWT.BOTTOM);
        // formData.left = new FormAttachment(toolBar, 0, SWT.LEFT);
        // saveJobBeforeRunButton.setLayoutData(formData);

        // clearBeforeExec = new Button(execHeader, SWT.CHECK);
        //        clearBeforeExec.setText(Messages.getString("ProcessComposite.clearBefore")); //$NON-NLS-1$
        //        clearBeforeExec.setToolTipText(Messages.getString("ProcessComposite.clearBeforeHint")); //$NON-NLS-1$
        // // clearBeforeExec.setEnabled(false);
        // clearBeforeExec.setSelection(RunProcessPlugin.getDefault().getPreferenceStore().getBoolean(
        // RunProcessPrefsConstants.ISCLEARBEFORERUN));
        // data = new GridData();
        // data.horizontalSpan = 2;
        // data.horizontalAlignment = SWT.END;
        // clearBeforeExec.setLayoutData(data);
        // formData = new FormData();
        // formData.top = new FormAttachment(toolBar, 0, SWT.BOTTOM);
        // formData.left = new FormAttachment(saveJobBeforeRunButton, 0, SWT.RIGHT);
        // clearBeforeExec.setLayoutData(formData);
        //
        // watchBtn = new Button(execHeader, SWT.CHECK);
        //        watchBtn.setText(Messages.getString("ProcessComposite.execTime")); //$NON-NLS-1$
        //        watchBtn.setToolTipText(Messages.getString("ProcessComposite.execTimeHint")); //$NON-NLS-1$
        // watchBtn.setEnabled(false);
        // watchBtn.setSelection(RunProcessPlugin.getDefault().getPreferenceStore().getBoolean(
        // RunProcessPrefsConstants.ISEXECTIMERUN));
        // data = new GridData();
        // data.horizontalSpan = 2;
        // data.horizontalAlignment = SWT.END;
        // watchBtn.setLayoutData(data);
        // formData = new FormData();
        // formData.top = new FormAttachment(killBtn, 0, SWT.BOTTOM);
        // formData.left = new FormAttachment(clearBeforeExec, 0, SWT.RIGHT);
        // watchBtn.setLayoutData(formData);
        //
        // Group statisticsComposite = new Group(execHeader, SWT.NONE);
        //        statisticsComposite.setText(Messages.getString("ProcessComposite2.statsComposite")); //$NON-NLS-1$
        // layout = new GridLayout(3, false);
        // layout.marginWidth = 0;
        // statisticsComposite.setLayout(layout);
        // formData = new FormData();
        // // formData.right = new FormAttachment(100, 0);
        // / formData.left = new FormAttachment(watchBtn, 0, SWT.RIGHT);
        // statisticsComposite.setLayoutData(formData);
        //
        // Composite statisticsButtonComposite = new Composite(statisticsComposite, SWT.NONE);
        // layout = new GridLayout(1, false);
        // layout.marginWidth = 0;
        // statisticsButtonComposite.setLayout(layout);
        // statisticsButtonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        // perfBtn = new Button(statisticsButtonComposite, SWT.CHECK);
        //        perfBtn.setText(Messages.getString("ProcessComposite.stat")); //$NON-NLS-1$
        //        perfBtn.setToolTipText(Messages.getString("ProcessComposite.statHint")); //$NON-NLS-1$
        // perfBtn.setEnabled(false);
        // perfBtn.setSelection(RunProcessPlugin.getDefault().getPreferenceStore().getBoolean(
        // RunProcessPrefsConstants.ISSTATISTICSRUN));
        // traceBtn = new Button(statisticsButtonComposite, SWT.CHECK);
        //        traceBtn.setText(Messages.getString("ProcessComposite.trace")); //$NON-NLS-1$
        //        traceBtn.setToolTipText(Messages.getString("ProcessComposite.traceHint")); //$NON-NLS-1$
        // traceBtn.setEnabled(false);
        // traceBtn
        // .setSelection(RunProcessPlugin.getDefault().getPreferenceStore().getBoolean(RunProcessPrefsConstants.ISTRACESRUN));

        clearTracePerfBtn = new Button(execHeader, SWT.PUSH);
        clearTracePerfBtn.setText(Messages.getString("ProcessComposite.clear")); //$NON-NLS-1$
        clearTracePerfBtn.setToolTipText(Messages.getString("ProcessComposite.clearHint")); //$NON-NLS-1$
        clearTracePerfBtn.setImage(ImageProvider.getImage(RunProcessPlugin.imageDescriptorFromPlugin(RunProcessPlugin.PLUGIN_ID,
                "icons/process_stat_clear.gif"))); //$NON-NLS-1$
        clearTracePerfBtn.setEnabled(false);
        formData = new FormData();
        formData.top = new FormAttachment(killBtn, 0, SWT.TOP);
        formData.left = new FormAttachment(killBtn, 0, SWT.RIGHT);
        formData.right = new FormAttachment(killBtn, 10 + 70, SWT.RIGHT);
        formData.height = 30;
        clearTracePerfBtn.setLayoutData(formData);

        statusBtn = new Button(execHeader, SWT.PUSH);
        statusBtn.setText("Status"); //$NON-NLS-1$
        statusBtn.setToolTipText(Messages.getString("ProcessComposite.clearHint")); //$NON-NLS-1$
        statusBtn.setImage(ImageProvider.getImage(RunProcessPlugin.imageDescriptorFromPlugin(RunProcessPlugin.PLUGIN_ID,
                "icons/trace_on.png"))); //$NON-NLS-1$
        statusBtn.setEnabled(true);
        formData = new FormData();
        formData.top = new FormAttachment(clearTracePerfBtn, 0, SWT.TOP);
        formData.left = new FormAttachment(clearTracePerfBtn, 0, SWT.RIGHT);
        formData.right = new FormAttachment(clearTracePerfBtn, 10 + 70, SWT.RIGHT);
        formData.height = 30;
        statusBtn.setLayoutData(formData);

        updateBtn = new Button(execHeader, SWT.PUSH);
        updateBtn.setText("Update"); //$NON-NLS-1$
        updateBtn.setToolTipText(Messages.getString("ProcessComposite.clearHint")); //$NON-NLS-1$
        updateBtn.setImage(ImageProvider.getImage(RunProcessPlugin.imageDescriptorFromPlugin(RunProcessPlugin.PLUGIN_ID,
                "icons/process_view.png"))); //$NON-NLS-1$
        updateBtn.setEnabled(true);
        formData = new FormData();
        formData.top = new FormAttachment(statusBtn, 0, SWT.TOP);
        formData.left = new FormAttachment(statusBtn, 0, SWT.RIGHT);
        formData.right = new FormAttachment(statusBtn, 10 + 70, SWT.RIGHT);
        formData.height = 30;
        updateBtn.setLayoutData(formData);

        Link runtimePrefLink = new Link(execContent, SWT.NONE);
        FormData fd_link = new FormData();
        fd_link.left = new FormAttachment(2);
        fd_link.right = new FormAttachment(20);
        fd_link.top = new FormAttachment(0, 52);
        runtimePrefLink.setLayoutData(fd_link);
        runtimePrefLink.setText("<a>Runtime Environment</a>");
        runtimePrefLink.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog d = new PreferenceDialog(getShell(), PlatformUI.getWorkbench().getPreferenceManager());
                d.setSelectedNode(ESBRunContainerPreferencePage.ID);
                d.open();
            }
        });

        Combo combo = new Combo(execContent, SWT.DROP_DOWN | SWT.READ_ONLY);
        fd_link = new FormData();
        fd_link.left = new FormAttachment(25);
        fd_link.right = new FormAttachment(54);
        fd_link.top = new FormAttachment(0, 50);
        fd_link.width = 10;
        combo.setLayoutData(fd_link);
        combo.add("localhost");

        consoleText = new StyledText(execContent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
        layouData = new FormData();
        layouData.left = new FormAttachment(0, 10);
        layouData.right = new FormAttachment(100, 0);
        layouData.top = new FormAttachment(33);
        layouData.bottom = new FormAttachment(100, -30);

        consoleText.setLayoutData(layouData);
        consoleText.setWordWrap(true);

        // feature 6875, add searching capability, nma
        consoleText.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent evt) {
                // select all
                if ((evt.stateMask == SWT.CTRL) && (evt.keyCode == 'a')) {
                    if (consoleText.getText().length() > 0) {
                        consoleText.setSelection(0, (consoleText.getText().length() - 1));
                    }
                }
                // search special string value
                else if ((evt.stateMask == SWT.CTRL) && (evt.keyCode == 'f')) {
                    FindDialog td = new FindDialog(Display.getCurrent().getActiveShell());
                    td.setConsoleText(consoleText);
                    td.setBlockOnOpen(true);
                    td.open();

                }
            }

            @Override
            public void keyReleased(KeyEvent arg0) {

            }
        });

        // see feature 0004895: Font size of the output console are very small
        setConsoleFont();
        IPreferenceStore preferenceStore = CorePlugin.getDefault().getPreferenceStore();
        preferenceStore.addPropertyChangeListener(new IPropertyChangeListener() {

            @Override
            public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
                if (TalendDesignerPrefConstants.CONSOLT_TEXT_FONT.endsWith(event.getProperty())) {
                    setConsoleFont();
                }

            }
        });

        // execScroll.setMinSize(execContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        // sash.setSashWidth(1);
        // sash.setWeights(new int[] { 7, 1, H_WEIGHT });

        pcl = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                runProcessContextChanged(evt);
            }
        };

        streamListener = new IStreamListener() {

            @Override
            public void streamAppended(String text, IStreamMonitor monitor) {
                IProcessMessage message = new ProcessMessage(ProcessMessage.MsgType.STD_OUT, text);
                processContext.addDebugResultToConsole(message);
            }
        };

        TabItem serverItem = new TabItem(tabFolder, SWT.NONE);
        serverItem.setText("Server");

        Composite execFooter = new Composite(tabFolder, SWT.NONE);
        execFooter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        execFooter.setLayout(new FormLayout());
        FormData layouDatat = new FormData();
        layouDatat.left = new FormAttachment(0, 0);
        layouDatat.right = new FormAttachment(100, 0);
        layouDatat.top = new FormAttachment(0);
        layouDatat.bottom = new FormAttachment(100);
        execFooter.setLayoutData(layoutData);
        serverItem.setControl(execFooter);

        // Text btn1 = new Text(execFooter, SWT.BORDER);
        // btn1.setLayoutData(layouDatag);
        // btn1.setText("hahahaha");

        // DONOT REMOVE
        // TextConsole c1 = new ESBRunContainerConsole("aa", "java", null, false);
        // TextConsoleViewer v1 = new TextConsoleViewer(execFooter, c1);
        // v1.setConsoleWidth(100);
        // v1.setTabWidth(200);

        Browser browser = new Browser(execFooter, SWT.NONE);
        browser.setUrl("http://localhost:8040/system/console/gogo");
        browser.addProgressListener(new ProgressListener() {

            @Override
            public void completed(ProgressEvent event) {
                // System.out.println("------>" + browser.evaluate("return document.getElementById('main').innerHTML"));
                System.out.println("------>" + browser.evaluate("document.getElementById('term').style.position='absolute';"));
                System.out.println("------>" + browser.evaluate("document.getElementById('term').style.height='100%';"));
                System.out.println("------>" + browser.evaluate("document.getElementById('term').style.width='100%';"));
                System.out.println("------>" + browser.evaluate("document.getElementById('term').style.top='0';"));
                System.out.println("------>" + browser.evaluate("document.getElementById('term').style.left='0';"));
            }

            @Override
            public void changed(ProgressEvent event) {
            }
        });
        browser.setLayoutData(new FormData());
        layouDatat = new FormData();
        layouDatat.left = new FormAttachment(0, 0);
        layouDatat.right = new FormAttachment(100, 0);
        layouDatat.top = new FormAttachment(0);
        layouDatat.bottom = new FormAttachment(100);
        browser.setLayoutData(layouDatat);

        addListeners();
        createLineLimitedControl(execContent);

    }

    public void execRun() {
        // ToolItem item = (ToolItem) event.widget;
        errorMessMap.clear();
        // if (item.getData().equals(ProcessView.DEBUG_ID)) {
        // debug();
        // } else {
        execButtonPressed();
        // }
    }

    /**
     * DOC bqian Comment method "createLineLimitedControl".
     */
    private void createLineLimitedControl(Composite container) {
        Composite composite = new Composite(container, SWT.NONE);
        FormData layouData = new FormData();
        layouData.left = new FormAttachment(0, 10);
        layouData.right = new FormAttachment(100, 0);
        layouData.top = new FormAttachment(100, -30);
        layouData.bottom = new FormAttachment(100, -3);
        composite.setLayoutData(layouData);

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = 7;
        formLayout.marginHeight = 4;
        formLayout.spacing = 7;
        composite.setLayout(formLayout);

        enableLineLimitButton = new Button(composite, SWT.CHECK);
        enableLineLimitButton.setText(Messages.getString("ProcessComposite.lineLimited")); //$NON-NLS-1$
        FormData formData = new FormData();
        enableLineLimitButton.setLayoutData(formData);
        enableLineLimitButton.setEnabled(false);
        enableLineLimitButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                lineLimitText.setEditable(enableLineLimitButton.getSelection());
                RunProcessPlugin.getDefault().getPluginPreferences()
                        .setValue(RunprocessConstants.ENABLE_CONSOLE_LINE_LIMIT, enableLineLimitButton.getSelection());
            }
        });

        lineLimitText = new Text(composite, SWT.BORDER);
        formData = new FormData();
        formData.width = 120;
        formData.left = new FormAttachment(enableLineLimitButton, 0, SWT.RIGHT);
        lineLimitText.setLayoutData(formData);
        lineLimitText.setEnabled(false);
        lineLimitText.addListener(SWT.Verify, new Listener() {

            // this text only receive number here.
            @Override
            public void handleEvent(Event e) {
                String s = e.text;
                if (!s.equals("")) { //$NON-NLS-1$
                    try {
                        Integer.parseInt(s);
                        RunProcessPlugin.getDefault().getPluginPreferences()
                                .setValue(RunprocessConstants.CONSOLE_LINE_LIMIT_COUNT, lineLimitText.getText() + s);
                    } catch (Exception ex) {
                        e.doit = false;
                    }
                }
            }
        });
        lineLimitText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                RunProcessPlugin.getDefault().getPluginPreferences()
                        .setValue(RunprocessConstants.CONSOLE_LINE_LIMIT_COUNT, lineLimitText.getText());
            }
        });

        boolean enable = RunProcessPlugin.getDefault().getPluginPreferences()
                .getBoolean(RunprocessConstants.ENABLE_CONSOLE_LINE_LIMIT);
        enableLineLimitButton.setSelection(enable);
        lineLimitText.setEditable(enable);
        String count = RunProcessPlugin.getDefault().getPluginPreferences()
                .getString(RunprocessConstants.CONSOLE_LINE_LIMIT_COUNT);
        if (count.equals("")) { //$NON-NLS-1$
            count = "100"; //$NON-NLS-1$
        }
        lineLimitText.setText(count);

        wrapButton = new Button(composite, SWT.CHECK);
        formData = new FormData();
        formData.left = new FormAttachment(lineLimitText, 15, SWT.RIGHT);
        wrapButton.setLayoutData(formData);
        wrapButton.setText(Messages.getString("ProcessComposite.wrapbutton")); //$NON-NLS-1$
        wrapButton.setSelection(true);
        wrapButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (wrapButton.getSelection()) {
                    consoleText.setWordWrap(true);
                } else {
                    consoleText.setWordWrap(false);
                }
            }
        });
    }

    private int getConsoleRowLimit() {
        if (!enableLineLimitButton.isDisposed()) {
            if (enableLineLimitButton.getSelection()) {
                try {
                    return Integer.parseInt(lineLimitText.getText());
                } catch (Exception e) {
                }
            }
        }
        return SWT.DEFAULT;
    }

    /**
     * DOC amaumont Comment method "getTargetExecutionComposite".
     * 
     * @param parent
     * @return
     */
    protected Composite createTargetExecutionComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        StyledText text = new StyledText(composite, SWT.NONE);
        text.setText(Messages.getString("ProcessComposite.targetExecutionTabTooltipAvailable")); //$NON-NLS-1$
        text.setWordWrap(true);
        text.setEditable(false);
        text.setLayoutData(new GridData(GridData.FILL_BOTH));

        return composite;
    }

    public boolean hasProcess() {
        return processContext != null;
    }

    public org.talend.core.model.process.IProcess2 getProcess() {
        return processContext.getProcess();
    }

    protected void addListeners() {
        // qli comment "addSelectionListener" .
        /*
         * moveButton.addSelectionListener(new SelectionAdapter() {
         * 
         * @Override public void widgetSelected(final SelectionEvent e) { if (moveButton.getText().equals("<<")) {
         * //$NON-NLS-1$ sash.setWeights(new int[] { 0, 1, 23 }); moveButton.setText(">>"); //$NON-NLS-1$
         * moveButton.setToolTipText(Messages.getString("ProcessComposite.showContext")); //$NON-NLS-1$ } else if
         * (moveButton.getText().equals(">>")) { //$NON-NLS-1$ sash.setWeights(new int[] { 7, 1, H_WEIGHT });
         * moveButton.setText("<<"); //$NON-NLS-1$
         * moveButton.setToolTipText(Messages.getString("ProcessComposite.hideContext"));//$NON-NLS-1$ } } });
         */
        // perfBtn.addSelectionListener(new SelectionAdapter() {
        //
        // @Override
        // public void widgetSelected(final SelectionEvent e) {
        // processContext.setMonitorPerf(perfBtn.getSelection());
        // }
        // });
        //
        // traceBtn.addSelectionListener(new SelectionAdapter() {
        //
        // @Override
        // public void widgetSelected(final SelectionEvent e) {
        // processContext.setMonitorTrace(traceBtn.getSelection());
        // org.talend.core.model.process.IProcess process = processContext.getProcess();
        // List<INode> nodeList = (List<INode>) process.getGraphicalNodes();
        // for (INode node : nodeList) {
        // for (Connection connection : (List<Connection>) node.getOutgoingConnections()) {
        // ConnectionTrace traceNode = connection.getConnectionTrace();
        // if (traceNode == null) {
        // continue;
        // }
        // traceNode.setPropertyValue(EParameterName.TRACES_SHOW_ENABLE.getName(), traceBtn.getSelection());
        // if (connection != null) {
        // connection.setPropertyValue(EParameterName.TRACES_SHOW_ENABLE.getName(), traceBtn.getSelection());
        // }
        // }
        // }
        //
        // }
        // });

        clearTracePerfBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (processContext == null) {
                    clearTracePerfBtn.setEnabled(false);
                    return;
                }
                if (processContext.isRunning()) {
                    if (consoleText != null && !consoleText.isDisposed()) {
                        processContext.clearMessages();
                    }
                } else {
                    ClearPerformanceAction clearPerfAction = new ClearPerformanceAction();
                    clearPerfAction.setProcess(processContext.getProcess());
                    clearPerfAction.run();
                    ClearTraceAction clearTraceAction = new ClearTraceAction();
                    clearTraceAction.setProcess(processContext.getProcess());
                    clearTraceAction.run();
                    consoleText.setText(""); //$NON-NLS-1$
                    processContext.clearMessages();
                    refreshNodeContainer();
                    refreshSubjobContainer();
                }
            }
        });

        killBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                kill();
            }
        });
        // watchBtn.addSelectionListener(new SelectionAdapter() {
        //
        // @Override
        // public void widgetSelected(SelectionEvent e) {
        // processContext.setWatchAllowed(watchBtn.getSelection());
        // }
        //
        // });
        // saveJobBeforeRunButton.addSelectionListener(new SelectionAdapter() {
        //
        // @Override
        // public void widgetSelected(SelectionEvent e) {
        // processContext.setSaveBeforeRun(saveJobBeforeRunButton.getSelection());
        // }
        // });

        // clearBeforeExec.addSelectionListener(new SelectionAdapter() {
        //
        // @Override
        // public void widgetSelected(SelectionEvent e) {
        // processContext.setClearBeforeExec(clearBeforeExec.getSelection());
        // }
        // });
    }

    protected boolean checkKillAllowed() {
        return true;
    }

    /**
     * @see void org.talend.designer.runprocess.RunProcessContext.exec(Shell shell)
     */
    public void execButtonPressed() {
        if (processContext == null) {
            run.setEnabled(false);
            return;
        }
        if (run.getData().equals(ProcessView.PAUSE_ID)) {
            pause(ProcessView.PAUSE_ID);
        } else if (run.getData().equals(ProcessView.RESUME_ID)) {
            pause(ProcessView.RESUME_ID);
        } else if (run.getData().equals(ProcessView.EXEC_ID)) {

            // addInHistoryRunningList();
            // run.setData(ProcessView.PAUSE_ID);
            // exec();

            processContext.exec(getShell());
//
            if (1 == 1) {

                return;
            }
            IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
            try {
                progressService.run(false, true, new IRunnableWithProgress() {

                    @Override
                    public void run(final IProgressMonitor monitor) {
                        processContext.setRunning(true);
                        // final IProgressMonitor progressMonitor = new EventLoopProgressMonitor(monitor);
                        //                        progressMonitor.beginTask("Start log listener", IProgressMonitor.UNKNOWN); //$NON-NLS-1$

                        // RuntimeLogHTTPMonitor logMonitor = RuntimeLogHTTPMonitor.createRuntimeLogHTTPMonitor();
                        // try {
                        // logMonitor.startLogging();
                        // logMonitor.addLogLictener(new RuntimeLogHTTPAdapter() {
                        //
                        // @Override
                        // public void logReceived(FelixLogsModel logsModel) {
                        //
                        // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
                        // Date date = new Date(logsModel.getReceived());
                        //
                        // String eventlog = sdf.format(date) + " | " + logsModel.getLevel() + " | "
                        // + logsModel.getBundleId() + " - " + logsModel.getBundleName() + " | "
                        // + logsModel.getMessage();
                        // System.out.println(eventlog);
                        // Display.getDefault().asyncExec(new Runnable() {
                        //
                        // @Override
                        // public void run() {
                        // Display.getDefault().asyncExec(new Runnable() {
                        //
                        // @Override
                        // public void run() {
                        // processContext.addMessage(new ProcessMessage(MsgType.STD_OUT, eventlog));
                        // }
                        // });
                        // }
                        // });
                        // }
                        // });
                        // } catch (Exception e) {
                        //
                        // }

                        //                        progressMonitor.beginTask("build ", IProgressMonitor.UNKNOWN); //$NON-NLS-1$

                        // processContext.addMessage(new ProcessMessage(MsgType.STD_OUT, "eventlog"));
                        try {

//                            IProcess2 process = processContext.getProcess();
//                            IRepositoryViewObject routeViewObject = findJob(process.getId());

//                            RepositoryNode routeNode = new RepositoryNode(routeViewObject, null, ENodeType.REPOSITORY_ELEMENT);
//                            JavaCamelJobScriptsExportWSForRuntimeAction action = new JavaCamelJobScriptsExportWSForRuntimeAction(
//                                    routeNode, process.getVersion(), "e:/tmp/alltest/run_c.kar", true);
//                            action.run(new RunESBContainerProgressMonitor(processContext));
//                            processContext.addMessage(new ProcessMessage(MsgType.CORE_ERR, "Starting log listener"));
//                            String username = "karaf";
//                            String password = "karaf";
//
//                            String host = "localhost";
//                            String jmxPort = "44444";
//                            String karafPort = "1099";
//                            String instanceName = "trun";
//                            String serviceUrl = "service:jmx:rmi://" + host + ":" + jmxPort + "/jndi/rmi://" + host + ":"
//                                    + karafPort + "/karaf-" + instanceName;
//
//                            HashMap<String, String[]> env = new HashMap<String, String[]>();
//                            String[] credentials = new String[] { username, password };
//                            env.put("jmx.remote.credentials", credentials);
//
//                            JMXServiceURL url = new JMXServiceURL(serviceUrl);
//                            JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
//                            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
//
//                            String KARAF_BUNDLE_MBEAN = "org.apache.karaf:type=feature,name=trun";
//                            // String KARAF_BUNDLE_MBEAN = "org.apache.karaf:type=bundle,name=trun";
//                            ObjectName objectName = new ObjectName(KARAF_BUNDLE_MBEAN);
//
//                            mbsc.invoke(
//                                    objectName,
//                                    "addRepository",
//                                    new Object[] { "file:E:/tmp/alltest/run_r1simple-feature/repository/local_project/run_r1simple/run_r1simple-feature/0.1/run_r1simple-feature-0.1.xml" },
//                                    new String[] { String.class.getName() });
//
//                            mbsc.invoke(objectName, "installFeature", new Object[] { "run_r1simple-feature" },
//                                    new String[] { String.class.getName() });
//
//                            Object info = mbsc.invoke(objectName, "infoFeature", new Object[] { "run_r1simple-feature" },
//                                    new String[] { String.class.getName() });
                            // if (info instanceof Long) {
                            // System.out.println("------>" + (Long) info);
                            // processContext
                            // .addMessage(new ProcessMessage(MsgType.CORE_OUT, ">>>>>>>>deployed success!" +
                            // info.toString()));
                            // } else if (info instanceof TabularDataSupport) {
                            //
                            // processContext.addMessage(new ProcessMessage(MsgType.STD_OUT, ((TabularDataSupport)
                            // info).get("Bundles")
                            // .toString()));
                            // }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                processContext.addMessage(new ProcessMessage(MsgType.CORE_ERR, e.getMessage()));
            }

        }
        refreshNodeContainer();
        refreshSubjobContainer();
    }

    private IRepositoryViewObject findJob(String jobID) throws PersistenceException {

        ProxyRepositoryFactory proxyRepositoryFactory = ProxyRepositoryFactory.getInstance();

        return proxyRepositoryFactory.getLastVersion(jobID);
    }

    public void pause(int id) {
        boolean isPause = id == ProcessView.PAUSE_ID;
        setExecBtn(isPause);
        // if (isPause) {
        //            run.setText(Messages.getString("ProcessComposite.textContent")); //$NON-NLS-1$
        //            run.setToolTipText(Messages.getString("ProcessComposite.tipTextContent")); //$NON-NLS-1$
        // run.setData(ProcessView.RESUME_ID);
        // } else {
        // run.setData(ProcessView.PAUSE_ID);
        // }
        // processContext.setTracPause(isPause);
    }

    /**
     * 
     * DOC ggu Comment method "setCurRunMode".
     * 
     * for the F6 shortcut to run
     */
    public void setCurRunMode(int id) {
        pause(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        if (processContext != null) {
            processContext.removePropertyChangeListener(pcl);
        }
        super.dispose();

    }

    /**
     * Set the layout data of the button to a GridData with appropriate heights and widths.
     * 
     * @param button
     */
    protected static void setButtonLayoutData(final Button button) {
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        final int widthHint = 80;
        Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        data.widthHint = Math.max(widthHint, minSize.x);
        button.setLayoutData(data);
    }

    public void setProcessContext(RunProcessContext processContext) {

        IPreferenceStore preferenceStore = DesignerPlugin.getDefault().getPreferenceStore();
        String languagePrefix = LanguageManager.getCurrentLanguage().toString() + "_"; //$NON-NLS-1$
        if (this.processContext != null) {
            this.processContext.removePropertyChangeListener(pcl);
        }
        this.processContext = processContext;
        if (processContext != null) {
            processContext.addPropertyChangeListener(pcl);
        }
        // if (argumentsViewer != null) {
        // argumentsViewer.setProcessContext(processContext);
        // }
        boolean disableAll = false;
        if (processContext != null) {
            disableAll = processContext.getProcess().disableRunJobView();

        }
        // perfBtn.setSelection(processContext != null && processContext.isMonitorPerf());
        // traceBtn.setSelection(processContext != null && processContext.isMonitorTrace());
        // watchBtn.setSelection(processContext != null && processContext.isWatchAllowed());
        // perfBtn.setSelection(RunProcessPlugin.getDefault().getPreferenceStore(
        // ).getBoolean(
        // RunProcessPrefsConstants.ISSTATISTICSRUN)
        // && !disableAll);
        // traceBtn.setSelection(RunProcessPlugin.getDefault().getPreferenceStore
        // ().getBoolean(RunProcessPrefsConstants.
        // ISTRACESRUN)
        // && !disableAll);
        if (this.processContext == null) {
            // this.processContext.setMonitorTrace(traceBtn.getSelection());
            processManager.setBooleanTrace(false);
        }

        // watchBtn.setSelection(RunProcessPlugin.getDefault().getPreferenceStore
        // ().getBoolean(
        // RunProcessPrefsConstants.ISEXECTIMERUN)
        // && !disableAll);
        // saveJobBeforeRunButton.setSelection(RunProcessPlugin.getDefault().
        // getPreferenceStore().getBoolean(
        // RunProcessPrefsConstants.ISSAVEBEFORERUN)
        // && !disableAll);
        // clearBeforeExec.setSelection(RunProcessPlugin.getDefault().
        // getPreferenceStore().getBoolean(
        // RunProcessPrefsConstants.ISCLEARBEFORERUN)
        // && !disableAll);
        // saveJobBeforeRunButton.setSelection(processContext != null && processContext.isSaveBeforeRun());
        setRunnable(processContext != null && !processContext.isRunning() && !disableAll);
        if (killBtn != null && !killBtn.isDisposed()) {
            killBtn.setEnabled(processContext != null && processContext.isRunning() && !disableAll);
        }
        // clearBeforeExec.setEnabled(processContext != null);
        // clearBeforeExec.setSelection(processContext != null && processContext.isClearBeforeExec());
        // contextComposite.setProcess(((processContext != null) && !disableAll ? processContext.getProcess() : null));
        fillConsole(processContext != null ? processContext.getMessages() : new ArrayList<IProcessMessage>());

        // remove trace if basic run tab active
        if (processContext != null) {
            processContext.setMonitorTrace(false);
            org.talend.core.model.process.IProcess process = processContext.getProcess();
            List<INode> nodeList = (List<INode>) process.getGraphicalNodes();
            for (INode node : nodeList) {
                for (Connection connection : (List<Connection>) node.getOutgoingConnections()) {
                    ConnectionTrace traceNode = connection.getConnectionTrace();
                    if (traceNode == null) {
                        continue;
                    }
                    traceNode.setPropertyValue(EParameterName.TRACES_SHOW_ENABLE.getName(), false);
                    if (connection != null && connection.checkTraceShowEnable()) {
                        connection.setPropertyValue(EParameterName.TRACES_SHOW_ENABLE.getName(), false);
                    }
                }
            }
        }
    }

    protected void setRunnable(boolean runnable) {
        // perfBtn.setEnabled(runnable);
        if (clearTracePerfBtn != null && !clearTracePerfBtn.isDisposed()) {
            IProcess2 iProcess = null;
            boolean enableClearBtn = true;
            if (processContext != null && (iProcess = processContext.getProcess()) != null) {
                if (iProcess.disableRunJobView()) {
                    enableClearBtn = false;
                }
            } else {
                enableClearBtn = false;
            }
            clearTracePerfBtn.setEnabled(enableClearBtn);
        }

        setExecBtn(runnable);
        // contextComposite.setEnabled(runnable);
        // if (argumentsComposite != null) {
        // argumentsComposite.setEnabled(runnable);
        // }
        // clearBeforeExec.setEnabled(runnable);
        // saveJobBeforeRunButton.setEnabled(runnable);
        // watchBtn.setEnabled(runnable);
        if (enableLineLimitButton != null && !enableLineLimitButton.isDisposed()) {
            enableLineLimitButton.setEnabled(runnable);
        }
        if (lineLimitText != null && !lineLimitText.isDisposed()) {
            lineLimitText.setEnabled(runnable);
        }
        if (wrapButton != null && !wrapButton.isDisposed()) {
            wrapButton.setEnabled(runnable);
        }
    }

    /**
     * qzhang Comment method "setExecBtn".
     * 
     * @param runnable
     */
    private void setExecBtn(final boolean runnable) {
        /*
         * if (traceBtn.getSelection()) { boolean b = processContext != null; if (!runnable && b) {
         * itemDropDown.setText(" " + Messages.getString("ProcessComposite.pause")); //$NON-NLS-1$//$NON-NLS-2$
         * itemDropDown.setToolTipText(Messages.getString("ProcessComposite.pauseJob")); //$NON-NLS-1$
         * itemDropDown.setImage(ImageProvider.getImage(ERunprocessImages.PAUSE_PROCESS_ACTION));
         * itemDropDown.setData(ProcessView.PAUSE_ID); toolBar.getParent().layout(); } else { itemDropDown.setText(" " +
         * Messages.getString("ProcessComposite.exec") + "  "); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
         * itemDropDown.setData(ProcessView.EXEC_ID);
         * itemDropDown.setToolTipText(Messages.getString("ProcessComposite.execHint")); //$NON-NLS-1$
         * itemDropDown.setImage(ImageProvider.getImage(ERunprocessImages.RUN_PROCESS_ACTION)); } toolBar.setEnabled(b);
         * } else {
         */
        // qli modified to fix the bug 7354.
        if (run != null && !run.isDisposed()) {
            run.setEnabled(runnable);
            run.redraw();
            run.getParent().layout();
            // if (itemDropDown.getData().equals(ProcessView.DEBUG_ID)) {
            //            debugMenuItem.setText(" " + Messages.getString("ProcessDebugDialog.debugBtn")); //$NON-NLS-1$//$NON-NLS-2$
            // debugMenuItem.setData(ProcessView.DEBUG_ID);
            // debugMenuItem.setImage(ImageProvider.getImage(ERunprocessImages.DEBUG_PROCESS_ACTION));
            // } else {
            run.setText(" " + Messages.getString("ProcessComposite.exec")); //$NON-NLS-1$//$NON-NLS-2$
            run.setToolTipText(Messages.getString("ProcessComposite.execHint")); //$NON-NLS-1$
            run.setImage(ImageProvider.getImage(ERunprocessImages.RUN_PROCESS_ACTION));
            run.setData(ProcessView.EXEC_ID);
        }
        // }
        // }

    }

    private ConcurrentLinkedQueue<IProcessMessage> newMessages = new ConcurrentLinkedQueue<IProcessMessage>();

    private ConcurrentLinkedQueue<IProcessMessage> messagesToDisplay = new ConcurrentLinkedQueue<IProcessMessage>();

    protected void processNextMessage() {
        // one list for display, one list for the waiting pool.
        // don't try to display once the list to display is not finished to handle.
        if (messagesToDisplay.isEmpty() && !newMessages.isEmpty()) {
            IProcessMessage message = newMessages.poll();
            if (message == null) {
                return;
            }
            messagesToDisplay.add(message);
            getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    List<IProcessMessage> messages = new ArrayList<IProcessMessage>();
                    // only do a peek here, to get the first message, but without remove it (to make sure nothing else
                    // call the appendConsole)
                    IProcessMessage msg = messagesToDisplay.peek();
                    if (msg != null) {
                        messages.add(msg);
                        doAppendToConsole(messages);
                        scrollToEnd();
                    }

                    // do a poll here to remove the first element that we just displayed.
                    messagesToDisplay.poll();
                }
            });
        }
    }

    protected void appendToConsole(final IProcessMessage message) {
        newMessages.add(message);
    }

    /**
     * DOC ldong Comment method "getLog4jMsgType".
     * 
     * @param outType
     * @param lineContent
     * @return
     */
    private MsgType getLog4jMsgType(MsgType outType, String lineContent) {
        MsgType msgLog4jType = null;
        if (outType.equals(MsgType.STD_OUT)) {
            if (lineContent.startsWith("[TRACE]")) { //$NON-NLS-1$
                return MsgType.LOG4J_TRACE;
            } else if (lineContent.startsWith("[INFO ]")) { //$NON-NLS-1$
                return MsgType.LOG4J_INFO;
            } else if (lineContent.startsWith("[DEBUG]")) { //$NON-NLS-1$
                return MsgType.LOG4J_DEBUG;
            } else if (lineContent.startsWith("[WARN ]")) { //$NON-NLS-1$
                return MsgType.LOG4J_WARN;
            } else if (lineContent.startsWith("[ERROR]")) { //$NON-NLS-1$
                return MsgType.LOG4J_ERROR;
            } else if (lineContent.startsWith("[FATAL]")) { //$NON-NLS-1$
                return MsgType.LOG4J_FATAL;
            } else {
                return outType;
            }
        }
        return msgLog4jType;
    }

    private boolean processMessage(StringBuffer consoleText, IProcessMessage message, int startLength, List<StyleRange> styles) {
        String content = message.getContent();

        int lengthBeforeAdd = consoleText.length();

        String[] contents = content.split("\n"); //$NON-NLS-1$
        for (String content2 : contents) {
            if (isPattern(content2) || isPatternFor(content2)) {
                consoleText.append(""); //$NON-NLS-1$
                content = ""; //$NON-NLS-1$
            } else {
                consoleText.append(content2);
                consoleText.append("\n"); //$NON-NLS-1$
            }
        }
        boolean newStyle = false;
        if (message.getType() != MsgType.STD_OUT) {
            StyleRange style = new StyleRange();
            style.start = startLength + lengthBeforeAdd;
            style.length = content.length();
            if (message.getType() == MsgType.CORE_OUT || message.getType() == MsgType.CORE_ERR) {
                style.fontStyle = SWT.ITALIC;
            }
            Color color = getColor((MsgType) message.getType());
            style.foreground = color;

            if ((style.start + style.length) > (startLength + consoleText.length())) {
                style.length = startLength + consoleText.length() - style.start;
            }
            styles.add(style);
            newStyle = true;
        }
        return newStyle;
    }

    private void doAppendToConsole(Collection<IProcessMessage> messages) {
        if (consoleText == null || consoleText.isDisposed()) {
            return;
        }
        int linesLimit = getConsoleRowLimit();
        int currentLines = consoleText.getLineCount();
        if (linesLimit > 0 && currentLines > linesLimit) {
            return;
        }

        List<StyleRange> styles = new ArrayList<StyleRange>();
        StringBuffer consoleMsgText = new StringBuffer();
        int startLength = consoleText.getText().length();
        for (StyleRange curStyle : consoleText.getStyleRanges()) {
            styles.add(curStyle);
        }

        boolean newStyle = false;
        for (IProcessMessage message : messages) {
            if (message.getType() == MsgType.STD_OUT) {
                String[] splitLines = message.getContent().split("\n"); //$NON-NLS-1$
                for (String lineContent : splitLines) {
                    if (linesLimit > 0 && currentLines > linesLimit) {
                        return;
                    }
                    currentLines++;
                    IProcessMessage lineMsg = new ProcessMessage(getLog4jMsgType(MsgType.STD_OUT, lineContent), lineContent);
                    newStyle = newStyle | processMessage(consoleMsgText, lineMsg, startLength, styles);
                }
            } else {
                if (linesLimit > 0 && currentLines > linesLimit) {
                    return;
                }
                currentLines++;
                // count as only one line for the error, to avoid the error to be cut from original
                newStyle = newStyle | processMessage(consoleMsgText, message, startLength, styles);
            }
        }

        if (messages.size() > 1) {
            consoleText.setText(consoleText.getText() + consoleMsgText);
        } else {
            consoleText.append(consoleMsgText.toString());
        }
        if (newStyle) {
            consoleText.setStyleRanges(styles.toArray(new StyleRange[0]));
        }
    }

    /**
     * DOC chuang Comment method "setConsoleFont".
     */
    private void setConsoleFont() {
        IPreferenceStore preferenceStore = CorePlugin.getDefault().getPreferenceStore();
        FontData fontData = PreferenceConverter.getFontData(preferenceStore, TalendDesignerPrefConstants.CONSOLT_TEXT_FONT);
        if (fontData != null) {
            if (consoleText.getFont() != null) {
                FontData oldFont = consoleText.getFont().getFontData()[0];
                // font is same
                if (!oldFont.equals(fontData)) {
                    Font font = new Font(this.getDisplay(), fontData);
                    consoleText.setFont(font);
                } // else no need to change the font it is the same
            } else {
                Font font = new Font(this.getDisplay(), fontData);
                consoleText.setFont(font);
            }
        } else {// should never happend
            log.info("Could not find default font for the console"); //$NON-NLS-1$
        }
    }

    protected void fillConsole(Collection<IProcessMessage> messages) {
        if (consoleText == null || consoleText.isDisposed()) {
            return;
        }
        consoleText.setText(""); //$NON-NLS-1$
        doAppendToConsole(messages);
        scrollToEnd();
    }

    private void scrollToEnd() {
        if (consoleText.isDisposed()) {
            return;
        }
        consoleText.setTopIndex(consoleText.getLineCount() - 1);
    }

    private String getRowLimitContent(IProcessMessage message) {
        int currentRows = 0;
        String content = null;
        String[] rows = message.getContent().split("\n"); //$NON-NLS-1$
        int rowLimit = getConsoleRowLimit();

        if (rowLimit != SWT.DEFAULT) {
            currentRows++;
            if (currentRows >= rowLimit) {
                return ""; //$NON-NLS-1$
            } else if (currentRows + rows.length <= rowLimit) {
                content = message.getContent();
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < rowLimit - currentRows; i++) {
                    sb.append(rows[i]).append("\n"); //$NON-NLS-1$
                }
                content = sb.toString();
            }
        }
        return content;
    }

    private Color getColor(MsgType type) {
        Color color = null;
        switch (type) {
        case CORE_OUT:
            color = getDisplay().getSystemColor(SWT.COLOR_BLUE);
            break;
        case CORE_ERR:
            color = getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
            break;
        case STD_ERR:
            color = getDisplay().getSystemColor(SWT.COLOR_RED);
            break;
        case LOG4J_TRACE:
        case LOG4J_DEBUG:
        case LOG4J_INFO:
            color = getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
            break;
        case LOG4J_WARN:
            color = getDisplay().getSystemColor(SWT.COLOR_DARK_YELLOW);
            break;
        case LOG4J_ERROR:
        case LOG4J_FATAL:
            color = getDisplay().getSystemColor(SWT.COLOR_RED);
            break;
        case STD_OUT:
        default:
            color = getDisplay().getSystemColor(SWT.COLOR_BLACK);
            break;
        }
        return color;
    }

    /**
     * DOC bqian Comment method "addInHistoryRunningList".
     */
    protected void addInHistoryRunningList() {
        if (getProcessContext() == null) {
            return;
        }
        // Add this job to running history list.
        addTrace(1);
        IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (CorePlugin.getDefault().getDesignerCoreService().isTalendEditor(activeEditor)) {
            JobLaunchShortcutManager.run(activeEditor);
        } else {
            exec();
        }

    }

    private void addTrace(int itemId) {
        Boolean trace = false;
        processContext.setMonitorTrace(trace);
        processManager.setBooleanTrace(trace);
        org.talend.core.model.process.IProcess process = processContext.getProcess();
        List<INode> nodeList = (List<INode>) process.getGraphicalNodes();
        for (INode node : nodeList) {
            for (Connection connection : (List<Connection>) node.getOutgoingConnections()) {
                ConnectionTrace traceNode = connection.getConnectionTrace();
                if (traceNode == null) {
                    continue;
                }
                traceNode.setPropertyValue(EParameterName.TRACES_SHOW_ENABLE.getName(), trace);
                if (connection != null && connection.checkTraceShowEnable()) {
                    connection.setPropertyValue(EParameterName.TRACES_SHOW_ENABLE.getName(), trace);
                }
            }
        }
    }

    public void exec() {

        setHideconsoleLine(false);
        if (getProcessContext() == null) {
            return;
        }
        if (getProcessContext().getProcess() instanceof IProcess2) {
            ReplaceNodesInProcessProvider.beforeRunJobInGUI(getProcessContext().getProcess());
        }
        CorePlugin.getDefault().getRunProcessService().saveJobBeforeRun(getProcessContext().getProcess());
        if (processContext.isClearBeforeExec()) {
            processContext.clearMessages();
        }
        // processContext.
        // if (processContext.isWatchAllowed()) {
        // processContext.switchTime();
        // }
        processContext.setMonitorTrace(false);
        processContext.setWatchAllowed(processManager.getExecTime());
        processContext.setMonitorPerf(processManager.getStat());
        // processContext.setMonitorTrace(traceBtn.getSelection());
        /* check and save should be execute before processContext.exec,or it will cause dirty problem,bug 16791 */
        checkSaveBeforeRunSelection();
        processContext.setSelectedContext(processManager.getSelectContext());
        processContext.exec(processManager.getProcessShell());
        processContext.cleanWorkingDirectory();

        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunch[] launches = manager.getLaunches();
        manager.removeLaunches(launches);

    }

    public void kill() {
        killBtn.setEnabled(false);
        setHideconsoleLine(true);
        processContext.kill();
    }

    boolean debugMode = false;

    private MenuItem debugMenuItem;

    public void debug() {

        setHideconsoleLine(false);
        if ((processContext.getProcess()) instanceof org.talend.designer.core.ui.editor.process.Process) {
            ((org.talend.designer.core.ui.editor.process.Process) processContext.getProcess()).checkDifferenceWithRepository();
        }

        // final IPreferenceStore preferenceStore = DebugUIPlugin.getDefault().getPreferenceStore();
        final IPreferenceStore preferenceStore = DebugUITools.getPreferenceStore();
        final boolean oldValueConsoleOnOut = preferenceStore.getBoolean(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT);
        final boolean oldValueConsoleOnErr = preferenceStore.getBoolean(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR);

        preferenceStore.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT, false);

        preferenceStore.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR, false);

        checkSaveBeforeRunSelection();

        if (contextComposite.promptConfirmLauch()) {
            setRunnable(false);
            final IContext context = contextComposite.getSelectedContext();

            IRunnableWithProgress worker = new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) {
                    IProcessor processor = ProcessorUtilities.getProcessor(processContext.getProcess(), processContext
                            .getProcess().getProperty(), context);
                    monitor.beginTask("Launching debugger", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
                    try {
                        // use this function to generate childrens also.
                        ProcessorUtilities.generateCode(processContext.getProcess(), context, false, false, true, monitor);

                        ILaunchConfiguration config = ((Processor) processor).getDebugConfiguration(
                                processContext.getStatisticsPort(), processContext.getTracesPort(), null);

                        // see feature 0004820: The run job doesn't verify if
                        // code is correct before launching
                        if (!JobErrorsChecker.hasErrors(RunESBContainerComposite.this.getShell())) {

                            if (config != null) {
                                // PlatformUI.getWorkbench().
                                // getActiveWorkbenchWindow
                                // ().addPerspectiveListener(new
                                // DebugInNewWindowListener());
                                DebugUITools.launch(config, ILaunchManager.DEBUG_MODE);

                            } else {
                                MessageDialog.openInformation(getShell(), Messages.getString("ProcessDebugDialog.debugBtn"), //$NON-NLS-1$
                                        Messages.getString("ProcessDebugDialog.errortext")); //$NON-NLS-1$ 
                            }
                        }
                    } catch (ProcessorException e) {
                        IStatus status = new Status(IStatus.ERROR, RunProcessPlugin.PLUGIN_ID, IStatus.OK,
                                "Debug launch failed.", e); //$NON-NLS-1$
                        RunProcessPlugin.getDefault().getLog().log(status);
                        MessageDialog.openError(getShell(), Messages.getString("ProcessDebugDialog.debugBtn"), ""); //$NON-NLS-1$ //$NON-NLS-2$
                    } finally {
                        monitor.done();
                    }
                }
            };

            IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
            try {
                progressService.runInUI(PlatformUI.getWorkbench().getProgressService(), worker, ResourcesPlugin.getWorkspace()
                        .getRoot());
            } catch (InvocationTargetException e) {
                // e.printStackTrace();
                ExceptionHandler.process(e);
            } catch (InterruptedException e) {
                // e.printStackTrace();
                ExceptionHandler.process(e);
            }
        }

        debugMode = true;
        try {
            Thread thread = new Thread() {

                @Override
                public void run() {
                    while (debugMode) {
                        final IProcess process = DebugUITools.getCurrentProcess();
                        if (process != null && process.isTerminated()) {
                            getDisplay().asyncExec(new Runnable() {

                                @Override
                                public void run() {
                                    setRunnable(true);
                                    killBtn.setEnabled(false);
                                    preferenceStore.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT, oldValueConsoleOnOut);

                                    preferenceStore.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR, oldValueConsoleOnErr);

                                    if (isAddedStreamListener) {
                                        process.getStreamsProxy().getOutputStreamMonitor().removeListener(streamListener);
                                        isAddedStreamListener = false;

                                        if (processContext.isRunning()) {
                                            final String endingPattern = Messages.getString("ProcessComposite.endPattern"); //$NON-NLS-1$
                                            MessageFormat mf = new MessageFormat(endingPattern);
                                            String byeMsg;
                                            try {
                                                byeMsg = "\n" //$NON-NLS-1$
                                                        + mf.format(new Object[] { processContext.getProcess().getName(),
                                                                new Date(), new Integer(process.getExitValue()) });
                                                processContext.addDebugResultToConsole(new ProcessMessage(MsgType.CORE_OUT,
                                                        byeMsg));
                                            } catch (DebugException e) {
                                                // e.printStackTrace();
                                                ExceptionHandler.process(e);
                                            }
                                            processContext.setRunning(false);

                                        }
                                    }
                                    debugMode = false;
                                }
                            });
                        } else {
                            if (process != null) { // (one at leat) process
                                // still running
                                getDisplay().asyncExec(new Runnable() {

                                    @Override
                                    public void run() {
                                        setRunnable(false);
                                        killBtn.setEnabled(true);
                                        processContext.setRunning(true);
                                        processContext.setDebugProcess(process);
                                        if (!isAddedStreamListener) {
                                            process.getStreamsProxy().getOutputStreamMonitor().addListener(streamListener);
                                            // if (clearBeforeExec.getSelection()) {
                                            // processContext.clearMessages();
                                            // }
                                            // if (watchBtn.getSelection()) {
                                            // processContext.switchTime();
                                            // }

                                            ClearPerformanceAction clearPerfAction = new ClearPerformanceAction();
                                            clearPerfAction.setProcess(processContext.getProcess());
                                            clearPerfAction.run();

                                            ClearTraceAction clearTraceAction = new ClearTraceAction();
                                            clearTraceAction.setProcess(processContext.getProcess());
                                            clearTraceAction.run();
                                            isAddedStreamListener = true;

                                            final String startingPattern = Messages.getString("ProcessComposite.startPattern"); //$NON-NLS-1$
                                            MessageFormat mf = new MessageFormat(startingPattern);
                                            String welcomeMsg = mf.format(new Object[] { processContext.getProcess().getName(),
                                                    new Date() });
                                            processContext.addDebugResultToConsole(new ProcessMessage(MsgType.CORE_OUT,
                                                    welcomeMsg + "\r\n"));//$NON-NLS-1$
                                        }
                                    }
                                });
                            } else { // no process running
                                getDisplay().asyncExec(new Runnable() {

                                    @Override
                                    public void run() {
                                        setRunnable(true);
                                        killBtn.setEnabled(false);
                                    }
                                });
                            }
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            // e.printStackTrace();
                            ExceptionHandler.process(e);
                        }
                    }
                }
            };
            thread.start();
        } catch (Exception e) {
            ExceptionHandler.process(e);
            processContext.addErrorMessage(e);
            kill();
        }
    }

    /**
     * DOC Administrator Comment method "checkSaveBeforeRunSelection".
     */
    private void checkSaveBeforeRunSelection() {
        /* processContext can store the save button value of current processView,bug 16791 */
        if (processContext.isSaveBeforeRun()) {
            SaveJobBeforeRunAction action = new SaveJobBeforeRunAction(processContext.getProcess());
            action.run();
        }
    }

    private void runProcessContextChanged(final PropertyChangeEvent evt) {
        if (isDisposed()) {
            return;
        }
        // add by Mike
        processNextMessage();
        String propName = evt.getPropertyName();
        if (ProcessMessageManager.UPDATE_CONSOLE.equals(propName)) {
            processNextMessage();
        } else if (ProcessMessageManager.PROP_MESSAGE_ADD.equals(propName)
                || ProcessMessageManager.PROP_DEBUG_MESSAGE_ADD.equals(propName)) {
            IProcessMessage psMess = (IProcessMessage) evt.getNewValue();

            if (errorMessMap.size() <= CorePlugin.getDefault().getPreferenceStore()
                    .getInt(ITalendCorePrefConstants.PREVIEW_LIMIT)) {
                if (!(LanguageManager.getCurrentLanguage().equals(ECodeLanguage.PERL))) {
                    getAllErrorMess(psMess);
                } else {
                    addPerlMark(psMess);
                }
            }
            appendToConsole(psMess);
        } else if (ProcessMessageManager.PROP_MESSAGE_CLEAR.equals(propName)) {
            newMessages.clear();
            messagesToDisplay.clear();
            getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    if (!consoleText.isDisposed()) {
                        consoleText.setText(""); //$NON-NLS-1$
                    }
                }
            });
        } else if (RunProcessContext.PROP_MONITOR.equals(propName)) {
            // perfBtn.setSelection(((Boolean) evt.getNewValue()).booleanValue());
        } else if (RunProcessContext.TRACE_MONITOR.equals(propName)) {
            // traceBtn.setSelection(((Boolean) evt.getNewValue()).booleanValue());
        } else if (RunProcessContext.PROP_RUNNING.equals(propName)) {
            getDisplay().asyncExec(new Runnable() {

                @Override
                public void run() {
                    if (isDisposed()) {
                        return;
                    }
                    boolean running = ((Boolean) evt.getNewValue()).booleanValue();
                    setRunnable(!running);
                    killBtn.setEnabled(running);
                    while (!newMessages.isEmpty()) {
                        messagesToDisplay.add(newMessages.poll());
                    }
                    doAppendToConsole(messagesToDisplay);
                    scrollToEnd();
                    messagesToDisplay.clear();
                }
            });
        }
    }

    @Override
    public Display getDisplay() {
        return Display.getDefault();
    }

    /**
     * Getter for targetExecutionTabItem.
     * 
     * @return the targetExecutionTabItem
     */
    // public CTabItem getTargetExecutionTabItem() {
    // return this.targetExecutionTabItem;
    // }
    //
    // /**
    // * Getter for leftTabFolder.
    // *
    // * @return the leftTabFolder
    // */
    // public CTabFolder getLeftTabFolder() {
    // return this.leftTabFolder;
    // }

    public static RunProcessContext getProcessContext() {
        return processContext;
    }

    /**
     * Getter for infoview.
     * 
     * @return the infoview
     */
    public boolean isHideConsoleLine() {
        return this.hideConsoleLine;
    }

    /**
     * Sets the infoview.
     * 
     * @param infoview the infoview to set
     */
    public void setHideconsoleLine(boolean infoview) {
        this.hideConsoleLine = infoview;
    }

    public void setDebugEnabled(boolean enabled) {
        IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                IBrandingService.class);
        if (brandingService.getBrandingConfiguration().isAllowDebugMode()) {
            debugMenuItem.setEnabled(enabled);
        }
    }

    public void getAllErrorMess(IProcessMessage psMess) {
        errorMessMap.putAll(ProcessErrorUtil.getAllErrorMess(psMess, processContext));
        refreshNode(psMess);
    }

    public void refreshNode(final IProcessMessage psMess) {
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                if (processContext == null) {
                    return;
                }
                org.talend.core.model.process.IProcess process = processContext.getProcess();
                if (process == null) {
                    return;
                }
                List<INode> nodeList = (List<INode>) process.getGraphicalNodes();
                for (INode inode : nodeList) {
                    if (!inode.isActivate()) {
                        continue;
                    }
                    String nodeUniqueName = inode.getUniqueName();
                    if (LanguageManager.getCurrentLanguage().equals(ECodeLanguage.PERL) && Problems.nodeList.size() > 0) {
                        errorMessMap.clear();
                    }
                    if (errorMessMap.get(nodeUniqueName) != null) {
                        if (inode instanceof Node) {
                            IProcessMessage messPro = errorMessMap.get(nodeUniqueName);
                            Node node = (Node) inode;
                            node.setErrorFlag(true);
                            node.setCompareFlag(false);
                            node.setErrorInfo(messPro.getContent());
                            node.getNodeError().updateState("UPDATE_STATUS", true); //$NON-NLS-1$
                            if (node.isFileScaleComponent()) {
                                refreshProgress(psMess, node, nodeUniqueName);
                            }
                            node.setErrorInfoChange("ERRORINFO", true); //$NON-NLS-1$
                        }
                    } else {
                        if (inode instanceof Node) {
                            Node node = (Node) inode;
                            if (Problems.nodeList.size() > 0) {
                                String befor = "Error in the component's properties:"; //$NON-NLS-1$
                                Iterator<Entry<Node, StringBuffer>> set = Problems.nodeList.entrySet().iterator();
                                while (set.hasNext()) {
                                    Entry<Node, StringBuffer> en = set.next();
                                    Node no = en.getKey();
                                    String des = en.getValue().toString();
                                    if (node == no) {
                                        node.setErrorFlag(true);
                                        node.setCompareFlag(false);
                                        node.setErrorInfo(befor + des);
                                        node.getNodeError().updateState("UPDATE_STATUS", false);//$NON-NLS-1$
                                        node.setErrorInfoChange("ERRORINFO", true);//$NON-NLS-1$
                                    }
                                }
                            } else {
                                if (node.isErrorFlag() == true) {
                                    node.setErrorFlag(false);
                                    node.setCompareFlag(false);
                                    node.setErrorInfo(null);
                                    node.getNodeError().updateState("UPDATE_STATUS", false); //$NON-NLS-1$
                                }

                                if (node.isFileScaleComponent()) {
                                    refreshProgress(psMess, node, nodeUniqueName);
                                }
                                if (node.isErrorFlag() == true) {
                                    node.setErrorInfoChange("ERRORINFO", false); //$NON-NLS-1$
                                }
                            }
                        }
                    }
                }
            }

        });
    }

    public void refreshProgress(IProcessMessage psMess, Node node, String nodeUniqueName) {
        String mess = ""; //$NON-NLS-1$
        String uniqueName = ""; //$NON-NLS-1$
        String[] message = psMess.getContent().split("\n"); //$NON-NLS-1$

        for (String element : message) {
            if (isPattern(element)) {

                int firIndex = element.indexOf("$"); //$NON-NLS-1$
                int secIndex = element.indexOf("%"); //$NON-NLS-1$
                uniqueName = element.substring(0, firIndex);
                mess = element.substring(firIndex + 1, secIndex);
            }

            Double extentPro = new Double(0);
            if ((!"".equals(mess)) && mess != null) { //$NON-NLS-1$  
                extentPro = Double.parseDouble(mess);
            }

            if (((extend != extentPro) && nodeUniqueName.equals(uniqueName))) {
                node.getNodeProgressBar().updateState("UPDATE_STATUS", extentPro); //$NON-NLS-1$
                extend = extentPro;
            }

        }

    }

    public void refreshNodeContainer() {
        org.talend.core.model.process.IProcess process = processContext.getProcess();
        List<? extends INode> nodeList = process.getGraphicalNodes();
        for (INode node : nodeList) {
            if ((node instanceof Node) && (((Node) node).isMapReduceStart())) {
                ((JobletContainer) ((Node) node).getNodeContainer()).updateState(
                        "UPDATE_STATUS", "CLEAR", new Double(0), new Double(0)); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    /*
     * This method is used to clean the Spark Streaming statistics.
     */
    public void refreshSubjobContainer() {
        org.talend.core.model.process.IProcess2 process = processContext.getProcess();
        List<? extends ISubjobContainer> subjobContainers = process.getSubjobContainers();
        for (ISubjobContainer subjobContainer : subjobContainers) {
            if (subjobContainer instanceof SparkStreamingSubjobContainer) {
                ((SparkStreamingSubjobContainer) subjobContainer).updateState(
                        "UPDATE_SPARKSTREAMING_STATUS", null, 0, 0, "", "", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            }
        }
    }

    private boolean isPattern(String content) {
        Pattern pattern = Pattern.compile("\\$\\s*\\d+(\\.\\d*)?%"); //$NON-NLS-1$
        Matcher m = pattern.matcher(content);
        return m.find();
    }

    private boolean isPatternFor(String content) {
        Pattern pattern = Pattern.compile("\\[\\s*\\d+(\\.\\d*)?%\\]"); //$NON-NLS-1$
        Matcher m = pattern.matcher(content);
        return m.find();
    }

    protected void addPerlMark(IProcessMessage psMess) {
        if (psMess.getType().equals(MsgType.STD_ERR)) {
            String content = psMess.getContent();
            String path = null;
            String uniName = null;
            int lineNo = -1;
            Pattern errorPattern = Pattern.compile("(.*) at (\\S+) line (\\d+)[\\.,]");//$NON-NLS-1$
            Matcher m = errorPattern.matcher(content);
            String matchContent = null;

            while (m.find()) {
                path = m.group(2);
                lineNo = parseInt(m.group(3));

                matchContent = m.group();

                if ((!("".equals(path)) && path != null) && lineNo > 0) {//$NON-NLS-1$ 
                    uniName = Problems.setErrorMark(path, lineNo);
                }

                if (uniName != null) {
                    if (!errorMessMap.containsKey(uniName)) {
                        errorMessMap.put(uniName, new ProcessMessage(MsgType.STD_ERR, matchContent));
                    } else {
                        String uniMess = errorMessMap.get(uniName).getContent();
                        errorMessMap.put(uniName, new ProcessMessage(MsgType.STD_ERR, uniMess.concat(matchContent)));
                    }
                }

            }
        }
        refreshNode(psMess);
    }

    private int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private List<Node> getTRunjobList(org.talend.core.model.process.IProcess process) {
        List<Node> trunjobList = new ArrayList<Node>();
        if (!(process instanceof Process)) {
            return trunjobList;
        }
        List<INode> nodeList = (List<INode>) ((Process) process).getGraphicalNodes();
        for (INode node : nodeList) {
            if (node.getComponent().getName().equals("tRunJob")) { //$NON-NLS-1$
                if (node instanceof Node) {
                    trunjobList.add((Node) node);
                }
            }
        }
        return trunjobList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.properties.tab.IDynamicProperty#getComposite()
     */
    @Override
    public Composite getComposite() {
        // TODO Auto-generated method stub
        return composite;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.properties.tab.IDynamicProperty#getCurRowSize()
     */
    @Override
    public int getCurRowSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.properties.tab.IDynamicProperty#getElement()
     */
    @Override
    public Element getElement() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.properties.tab.IDynamicProperty#getHashCurControls()
     */
    @Override
    public BidiMap getHashCurControls() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.properties.tab.IDynamicProperty#getPart()
     */
    @Override
    public IMultiPageTalendEditor getPart() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.talend.core.properties.tab.IDynamicProperty#getRepositoryAliasName(org.talend.core.model.properties.
     * ConnectionItem)
     */
    @Override
    public String getRepositoryAliasName(ConnectionItem connectionItem) {
        // TODO Auto-generated method stub
        return null;
    }

    /* 16969 */
    // /*
    // * (non-Javadoc)
    // *
    // * @see org.talend.core.properties.tab.IDynamicProperty#getRepositoryConnectionItemMap()
    // */
    // public Map<String, ConnectionItem> getRepositoryConnectionItemMap() {
    // // TODO Auto-generated method stub
    // return null;
    // }

    // /*
    // * (non-Javadoc)
    // *
    // * @see org.talend.core.properties.tab.IDynamicProperty#getRepositoryQueryStoreMap()
    // */
    // public Map<String, Query> getRepositoryQueryStoreMap() {
    // // TODO Auto-generated method stub
    // return null;
    // }

    // /*
    // * (non-Javadoc)
    // *
    // * @see org.talend.core.properties.tab.IDynamicProperty#getRepositoryTableMap()
    // */
    // public Map<String, IMetadataTable> getRepositoryTableMap() {
    // // TODO Auto-generated method stub
    // return null;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.properties.tab.IDynamicProperty#getSection()
     */
    @Override
    public EComponentCategory getSection() {
        // TODO Auto-generated method stub
        return EComponentCategory.BASICRUN;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.properties.tab.IDynamicProperty#getTableIdAndDbSchemaMap()
     */
    @Override
    public Map<String, String> getTableIdAndDbSchemaMap() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.properties.tab.IDynamicProperty#getTableIdAndDbTypeMap()
     */
    @Override
    public Map<String, String> getTableIdAndDbTypeMap() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.properties.tab.IDynamicProperty#refresh()
     */
    @Override
    public void refresh() {
        // TODO Auto-generated method stub
        if (!isDisposed()) {
            getParent().layout();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.properties.tab.IDynamicProperty#setCurRowSize(int)
     */
    @Override
    public void setCurRowSize(int i) {
        // TODO Auto-generated method stub

    }

    /**
     * DOC Administrator Comment method "setProcessViewHelper".
     * 
     * @param processViewHelper
     */
    public void setProcessViewHelper(IProcessViewHelper processViewHelper) {
        // TODO Auto-generated method stub
        this.processViewHelper = processViewHelper;
    }

}