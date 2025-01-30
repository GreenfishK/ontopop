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
package org.scify.jedai.progressivejoin;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.atlas.json.JsonArray;
import org.scify.jedai.configuration.gridsearch.DblGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.DblRandomSearchConfiguration;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.joins.IntPair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 *
 * @author mthanos
 */
public class Topk extends AbstractProgressiveJoin {

    private int[] originalId;

    protected float threshold;

    protected final DblGridSearchConfiguration gridThreshold;
    protected final DblRandomSearchConfiguration randomThreshold;

    private final List<String> attributeValues;
    private TIntList[] records;

    public Topk(float thr, int budget) {
        super(budget);
        threshold = thr;
        attributeValues = new ArrayList<>();

        gridThreshold = new DblGridSearchConfiguration(1.0f, 0.025f, 0.025f);
        randomThreshold = new DblRandomSearchConfiguration(1.0f, 0.01f);
    }

    @Override
    public void prepareJoin(String attributeName1, String attributeName2, List<EntityProfile> dataset1, List<EntityProfile> dataset2) {
        init();

        final List<Comparison> comparisons = performJoin();
        getSimilarityPairs(comparisons);
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

        final TIntIntMap freqMap = new TIntIntHashMap();
        for (int sIndex = 0; sIndex < noOfEntities; sIndex++) {

            final String s = attributeValues.get(sIndex).trim();
            if (s.length() < 1) {
                continue;
            }

            String[] split = s.split(" ");
            for (String value : split) {
                int token = djbHash(value);
                records[sIndex].add(token);
                int freq = freqMap.get(token);
                freqMap.put(token, (freq + 1));
            }

        }

        final List<IntPair> tokenFreq = new ArrayList<>();
        for (TIntIntIterator iterator = freqMap.iterator(); iterator.hasNext();) {
            iterator.advance();
            tokenFreq.add(new IntPair(iterator.key(), iterator.value()));
        }
        tokenFreq.sort((p1, p2) -> p2.getValue() - p1.getValue());
        freqMap.clear();
        for (int i = 0; i < tokenFreq.size(); i++) {
            freqMap.put(tokenFreq.get(i).getKey(), i);
        }

        for (int sIndex = 0; sIndex < noOfEntities; sIndex++) {

            for (int iii = 0; iii < records[sIndex].size(); iii++) {
                int idfid = freqMap.get(records[sIndex].get(iii)) * 2 + 2;
                records[sIndex].set(iii, idfid);
            }
            records[sIndex].sort();
        }

    }

    private List<Comparison> performJoin() {
        final SetSimJoin ssj = new SetSimJoin();
        LinkedHashMap<String, ArrayList<Integer>> recordsForTopk = new LinkedHashMap<>();
        int cnter = 0;
        for (TIntList rec : records) {
            ArrayList<Integer> tokens = new ArrayList<>();
            String id = ("" + originalId[cnter++]);
            for (int inrec = 0; inrec < rec.size(); inrec++) {
                tokens.add(rec.get(inrec));
            }
            recordsForTopk.put(id, tokens);
        }
        ssj.setCleanCleanER(isCleanCleanER);
        ssj.setNoOfEntities(noOfEntities);
        ssj.setDatasetDelimiter(datasetDelimiter);
        ssj.setRecords(recordsForTopk);
        ssj.topkGlobal(comparisonsBudget);

        ArrayList<Comparison> results = ssj.getResults();
        final List<Comparison> executedComparisons = new ArrayList<>();

        for (Comparison res : results) {
            int id1 = res.getEntityId1();
            int id2 = res.getEntityId2();
            if (id1 == id2) {
                continue;
            }
            if (isCleanCleanER) {
                if (((id1 < datasetDelimiter) && (id2 < datasetDelimiter)) || ((id1 >= datasetDelimiter) && (id2 >= datasetDelimiter))) {
                    continue;
                }
            }
            if (res.getUtilityMeasure() >= threshold) {
                final Comparison currentComp = getComparison(id1, id2);
                currentComp.setUtilityMeasure(res.getUtilityMeasure());
                executedComparisons.add(currentComp);
            }
        }

        return executedComparisons;
    }

    @Override
    public int getNumberOfGridConfigurations() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setNextRandomConfiguration() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getMethodConfiguration() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getMethodInfo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getMethodName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getMethodParameters() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JsonArray getParameterConfiguration() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getParameterDescription(int parameterId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getParameterName(int parameterId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasNext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Comparison next() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
