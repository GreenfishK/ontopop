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

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datamodel.joins.IntPair;
import org.scify.jedai.datamodel.joins.ListItemPPJ;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author mthanos
 */
public class EdJoin extends AbstractCharacterBasedJoin {

    private int maxLength;
    private final int q;
    private int widowBound;

    private int[] originalId;
    private final List<String> attributeValues;
    private List<Pair<Integer, Integer>>[] tokens;

    public EdJoin(int thr) {
        this(3, thr);
    }

    public EdJoin(int q, int thr) {
        super(thr);

        this.q = q;
        attributeValues = new ArrayList<>();
    }

    @Override
    protected SimilarityPairs applyJoin() {
        int rangeBound = init();

        getTokens(rangeBound);
        final List<Comparison> comparisons = performJoin(rangeBound);
        return getSimilarityPairs(comparisons);
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it adapts Prefix Filtering to Edit Distance";
    }

    @Override
    public String getMethodName() {
        return "Character-based All Pairs";
    }

    private int getOverlap(int x, int y) {
        int posx = 0;
        int posy = 0;
        int result = 0;
        while (posx < tokens[x].size() && posy < tokens[y].size()) {
            if (tokens[x].get(posx).getKey().equals(tokens[y].get(posy).getKey())) {
                result++;
                posx++;
                posy++;
            } else if (tokens[x].get(posx).getKey() < tokens[y].get(posy).getKey()) {
                posx++;
            } else {
                posy++;
            }
        }
        return result;
    }

    private void getTokens(int rangeBound) {
        final TIntIntMap freqMap = new TIntIntHashMap();
        for (int k = rangeBound; k < noOfEntities; k++) {
            final String currentValue = attributeValues.get(k);
            for (int sp = 0; sp < currentValue.length() - q + 1; sp++) {
                int token = djbHash(currentValue.substring(sp), q);
                tokens[k].add(new ImmutablePair<>(token, sp));
                int cur = freqMap.get(token);
                freqMap.put(token, cur + 1);
            }
        }

        final List<IntPair> packages = new ArrayList<>();
        for (TIntIntIterator iterator = freqMap.iterator(); iterator.hasNext(); ) {
            iterator.advance();
            packages.add(new IntPair(iterator.key(), iterator.value()));
        }
        packages.sort(Comparator.comparingInt(IntPair::getValue));

        int noOfPackages = packages.size();
        for (int k = 0; k < noOfPackages; k++) {
            final IntPair currentPair = packages.get(k);
            if (currentPair.getValue() == 1) { // value = frequency
                widowBound = k;
            }
            freqMap.put(currentPair.getKey(), k); // key = token
        }

        for (int k = rangeBound; k < noOfEntities; k++) {
            final List<Pair<Integer, Integer>> token = tokens[k];
            for (int t = 0; t < token.size(); t++) {
                int oldsecond = token.get(t).getValue();
                token.set(t, new ImmutablePair<>(freqMap.get(token.get(t).getKey()), oldsecond));
            }
            token.sort(Comparator.comparingInt(Pair::getKey));
        }
    }

