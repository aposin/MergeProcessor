/**
 * Copyright 2019 Association for the promotion of open-source insurance software and for the establishment of open interface standards in the insurance industry (Verein zur Förderung quelloffener Versicherungssoftware und Etablierung offener Schnittstellenstandards in der Versicherungsbranche)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aposin.mergeprocessor.application;

import java.net.URL;

import javax.inject.Inject;

import org.aposin.mergeprocessor.configuration.Configuration;
import org.aposin.mergeprocessor.configuration.IConfiguration;
import org.aposin.mergeprocessor.configuration.PreferenceInitializer;
import org.aposin.mergeprocessor.model.ICredentialProvider;
import org.aposin.mergeprocessor.model.IVersionProvider;
import org.aposin.mergeprocessor.model.InstantUserAuthentication;
import org.aposin.mergeprocessor.model.PomFileVersionProvider;
import org.aposin.mergeprocessor.model.svn.ISvnClient;
import org.aposin.mergeprocessor.model.svn.SvnClientJavaHl;
import org.aposin.mergeprocessor.renaming.H2DatabaseSetup;
import org.aposin.mergeprocessor.renaming.IFileSystemProvider;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.aposin.mergeprocessor.utils.MergeProcessorUtilException;
import org.aposin.mergeprocessor.utils.SftpFileSystemProvider;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.UIEvents.UILifeCycle;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessAdditions;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.event.Event;

/**
 * Life cycle hook class for the merge processor.
 * 
 * @author Stefan Weiser
 *
 */
public class LifeCycleHook {

	private static final String TRAY_ICON_PATH = "icons/v_collection_png/16x16/plain/graph_edge_directed.png";
	private static final String TRAY_ICON = "trayIcon";

	/**
	 * Adds the configuration directly after context creation, because it is
	 * required in the {@link ToolbarProcessor}.
	 * 
	 * @param context the context to enrich
	 */
	@PostContextCreate
	public void postContextCreate(IEclipseContext context) {
		context.set(IConfiguration.class, ContextInjectionFactory.make(Configuration.class, context));
	}

	/**
	 * Adds additional elements to the application context.
	 * 
	 * @param context the context to enrich
	 * @param display the display
	 */
	@ProcessAdditions
	public void addElementsToContext(IEclipseContext context, Display display) {
		/*
		 * The PreferenceInitializer is not registered via the extension point
		 * org.eclipse.core.runtime.preferences as we cannot access the instance of
		 * IConfiguration because the workbench is not initialized at the time when it
		 * is required. So it is called directly here and the IConfiguration is handed
		 * directly.
		 */
		new PreferenceInitializer(context.get(IConfiguration.class)).initializeDefaultPreferences();
		context.set(ICredentialProvider.class, ContextInjectionFactory.make(InstantUserAuthentication.class, context));
		context.set(ISvnClient.class, ContextInjectionFactory.make(SvnClientJavaHl.class, context));
		context.set(IVersionProvider.class, ContextInjectionFactory.make(PomFileVersionProvider.class, context));
		context.set(IFileSystemProvider.class, ContextInjectionFactory.make(SftpFileSystemProvider.class, context));

		copyH2ToLocalIfRequired(context.get(IConfiguration.class), display);
	}

	/**
	 * Executes startup tasks when the application start has been finished.
	 * 
	 * @param event the event on {@link UILifeCycle#APP_STARTUP_COMPLETE}
	 * @param view
	 * @param context
	 * @param part
	 */
	@Optional
	@Inject
	public void appStartupComplete(@UIEventTopic(UILifeCycle.APP_STARTUP_COMPLETE) final Event event,
			@Active IShellProvider shellProvider, IConfiguration configuration, IEclipseContext context) {
		registerSystemTray(shellProvider.getShell());
	}

	/**
	 * Registers the system tray icon, so the merge processor is running behind, even if it gets closed by
	 * the user. The application should only be closed when shutting down from the system tray.
	 * 
	 * @param shell the shell of the application
	 */
	private void registerSystemTray(final Shell shell) {
		final Display display = shell.getDisplay();
		Tray systemTray = display.getSystemTray();

		if (systemTray != null) {
			Listener listenerOpen = e -> {
				shell.setVisible(true);
				shell.setFocus();
				shell.setMinimized(false);

			};

			TrayItem trayItem = new TrayItem(systemTray, SWT.NONE);
			trayItem.setToolTipText(Messages.ApplicationWorkbenchWindowAdvisor_Tray_Tooltip);

			initializeImage(TRAY_ICON, TRAY_ICON_PATH);
			trayItem.setImage(JFaceResources.getImage(TRAY_ICON));

			trayItem.addListener(SWT.Selection, listenerOpen);

			final Menu menu = new Menu(shell, SWT.POP_UP);
			MenuItem menuItem;
			menuItem = new MenuItem(menu, SWT.PUSH);
			menuItem.setText(Messages.ApplicationWorkbenchWindowAdvisor_Tray_Open);
			menuItem.addListener(SWT.Selection, listenerOpen);

			menuItem = new MenuItem(menu, SWT.PUSH);
			menuItem.setText(Messages.ApplicationWorkbenchWindowAdvisor_Tray_Close);
			menuItem.addListener(SWT.Selection, e -> {
				// shutdown
				display.syncExec(() -> {
					// save window position and size
					Point location = shell.getLocation();
					Point size = shell.getSize();

					Configuration.setWindowLocation(location);
					Configuration.setWindowSize(size);

					// close
					PlatformUI.getWorkbench().close();
				});
			});

			trayItem.addListener(SWT.MenuDetect, e -> display.syncExec(() -> menu.setVisible(true)));
		}

		shell.addShellListener(new ShellAdapter() {

			@Override
			public void shellClosed(ShellEvent e) {
				// for actual prevention of a shutdown see
				// org.aposin.mergeprocessor.application.ApplicationWorkbenchWindowAdvisor.preWindowShellClose()
				// this code here doesn't work :(

				// "minimize" shell to try
				shell.setVisible(false);
				// don't allow the operation
				e.doit = false;
			}
		});
	}

	/**
	 * Copies the H2 renaming database to the local directory if required.
	 * 
	 * @param configuration the configuration
	 * @param shell the shell of the application
	 */
	private void copyH2ToLocalIfRequired(final IConfiguration configuration, final Display display) {
		try {
			new H2DatabaseSetup(display, configuration).downloadH2FileDatabaseIfRequired();
		} catch (MergeProcessorUtilException e) {
			LogUtil.throwing(e);
			StatusManager.getManager().handle(
					ValidationStatus.error("An error occurred during copying H2 renaming database.", e),
					StatusManager.BLOCK | StatusManager.SHOW);
		}
	}

	/**
	 * Executes shutdown tasks when the application gets closed.
	 * 
	 * @param event the event on {@link UILifeCycle#APP_SHUTDOWN_STARTED}
	 * @param svnClient the registered SVN client which has to be closed
	 */
	@Optional
	@Inject
	public void appShutdownStarted(@UIEventTopic(UILifeCycle.APP_SHUTDOWN_STARTED) final Event event,
			final ISvnClient svnClient) {
		svnClient.close();
	}

	private static void initializeImage(final String name, final String path) {
		final ImageRegistry imageRegistry = JFaceResources.getImageRegistry();
		if (imageRegistry.get(name) == null) {
			URL imageClockUrl = FileLocator.find(Activator.getDefault().getBundle(), new Path(path), null);
			if (imageClockUrl != null) {
				imageRegistry.put(name, ImageDescriptor.createFromURL(imageClockUrl));
			}
		}
	}

}
