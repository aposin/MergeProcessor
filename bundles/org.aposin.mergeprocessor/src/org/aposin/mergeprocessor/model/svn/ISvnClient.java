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
package org.aposin.mergeprocessor.model.svn;

import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * This interfaces defines the SVN methods which are required in the merge
 * processor. This interface does not define a general SVN abstraction, it is
 * only used for the merge processor interactions.
 * 
 * @author Stefan Weiser
 *
 */
public interface ISvnClient extends AutoCloseable {

	/**
	 * Returns the content of a given SVN URL.
	 * 
	 * @param url the SVN URL of the file
	 * @return the content of the file for the given SVN URL.
	 * @throws SvnClientException
	 */
	String cat(URL url) throws SvnClientException;

	/**
	 * Returns the list of differences for the given SVN URL.
	 * 
	 * @param url          the SVN URL
	 * @param fromRevision the revision number to start from
	 * @param toRevision   the revision number to end to
	 * @return a list of differences
	 * @throws SvnClientException
	 */
	List<SvnDiff> diff(URL url, long fromRevision, long toRevision) throws SvnClientException;

	boolean hasModifications(Path path) throws SvnClientException;

	/**
	 * Returns the current revision number for the given SVN URL.
	 * 
	 * @param url the SVN URL
	 * @return the current revision number
	 * @throws SvnClientException
	 */
	long showRevision(URL url) throws SvnClientException;

	/**
	 * Returns a list of logs changed by the given author.
	 * 
	 * @param url          the SVN URL
	 * @param fromRevision the revision number to start from (inclusivly)
	 * @param toRevision   the revision number to end to
	 * @param author       the author of changes or {@code null} if the author
	 *                     should not be recognized
	 * @return the log list
	 * @throws SvnClientException
	 */
	List<SvnLog> log(URL url, long fromRevision, long toRevision, String author) throws SvnClientException;

	/**
	 * Returns a list of logs changed by the given author.
	 * 
	 * @param url          the SVN URL
	 * @param fromRevision the revision number to start from (inclusivly)
	 * @param toRevision   the revision number to end to
	 * @return the log list
	 * @throws SvnClientException
	 */
	default List<SvnLog> log(URL url, long fromRevision, long toRevision) throws SvnClientException {
		return log(url, fromRevision, toRevision, null);
	}

	/**
	 * Returns a list of all existing directories for the given SVN URL. This call
	 * only recognizes immediate children, so the depth of the tree to walk is only
	 * 1.
	 * 
	 * @param url the SVN URL
	 * @return a list of all existing directories
	 * @throws SvnClientException
	 */
	List<String> listDirectories(URL url) throws SvnClientException;

	/**
	 * Updates the given list of paths as 'empty', i.e. only the files without their
	 * subtrees are updated.
	 * 
	 * @param paths the path to update 'empty'
	 * @return an array of the current revisions for the given paths.
	 * @throws SvnClientException
	 */
	long[] updateEmpty(List<Path> paths) throws SvnClientException;

	default long updateEmpty(Path path) throws SvnClientException {
		return updateEmpty(Arrays.asList(path))[0];
	}

	/**
	 * Checks out the given SVN URL to the given path without any content, i.e. only
	 * the target root directory is getting checked out and it will not contain any
	 * content.
	 * 
	 * @param path the local path, which must not be {@code null}
	 * @param url  the given SVN URL, which must not be {@code null}
	 * @throws SvnClientException
	 */
	void checkoutEmpty(Path path, URL url) throws SvnClientException;

	/**
	 * Merges the given path with the given SVN URL and the given revision.
	 * 
	 * @param path     the local path where to merge into
	 * @param url      the SVN URL where the merge is coming from
	 * @param revision the revision to merge
	 * @throws SvnClientException
	 */
	default void merge(Path path, URL url, long revision) throws SvnClientException {
		merge(path, url, revision, true);
	}

	default void merge(Path path, URL url, long revision, boolean recursivly) throws SvnClientException {
		merge(path, url, revision, recursivly, false);
	}

	void merge(Path path, URL url, long revision, boolean recursivly, boolean recordOnly) throws SvnClientException;

	/**
	 * Commits the given path with the given message.
	 * 
	 * @param path    the local path where to commit the existing changes
	 * @param message the commit message
	 * @throws SvnClientException
	 */
	void commit(Path path, String message) throws SvnClientException;

	/**
	 * Return the list of existing conflicts for the given path.
	 * 
	 * @param path the local path where conflicts may exist
	 * @return the list of existing conflicts
	 * @throws SvnClientException
	 */
	List<String> getConflicts(Path path) throws SvnClientException;

