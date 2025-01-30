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
package org.scify.jedai.blockbuilding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;

/**
 *
 * @author gap2
 */
public class QGramsBlocking extends StandardBlocking {
    private static final long serialVersionUID = -6041703195630324565L;

    protected int nGramSize;

    protected final IntGridSearchConfiguration gridNGSize;
    protected final IntRandomSearchConfiguration randomNGSize;

    public QGramsBlocking() {
        this(6);
    }

    public QGramsBlocking(int n) {
        super();
        nGramSize = n;

        gridNGSize = new IntGridSearchConfiguration(6, 2, 1);
        randomNGSize = new IntRandomSearchConfiguration(6, 2);
    }

    public void setnGramSize(int nGramSize) {
        this.nGramSize = nGramSize;
    }
    
    @Override
    protected Set<String> getBlockingKeys(String attributeValue) {
        final Set<String> nGrams = new HashSet<>();
        for (String token : getTokens(attributeValue)) {
            nGrams.addAll(getNGrams(nGramSize, token));
        }

        return nGrams;
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + nGramSize;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it creates one block for every q-gram that is extracted from any token in the attribute values of any entity.\n"
                + "The q-gram must be shared by at least two entities.";
    }

    @Override
    public String getMethodName() {
        return "Q-Grams Blocking";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves a single parameter:\n"
                + "1)" + getParameterDescription(0) + ".\n";
    }

    protected List<String> getNGrams(int n, String blockingKey) {
        final List<String> nGrams = new ArrayList<>();
        if (blockingKey.length() < n) {
            nGrams.add(blockingKey);
        } else {
            int currentPosition = 0;
            final int length = blockingKey.length() - (n - 1);
            while (currentPosition < length) {
                nGrams.add(blockingKey.substring(currentPosition, currentPosition + n));
                currentPosition++;
            }
        }
        return nGrams;
    }

    @Override
    public int getNumberOfGridConfigurations() {
        return gridNGSize.getNumberOfConfigurations();
    }

    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "java.lang.Integer");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "6");
        obj1.put("minValue", "2");
        obj1.put("maxValue", "6");
        obj1.put("stepValue", "1");
        obj1.put("description", getParameterDescription(0));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " defines the number of characters that comprise every q-gram.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Q-gram Size";
            default:
                return "invalid parameter id";
        }
    }
    
    @Override
    public void setNextRandomConfiguration() {
        nGramSize = (Integer) randomNGSize.getNextRandomValue();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        nGramSize = (Integer) gridNGSize.getNumberedValue(iterationNumber);
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        nGramSize = (Integer) randomNGSize.getNumberedRandom(iterationNumber);
    }
}
