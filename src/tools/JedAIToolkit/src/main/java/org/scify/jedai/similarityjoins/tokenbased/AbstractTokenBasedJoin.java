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

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.configuration.gridsearch.DblGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.DblRandomSearchConfiguration;
import org.scify.jedai.similarityjoins.AbstractSimilarityJoin;

/**
 *
 * @author gap2
 */
public abstract class AbstractTokenBasedJoin extends AbstractSimilarityJoin {
    
    protected float threshold;
    
    protected final DblGridSearchConfiguration gridThreshold;
    protected final DblRandomSearchConfiguration randomThreshold;
    protected boolean SetVersion = false;

    AbstractTokenBasedJoin(float thr) {
        super();
        threshold = thr;
        
        gridThreshold = new DblGridSearchConfiguration(1.0f, 0.025f, 0.025f);
        randomThreshold = new DblRandomSearchConfiguration(1.0f, 0.01f);
    }
    
    protected float calcSimilarity(int l1, int l2, float overlap) {
        return overlap / (l1 + l2 - overlap) + 1e-6f;
    }
    
    protected int djbHash(String str) {
        int hash = 5381;
        int len = str.length();

        for (int k = 0; k < len; k++) {
            hash += (hash << 5) + str.charAt(k);
        }

        return (hash & 0x7FFFFFFF);
    }
 
    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + threshold;
    }
    
    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves a single parameter:\n"
                + "1)" + getParameterDescription(0) + ".\n";
    }
    
    @Override
    public int getNumberOfGridConfigurations() {
        return gridThreshold.getNumberOfConfigurations();
    }
    
    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj = new JsonObject();
        obj.put("class", "java.lang.Float");
        obj.put("name", getParameterName(0));
        obj.put("defaultValue", "0.8");
        obj.put("minValue", "0.025");
        obj.put("maxValue", "1.0");
        obj.put("stepValue", "0.025");
        obj.put("description", getParameterDescription(0));

        final JsonArray array = new JsonArray();
        array.add(obj);

        return array;
    }
    
    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " specifies the minimum Jaccard similarity between two attribute values, "
                        + "above which they are considered as matches. ";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Similarity Threshold";
            default:
                return "invalid parameter id";
        }
    }
    
    protected int indexLength(int l) {
        return (int) ((1 - 2 * threshold / (1 + threshold)) * l + 1 + 1e-6);
    }

    protected int maxPossibleLength(int l) {
        return (int) (l / threshold + 1e-6);
    }

    protected int minPossibleLength(int l) {
        return (int) Math.ceil(l * threshold - 1e-6);
    }
    
    protected int probeLength(int l) {
        if (l==0) return 0;
        return (int) ((1 - threshold) * l + 1 + 1e-6);
    }

    protected int requireOverlap(int l1, int l2) {
        return (int) Math.ceil(threshold / (1 + threshold) * (l1 + l2) - 1e-6);
    }
    
    @Override
    public void setNextRandomConfiguration() {
        threshold = (Float) randomThreshold.getNextRandomValue();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        threshold = (Float) gridThreshold.getNumberedValue(iterationNumber);
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        threshold = (Float) randomThreshold.getNumberedRandom(iterationNumber);
    }

    public void setSetVersion(boolean setVersion) {
        SetVersion = setVersion;
    }

}
