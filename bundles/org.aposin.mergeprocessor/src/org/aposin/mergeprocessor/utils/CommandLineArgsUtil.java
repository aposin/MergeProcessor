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
package org.aposin.mergeprocessor.utils;

import java.util.logging.Logger;

import org.eclipse.core.runtime.Platform;

/**
 * Methods to parse command line arguments for mergeprocessor.
 */
public class CommandLineArgsUtil {

	private static final Logger LOGGER = Logger.getLogger(CommandLineArgsUtil.class.getName());

	private static final String[] USAGE_PARAMETERS = { "/?", "-?", "/h", "-h", "--help", "--usage" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	private static boolean printUsage = false;

	/**
	 * Parse all command line arguments
	 */
	public static void parseCommandLineArgs() {
		LogUtil.entering();

		String[] arguments = Platform.getApplicationArgs();
		for (String argument : arguments) {
			if (isUsageParameter(argument)) {
				parseUsageParameter();
			} else {
				LOGGER.fine(() -> String.format("Ignored unknown argument. argument=%s.", argument)); //$NON-NLS-1$
			}
		}

		if (printUsage) {
			printUsage();
		}

		LogUtil.exiting();
	}

	private static boolean isUsageParameter(String argument) {
		LogUtil.entering(argument);

		boolean isUsageParameter = false;
		for (String usageParameter : USAGE_PARAMETERS) {
			if (usageParameter.equalsIgnoreCase(argument)) {
				LOGGER.info(() -> String.format("Found usage parameter %s", usageParameter)); //$NON-NLS-1$
				isUsageParameter = true;
				break;
			}
		}

		return LogUtil.exiting(isUsageParameter);
	}

	private static void parseUsageParameter() {
		printUsage = true;
		LOGGER.fine("Usage will be print."); //$NON-NLS-1$
	}

	private static void printUsage() {
		System.out.println(Messages.CommandLineArgsUtil_Title);
		System.out.println(Messages.CommandLineArgsUtil_Description);
		System.out.println(Messages.CommandLineArgsUtil_Usage);
	}
}
