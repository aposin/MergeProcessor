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
package org.aposin.mergeprocessor.model.git;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.aposin.mergeprocessor.model.git.messages"; //$NON-NLS-1$
	public static String GitMergeUtil_cancel;
	public static String GitMergeUtil_checkoutBranch;
	public static String GitMergeUtil_checkStatus;
	public static String GitMergeUtil_cherryPick;
	public static String GitMergeUtil_clone;
	public static String GitMergeUtil_commit;
	public static String GitMergeUtil_createRepositoryDirectory;
	public static String GitMergeUtil_evaluteCommitMessage;
	public static String GitMergeUtil_mergeErrorDetailMessage;
	public static String GitMergeUtil_mergeErrorMessage;
	public static String GitMergeUtil_mergeErrorTitle;
	public static String GitMergeUtil_mergeGitMergeUnit;
	public static String GitMergeUtil_moveMergeUnit;
	public static String GitMergeUtil_pull;
	public static String GitMergeUtil_push;
	public static String GitMergeUtil_retry;
	public static String GitMergeUtil_revertAndRetry;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
