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
package org.scify.jedai.similarityjoins.tokenbased;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datamodel.joins.IntPair;
import org.scify.jedai.datamodel.joins.ListItemPPJ;

import java.util.*;

/**
 *
 * @author mthanos
 */
public class PPJoin extends AbstractTokenBasedJoin {

    private final static int PRUNE_FLAG = -7;

    private int[] originalId;
    private final List<String> attributeValues;
    private TIntList[] records;

    public PPJoin(float thr) {
        super(thr);
        attributeValues = new ArrayList<>();
    }

    @Override
    public SimilarityPairs applyJoin() {
        init();

        final List<Comparison> comparisons = performJoin();
        return getSimilarityPairs(comparisons);
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it sorts the tokens of every attribute value in increasing global order of frequency across all "
                + "values and then it combines Prefix Filtering with Positional Filtering, which estimates a tighter upper "
                + "bound for the overlap between the two sets of tokens, based on the positions where the common tokens in the prefix occur.";
    }

    @Override
    public String getMethodName() {
        return "PPJoin";
    }

    private int getOverlap(int x, int y, int requireOverlap, int... poslen) {
        int posx = poslen.length > 0 ? poslen[0] : 0;
        int posy = poslen.length > 1 ? poslen[1] : 0;
        int currentOverlap = poslen.length > 2 ? poslen[2] : 0;

        while (posx < records[x].size() && posy < records[y].size()) {
            if (records[x].size() - posx + currentOverlap < requireOverlap
                    || records[y].size() - posy + currentOverlap < requireOverlap) {
                return -1;
            }
            if (records[x].get(posx) == records[y].get(posy)) {
                currentOverlap++;
                posx++;
                posy++;
            } else if (records[x].get(posx) < records[y].get(posy)) {
                posx++;
            } else {
                posy++;
            }
        }
        return currentOverlap;
    }

    private void init() {
        int counter = 0;
        final List<Pair<String, Integer>> idIdentifier = new ArrayList<>();
        for (EntityProfile profile : profilesD1) {
            final String nextValue = getAttributeValue(attributeNameD1, profile);
            idIdentifier.add(new ImmutablePair<>(nextValue, counter++));
        }

        if (isCleanCleanER) {
            for (EntityProfile profile : profilesD2) {
                final String nextValue = getAttributeValue(attributeNameD2, profile);
                idIdentifier.add(new ImmutablePair<>(nextValue, counter++));
            }
        }

        if (this.SetVersion) {
            int[] setversionsizes = new int[idIdentifier.size()];
            for (int i = 0; i < idIdentifier.size(); i++) {
                String togetSize = idIdentifier.get(i).getKey();
                String[] split = togetSize.split(" ");
                Set<String> settogetsize = new HashSet<>();
                for (String s : split) {
                    settogetsize.add(s);
                }
                setversionsizes[idIdentifier.get(i).getValue()] = settogetsize.size();

            }
            idIdentifier.sort((s1, s2) -> setversionsizes[s1.getValue()] - setversionsizes[s2.getValue()]);
        } else {
            idIdentifier.sort(Comparator.comparingInt(s -> s.getKey().split(" ").length));
        }
        attributeValues.clear();
        originalId = new int[noOfEntities];
        records = new TIntList[noOfEntities];
        for (int i = 0; i < noOfEntities; i++) {
            final Pair<String, Integer> currentPair = idIdentifier.get(i);
            attributeValues.add(currentPair.getKey());
            originalId[i] = currentPair.getValue();
            records[i] = new TIntArrayList();
        }

        for (int sIndex = 0; sIndex < noOfEntities; sIndex++) {

            final String s = attributeValues.get(sIndex).trim();
            if (s.length() < 1) {
                continue;
            }

            String[] split = s.split(" ");
            for (String value : split) {
                int token = djbHash(value);
                if (this.SetVersion && (records[sIndex].contains(token))) {
                    continue; //case where Set is used instead of Bag
                }
                records[sIndex].add(token);
            }
            records[sIndex].sort();

        }
    }

