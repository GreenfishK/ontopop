/*
* Copyright [2016-2020] [George Papadakis (gpapadis@yahoo.gr)]
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
 */
package org.scify.jedai.similarityjoins.characterbased;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datamodel.joins.PIndex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author mthanos
 */
public class PassJoin extends AbstractCharacterBasedJoin {

    private int N, PN;
    private int MaxDictLen = 0;
    private int MinDictLen = 0x7FFFFFFF;

    private int[] dist;
    private int[] originalId;
    private int[][] partPos;
    private int[][] partLen;

    private final List<String> dict;
    private List<PIndex>[][] partIndex;
    private TIntObjectMap<TIntList>[][] invLists;

    public PassJoin(int thr) {
        super(thr);

        dict = new ArrayList<>();
    }

    @Override
    public SimilarityPairs applyJoin() {
        init();
        prepare();
        final List<Comparison> comparisons = performJoin();
        return getSimilarityPairs(comparisons);
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it partitions a textual value into a set of non-overlapping character q-grams and, "
                + "based on the pigeon-hole principle, it considers as candidate matches the values that share at least one of these q-grams.";
    }

    @Override
    public String getMethodName() {
        return "PassJoin";
    }

    private void init() {
        PN = threshold + 1;

        int counter = 0;
        final List<Pair<String, Integer>> idIdentifier = new ArrayList<>();
        for (EntityProfile profile : profilesD1) {
            final String nextValue = getAttributeValue(attributeNameD1, profile);

            MaxDictLen = Math.max(MaxDictLen, nextValue.length());
            MinDictLen = Math.min(MinDictLen, nextValue.length());
            idIdentifier.add(new ImmutablePair<>(nextValue, counter++));
        }

        if (isCleanCleanER) {
            for (EntityProfile profile : profilesD2) {
                final String nextValue = getAttributeValue(attributeNameD2, profile);

                MaxDictLen = Math.max(MaxDictLen, nextValue.length());
                MinDictLen = Math.min(MinDictLen, nextValue.length());
                idIdentifier.add(new ImmutablePair<>(nextValue, counter++));
            }
        }

        N = idIdentifier.size();
        matrixDimension1 = MaxDictLen + 1;
        matrixDimension2 = 2 * threshold + 1;

        dict.clear();
        idIdentifier.sort(Comparator.comparingInt(s -> s.getKey().length()));
        originalId = new int[N];
        for (int i = 0; i < N; i++) {
            final Pair<String, Integer> currentPair = idIdentifier.get(i);
            dict.add(currentPair.getKey());
            originalId[i] = currentPair.getValue();
        }

        partIndex = new List[PN][MaxDictLen + 1];
        for (int ii = 0; ii < PN; ii++) {
            for (int jj = 0; jj < MaxDictLen + 1; jj++) {
                partIndex[ii][jj] = new ArrayList<>();
            }
        }

        invLists = new TIntObjectMap[PN][MaxDictLen + 1];
        for (int ii = 0; ii < PN; ii++) {
            for (int jj = 0; jj < MaxDictLen + 1; jj++) {
                invLists[ii][jj] = new TIntObjectHashMap<>();
            }
        }

        dist = new int[MaxDictLen + 2];
        for (int lp = 0; lp <= MaxDictLen + 1; lp++) {
            dist[lp] = N;
        }

        partLen = new int[PN][MaxDictLen + 1];
        partPos = new int[PN + 1][MaxDictLen + 1];
        for (int len = MinDictLen; len <= MaxDictLen; len++) {
            partPos[0][len] = 0;
            partLen[0][len] = len / PN;
            partPos[PN][len] = len;
        }

        for (int pid = 1; pid < PN; pid++) {
            for (int len = MinDictLen; len <= MaxDictLen; len++) {
                partPos[pid][len] = partPos[pid - 1][len] + partLen[pid - 1][len];
                if (pid == (PN - len % PN)) {
                    partLen[pid][len] = partLen[pid - 1][len] + 1;
                } else {
                    partLen[pid][len] = partLen[pid - 1][len];
                }
            }
        }
    }

