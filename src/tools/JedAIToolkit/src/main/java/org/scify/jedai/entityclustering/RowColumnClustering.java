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

import com.esotericsoftware.minlog.Log;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;

import java.util.Iterator;

/**
 *
 * @author Manos
 */
public class RowColumnClustering extends AbstractCcerEntityClustering {
    private static final long serialVersionUID = 2339477768564905457L;

    protected float[][] matrix; // inverted similarity matrix (cost matrix)

    protected int[] selectedRow, selectedColumn, columnsFromSelectedRow;

    protected float costRowScan, costColumnScan;

    protected boolean[] isRowCovered, isColumnCovered;

    public RowColumnClustering() {
        this(0.5f);
    }

    public RowColumnClustering(float simTh) {
        super(simTh);
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
    
    private int columnWithMin(int rowNumber) {
        int pos = -1;
        float min = Float.MAX_VALUE;
        for (int col = 0; col < matrix[rowNumber].length; col++) {
            if (isColumnCovered[col]) {
                continue;
            }
            if (matrix[rowNumber][col] < min) {
                pos = col;
                min = matrix[rowNumber][col];
            }
        }
        return pos;
    }

    private void getColumnAssignment() {
        costColumnScan = 0;
        for (int col = 0; col < matrix[0].length; col++) {
            selectedRow[col] = rowWithMin(col);
            if (selectedRow[col]==-1) break;
            columnsFromSelectedRow[selectedRow[col]] = col;
            isRowCovered[selectedRow[col]] = true;
            costColumnScan += matrix[selectedRow[col]][col];


        }
    }
    
    @Override
    public EquivalenceCluster[] getDuplicates(SimilarityPairs simPairs) {
        Log.info("Input comparisons\t:\t" + simPairs.getNoOfComparisons());

        matchedIds.clear();
        if (simPairs.getNoOfComparisons() == 0) {
            return new EquivalenceCluster[0];
        }

        initializeData(simPairs);
        if (!isCleanCleanER) {
            return null; //the method is only applicable to Clean-Clean ER
        }

        final Iterator<Comparison> iterator = simPairs.getPairIterator();
        float[][] simMatrix = new float[datasetLimit][noOfEntities - datasetLimit];
        while (iterator.hasNext()) {
            final Comparison comparison = iterator.next();
            if (threshold < comparison.getUtilityMeasure()) {
                simMatrix[comparison.getEntityId1()][comparison.getEntityId2()] = comparison.getUtilityMeasure();
            }
        }

        init(getNegative(simMatrix));

        int[] solutionProxy = getSolution();

        for (int i = 0; i < solutionProxy.length; i++) {
            int e1 = i;
            int e2 = solutionProxy[i];
            if (simMatrix[e1][e2] < threshold) {
                continue;
            }
            e2 += datasetLimit;
            //skip already matched entities (unique mapping contraint for clean-clean ER)
            if (matchedIds.contains(e1) || matchedIds.contains(e2)) {
                System.err.println("id already in the graph");
            }

            similarityGraph.addEdge(e1, e2);
            matchedIds.add(e1);
            matchedIds.add(e2);
        }

        return getConnectedComponents();
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it create a cluster after approximately solving the assignment problem. ";
    }

    @Override
    public String getMethodName() {
        return "Row-Column Proxy Clustering";
    }
    
    //inverts the input to 1.0-simMatrix in order to apply the minimization problem
    private float[][] getNegative(float[][] initMatrix) {
        int N = initMatrix.length;
        float[][] negMatrix = new float[N][initMatrix[0].length];
        for (int i = 0; i < initMatrix.length; i++) {
            for (int j = 0; j < initMatrix[i].length; j++) {
                negMatrix[i][j] = 1.0f - initMatrix[i][j];
            }
        }
        return negMatrix;
    }
    
    private void getRowAssignment() {
        costRowScan = 0;
        for (int row = 0; row < matrix.length; row++) {
            selectedColumn[row] = columnWithMin(row);
            if (selectedColumn[row]==-1) break;

            isColumnCovered[selectedColumn[row]] = true;
            costRowScan += matrix[row][selectedColumn[row]];
        }
    }
    
    private int[] getSolution() {
        getRowAssignment();
        getColumnAssignment();
        if (costRowScan < costColumnScan) {
            return selectedColumn;
        } else {
            return columnsFromSelectedRow;
        }
    }
    
    private void init(float[][] matrix) {
        this.matrix = matrix;

        this.selectedColumn = new int[matrix.length];
        this.isColumnCovered = new boolean[matrix[0].length];

        this.selectedRow = new int[matrix[0].length];
        this.columnsFromSelectedRow = new int[matrix.length];
        this.isRowCovered = new boolean[matrix.length];
    }
    
    private int rowWithMin(int columnNumber) {
        int pos = -1;
        float min = Float.MAX_VALUE;
        for (int row = 0; row < matrix.length; row++) {
            if (isRowCovered[row]) {
                continue;
            }
            if (matrix[row][columnNumber] < min) {
                pos = row;
                min = matrix[row][columnNumber];
            }
        }
        return pos;
    }
}