    private List<Comparison> performJoin() {
        final List<Comparison> executedComparisons = new ArrayList<>();
        final TIntObjectMap<ListItemPPJ> index = new TIntObjectHashMap<>();
        for (int k = 0; k < records.length; k++) {
            /*if (isCleanCleanER) {
                if (originalId[k] >= datasetDelimiter ) { // dataset 2
                    continue;
                }
            }*/
            final TIntList record = records[k];
            int minLength = minPossibleLength(record.size());
            int probeLength = probeLength(record.size());
            int indexLength = indexLength(record.size());

            int[] requireOverlaps = new int[record.size() + 1];
            for (int l = minLength; l <= record.size(); l++) {
                requireOverlaps[l] = requireOverlap(record.size(), l);
            }

            final TIntIntMap occurances = new TIntIntHashMap();
            for (int t = 0; t < probeLength; t++) {
                int token = record.get(t);

                ListItemPPJ item = index.get(token);
                if (item == null) {
                    item = new ListItemPPJ();
                    index.put(token, item);
                }

                int pos = item.getPos();
                final List<IntPair> list = item.getIds();
                int noOfIds = list.size();
                while (pos < noOfIds && records[list.get(pos).getKey()].size() < minLength) {
                    pos++;
                }

                for (int p = pos; p < noOfIds; p++) {
                    int candId = list.get(p).getKey();
                    if (isCleanCleanER) {
                        if (originalId[k] < datasetDelimiter && originalId[candId] < datasetDelimiter) { // both belong to dataset 1
                            continue;
                        }

                        if (datasetDelimiter <= originalId[k] && datasetDelimiter <= originalId[candId]) { // both belong to dataset 2
                            continue;
                        }
                    }
                    int candPos = list.get(p).getValue();
                    int candLength = records[candId].size();

                    int value = occurances.get(candId);
                    if (value == 0) {
                        if ((records[k].size() - t) < requireOverlaps[candLength]
                                || (candLength - candPos) < requireOverlaps[candLength]) {
                            continue;
                        }
                        value = 1;
                    } else {
                        if (value + (records[k].size() - t) < requireOverlaps[candLength]
                                || value + (candLength - candPos) < requireOverlaps[candLength]) {
                            value = PRUNE_FLAG;
                        } else {
                            value++;
                        }
                    }
                    occurances.put(candId, value);//was replace before
                }

                if (t < indexLength) {
                    list.add(new IntPair(k, t));
                }
            }

            for (int cand : occurances.keys()) {
                if (k == cand) {
                    continue;
                }

                if (isCleanCleanER) {
                    if (originalId[k] < datasetDelimiter && originalId[cand] < datasetDelimiter) { // both belong to dataset 1
                        continue;
                    }

                    if (datasetDelimiter <= originalId[k] && datasetDelimiter <= originalId[cand]) { // both belong to dataset 2
                        continue;
                    }
                }

                if (occurances.get(cand) == PRUNE_FLAG) {
                    continue;
                }

                int currentSize = records[k].size();
                int candidateSize = records[cand].size();
                int newindexLength = indexLength(candidateSize);

                if (records[cand].get(newindexLength - 1) < records[k].get(probeLength - 1)) {
                    if (occurances.get(cand) + candidateSize - newindexLength < requireOverlaps[candidateSize]) {
                        continue;
                    }
                } else {
                    if (occurances.get(cand) + currentSize - probeLength < requireOverlaps[candidateSize]) {
                        continue;
                    }
                }

                int realOverlap = getOverlap(k, cand, requireOverlaps[candidateSize]);

                if (realOverlap != -1) {
                    float jaccardSim = calcSimilarity(currentSize, candidateSize, realOverlap);
                    if (jaccardSim >= threshold) {
                        final Comparison currentComp = getComparison(originalId[k], originalId[cand]);
                        currentComp.setUtilityMeasure(jaccardSim);
                        executedComparisons.add(currentComp);
                    }
                }
            }
        }

        return executedComparisons;
    }
}