    private List<Comparison> performJoin() {
        final List<Comparison> executedComparisons = new ArrayList<>();
        for (int id = 0; id < N; id++) {
            final String currentString = dict.get(id);
            final TIntSet checkedIds = new TIntHashSet();
            int clen = currentString.length();
            for (int partId = 0; partId < PN; partId++) {
                for (int lp = 0; lp < partIndex[partId][clen].size(); lp++) {
                    final PIndex currentPI = partIndex[partId][clen].get(lp);

                    int hashValue = djbHash(currentString.substring(currentPI.getStPos()), currentPI.getPartLen());
                    if (!invLists[partId][currentPI.getLen()].containsKey(hashValue)) {
                        continue;
                    }

                    final TIntList candidateList = invLists[partId][currentPI.getLen()].get(hashValue);
                    for (TIntIterator iterator = candidateList.iterator(); iterator.hasNext();) {
                        int cand = iterator.next();

                        if (isCleanCleanER) {
                            if (originalId[id] < datasetDelimiter && originalId[cand] < datasetDelimiter) { // both belong to dataset 1
                                continue;
                            }

                            if (datasetDelimiter <= originalId[id] && datasetDelimiter <= originalId[cand]) { // both belong to dataset 2
                                continue;
                            }
                        }

                        if (!checkedIds.contains(cand)) {
                            final String currentCandidate = dict.get(cand);
                            if (partId == threshold) {
                                checkedIds.add(cand);
                            }
                            if (partId == 0 || getEditDistance(currentCandidate, currentString, partId, 0, 0, currentPI.getLo(), currentPI.getStPos()) <= partId) {
                                if (partId == 0) {
                                    checkedIds.add(cand);
                                }
                                if (partId == threshold || getEditDistance(currentCandidate, currentString, threshold - partId, currentPI.getLo() + currentPI.getPartLen(), currentPI.getStPos() + currentPI.getPartLen()) <= threshold - partId) {
                                    float distance = getEditDistance(currentCandidate, currentString, threshold);
                                    if (distance <= threshold) {
                                        checkedIds.add(cand);

                                        final Comparison currentComp = getComparison(originalId[id], originalId[cand]);
                                        currentComp.setUtilityMeasure(1 - distance / threshold);
                                        executedComparisons.add(currentComp);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (int partId = 0; partId < PN; partId++) {
                int pLen = partLen[partId][clen];
                int stPos = partPos[partId][clen];

                int hashValue = djbHash(currentString.substring(stPos), pLen);
                TIntList list = invLists[partId][clen].get(hashValue);
                if (list == null) {
                    list = new TIntArrayList();
                    invLists[partId][clen].put(hashValue, list);
                }
                list.add(id);
            }
        }
        return executedComparisons;
    }

    private void prepare() {
        int clen = 0;
        for (int id = 0; id < N; id++) {
            int currentLength = dict.get(id).length();
            if (clen == currentLength) {
                continue;
            }
            for (int lp = clen + 1; lp <= currentLength; lp++) {
                dist[lp] = id;
            }
            clen = currentLength;
        }

        clen = 0;
        for (int id = 0; id < N; id++) {
            int currentLength = dict.get(id).length();
            if (clen == currentLength) {
                continue;
            }
            clen = currentLength;

            for (int pid = 0; pid < PN; pid++) {
                for (int len = Math.max(clen - threshold, MinDictLen); len <= clen; len++) {
                    if (dist[len] == dist[len + 1]) {
                        continue;
                    }

                    for (int stPos = max3(0, partPos[pid][len] - pid,
                            partPos[pid][len] + (clen - len) - (threshold - pid));
                            stPos <= min3(clen - partLen[pid][len], partPos[pid][len] + pid,
                                    partPos[pid][len] + (clen - len) + (threshold - pid)); stPos++) {
                        partIndex[pid][clen].add(new PIndex(stPos, partPos[pid][len], partLen[pid][len], len));
                    }
                }
            }
        }
    }
}
