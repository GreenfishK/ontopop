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

import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.similarityjoins.fuzzysets.FuzzySetSimJoin;

import java.util.*;
import org.scify.jedai.datamodel.Attribute;

/**
 *
 * @author mthanos
 */
public class SilkMoth extends AbstractTokenBasedJoin {

    private final int qSize;

    private final Map<String, List<Set<String>>> collection1;
    private final Map<String, List<Set<String>>> collection2;

    public SilkMoth(int qSize, float thr) {
        super(thr);
        this.qSize = qSize;
        collection1 = new LinkedHashMap<>();
        collection2 = new LinkedHashMap<>();
    }

    @Override
    public SimilarityPairs applyJoin() {
        init();

        final List<Comparison> comparisons = performJoin();
        return getSimilarityPairs(comparisons);
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it implements the Silkmoth similarity join algorithm";
    }

    @Override
    public String getMethodName() {
        return "FuzzySetJoin";
    }

    private Set<String> getTokens(String attributeValue) {
        if (attributeValue.isEmpty()) {
            return null;
        }

        if (qSize < 0) { // tokenize on whitespace
            String[] tokens = attributeValue.split("[\\W_]");
            return new HashSet<>(Arrays.asList(tokens));
        }

        final Set<String> nGrams = new HashSet<>();
        if (attributeValue.length() < qSize) {
            nGrams.add(attributeValue);
        } else {
            int currentPosition = 0;
            final int length = attributeValue.length() - (qSize - 1);
            while (currentPosition < length) {
                nGrams.add(attributeValue.substring(currentPosition, currentPosition + qSize));
                currentPosition++;
            }
        }
        return nGrams;
    }

    private void init() {
        int counter = 0;
        for (EntityProfile profile : profilesD1) {
            final List<Set<String>> asList = new ArrayList<>();
            for (Attribute attribute : profile.getAttributes()) {
                final Set<String> tokens = getTokens(attribute.getValue().trim());
                asList.add(tokens);
            }
            collection1.put(Integer.toString(counter), asList);
            counter++;
        }
        
        counter = 0;
        for (EntityProfile profile : profilesD2) {
            final List<Set<String>> asList = new ArrayList<>();
            for (Attribute attribute : profile.getAttributes()) {
                final Set<String> tokens = getTokens(attribute.getValue().trim());
                asList.add(tokens);
            }
            collection2.put(Integer.toString(counter), asList);
            counter++;
        }
    }

    private List<Comparison> performJoin() {
        final FuzzySetSimJoin fssj = new FuzzySetSimJoin();
        final Map<String, Float> matchingPairs = fssj.join(collection1, collection2, threshold);
        final List<Comparison> executedComparisons = new ArrayList<>();
        for (String allres : matchingPairs.keySet()) {
            String[] res = allres.split("_");
            int id1 = Integer.parseInt(res[0]);
            int id2 = Integer.parseInt(res[1]);
            float jaccardSim = matchingPairs.get(allres);

            if (isCleanCleanER) {
                id2 += datasetDelimiter;
            } else if (id1 == id2) {
                continue;
            }
            
            if (jaccardSim >= threshold) {
                final Comparison currentComp = getComparison(id1, id2);
                currentComp.setUtilityMeasure(jaccardSim);
                executedComparisons.add(currentComp);
            }
        }

        return executedComparisons;
    }
}