	/**
	 * Checks, if the given path contains any conflicts.
	 * 
	 * @param path the local path where conflicts may exist
	 * @return {@code true} if conflicts exist
	 * @throws SvnClientException
	 */
	default boolean hasConflicts(Path path) throws SvnClientException {
		return !getConflicts(path).isEmpty();
	}

	/**
	 * Updates the given path to the HEAD revision.
	 * 
	 * @param path the path to update
	 * @throws SvnClientException
	 */
	void update(Path path) throws SvnClientException;

	URL getSvnUrl(Path path) throws SvnClientException;

	URL getRepositoryUrl(Path path) throws SvnClientException;

	void addCommandLineListener(Consumer<String> consumer);

	void removeCommandLineListener(Consumer<String> consumer);

	/**
	 * {@inheritDoc}
	 */
	@Override
	void close();

	/**
	 * Container object showing the differences for SVN diff.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	public static class SvnDiff {

		public enum SvnDiffAction {
			MODIFIED, //
			REPLACED, //
			DELETED, //
			ADDED, //
			PROPERTY_CHANGED;
		}

		private final SvnDiffAction action;
		private final URL url;

		/**
		 * @param action
		 * @param url
		 */
		protected SvnDiff(SvnDiffAction action, URL url) {
			this.action = action;
			this.url = url;
		}

		/**
		 * @return the action
		 */
		public SvnDiffAction getAction() {
			return action;
		}

		/**
		 * @return the url
		 */
		public URL getUrl() {
			return url;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return action.toString() + ' ' + url;
		}

	}

	public static class SvnLog {

		private final long revision;
		private final List<SvnLogEntry> entries;
		private final String message;
		private final LocalDateTime date;
		private final String author;

		/**
		 * @param action
		 * @param url
		 */
		protected SvnLog(long revision, final List<SvnLogEntry> entries, final String message, final LocalDateTime date,
				final String author) {
			this.revision = revision;
			this.entries = entries;
			this.message = message;
			this.date = date;
			this.author = author;
		}

		/**
		 * @return the revision
		 */
		public long getRevision() {
			return revision;
		}

		/**
		 * @return the entries
		 */
		public List<SvnLogEntry> getEntries() {
			return Collections.unmodifiableList(entries);
		}

		/**
		 * @return the message
		 */
		public String getMessage() {
			return message;
		}

		/**
		 * @return the date
		 */
		public LocalDateTime getDate() {
			return date;
		}

		/**
		 * @return the author
		 */
		public String getAuthor() {
			return author;
		}

		public enum SvnLogAction {
			MODIFIED, //
			REPLACED, //
			DELETED, //
			ADDED, //
			PROPERTY_CHANGED; //
		}

		public static class SvnLogEntry {

			private final SvnLogAction action;
			private final URL url;

			/**
			 * @param action
			 * @param url
			 */
			protected SvnLogEntry(SvnLogAction action, URL url) {
				this.action = action;
				this.url = url;
			}

			/**
			 * @return the action
			 */
			public SvnLogAction getAction() {
				return action;
			}

			/**
			 * @return the url
			 */
			public URL getUrl() {
				return url;
			}

		}

	}

	/**
	 * This exception is thrown when an error occurs in usage of {@link ISvnClient}.
	 * 
	 * @author Stefan Weiser
	 *
	 */
	public static class SvnClientException extends Exception {

		private static final long serialVersionUID = 2504832240860604333L;

		/**
		 * Constructs a new exception with the specified detail message. The cause is
		 * not initialized, and may subsequently be initialized by a call to
		 * {@link #initCause}.
		 *
		 * @param message the detail message. The detail message is saved for later
		 *                retrieval by the {@link #getMessage()} method.
		 */
		public SvnClientException(String message) {
			super(message);
		}

		/**
		 * Constructs a new exception with the specified cause and a detail message of
		 * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains
		 * the class and detail message of <tt>cause</tt>). This constructor is useful
		 * for exceptions that are little more than wrappers for other throwables (for
		 * example, {@link java.security.PrivilegedActionException}).
		 *
		 * @param cause the cause (which is saved for later retrieval by the
		 *              {@link #getCause()} method). (A <tt>null</tt> value is
		 *              permitted, and indicates that the cause is nonexistent or
		 *              unknown.)
		 */
		public SvnClientException(Throwable cause) {
			super(cause);
		}

		/**
		 * Constructs a new exception with the specified detail message and cause.
		 * <p>
		 * Note that the detail message associated with {@code cause} is <i>not</i>
		 * automatically incorporated in this exception's detail message.
		 *
		 * @param message the detail message (which is saved for later retrieval by the
		 *                {@link #getMessage()} method).
		 * @param cause   the cause (which is saved for later retrieval by the
		 *                {@link #getCause()} method). (A <tt>null</tt> value is
		 *                permitted, and indicates that the cause is nonexistent or
		 *                unknown.)
		 */
		public SvnClientException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
