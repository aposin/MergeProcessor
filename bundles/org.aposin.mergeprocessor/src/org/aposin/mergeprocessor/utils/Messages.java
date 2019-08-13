/**
 * Copyright 2019 Association for the promotion of open-source insurance software and for the establishment of open interface standards in the insurance industry (Verein zur Förderung quelloffener Versicherungssoftware und Etablierung offener Schnittstellenstandards in der Versicherungsbranche)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aposin.mergeprocessor.utils;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.aposin.mergeprocessor.utils.messages"; //$NON-NLS-1$
	public static String CommandLineArgsUtil_Description;
	public static String CommandLineArgsUtil_Title;
	public static String CommandLineArgsUtil_Usage;
	public static String FileUtils2_copyCancelClean;
	public static String FileUtils2_copyTask;
	public static String FileUtils2_copyXCopied;
	public static String FileUtils2_copyZeroCopied;
	public static String MergeProcessorUtil_BugUserToFixProblem_Cancel;
	public static String MergeProcessorUtil_BugUserToFixProblem_Retry;
	public static String MergeProcessorUtil_BugUserToFixProblem_Title;
	public static String MergeProcessorUtil_BuildMinimalWorkingCopy_Error_Message;
	public static String MergeProcessorUtil_BuildMinimalWorkingCopy_Error_MessageScrollable_Prefix;
	public static String MergeProcessorUtil_CheckIsCommittable_Conflicts_Cancel;
	public static String MergeProcessorUtil_CheckIsCommittable_Conflicts_DialogMessage;
	public static String MergeProcessorUtil_CheckIsCommittable_Conflicts_OpenTortoiseSvn;
	public static String MergeProcessorUtil_CheckIsCommittable_Conflicts_OpenWorkingCopy;
	public static String MergeProcessorUtil_CheckIsCommittable_Conflicts_Prefix;
	public static String MergeProcessorUtil_CheckIsCommittable_Conflicts_Retry;
	public static String MergeProcessorUtil_CheckIsCommittable_Conflicts_Title;
	public static String MergeProcessorUtil_CheckIsCommittable_Error_Message_Prefix;
	public static String MergeProcessorUtil_CheckIsCommittable_Error_Title;
	public static String MergeProcessorUtil_CheckIsCommittable_TortoiseSvn_Error_Message_Prefix;
	public static String MergeProcessorUtil_CheckIsCommittable_TortoiseSvn_Error_Title;
	public static String MergeProcessorUtil_Choices_Invalid;
	public static String MergeProcessorUtil_CommitChanges_Commit_Error_Message;
	public static String MergeProcessorUtil_CommitChanges_Commit_Error_Title;
	public static String MergeProcessorUtil_CommitChanges_Conflicts_Error_Title;
	public static String MergeProcessorUtil_CommitChanges_Update_Error_Title;
	public static String MergeProcessorUtil_CopyLocalToDone_Error_Details;
	public static String MergeProcessorUtil_CopyLocalToDone_Error_Message;
	public static String MergeProcessorUtil_CopyRemoteToLocal_Error_Message;
	public static String MergeProcessorUtil_CopyRemoteToLocal_Error_MessageScrollable;
	public static String MergeProcessorUtil_DeleteLocal_Error_Message;
	public static String MergeProcessorUtil_DeleteLocal_Error_Title;
	public static String MergeProcessorUtil_Ignore_Error_Message;
	public static String MergeProcessorUtil_Ignore_Error_Title;
	public static String MergeProcessorUtil_MergeChangesIntoWorkingCopy_Error_Message_Prefix;
	public static String MergeProcessorUtil_MergeChangesIntoWorkingCopy_Error_Title;
	public static String MergeProcessorUtil_MergeProcessorUtil_CommitChanges_Conflicts_Error_Message;
	public static String MergeProcessorUtil_MergeProcessorUtil_CommitChanges_Update_Error_Details;
	public static String MergeProcessorUtil_Process_TaskName;
	public static String MergeProcessorUtil_Unignore_Error_Message;
	public static String MergeProcessorUtil_Unignore_Error_Title;
	public static String SvnUtilJavaHl_AskQuestion_Title;
	public static String SvnUtilJavaHl_AskTrustSSLServer_AcceptPermanently;
	public static String SvnUtilJavaHl_AskTrustSSLServer_AcceptTemporary;
	public static String SvnUtilJavaHl_AskTrustSSLServer_Reject;
	public static String SvnUtilJavaHl_AskTrustSSLServer_Title;
	public static String SvnUtilJavaHl_AskYesNo_Title;
	public static String SvnUtilSvnkit_MovedFiles_CancelMerge;
	public static String SvnUtilSvnkit_MovedFiles_ContinueWithoutMergingMissingFiles;
	public static String SvnUtilSvnkit_MovedFiles_Description;
	public static String SvnUtilSvnkit_MovedFiles_Intro;
	public static String SvnUtilSvnkit_MovedFiles_Title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
