// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.camel.spring.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.talend.camel.core.model.camelProperties.CamelProcessItem;
import org.talend.camel.core.model.camelProperties.CamelPropertiesFactory;
import org.talend.camel.designer.util.ECamelCoreImage;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.runtime.exception.ExceptionHandler;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.Property;
import org.talend.core.model.relationship.RelationshipItemBuilder;
import org.talend.core.model.routines.RoutinesUtil;
import org.talend.designer.camel.spring.core.CamelSpringParser;
import org.talend.designer.camel.spring.ui.listeners.SpringParserListener;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.model.utils.emf.talendfile.ParametersType;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;
import org.talend.designer.core.model.utils.emf.talendfile.RoutinesParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.TalendFileFactory;
import org.talend.repository.RepositoryWorkUnit;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.ui.views.IRepositoryView;

/**
 * Wizard for the creation of a new project. <br/>
 * 
 * $Id: ImportSpringXMLWizard.java 52559 2010-12-13 04:14:06Z $
 * 
 */
public class ImportSpringXMLWizard extends Wizard {

    /** Main page. */
    private ImportSpringXMLWizardPage mainPage;

    /** Created project. */
    private CamelProcessItem processItem;

    private Property property;

    private IPath path;

    private IProxyRepositoryFactory repositoryFactory;

    private String springXMLPath;

    /**
     * Constructs a new NewProjectWizard.
     * 
     * @param author Project author.
     * @param server
     * @param password
     */
    public ImportSpringXMLWizard(IPath path) {
        super();
        this.path = path;

        this.property = PropertiesFactory.eINSTANCE.createProperty();
        this.property.setAuthor(((RepositoryContext) CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY))
                .getUser());
        this.property.setVersion(VersionUtils.DEFAULT_VERSION);
        this.property.setStatusCode(""); //$NON-NLS-1$

        this.processItem = CamelPropertiesFactory.eINSTANCE.createCamelProcessItem();

        this.processItem.setProperty(property);

        this.repositoryFactory = DesignerPlugin.getDefault().getRepositoryService().getProxyRepositoryFactory();

        this.setDefaultPageImageDescriptor(ImageProvider.getImageDesc(ECamelCoreImage.ROUTES_WIZ));

        this.setHelpAvailable(false);

        this.setNeedsProgressMonitor(true);
    }

    /**
     * @see org.eclipse.jface.wizard.Wizard#addPages()
     */
    @Override
    public void addPages() {
        mainPage = new ImportSpringXMLWizardPage(property, path);
        addPage(mainPage);
        setWindowTitle("Import Routes From Spring XML");
    }

    /**
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {

        try {
            property.setId(repositoryFactory.getNextId());
            springXMLPath = mainPage.getXMLPath();

            ProcessType process = TalendFileFactory.eINSTANCE.createProcessType();
            ParametersType parameterType = TalendFileFactory.eINSTANCE.createParametersType();
            // add depended routines.
            List<RoutinesParameterType> dependenciesInPreference = RoutinesUtil.createDependenciesInPreference();
            parameterType.getRoutinesParameter().addAll(dependenciesInPreference);
            process.setParameters(parameterType);
            processItem.setProcess(process);
            RepositoryWorkUnit<Object> workUnit = new RepositoryWorkUnit<Object>(this.getWindowTitle(), this) {

                @Override
                protected void run() throws LoginException, PersistenceException {
                    repositoryFactory.create(processItem, mainPage.getDestinationPath());
                    RelationshipItemBuilder.getInstance().addOrUpdateItem(processItem);
                }
            };
            workUnit.setAvoidUnloadResources(true);
            repositoryFactory.executeRepositoryWorkUnit(workUnit);
            parseAndImportContent();

        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Error", "Import Spring XML failed, details: " + e.getMessage());
            ExceptionHandler.process(e);
            // IRepositoryNode repositoryNode =
            // RepositoryNodeUtilities.getRepositoryNode(processItem.getProperty().getId(), false);
            // try {
            // repositoryFactory.deleteObjectPhysical(repositoryNode.getObject());
            // getViewPart().refresh(repositoryNode.getObjectType());
            // } catch (PersistenceException e1) {
            // e1.printStackTrace();
            // }
        }
        return processItem != null;
    }

    /**
     * 
     * DOC LiXP Comment method "parseAndImportContent".
     * 
     * @throws Exception
     */
    private void parseAndImportContent() throws Exception {

        ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
        dialog.run(false, false, new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                CamelSpringParser parser = new CamelSpringParser();
                parser.addListener(new SpringParserListener(processItem, getViewPart()));
                try {
                    parser.startParse(springXMLPath);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }

            }
        });
        dialog.open();

    }

    /**
     * 
     * Returns the repository view..
     * 
     * @return - the repository biew
     */
    public IRepositoryView getViewPart() {
        IViewPart viewPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .findView(IRepositoryView.VIEW_ID);
        return (IRepositoryView) viewPart;
    }

    /**
     * Getter for project.
     * 
     * @return the project
     */
    public CamelProcessItem getProcess() {
        return this.processItem;
    }

    /*
     * 
     */
    public String getSpringXMLPath() {
        return springXMLPath;
    }
}
