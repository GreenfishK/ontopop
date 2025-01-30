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
package org.scify.jedai.entityclustering;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.configuration.gridsearch.DblGridSearchConfiguration;
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.DblRandomSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;

import java.util.Iterator;

/**
 *
 * @author G.A.P. II
 */
public class MarkovClustering extends AbstractEntityClustering {
    private static final long serialVersionUID = -5802817433513198173L;
    protected float clusterThreshold;//define similarity threshold for including in final graph
    protected float matrixSimThreshold;//define similarity threshold for matrix comparison
    protected int similarityChecksLimit;//define check repetitions limit for the expansion-inflation process

    protected final DblGridSearchConfiguration gridCThreshold;
    protected final DblGridSearchConfiguration gridMSThreshold;
    protected final DblRandomSearchConfiguration randomCThreshold;
    protected final DblRandomSearchConfiguration randomMSThreshold;
    protected final IntGridSearchConfiguration gridSCLimit;
    protected final IntRandomSearchConfiguration randomSCLimit;

    public MarkovClustering() {
        this(0.001f, 0.00001f, 2, 0.5f);
    }

    public MarkovClustering(float ct, float mst, int scl, float st) {
        super(st);
        clusterThreshold = ct;
        matrixSimThreshold = mst;
        similarityChecksLimit = scl;

        gridCThreshold = new DblGridSearchConfiguration(0.100f, 0.002f, 0.002f);
        gridMSThreshold = new DblGridSearchConfiguration(0.001f, 0.00001f, 0.00001f);
        gridSCLimit = new IntGridSearchConfiguration(10, 1, 1);
        randomCThreshold = new DblRandomSearchConfiguration(0.100f, 0.001f);
        randomMSThreshold = new DblRandomSearchConfiguration(0.001f, 0.00001f);
        randomSCLimit = new IntRandomSearchConfiguration(10, 1);
    }

    private void addSelfLoop(float[][] a) {
        int m1 = a.length;
        for (int i = 0; i < m1; i++) {
            a[i][i] = 1.0f;
        }
    }

    private boolean areSimilar(float[][] a, float[][] b) {
        int m1 = a.length;
        int m2 = b.length;
        if (m1 != m2) {
            return false;
        }

        int n1 = a[0].length;
        int n2 = b[0].length;
        if (n1 != n2) {
            return false;
        }

        for (int i = 0; i < m1; i++) {
            for (int j = 0; j < n1; j++) {
                if (Math.abs(a[i][j] - b[i][j]) > matrixSimThreshold) {
                    return false;
                }
            }
        }

        return true;
    }

    private void expand2(float[][] inputMatrix) {
        final float[][] input = multiply(inputMatrix, inputMatrix);
        for (int i = 0; i < inputMatrix.length; i++) {
            System.arraycopy(input[i], 0, inputMatrix[i], 0, inputMatrix[0].length);
        }
    }

    @Override
    public EquivalenceCluster[] getDuplicates(SimilarityPairs simPairs) {
        initializeData(simPairs);

        // add an edge for every pair of entities with a weight higher than the threshold
        final Iterator<Comparison> iterator = simPairs.getPairIterator();
        float[][] simMatrix = new float[noOfEntities][noOfEntities];
        while (iterator.hasNext()) {
            final Comparison comparison = iterator.next();
            if (threshold < comparison.getUtilityMeasure()) {
                simMatrix[comparison.getEntityId1()][comparison.getEntityId2() + datasetLimit] = comparison.getUtilityMeasure();
            }
        }

        addSelfLoop(simMatrix);
        normalizeColumns(simMatrix);
        float[][] atStart = new float[noOfEntities][noOfEntities];
        int count = 0;
        do {
            for (int i = 0; i < noOfEntities; i++) {
                System.arraycopy(simMatrix[i], 0, atStart[i], 0, noOfEntities);
            }
            expand2(simMatrix);
            normalizeColumns(simMatrix);
            hadamard(simMatrix, 2);
            normalizeColumns(simMatrix);
            count++;
        } while ((!areSimilar(atStart, simMatrix)) && (count < similarityChecksLimit));

        int n1 = simMatrix.length;
        int upLimit = n1;
        int lowLimit = 0;
        if (datasetLimit != 0) {
            upLimit = datasetLimit;
            lowLimit = datasetLimit;
        }

        for (int i = 0; i < upLimit; i++) {
            for (int j = lowLimit; j < n1; j++) {
                float sim = Math.max(simMatrix[i][j], simMatrix[j][i]);
                if ((sim > clusterThreshold) && (i != j)) {
                    similarityGraph.addEdge(i, j);
                }
            }
        }

        return getConnectedComponents();
    }

    @Override
    public String getMethodConfiguration() {
        return super.getMethodConfiguration() + ",\t"
                + getParameterName(1) + "=" + clusterThreshold + ",\t"
                + getParameterName(2) + "=" + matrixSimThreshold + ",\t"
                + getParameterName(3) + "=" + similarityChecksLimit;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it implements the Markov Cluster Algorithm.";
    }

    @Override
    public String getMethodName() {
        return "Markov Clustering";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves four parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".\n"
                + "3)" + getParameterDescription(2) + ".\n"
                + "4)" + getParameterDescription(3) + ".";
    }

