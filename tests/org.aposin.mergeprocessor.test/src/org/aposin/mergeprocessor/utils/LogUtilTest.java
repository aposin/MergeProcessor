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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for testing {@link LogUtil}.
 * 
 * @author Stefan Weiser
 *
 */
public class LogUtilTest {

	private static Logger LOGGER_OF_LOGCAUSER;
	private LogUtilTestHandler handler;

	/**
	 * Setup the log level to all for the logger of this class
	 */
	@BeforeAll
	public static void setUp() {
		LOGGER_OF_LOGCAUSER = Logger.getLogger(LogCauser.class.getName());
		LOGGER_OF_LOGCAUSER.setUseParentHandlers(false);
		LOGGER_OF_LOGCAUSER.setLevel(Level.ALL);
	}

	/**
	 * Setup the handler and add it to the logger of the class
	 */
	@BeforeEach
	public void setupHandler() {
		handler = new LogUtilTestHandler();
		LOGGER_OF_LOGCAUSER.addHandler(handler);
	}

	/**
	 * Remove the handler from the logger of the class
	 */
	@AfterEach
	public void closeHandler() {
		LOGGER_OF_LOGCAUSER.removeHandler(handler);
		handler = null;
	}

	@Test
	public void testExitingWithReturnValue() {
		assertEquals("String", LogCauser.exitingInLogCauser("String"));
		assertNotNull(handler.record);
		assertEquals(LogCauser.class.getName(), handler.record.getSourceClassName());
		assertEquals("exitingInLogCauser", handler.record.getSourceMethodName());
	}

	@Test
	public void testExistingForVoidMethod() {
		LogCauser.exitingInLogCauser();
		assertNotNull(handler.record);
		assertEquals(LogCauser.class.getName(), handler.record.getSourceClassName());
		assertEquals("exitingInLogCauser", handler.record.getSourceMethodName());
	}

	@Test
	public void testThrowing() {
		final Exception exception = LogCauser.throwingInLogCauser();
		assertNotNull(handler.record);
		assertSame(exception, handler.record.getThrown());
		assertEquals(LogCauser.class.getName(), handler.record.getSourceClassName());
		assertEquals("throwingInLogCauser", handler.record.getSourceMethodName());
	}

	@Test
	public void testEnteringWithoutParameters() {
		LogCauser.enteringInLogCauser();
		assertNotNull(handler.record);
		assertEquals(LogCauser.class.getName(), handler.record.getSourceClassName());
		assertEquals("enteringInLogCauser", handler.record.getSourceMethodName());
		assertNull(handler.record.getParameters());
	}

	@Test
	public void testEnteringWith1Parameter() {
		LogCauser.enteringInLogCauser("Test");
		assertNotNull(handler.record);
		assertEquals(LogCauser.class.getName(), handler.record.getSourceClassName());
		assertEquals("enteringInLogCauser", handler.record.getSourceMethodName());
		assertArrayEquals(new Object[] { "Test" }, handler.record.getParameters());
	}

	@Test
	public void testEnteringWith3Parameter() {
		LogCauser.enteringInLogCauser("Test1", "Test2", "Test3");
		assertNotNull(handler.record);
		assertEquals(LogCauser.class.getName(), handler.record.getSourceClassName());
		assertEquals("enteringInLogCauser", handler.record.getSourceMethodName());
		assertArrayEquals(new Object[] { "Test1", "Test2", "Test3" }, handler.record.getParameters());
	}

	/**
	 * Class causing log entries.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class LogCauser {

		private static Exception throwingInLogCauser() {
			return LogUtil.throwing(new Exception());
		}

		private static void enteringInLogCauser(final Object... params) {
			LogUtil.entering(params);
		}

		private static void exitingInLogCauser() {
			LogUtil.exiting();
		}

		private static <A> A exitingInLogCauser(A param) {
			return LogUtil.exiting(param);
		}

	}

	/**
	 * Handler to identify the source class name and the source method name for the first published {@link LogRecord}.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	private static class LogUtilTestHandler extends Handler {

		private LogRecord record;

		public LogUtilTestHandler() {
			setLevel(Level.ALL);
		}

		@Override
		public void publish(LogRecord record) {
			this.record = record;
		}

		@Override
		public void flush() {
			// NOOP
		}

		@Override
		public void close() throws SecurityException {
			// NOOP
		}

	}

}
