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
package org.aposin.mergeprocessor.configuration;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.aposin.mergeprocessor.application.Activator;
import org.aposin.mergeprocessor.utils.MergeProcessorUtil;
import org.aposin.mergeprocessor.view.GitRepositoryPreferencePageView;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This preference page shows the currently available GIT repositories managed by the merge processor. 
 * 
 * @author Stefan Weiser
 *
 */
public class GitRepositoriesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String GIT_REPOSITORIES_AUTO_REPO_CREATE = "GIT_REPOSITORIES_AUTO_REPO_CREATE"; //$NON-NLS-1$
    public static final String GIT_REPOSITORIES_FOLDER = "GIT_REPOSITORIES_FOLDER"; //$NON-NLS-1$

    private TableViewer tableViewer;
    private IConfiguration configuration;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription(Messages.GitRepositoriesPreferencePage_description);
        configuration = ((org.eclipse.e4.ui.workbench.IWorkbench) workbench).getApplication().getContext()
                .get(IConfiguration.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        /*
         * The following field may be required if GIT repositories should not be created automatically,
         * as they are provided by a central shared directory, where many developers use these repositories
         * for GIT merge.
         */
        final Path gitRepositories = Paths
                .get(Activator.getDefault().getPreferenceStore().getString(GIT_REPOSITORIES_FOLDER));
        if (!gitRepositories.toFile().exists()) {
            try {
                Files.createDirectories(gitRepositories);
            } catch (IOException e) {
                Logger.getLogger(GitRepositoriesPreferencePage.class.getName()).log(Level.SEVERE, e.getMessage(), e);
            }
        }
        final DirectoryFieldEditor repositoryFolderEditor = new DirectoryFieldEditor(GIT_REPOSITORIES_FOLDER,
                Messages.GitRepositoriesPreferencePage_title, getFieldEditorParent()) {

            /**
             * {@inheritDoc}
             */
            @Override
            protected void fireValueChanged(String property, Object oldValue, Object newValue) {
                super.fireValueChanged(property, oldValue, newValue);
                if (FieldEditor.VALUE.equals(property) && !Objects.equals(oldValue, newValue)) {
                    tableViewer.setInput(getGitRepositoryPaths(newValue.toString()));
                }
            }

        };
        addField(repositoryFolderEditor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createContents(Composite parent) {
        final Composite fieldEditorParent = (Composite) super.createContents(parent);

        final GitRepositoryPreferencePageView view = new GitRepositoryPreferencePageView(fieldEditorParent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(view);
        tableViewer = view.getTableViewer();
        tableViewer.setContentProvider(new ArrayContentProvider());
        view.getColumnRepository().setLabelProvider(new RepositoryColumnLabelProvider());
        view.getColumnLocalPath().setLabelProvider(new LocalPathColumnLabelProvider());
        view.getColumnMemory().setLabelProvider(new MemoryColumnLabelProvider());
        tableViewer.setInput(getGitRepositoryPaths(configuration.getGitRepositoryFolder()));

        final MenuItem item = view.getMenuItemGoToRepository();
        item.setEnabled(!tableViewer.getSelection().isEmpty());
        item.addListener(SWT.Selection, e -> openRepositoryOnSelection(tableViewer));
        tableViewer.addSelectionChangedListener(e -> item.setEnabled(!e.getSelection().isEmpty()));

        return fieldEditorParent;
    }

    /**
     * @param tableViewer the table viewer on which selected items the folder should be opened
     */
    private static void openRepositoryOnSelection(final TableViewer tableViewer) {
        final Iterator<?> iterator = tableViewer.getStructuredSelection().iterator();
        while (iterator.hasNext()) {
            MergeProcessorUtil.openFolder(iterator.next().toString());
        }
    }

    /**
     * @return a list of existing GIT repositories
     */
    private List<Path> getGitRepositoryPaths(final String gitRepositoryFolder) {
        if (Paths.get(gitRepositoryFolder).toFile().exists()) {
            final GitRepositoryFileVisitor fileVisitor = new GitRepositoryFileVisitor();
            try {
                Files.walkFileTree(Paths.get(gitRepositoryFolder), fileVisitor);
                return fileVisitor.getRepositories();
            } catch (IOException e) {
                Logger.getLogger(GitRepositoriesPreferencePage.class.getName()).log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * File Visitor for searching GIT repositories.
     * 
     * @author Stefan Weiser
     *
     */
    public static class GitRepositoryFileVisitor extends SimpleFileVisitor<Path> {

        private final List<Path> gitRepos = new ArrayList<>();

        /**
         * {@inheritDoc}
         */
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            super.preVisitDirectory(dir, attrs);
            if (dir.endsWith(".git")) { //$NON-NLS-1$
                gitRepos.add(dir.getParent());
                return FileVisitResult.SKIP_SUBTREE;
            } else {
                return FileVisitResult.CONTINUE;
            }
        }

        /**
         * @return the list of existing GIT repositories
         */
        public List<Path> getRepositories() {
            return gitRepos;
        }

    }

    /**
     * {@link ColumnLabelProvider} for the repository column.
     * 
     * @author Stefan Weiser
     *
     */
    private static class RepositoryColumnLabelProvider extends ColumnLabelProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText(Object element) {
            if (element instanceof Path) {
                try (final Git repo = Git.open(((Path) element).toFile())) {
                    return repo.getRepository().getConfig().getString(ConfigConstants.CONFIG_REMOTE_SECTION, "origin", //$NON-NLS-1$
                            ConfigConstants.CONFIG_KEY_URL);
                } catch (IOException e) {
                    Logger.getLogger(GitRepositoriesPreferencePage.class.getName()).log(Level.SEVERE, e.getMessage(),
                            e);
                }
            }
            return super.getText(element);
        }

    }

    /**
     * {@link ColumnLabelProvider} for the local path column.
     * 
     * @author Stefan Weiser
     *
     */
    private static class LocalPathColumnLabelProvider extends ColumnLabelProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText(Object element) {
            if (element instanceof Path) {
                final Path path = (Path) element;
                final Path parent = ((Path) element).getParent();
                return path.relativize(parent).resolve(path.getFileName()).toString();
            }
            return super.getText(element);
        }

    }

    /**
     * {@link ColumnLabelProvider}  for the memory column.
     * 
     * @author Stefan Weiser
     *
     */
    private static class MemoryColumnLabelProvider extends ColumnLabelProvider {

        private static final DecimalFormat FORMAT = new DecimalFormat("#.00"); //$NON-NLS-1$

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText(Object element) {
            if (element instanceof Path) {
                final long size = FileUtils.sizeOfDirectory(((Path) element).toFile());
                if (size < 1024l) {
                    return String.valueOf(size) + " Bytes"; //$NON-NLS-1$
                } else if (size < 1_048_576l) { // 1KB
                    return FORMAT.format((double) size / 1024) + " KB"; //$NON-NLS-1$
                } else if (size < 1_073_741_824l) { // 1 MB
                    return FORMAT.format((double) size / (1024 * 1024)) + " MB"; //$NON-NLS-1$
                } else if (size < 1_099_511_627_776l) { //1 GB
                    return FORMAT.format((double) size / (1024 * 1024 * 1024)) + " GB"; //$NON-NLS-1$
                } else {
                    return String.valueOf(size);
                }
            }
            return super.getText(element);
        }

    }

}