    int get_prefix_length(List<Pair<Integer, Integer>> token) {
        int low = threshold + 1;
        int high = q * threshold + 1;
        while (low < high) {
            int mid = (low + high) / 2;
            int errors = 0, location = 0;
            List<Pair<Integer, Integer>> duplicate = new ArrayList<>(token);
            duplicate.sort(Comparator.comparingInt(Pair::getValue));

            for (int k = 0; k < mid; k++) {
                if (duplicate.get(k).getValue() >= location) {
                    errors++;
                    location = duplicate.get(k).getValue() + q;
                }
            }
            if (errors <= threshold) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        return low;
    }

    private int init() {
        widowBound = -1;

        tokens = new List[noOfEntities];
        for (int i = 0; i < noOfEntities; i++) {
            tokens[i] = new ArrayList<>();
        }

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

        attributeValues.clear();
        originalId = new int[noOfEntities];
        idIdentifier.sort(Comparator.comparingInt(s -> s.getKey().length()));
        for (int i = 0; i < noOfEntities; i++) {
            final Pair<String, Integer> currentPair = idIdentifier.get(i);
            attributeValues.add(currentPair.getKey());
            originalId[i] = currentPair.getValue();
        }
        maxLength = attributeValues.get(noOfEntities - 1).length();

        matrixDimension1 = maxLength + 1;
        matrixDimension2 = 2 * threshold + 1;

        int lengthBound = (threshold + 1) * q;
        int rangeBound = noOfEntities;
        for (int sIndex = 0; sIndex < noOfEntities; sIndex++) {
            final String s = attributeValues.get(sIndex);

            if (s.length() >= lengthBound) {
                rangeBound = sIndex;
                break;
            }
        }

        return rangeBound;
    }

    private List<Comparison> performJoin(int rangeBound) {
        final List<Comparison> executedComparisons = new ArrayList<>();
        for (int x = 0; x < rangeBound; x++) {
            for (int y = x + 1; y < rangeBound; y++) {
                if (isCleanCleanER) {
                    if (originalId[x] < datasetDelimiter && originalId[y] < datasetDelimiter) { // both belong to dataset 1
                        continue;
                    }
                    if (datasetDelimiter <= originalId[x] && datasetDelimiter <= originalId[y]) { // both belong to dataset 2
                        continue;
                    }
                }
                int distance = getEditDistance(attributeValues.get(x), attributeValues.get(y), threshold);
                if (distance <= threshold) {
                    final Comparison currentComp = getComparison(originalId[x], originalId[y]);
                    currentComp.setUtilityMeasure(1 - (float) distance / threshold);
                    executedComparisons.add(currentComp);
                }
            }
        }
        final TIntObjectMap<ListItemPPJ> index = new TIntObjectHashMap<>();
        for (int k = rangeBound; k < noOfEntities; k++) {
            int count = 0;
            int prefix_length = get_prefix_length(tokens[k]);

            final TIntSet occurances = new TIntHashSet();
            for (Pair<Integer, Integer> token : tokens[k]) {
                if (count++ >= prefix_length) {
                    break;
                }
                if (token.getKey() <= widowBound) {
                    continue;
                }

                ListItemPPJ item = index.get(token.getKey());
                if (item == null) {
                    item = new ListItemPPJ();
                    index.put(token.getKey(), item);
                }

                final List<IntPair> list = item.getIds();
                final int noOfIds = list.size();
                while ((item.getPos() < noOfIds)
                        && (attributeValues.get(list.get(item.getPos()).getKey()).length() + threshold < attributeValues.get(k).length())) {
                    int oldPos = item.getPos();
                    item.setPos(oldPos + 1);
                }
                for (int t = item.getPos(); t < noOfIds; t++) {
                    int cand = list.get(t).getKey();
                    if (cand != k) {
                        occurances.add(cand);
                    }
                }
                list.add(new IntPair(k, token.getValue()));
                item.setIds(list);
                index.put(token.getValue(), item);
            }

            for (TIntIterator setIterator = occurances.iterator(); setIterator.hasNext(); ) {
                int cand = setIterator.next();
                if (isCleanCleanER) {
                    if (originalId[k] < datasetDelimiter && originalId[cand] < datasetDelimiter) { // both belong to dataset 1
                        continue;
                    }

                    if (datasetDelimiter <= originalId[k] && datasetDelimiter <= originalId[cand]) { // both belong to dataset 2
                        continue;
                    }
                }
                int realOverlap = getOverlap(k, cand);
                int testValue = realOverlap + threshold * q;
                if (testValue < tokens[k].size() || testValue < tokens[cand].size()) {
                    continue;
                }

                float distance = getEditDistance(attributeValues.get(k), attributeValues.get(cand), threshold);
                if (distance <= threshold) {
                    int id1 = Math.min(originalId[k], originalId[cand]);
                    int id2 = Math.max(originalId[k], originalId[cand]);
                    final Comparison currentComp = getComparison(id1, id2);
                    /*if (isCleanCleanER) {
                        if(originalId[k]>datasetDelimiter)
                    }*/
                    currentComp.setUtilityMeasure(1 - distance / threshold);
                    executedComparisons.add(currentComp);
                }
            }

            if (attributeValues.get(k).length() - threshold >= (threshold + 1) * q) {
                continue;
            }

            int bound = noOfEntities;
            for (int sIndex = 0; sIndex < noOfEntities; sIndex++) {
                final String s = attributeValues.get(sIndex);

                if (s.length() >= attributeValues.get(k).length() - threshold) {
                    bound = sIndex;
                    break;
                }
            }

            while (bound != rangeBound) {
                if (isCleanCleanER) {
                    if (originalId[k] < datasetDelimiter && originalId[bound] < datasetDelimiter) { // both belong to dataset 1
                        bound++;
                        continue;
                    }

                    if (datasetDelimiter <= originalId[k] && datasetDelimiter <= originalId[bound]) { // both belong to dataset 2
                        bound++;
                        continue;
                    }
                }
                float distance = getEditDistance(attributeValues.get(k), attributeValues.get(bound), threshold);
                if (distance <= threshold) {
                    int id1 = Math.min(originalId[k], originalId[bound]);
                    int id2 = Math.max(originalId[k], originalId[bound]);
                    final Comparison currentComp = getComparison(id1, id2);
                    currentComp.setUtilityMeasure(1 - distance / threshold);
                    executedComparisons.add(currentComp);
                }
                bound++;
            }
        }
        return executedComparisons;
    }
}
