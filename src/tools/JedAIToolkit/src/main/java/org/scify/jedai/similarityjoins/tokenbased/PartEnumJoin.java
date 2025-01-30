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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datamodel.joins.Category;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author mthanos
 */
public class PartEnumJoin extends AbstractTokenBasedJoin {

    private final static int MAX_LEN = 3300;
    private final static int MAX_CATEGORY = 100;
    
    private int categoryN;
    private float categoryTHRESHOLD;

    private int[] originalId;
    
    private Category[] helper;
    private List<Comparison> executedComparisons;
    private final List<String> attributeValues;
    private TIntList[] records;

    public PartEnumJoin(float thr) {
        super(thr);
        attributeValues = new ArrayList<>();
    }

    @Override
    public SimilarityPairs applyJoin() {
        init();

        helper = new Category[MAX_CATEGORY];
        int len = 1;
        this.categoryTHRESHOLD = threshold;
        for (int k = 0; k < MAX_CATEGORY; k++) {
            helper[k] = new Category(len, threshold, categoryN);
            len = helper[k].e_len + 1;
            if (len > MAX_LEN) {
                break;
            }
        }

        convert_to_signature();

        final List<Comparison> comparisons = performJoin();
        return getSimilarityPairs(comparisons);
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": TO BE ADDED!.";
    }

    @Override
    public String getMethodName() {
        return "Part Enum Join";
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

        idIdentifier.sort(Comparator.comparingInt(s -> s.getKey().split(" ").length));

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
                records[sIndex].add(token);
            }
            records[sIndex].sort();
        }
    }

    private List<Comparison> performJoin() {
        return executedComparisons;
    }

    private int check_overlap(TIntList a, TIntList b, int overlap) {
        int posa = 0, posb = 0, count = 0;
        while (posa < a.size() && posb < b.size()) {
            if (count + Math.min(a.size() - posa, b.size() - posb) < overlap) {
                return -1;
            }
            if (a.get(posa) == b.get(posb)) {
                count++;
                posa++;
                posb++;
            } else if (a.get(posa) < b.get(posb)) {
                posa++;
            } else {
                posb++;
            }
        }
        return count;
    }

    private float verify(TIntList a, TIntList b) {
        float factor = categoryTHRESHOLD / (1 + categoryTHRESHOLD);
        int require_overlap = (int) Math.ceil(factor * (a.size() + b.size()) - 1e-6);
        int real_overlap = check_overlap(a, b, require_overlap);

        if (real_overlap == -1) {
            return -1;
        }
        return (real_overlap / (float) (a.size() + b.size() - real_overlap));
    }

    void perform_join(int k, int id, boolean[] checked_flag) {
        int index = -1;
        for (int i = 0; i < helper[k].N1; i++) {
            for (TIntList sub : helper[k].subs) {
                index++;
                TIntList prepare = new TIntArrayList();
                for (int j = 0; j < sub.size(); j++) {
                    int s_pos = -1;
                    for (int ppos = 0; ppos < records[id].size(); ppos++) {
                        if (records[id].get(ppos) >= helper[k].range_start[i][sub.get(j)]) {
                            s_pos = (ppos);
                            break;
                        }
                        if (ppos == records[id].size() - 1) {
                            s_pos = ppos + 1;
                        }
                    }
                    int e_pos = -1;
                    for (int ppos = 0; ppos < records[id].size(); ppos++) {
                        if (records[id].get(ppos) >= helper[k].range_end[i][sub.get(j)]) {
                            e_pos = (ppos);
                            break;
                        }
                        if (ppos == records[id].size() - 1) {
                            e_pos = ppos + 1;
                        }
                    }

                    while (s_pos != e_pos) {
                        prepare.add(records[id].get(s_pos));
                        s_pos++;
                    }
                }

                int hash_value = java.util.Arrays.hashCode(prepare.toArray());

                TIntList candidate;
                if ((helper[k].sig_map[index].get(hash_value)) == null) {
                    candidate = new TIntArrayList();
                } else {
                    candidate = (helper[k].sig_map[index].get(hash_value));// [index][hash_value];
                }
                for (int cand_id = 0; cand_id < candidate.size(); cand_id++) {
                    if (isCleanCleanER) {
                        if (originalId[id] < datasetDelimiter && originalId[cand_id] < datasetDelimiter) { // both belong to dataset 1
                            continue;
                        }

                        if (datasetDelimiter <= originalId[id] && datasetDelimiter <= originalId[cand_id]) { // both belong to dataset 2
                            continue;
                        }
                    }
                    if (!checked_flag[candidate.get(cand_id)]) {
                        checked_flag[candidate.get(cand_id)] = true;
                        float jacsim = verify(records[cand_id], records[id]);
                        if (jacsim >= threshold) {
                            final Comparison currentComp = getComparison(originalId[cand_id], originalId[id]);
                            currentComp.setUtilityMeasure(jacsim);
                            executedComparisons.add(currentComp);
                        }
                    }
                }
                if (candidate.size() == 0 || 
                        candidate.get(candidate.size() - 1) != id) {
                    candidate.add(id);
                }
                helper[k].sig_map[index].put(hash_value, candidate);
            }
        }
    }

    void convert_to_signature() {
        executedComparisons = new ArrayList<>();
        boolean[] checked_flag = new boolean[records.length];
        for (int i = 0; i < checked_flag.length; i++) {
            checked_flag[i] = true;
        }
        for (int id = 0; id < records.length; id++) {
            int k;
            for (k = 0; k < MAX_CATEGORY; k++) {
                if (helper[k].s_len <= records[id].size()
                        && helper[k].e_len >= records[id].size()) {
                    break;
                }
            }

            assert (k < MAX_CATEGORY);
            for (int i = 0; i < id; i++) {
                checked_flag[i] = false;
            }
            perform_join(k, id, checked_flag);
            perform_join(k + 1, id, checked_flag);
        }
    }
}
