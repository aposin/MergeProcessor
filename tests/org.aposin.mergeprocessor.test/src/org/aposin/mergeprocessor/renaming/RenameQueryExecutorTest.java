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
package org.aposin.mergeprocessor.renaming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.aposin.mergeprocessor.configuration.git.MockMergeUnit;
import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;

/**
 * Tests for {@link RenameQueryExecutor}.
 * 
 * @author Stefan Weiser
 *
 */
public class RenameQueryExecutorTest {

    /**
     * Simple test calling {@link RenameQueryExecutor#hasRenaming(IMergeUnit)} with {@code null}.
     */
    @Test
    public void testHasRenamingWithNull() {
        final RenameQueryExecutor executor = new RenameQueryExecutor();
        assertThrows(NullPointerException.class, () -> executor.hasRenaming(null));
    }

    /**
     * Simple test for {@link RenameQueryExecutor#hasRenaming(IMergeUnit)} returning {@code false}.
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testHasRenamingWithoutRenamings() throws InterruptedException, ExecutionException {
        final RenameQueryExecutor executor = new RenameQueryExecutor();
        assertFalse(executor.hasRenaming(new MockMergeUnit()).get());
    }

    /**
     * Simple test for {@link RenameQueryExecutor#hasRenaming(IMergeUnit)} returning {@code true}.
     * 
     * @throws InterruptedException
     */
    @Test
    public void testHasRenamingWithRenamings() throws InterruptedException, ExecutionException {
        final RenameQueryExecutor executor = new RenameQueryExecutor();
        assertTrue(executor.hasRenaming(new MockMergeUnit() {

            @Override
            public Map<Path, Path> getRenameMapping() {
                return Map.of(Paths.get("bla"), Paths.get("bla2"));
            }

        }).get());
    }

    /**
     * Test for {@link RenameQueryExecutor#hasRenaming(IMergeUnit)} checking that {@link IMergeUnit#hasRenaming()}
     * is only called once when {@link RenameQueryExecutor#hasRenaming(IMergeUnit)} is called more often.
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testHasRenamingTwiceButOnlyEvaluateOnce() throws InterruptedException, ExecutionException {
        final RenameQueryExecutor executor = new RenameQueryExecutor();
        final AtomicInteger counter = new AtomicInteger();
        final MockMergeUnit mergeUnit = new MockMergeUnit() {

            @Override
            public Map<Path, Path> getRenameMapping() {
                counter.incrementAndGet();
                return super.getRenameMapping();
            }

        };
        executor.hasRenaming(mergeUnit).get(); //Block
        executor.hasRenaming(mergeUnit).get(); //Block
        assertEquals(1, counter.get());
    }

    /**
     * Brutal test for {@link RenameQueryExecutor#hasRenaming(IMergeUnit)} checking that many threads are able
     * to call the method concurrently and all threads are executed correctly with the correct result. Also checks
     * that {@link IMergeUnit#hasRenaming()} is only called once.
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testHasRenamingLongRunningWithManyThreadsButOnlyEvaluateOnce()
            throws InterruptedException, ExecutionException {
        final RenameQueryExecutor executor = new RenameQueryExecutor();
        final MockMergeUnit mergeUnit = new MockMergeUnit() {

            @Override
            public Map<Path, Path> getRenameMapping() {
                try {
                    Thread.sleep(300l);
                } catch (InterruptedException e) {
                    LogUtil.throwing(e);
                }
                return super.getRenameMapping();
            }

        };
        final ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(50);
        @SuppressWarnings("unchecked")
        final Callable<Boolean>[] array = new Callable[50];
        final AtomicInteger counter = new AtomicInteger();
        Arrays.fill(array, new Callable<Boolean>() {

            @Override
            public Boolean call() {
                counter.incrementAndGet();
                try {
                    return executor.hasRenaming(mergeUnit).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new JUnitException("Error occurred during calling Future.get()", e);
                }
            }
        });
        final List<Future<Boolean>> futures = threadPoolExecutor.invokeAll(Arrays.asList(array));
        assertEquals(50, counter.get());
        assertEquals(50, futures.size());
        for (final Future<Boolean> future : futures) {
            assertFalse(future.get());
        }
    }

    /**
     * Test {@link RenameQueryExecutor#isResultAvailable(IMergeUnit)} when {@link IMergeUnit#hasRenaming()}
     * was not called.
     */
    @Test
    public void testIsResultAvailableWhenHasRenamingWasNotCalled() {
        final RenameQueryExecutor executor = new RenameQueryExecutor();
        assertFalse(executor.isResultAvailable(new MockMergeUnit()));
    }

