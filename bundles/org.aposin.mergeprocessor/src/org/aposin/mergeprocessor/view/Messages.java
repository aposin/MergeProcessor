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
package org.aposin.mergeprocessor.view;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.aposin.mergeprocessor.view.messages"; //$NON-NLS-1$
    public static String DialogSvnCredentials_AuthenticationError;
    public static String DialogSvnCredentials_Cancel;
    public static String DialogSvnCredentials_Description;
    public static String DialogSvnCredentials_Ok;
    public static String DialogSvnCredentials_Password;
    public static String DialogSvnCredentials_ShellText;
    public static String DialogSvnCredentials_Title;
    public static String DialogSvnCredentials_Username;
    public static String MergeScriptDialog_changeTargetBranchDialogMessage;
    public static String MergeScriptDialog_changeTargetBranchDialogNo;
    public static String MergeScriptDialog_changeTargetBranchDialogTitle;
    public static String MergeScriptDialog_changeTargetBranchDialogYes;
    public static String MergeScriptDialog_Close;
    public static String MergeScriptDialog_Content;
    public static String MergeScriptDialog_Date;
    public static String MergeScriptDialog_NeededFiles;
    public static String MergeScriptDialog_Path;
    public static String MergeScriptDialog_RevisionRange;
    public static String MergeScriptDialog_ShellText;
    public static String MergeScriptDialog_ShowChanges;
    public static String MergeScriptDialog_SourceBranch;
    public static String MergeScriptDialog_Status;
    public static String MergeScriptDialog_TargetBranch;
    public static String View_Button_Ignore_Selection;
    public static String View_Button_Merge_Selection;
    public static String View_Button_Merge_Selection_Manually;
    public static String View_Column_Date;
    public static String View_Column_Merge_Script;
    public static String View_Column_Renaming;
    public static String View_Column_Repository;
    public static String View_Column_Revision_Range;
    public static String View_Column_Source_Branch;
    public static String View_Column_Status;
    public static String View_Column_Target_Branch;
    public static String View_Column_Host;
    public static String View_Column_Vcs;
    public static String View_IgnoreSelection_Done_No;
    public static String View_IgnoreSelection_Done_Question;
    public static String View_IgnoreSelection_Done_Title;
    public static String View_IgnoreSelection_Done_Yes;
    public static String View_MergeSelection_Ignored_Todo_No;
    public static String View_MergeSelection_Ignored_Todo_Question;
    public static String View_MergeSelection_Ignored_Todo_Title;
    public static String View_MergeSelection_Ignored_Todo_Yes;
    public static String View_RefreshMergeUnits_Error_Description;
    public static String View_RefreshMergeUnits_Error_MessagePrefix;
    public static String View_RefreshMergeUnits_Error_Ok;
    public static String View_RefreshMergeUnits_Error_Title;
    public static String View_ShowHelp_Close;
    public static String View_ShowHelp_Description;
    public static String View_ShowHelp_Title;
    public static String View_ShowMergeScript_Error_Description;
    public static String View_ShowMergeScript_Error_Title;
    public static String View_Status_LastRefresh;
    public static String View_Status_Successfully_Started;
    public static String SvnCredentialsView_lblNewLabel_text;
    public static String SvnCredentialsView_text_text;
    public static String DirectorySelectionDialog_browse;
    public static String MergeScriptView_tbtmNewItem_text;
    public static String MergeScriptView_tbtmNewItem_1_text;
    public static String MergeScriptView_tblclmnNewColumn_text;
    public static String MergeScriptView_tblclmnNewColumn_1_text;
    public static String DirectorySelectionDialog_mergeFrom;
    public static String DirectorySelectionDialog_mergeTo;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {}
}
