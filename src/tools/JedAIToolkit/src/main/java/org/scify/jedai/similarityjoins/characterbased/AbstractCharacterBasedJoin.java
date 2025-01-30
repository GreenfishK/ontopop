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

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;
import org.scify.jedai.similarityjoins.AbstractSimilarityJoin;

/**
 *
 * @author gap2
 */

public abstract class AbstractCharacterBasedJoin extends AbstractSimilarityJoin {
    
    protected int matrixDimension1;
    protected int matrixDimension2;
    
    protected int threshold;
    
    protected final IntGridSearchConfiguration gridThreshold;
    protected final IntRandomSearchConfiguration randomThreshold;
    
    public AbstractCharacterBasedJoin(int thr) {
        super();
        threshold = thr;
        
        gridThreshold = new IntGridSearchConfiguration(10, 1, 1);
        randomThreshold = new IntRandomSearchConfiguration(10, 1);
    }
    
    protected int djbHash(String str, int len) {
        int hash = 5381;

        for (int k = 0; k < len; k++) {
            hash += (hash << 5) + str.charAt(k);
        }

        return (hash & 0x7FFFFFFF);
    }

    protected int getEditDistance(String s1, String s2, int THRESHOLD, int... poslen) {        
        int xpos = poslen.length > 0 ? poslen[0] : 0;
        int ypos = poslen.length > 1 ? poslen[1] : 0;
        int xlen = poslen.length > 2 ? poslen[2] : -1;
        int ylen = poslen.length > 3 ? poslen[3] : -1;
        if (xlen == -1) {
            xlen = s1.length() - xpos;
        }
        if (ylen == -1) {
            ylen = s2.length() - ypos;
        }
        if (xlen > ylen + THRESHOLD || ylen > xlen + THRESHOLD) {
            return THRESHOLD + 1;
        }
        if (xlen == 0) {
            return ylen;
        }

        final int[][] matrix = new int[matrixDimension1][matrixDimension2];
        for (int k = 0; k <= THRESHOLD; k++) {
            matrix[0][THRESHOLD + k] = k;
        }

        int right = (THRESHOLD + (ylen - xlen)) / 2;
        int left = (THRESHOLD - (ylen - xlen)) / 2;
        for (int i = 1; i <= xlen; i++) {
            boolean valid = false;
            if (i <= left) {
                matrix[i][THRESHOLD - i] = i;
                valid = true;
            }
            for (int j = (i - left >= 1 ? i - left : 1);
                    j <= (i + right <= ylen ? i + right : ylen); j++) {
                if (s1.charAt(xpos + i - 1) == s2.charAt(ypos + j - 1)) {
                    matrix[i][j - i + THRESHOLD] = matrix[i - 1][j - i + THRESHOLD];
                } else {
                    matrix[i][j - i + THRESHOLD] = min3(matrix[i - 1][j - i + THRESHOLD],
                            j - 1 >= i - left ? matrix[i][j - i + THRESHOLD - 1] : THRESHOLD,
                            j + 1 <= i + right ? matrix[i - 1][j - i + THRESHOLD + 1] : THRESHOLD) + 1;
                }
                if (Math.abs(xlen - ylen - i + j) + matrix[i][j - i + THRESHOLD] <= THRESHOLD) {
                    valid = true;
                }
            }
            if (!valid) {
                return THRESHOLD + 1;
            }
        }
        
        return matrix[xlen][ylen - xlen + THRESHOLD];
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
        obj.put("class", "java.lang.Integer");
        obj.put("name", getParameterName(0));
        obj.put("defaultValue", "3");
        obj.put("minValue", "1");
        obj.put("maxValue", "10");
        obj.put("stepValue", "1");
        obj.put("description", getParameterDescription(0));

        final JsonArray array = new JsonArray();
        array.add(obj);

        return array;
    }
    
    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " specifies the minimum edit distance between two attribute values, "
                        + "below which they are considered as matches. ";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Threshold";
            default:
                return "invalid parameter id";
        }
    }
    
    protected int max3(int i1, int i2, int i3) {
        return Math.max(i1, Math.max(i2, i3));
    }

    protected static int min3(int i1, int i2, int i3) {
        return Math.min(i1, Math.min(i2, i3));
    }
    
    @Override
    public void setNextRandomConfiguration() {
        threshold = (Integer) randomThreshold.getNextRandomValue();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        threshold = (Integer) gridThreshold.getNumberedValue(iterationNumber);
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        threshold = (Integer) randomThreshold.getNumberedRandom(iterationNumber);
    }
}