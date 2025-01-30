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
import org.scify.jedai.utilities.enumerations.EntityClusteringCcerMethod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Manos
 */
public class BAHafterUM extends AbstractCcerEntityClustering {
    private static final long serialVersionUID = -6575737223474754645L;

    protected float[][] matrix; // inverted similarity matrix (cost matrix)

    private int[] selectedColumn;

    private int[] selectedRow;

    private boolean dataset2isbigger;

    private int numMoves;

    private SimilarityPairs simPairs;

    public BAHafterUM() {
        this(0.5f);
    }

    public BAHafterUM(float simTh) {
        super(simTh);
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
    
    private boolean acceptSwap(float D) {
        return (D < 0.0);
    }

    private void execute() {
        long timeout = 120;//IN SECONDS
        Random rand = new Random();
        int numRows = matrix.length;
        int numColumns = matrix[0].length;
        long time2 = System.currentTimeMillis();
        long time3;
        System.out.println("sts");

        for (int i = 0; i < numMoves; i++) {
            if (dataset2isbigger) {
                    int col1 = rand.nextInt(numColumns);
                    int col2 = rand.nextInt(numColumns);
                    while (col1 == col2) {
                        col2 = rand.nextInt(numRows);
                    }
                    swapRows(col1, col2);
                } else {
                    int row1 = rand.nextInt(numRows);
                    int row2 = rand.nextInt(numRows);
                    while (row1 == row2) {
                        row2 = rand.nextInt(numRows);
                    }
                    swapColumns(row1, row2);
                }
            time3 = System.currentTimeMillis();
            if ((time3 - time2) > 1000 * timeout) {
                break;
            }
        }
    }

    @Override
    public EquivalenceCluster[] getDuplicates(SimilarityPairs simPairs) {
        Log.info("Input comparisons\t:\t" + simPairs.getNoOfComparisons());
        this.simPairs=simPairs;
        matchedIds.clear();
        if (simPairs.getNoOfComparisons() == 0) {
            return new EquivalenceCluster[0];
        }
//        long time0 = System.currentTimeMillis();
        initializeData(simPairs);
        if (!isCleanCleanER) {
            return null; //the method is only applicable to Clean-Clean ER
        }

        final Iterator<Comparison> iterator = simPairs.getPairIterator();
        int matrixSize = Math.max(noOfEntities - datasetLimit, datasetLimit);
        dataset2isbigger = false;//do not use it for now
        //if (noOfEntities - datasetLimit > datasetLimit) dataset2isbigger = true;
        float[][] simMatrix = new float[matrixSize][matrixSize];
        while (iterator.hasNext()) {
            final Comparison comparison = iterator.next();
            if (threshold < comparison.getUtilityMeasure()) {
                simMatrix[comparison.getEntityId1()][comparison.getEntityId2()] = comparison.getUtilityMeasure();
            }
        }
        init(getNegative(simMatrix));
//        long time1 = System.currentTimeMillis();
        //System.out.println( ((double)time1-(double)time0)/1000.0+" s");

        execute();

        if (dataset2isbigger) {
            int[] solutionHeuristic = getSolution();

            for (int i = 0; i < solutionHeuristic.length; i++) {
                int e2 = i;
                int e1 = solutionHeuristic[i];
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

        } else {
            int[] solutionHeuristic = getSolution();

            for (int i = 0; i < solutionHeuristic.length; i++) {
                int e1 = i;
                int e2 = solutionHeuristic[i];
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
        }

        return getConnectedComponents();
    }

    private void getInitialSolution() {
        EntityClusteringCcerMethod ecMethod= EntityClusteringCcerMethod.UNIQUE_MAPPING_CLUSTERING;
        IEntityClustering ec = EntityClusteringCcerMethod.getDefaultConfiguration(ecMethod);
        ec.setSimilarityThreshold(threshold);
        EquivalenceCluster[] entityClusters = ec.getDuplicates(simPairs);
        List<Integer> nonmatchedids1 = new ArrayList<>();
        for (int i =0;i<matrix.length;i++) nonmatchedids1.add(i);
        List<Integer> nonmatchedids2 = new ArrayList<>();
        for (int i =0;i<matrix.length;i++) nonmatchedids2.add(i);
        for (EquivalenceCluster ecluster : entityClusters) {
            if (ecluster.getEntityIdsD1().size() < 1 || ecluster.getEntityIdsD2().size() < 1) {
                continue;
            }
            int i= ecluster.getEntityIdsD1().get(0);
            nonmatchedids1.remove((Integer) i);
            selectedColumn[i] = ecluster.getEntityIdsD2().get(0);//-datasetLimit;
            nonmatchedids2.remove((Integer) selectedColumn[i]);
            //float D = matrix[i][selectedColumn[i]] ;
            //System.out.println(D);
        }
        //System.out.println("random now");
        for (int i:nonmatchedids1)
        {
            selectedColumn[i]=nonmatchedids2.remove(0);
            //nonmatchedids2.remove((Integer) selectedColumn[i]);
            //float D = matrix[i][selectedColumn[i]] ;
            //System.out.println(selectedColumn[i]+" "+D);
        }
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it creates clusters after heuristically solving the assignment problem. ";
    }

    @Override
    public String getMethodName() {
        return "Assignment Problem Heuristic Clustering w TO and initial solution from Unique Mapping";
    }

    private float[][] getNegative(float[][] initMatrix) {
        int N = initMatrix.length;
        float[][] negMatrix = new float[N][N];
        for (int i = 0; i < initMatrix.length; i++) {
            for (int j = 0; j < initMatrix[i].length; j++) {
                negMatrix[i][j] = 1.0f - initMatrix[i][j];
            }
        }
        return negMatrix;
    }

    private int[] getSolution() {
        if (dataset2isbigger) {
            return selectedRow;
        } else {
            return selectedColumn;
        }
    }

    public void init(float[][] matrix) {
        this.matrix = matrix;
        //System.out.println(this.numMoves);
        if (dataset2isbigger) {
            this.selectedRow = new int[matrix[0].length];
        } else {
            this.selectedColumn = new int[matrix.length];
        }
        //this.numMoves = noOfEntities;//9999999
        this.numMoves = 9999999;
        if (noOfEntities > 20000) {
            this.numMoves *= 100;
        }
        /*System.out.println(this.numMoves*150000+" *1");
        System.out.println(noOfEntities+" noEntities");*/
        getInitialSolution();
    }

    public void setNumMoves(int numMoves) {
        this.numMoves = numMoves;
    }

    private void swapColumns(int row1, int row2) {
        int col1 = selectedColumn[row1];
        int col2 = selectedColumn[row2];
        float D = matrix[row1][col2] + matrix[row2][col1] - (matrix[row1][col1] + matrix[row2][col2]);
        if (acceptSwap(D)) {
            selectedColumn[row1] = col2;
            selectedColumn[row2] = col1;
        }
    }

    private void swapRows(int col1, int col2) {
        int row1 = selectedRow[col1];
        int row2 = selectedRow[col2];
        float D = matrix[row1][col2] + matrix[row2][col1] - (matrix[row1][col1] + matrix[row2][col2]);
        if (acceptSwap(D)) {
            selectedRow[col1] = row2;
            selectedRow[col2] = row1;
        }
    }

}
