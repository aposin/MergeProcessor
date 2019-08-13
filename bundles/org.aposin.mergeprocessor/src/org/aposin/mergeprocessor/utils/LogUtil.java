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
/**
 * 
 */
package org.aposin.mergeprocessor.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class providing convenience methods for logging.
 * 
 * @author Stefan Weiser
 *
 */
public final class LogUtil {

	private LogUtil() {
		// Utility class
	}

	/**
	 * Returns the logger of the caller. The name of the logger is defined by the full qualified name
	 * of the caller class. 
	 * 
	 * @return the logger of the caller class
	 */
	public static Logger getLogger() {
		final StackTraceElement stackTraceElement = getCause();
		return Logger.getLogger(stackTraceElement.getClassName());
	}

	/**
	 * Logs the given exception with with the level {@link Level#SEVERE} and returns 
	 * the given Exception again. This method is useful to log and throw an exception
	 * in 1 line.
	 * 
	 * @param exception the exception to log and return
	 * @return the given exception
	 */
	public static <A extends Throwable> A throwing(final A exception) {
		final StackTraceElement stackTraceElement = getCause();
		Logger.getLogger(stackTraceElement.getClassName()).logp(Level.SEVERE, stackTraceElement.getClassName(),
				stackTraceElement.getMethodName(), exception.getMessage(), exception);
		return exception;
	}

	/**
	 * Log a method return and return the return value. This method is useful to log
	 * and return the method exit in 1 line.
	 * 
	 * @param result the object to return
	 * @return the result parameter
	 */
	public static <A> A exiting(final A result) {
		final StackTraceElement stackTraceElement = getCause();
		Logger.getLogger(stackTraceElement.getClassName()).exiting(stackTraceElement.getClassName(),
				stackTraceElement.getMethodName(), result);
		return result;
	}

	/**
	 * Log a method return.
	 */
	public static void exiting() {
		final StackTraceElement stackTraceElement = getCause();
		Logger.getLogger(stackTraceElement.getClassName()).exiting(stackTraceElement.getClassName(),
				stackTraceElement.getMethodName());
	}

	/**
	 * Log a method entry. This method is useful to log the method entry in 1 line.
	 * 
	 * @param params the parameters of the method
	 */
	public static void entering(Object... params) {
		final StackTraceElement stackTraceElement = getCause();
		final Logger logger = Logger.getLogger(stackTraceElement.getClassName());
		if (params.length == 0) {
			logger.entering(stackTraceElement.getClassName(), stackTraceElement.getMethodName());
		} else if (params.length == 1) {
			logger.entering(stackTraceElement.getClassName(), stackTraceElement.getMethodName(), params[0]);
		} else {
			logger.entering(stackTraceElement.getClassName(), stackTraceElement.getMethodName(), params);
		}
	}

	/**
	 * Identifies the cause of the current thread stack.
	 * 
	 * @return the cause {@link StackTraceElement}
	 */
	private static StackTraceElement getCause() {
		for (final StackTraceElement element : Thread.currentThread().getStackTrace()) {
			if (!StringUtils.equalsAny(element.getClassName(), Thread.class.getName(), LogUtil.class.getName())) {
				return element;
			}
		}
		// Only when initialized within LogUtil
		return Thread.currentThread().getStackTrace()[1];
	}

}