    @Override
    public int getNumberOfGridConfigurations() {
        return super.getNumberOfGridConfigurations() * gridCThreshold.getNumberOfConfigurations()
                * gridMSThreshold.getNumberOfConfigurations() * gridSCLimit.getNumberOfConfigurations();
    }

    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "java.lang.Float");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "0.5");
        obj1.put("minValue", "0.1");
        obj1.put("maxValue", "0.95");
        obj1.put("stepValue", "0.05");
        obj1.put("description", getParameterDescription(0));

        final JsonObject obj2 = new JsonObject();
        obj2.put("class", "java.lang.Float");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "0.001");
        obj2.put("minValue", "0.001");
        obj2.put("maxValue", "0.100");
        obj2.put("stepValue", "0.001");
        obj2.put("description", getParameterDescription(1));

        final JsonObject obj3 = new JsonObject();
        obj3.put("class", "java.lang.Float");
        obj3.put("name", getParameterName(2));
        obj3.put("defaultValue", "0.00001");
        obj3.put("minValue", "0.00001");
        obj3.put("maxValue", "0.00100");
        obj3.put("stepValue", "0.00001");
        obj3.put("description", getParameterDescription(2));

        final JsonObject obj4 = new JsonObject();
        obj4.put("class", "java.lang.Integer");
        obj4.put("name", getParameterName(3));
        obj4.put("defaultValue", "2");
        obj4.put("minValue", "1");
        obj4.put("maxValue", "10");
        obj4.put("stepValue", "1");
        obj4.put("description", getParameterDescription(3));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        array.add(obj2);
        array.add(obj3);
        array.add(obj4);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " determines the cut-off similarity threshold for connecting two entities with an edge in the (initial) similarity graph.";
            case 1:
                return "The " + getParameterName(1) + " determines the similarity threshold for including an edge in the similarity graph.";
            case 2:
                return "The " + getParameterName(1) + " determines the similarity threshold for compariing all cells of two matrices and considering them similar.";
            case 3:
                return "The " + getParameterName(1) + " determines the maximum number of repetitions we apply the expansion-inflation process.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Similarity Threshold";
            case 1:
                return "Cluster Threshold";
            case 2:
                return "Matrix Similarity Threshold";
            case 3:
                return "Similarity Checks Limit";
            default:
                return "invalid parameter id";
        }
    }

    private void hadamard(float[][] a, int pow) {
        int m1 = a.length;
        int n1 = a[0].length;
        for (int i = 0; i < m1; i++) {
            for (int j = 0; j < n1; j++) {
                a[i][j] = (float) Math.pow(a[i][j], pow);
            }
        }
    }

    private float[][] multiply(float[][] a, float[][] b) {
        int n1 = a.length;
        if (n1 != a[0].length) {
            throw new RuntimeException("Illegal matrix dimensions.");
        }

        int upLimit = n1;
        int lowLimit = 0;
        if (datasetLimit != 0) {
            upLimit = datasetLimit;
            lowLimit = datasetLimit;
        }

        final float[][] c = new float[n1][n1];
        for (int i = 0; i < upLimit; i++) {
            for (int j = lowLimit; j < n1; j++) {
                for (int k = 0; k < n1; k++) {
                    c[i][j] += a[i][k] * b[k][j];
                }
            }
        }

        if (datasetLimit == 0) {
            return c;
        }

        for (int i = 0; i < upLimit; i++) {
            c[i][i] += a[i][i] * b[i][i];
        }
        for (int j = lowLimit; j < n1; j++) {
            c[j][j] += a[j][j] * b[j][j];
        }
        return c;
    }

    private void normalizeColumns(float[][] a) {
        int m1 = a.length;
        int n1 = a[0].length;
        for (int j = 0; j < n1; j++) {
            float sumCol = 0.0f;
            for (float[] floats : a) {
                sumCol += floats[j];
            }

            for (int i = 0; i < m1; i++) {
                a[i][j] /= sumCol;
            }
        }
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
    
    public void setClusterThreshold(float clusterThreshold) {
        this.clusterThreshold = clusterThreshold;
    }

    public void setMatrixSimThreshold(float matrixSimThreshold) {
        this.matrixSimThreshold = matrixSimThreshold;
    }

    @Override
    public void setNextRandomConfiguration() {
        super.setNextRandomConfiguration();

        clusterThreshold = (Float) randomCThreshold.getNextRandomValue();
        matrixSimThreshold = (Float) randomMSThreshold.getNextRandomValue();
        similarityChecksLimit = (Integer) randomSCLimit.getNextRandomValue();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        int secondStepConfs = gridCThreshold.getNumberOfConfigurations() * gridMSThreshold.getNumberOfConfigurations() * gridSCLimit.getNumberOfConfigurations();
        int thrIteration = iterationNumber / secondStepConfs;
        super.setNumberedGridConfiguration(thrIteration);

        int remainingIterations = iterationNumber % secondStepConfs;
        int thirdStepConfs = gridCThreshold.getNumberOfConfigurations() * gridSCLimit.getNumberOfConfigurations();
        int mstIteration = remainingIterations / thirdStepConfs;
        matrixSimThreshold = (Float) gridMSThreshold.getNumberedValue(mstIteration);

        remainingIterations = remainingIterations % thirdStepConfs;
        int cthrIteration = remainingIterations / gridSCLimit.getNumberOfConfigurations();
        clusterThreshold = (Float) gridCThreshold.getNumberedValue(cthrIteration);

        int scIteration = remainingIterations % gridSCLimit.getNumberOfConfigurations();
        similarityChecksLimit = (Integer) gridSCLimit.getNumberedValue(scIteration);
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        super.setNumberedRandomConfiguration(iterationNumber);

        clusterThreshold = (Float) randomCThreshold.getNumberedRandom(iterationNumber);
        matrixSimThreshold = (Float) randomMSThreshold.getNumberedRandom(iterationNumber);
        similarityChecksLimit = (Integer) randomSCLimit.getNumberedRandom(iterationNumber);
    }

    public void setSimilarityChecksLimit(int similarityChecksLimit) {
        this.similarityChecksLimit = similarityChecksLimit;
    }
}