    /**
     * Test {@link RenameQueryExecutor#isResultAvailable(IMergeUnit)} when {@link IMergeUnit#hasRenaming()}
     * was called.
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testIsResultAvailableWhenHasRenamingWasCalled() throws InterruptedException, ExecutionException {
        final RenameQueryExecutor executor = new RenameQueryExecutor();
        final MockMergeUnit mergeUnit = new MockMergeUnit();
        executor.hasRenaming(mergeUnit).get();
        assertTrue(executor.isResultAvailable(mergeUnit));
    }

    /**
     * Test {@link RenameQueryExecutor#isResultAvailable(IMergeUnit)} with {@code null}
     */
    @Test
    public void testIsResultAvailableWithNull() {
        final RenameQueryExecutor executor = new RenameQueryExecutor();
        assertFalse(executor.isResultAvailable(null));
    }

    /**
     * Test {@link RenameQueryExecutor#cleanup(List)} when the {@link IMergeUnit} does not exist anymore. 
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testCleanupWhenMergeUnitIsNotExistingAnyMore() throws InterruptedException, ExecutionException {
        final RenameQueryExecutor executor = new RenameQueryExecutor();
        final AtomicInteger counter = new AtomicInteger();
        final MockMergeUnit mergeUnit = new MockMergeUnit() {

            @Override
            public boolean hasRenaming() {
                counter.incrementAndGet();
                return super.hasRenaming();
            }

        };
        executor.hasRenaming(mergeUnit).get();
        executor.cleanup(List.of(new MockMergeUnit()));
        executor.hasRenaming(mergeUnit).get();
        assertEquals(2, counter.get());
    }

    /**
     * Test {@link RenameQueryExecutor#cleanup(List)} when no {@link IMergeUnit} exists.
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testCleanupWhenNoMergeUnitsExistAnyMore() throws InterruptedException, ExecutionException {
        final RenameQueryExecutor executor = new RenameQueryExecutor();
        final AtomicInteger counter = new AtomicInteger();
        final MockMergeUnit mergeUnit = new MockMergeUnit() {

            @Override
            public boolean hasRenaming() {
                counter.incrementAndGet();
                return super.hasRenaming();
            }

        };
        executor.hasRenaming(mergeUnit).get();
        executor.cleanup(null);
        executor.hasRenaming(mergeUnit).get();
        assertEquals(2, counter.get());
    }

    /**
     * Test {@link RenameQueryExecutor#cleanup(List)} when the {@link IMergeUnit} still exists.
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testCleanupWhenMergeUnitIsStillExisting() throws InterruptedException, ExecutionException {
        final RenameQueryExecutor executor = new RenameQueryExecutor();
        final AtomicInteger counter = new AtomicInteger();
        final MockMergeUnit mergeUnit = new MockMergeUnit() {

            @Override
            public boolean hasRenaming() {
                counter.incrementAndGet();
                return super.hasRenaming();
            }

        };
        executor.hasRenaming(mergeUnit).get();
        executor.cleanup(List.of(mergeUnit));
        executor.hasRenaming(mergeUnit).get();
        assertEquals(1, counter.get());
    }

}
