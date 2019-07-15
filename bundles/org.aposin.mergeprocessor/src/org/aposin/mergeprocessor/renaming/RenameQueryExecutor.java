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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.aposin.mergeprocessor.model.IMergeUnit;
import org.aposin.mergeprocessor.utils.LogUtil;
import org.h2.util.DoneFuture;

/**
 * <p>This executor is responsible to check an query for renamings concurrently. The methods
 * return the results in {@link Future Futures} so the methods are answering immediately without
 * blocking. If the user requires the answer the futures blocks in the method {@link Future#get()}.</p>
 * 
 * <p>The method {@link #isResultAvailable(IMergeUnit)} is useful to check if the result is available and
 * cached. Callers can then rely on a immediate response when calling methods returing {@link Future Futures}</p>
 * 
 * @author Stefan Weiser
 *
 */
public class RenameQueryExecutor {

    //H2 embedded only supports 1 connection
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Map<IMergeUnit, Boolean> results = new ConcurrentHashMap<>();
    private final Map<IMergeUnit, Future<Boolean>> futures = new ConcurrentHashMap<>();

    /**
     * Checks if the given {@link IMergeUnit} has renamings and returns the result as {@link Future}.
     * The query is executed concurrently, so the method returns immediately.
     * 
     * @param mergeUnit the {@link IMergeUnit}
     * @return {@code true} if {@link IMergeUnit} has renamed artifacts.
     * @see IMergeUnit#hasRenaming()
     */
    public Future<Boolean> hasRenaming(final IMergeUnit mergeUnit) {
        Objects.requireNonNull(mergeUnit);
        Boolean result = results.get(mergeUnit);
        if (result != null) {
            //result available
            return new DoneFuture<Boolean>(result);
        } else {
            synchronized (mergeUnit) {
                //double check
                result = results.get(mergeUnit);
                if (result != null) {
                    //result available
                    return new DoneFuture<Boolean>(result);
                } else if (futures.containsKey(mergeUnit)) {
                    //Executor already scheduled or running, only add the consumer
                    return futures.get(mergeUnit);
                } else {
                    //Only run executor if not already running
                    final Future<Boolean> future = executorService.submit(new Callable<Boolean>() {

                        @Override
                        public Boolean call() {
                            final boolean hasRenaming = mergeUnit.hasRenaming();
                            if (results.containsKey(mergeUnit)) {
                                LogUtil.getLogger().warning("MergeUnit.hasRenaming() checked twice.");
                            }
                            synchronized (mergeUnit) {
                                results.put(mergeUnit, hasRenaming);
                                futures.remove(mergeUnit);
                            }
                            return hasRenaming;
                        }
                    });
                    futures.put(mergeUnit, future);
                    return future;
                }
            }
        }
    }

    /**
     * Checks if the renaming result of the {@link IMergeUnit} is already known. This method
     * uses an internal cache of the executor, so the method does not call methods of {@link IMergeUnit}.
     * 
     * @param mergeUnit the {@link IMergeUnit}
     * @return {@code true} if the result of the {@link IMergeUnit} is available.
     */
    public boolean isResultAvailable(final IMergeUnit mergeUnit) {
        if (mergeUnit == null) {
            return false;
        } else {
            return results.containsKey(mergeUnit);
        }
    }

    /**
     * Cleans of the internal cache against the given list of {@link IMergeUnit IMergeUnits}. All {@link IMergeUnit IMergeUnits}
     * not existing in the given list are removed from the internal caches of the executor.
     * 
     * @param existingMergeUnits the existing {@link IMergeUnit IMergeUnits}
     */
    public void cleanup(final List<IMergeUnit> existingMergeUnits) {
        if (existingMergeUnits == null || existingMergeUnits.isEmpty()) {
            results.clear();
            futures.clear();
        } else {
            results.keySet() //Iterate over keys
                    .stream() //With stream
                    .filter(unit -> !existingMergeUnits.contains(unit)) //Filter all units not existing any more
                    .forEach(unit -> results.remove(unit)); //Remove from results
            futures.keySet()//Iterate over keys
                    .stream() //With stream
                    .filter(unit -> !existingMergeUnits.contains(unit)) //Filter all units not existing any more
                    .forEach(unit -> futures.remove(unit));
        }
    }

}
