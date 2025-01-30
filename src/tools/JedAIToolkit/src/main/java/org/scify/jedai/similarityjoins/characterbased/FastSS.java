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

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datamodel.joins.IntListPair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author mthanos
 */
public class FastSS extends AbstractCharacterBasedJoin {

    private int id;

    private final HashMap<String, List<IntListPair>> stringHashIndex;
    private final TIntList delPos;
    private final TIntSet checkedFlag;

    public FastSS(int thr) {
        super(thr);
        this.checkedFlag = new TIntHashSet();
        this.stringHashIndex = new HashMap<>();
        this.delPos = new TIntArrayList();
    }

    @Override
    protected SimilarityPairs applyJoin() {
        checkedFlag.clear();
        delPos.clear();
        stringHashIndex.clear();

        final List<Comparison> comparisons = processDataset(attributeNameD1, profilesD1);
        if (profilesD2 != null) { // Dirty ER
            comparisons.addAll(processDataset(attributeNameD2, profilesD2));
        }
        return getSimilarityPairs(comparisons);
    }

    private int checkEditDistance(TIntList p1, TIntList p2) {
        int i = 0, j = 0, updates = 0;
        while (i < p1.size() && j < p2.size()) {
            if (p1.get(i) == p2.get(j)) {
                updates++;
                j++;
                i++;
            } else if (p1.get(i) < p2.get(j)) {
                i++;
            } else {
                j++;
            }
        }
        return p1.size() + p2.size() - updates;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it is ideal for short textual values. It associates every value with the set "
                + "of substrings that are produced after deleting a certain number of characters, "
                + "and every other value that shares one or more substrings is considered a candidate match.";
    }

    @Override
    public String getMethodName() {
        return "FastSS";
    }

    private List<Comparison> insertIndex(String attributeValue) {
        final TIntList delPos1 = new TIntArrayList(delPos);
        List<IntListPair> list = stringHashIndex.computeIfAbsent(attributeValue, k -> new ArrayList<>());

        final List<Comparison> executedComparisons = new ArrayList<>();
        for (IntListPair p : list) {
            if (id == p.getKey()) {
                continue;
            }

            if (isCleanCleanER) {
                if (id < datasetDelimiter && p.getKey() < datasetDelimiter) { // both belong to dataset 1
                    continue;
                }

                if (datasetDelimiter <= id && datasetDelimiter <= p.getKey()) { // both belong to dataset 2
                    continue;
                }
            }

            if (checkedFlag.contains(p.getKey())) {
                continue;
            }

            int ed = checkEditDistance(p.getValue(), delPos1);
            if (ed <= threshold) {
                checkedFlag.add(p.getKey());
                final Comparison currentComp = getComparison(id, p.getKey());
                currentComp.setUtilityMeasure(1 - (float) ed / threshold);
                executedComparisons.add(currentComp);
            }
        }

        final IntListPair kv = new IntListPair(id, delPos1);
        list.add(kv);

        return executedComparisons;
    }

    private void performDeletion(String s, int k) {
        if (k == 0) {
            insertIndex(s);
        } else {
            for (int pos = (delPos.isEmpty() ? 0 : delPos.get(delPos.size() - 1)); pos < s.length(); pos++) {
                delPos.add(pos);
                final String newS = s.substring(0, pos) + s.substring(pos + 1);
                performDeletion(newS, k - 1);
                delPos.removeAt(delPos.size() - 1);
            }
        }
    }

    private List<Comparison> processDataset(String attributeName, List<EntityProfile> dataset) {
        final List<Comparison> totalComparisons = new ArrayList<>();
        for (EntityProfile profile : dataset) {
            String nextValue = getAttributeValue(attributeName, profile);

            checkedFlag.clear();
            totalComparisons.addAll(insertIndex(nextValue));
            for (int k = 1; k <= threshold; k++) {
                performDeletion((nextValue), k);
            }
            id++;
        }
        return totalComparisons;
    }
}
