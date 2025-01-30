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

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 *
 * @author G.A.P. II
 */
public class CorrelationClustering extends AbstractEntityClustering {
    private static final long serialVersionUID = -5316716138615804328L;
    private int[] verticesToClusters;
    private int numClusters;
    private int maxNumClusters;
    private EquivalenceCluster[] clustersCreated;
    private boolean[][] areSimilar;
    private boolean[][] areNotSimilar;
    private final float thresholdForInitialClusters = 0.5f;
    private final float thresholdForSimilar = 0.8f;
    private final float thresholdForNotSimilar = 0.2f;
    private final int numOfLSIterations = 10000;

    private Random rand;

    public CorrelationClustering() {
        this(0.6f);
    }

    public CorrelationClustering(float simTh) {
        super(simTh);
    }
    
    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }
    
    @Override
    public EquivalenceCluster[] getDuplicates(SimilarityPairs simPairs) {
        initializeData(simPairs);

        // add an edge for every pair of entities with a weight higher than the threshold
        final Iterator<Comparison> iterator = simPairs.getPairIterator();
//        float totalSimilarity = 0.0f;
//        int numComparisons = 0;
        float[][] similarities = new float[noOfEntities][noOfEntities];
        while (iterator.hasNext()) {
            final Comparison comparison = iterator.next();

            float utilityMeasure = comparison.getUtilityMeasure();
            int id1 = comparison.getEntityId1();
            int id2 = comparison.getEntityId2();
            similarities[id1][id2 + datasetLimit] = utilityMeasure;
            similarities[id2 + datasetLimit][id1] = utilityMeasure;
//            totalSimilarity += utilityMeasure;
//            numComparisons++;
            if (thresholdForInitialClusters < utilityMeasure) {
                similarityGraph.addEdge(id1, id2 + datasetLimit);
            }
        }

        simPairs = null;
        //start from connected components

        EquivalenceCluster[] initialClusters = getConnectedComponents();
        numClusters = initialClusters.length;
        maxNumClusters = 10 + numClusters;
        clustersCreated = new EquivalenceCluster[maxNumClusters];
        System.arraycopy(initialClusters, 0, clustersCreated, 0, initialClusters.length);

        //map vertices to clusters
        verticesToClusters = new int[noOfEntities];
        for (int clCounter = 0; clCounter < numClusters; clCounter++) {
            for (int i = 0; i < noOfEntities; i++) {
                if (clustersCreated[clCounter].getEntityIdsD1().contains(i)) {
                    if (verticesToClusters[i] > 0) {
                        System.err.println("Float entrance ");
                    }
                    verticesToClusters[i] = clCounter;
                }
            }
        }

        //initialize similarity boolean arrays
        //entities are considered similar (+1) if they have similarity above the threshold
        //and not similar when they have similarity less than 1-threshold
        areSimilar = new boolean[noOfEntities][noOfEntities];
        areNotSimilar = new boolean[noOfEntities][noOfEntities];
        for (int i = 0; i < noOfEntities; i++) {
            for (int j = i + 1; j < noOfEntities; j++) {
                areNotSimilar[i][j] = false;
                areNotSimilar[j][i] = false;
                if (similarities[i][j] > thresholdForSimilar) {
                    areSimilar[i][j] = true;
                    areSimilar[j][i] = true;
                } else {
                    areSimilar[i][j] = false;
                    areSimilar[j][i] = false;
                    if (similarities[i][j] < thresholdForNotSimilar) {
                        areNotSimilar[i][j] = true;
                        areNotSimilar[j][i] = true;
                    }
                }
            }
        }

        //Optimization step for maximizing Objective function
        int prevOF = getOF();
        //System.out.println("average similarity = "+averageSimilarity);
//        System.out.println("old value=" + prevOF);
//        float time0 = System.currentTimeMillis();
        rand = new Random();
        int moveLimit = 1;//only change-cluster moves
        for (int t = 0; t < numOfLSIterations; t++) {

            int moveIndex = rand.nextInt(moveLimit);
            int OF = doMove(moveIndex, prevOF);
            /*if (prevOF <OF)
        	{
        		System.out.println("new best = "+OF+" at iteration "+t);
        	}*/
            prevOF = OF;
        }
//        float time1 = System.currentTimeMillis();
//        System.out.println("optimization step: " + (time1 - time0));
//        System.out.println("new OF value=" + prevOF);

        //return array after removing empty clusters
        List<EquivalenceCluster> list = new ArrayList<>();
        for (int clCounter = 0; clCounter < numClusters; clCounter++) {
            if (clustersCreated[clCounter].getEntityIdsD1().isEmpty()) {
                continue;
            }
            list.add(clustersCreated[clCounter]);
        }
        int numFinalClusters = list.size();
        EquivalenceCluster[] finalClusters = new EquivalenceCluster[numFinalClusters];
        for (int clCounter = 0; clCounter < numFinalClusters; clCounter++) {
            finalClusters[clCounter] = list.get(clCounter);
        }

        return finalClusters;
        //return getConnectedComponents();
    }

    private int getOF() {
        int OFvalue = 0;
        for (int i = 0; i < noOfEntities; i++) {
            for (int j = i + 1; j < noOfEntities; j++) {
                if (((areSimilar[i][j]) && (verticesToClusters[i] == verticesToClusters[j]))
                        || ((areNotSimilar[i][j]) && (verticesToClusters[i] != verticesToClusters[j]))) {
                    OFvalue++;
                }
            }
        }
        return OFvalue;
    }

    private int doMove(int moveIndex, int prevOF) {
        switch (moveIndex) {
            case 0:
                int randomEntity = rand.nextInt(noOfEntities);
                int randomCluster = rand.nextInt(numClusters);
                while (clustersCreated[randomCluster].getEntityIdsD1().isEmpty()) {
                    randomCluster = rand.nextInt(numClusters);
                }
                return changeCluster(prevOF, randomEntity, randomCluster);
            case 1: 
                int prevCluster = rand.nextInt(numClusters);
                while (clustersCreated[prevCluster].getEntityIdsD1().isEmpty()) {
                    prevCluster = rand.nextInt(numClusters);
                }
                int newCluster = rand.nextInt(numClusters);
                while ((prevCluster == newCluster) || (clustersCreated[newCluster].getEntityIdsD1().isEmpty())) {
                    newCluster = rand.nextInt(numClusters);
                }
                return unifyClusters(prevOF, prevCluster, newCluster);
            case 2: 
                prevCluster = rand.nextInt(numClusters);
                while (clustersCreated[prevCluster].getEntityIdsD1().isEmpty()) {
                    prevCluster = rand.nextInt(numClusters);
                }
                return separateClusters(prevOF, prevCluster);
            default:
                System.err.println("not valid move index");
                return Integer.MAX_VALUE;
        }
    }

    private int changeCluster(int prevOF, int entity, int newCluster) {
        int prevCluster = verticesToClusters[entity];
        verticesToClusters[entity] = newCluster;

        int newOF = getOF();
        if (newOF > prevOF) {
            clustersCreated[prevCluster].getEntityIdsD1().remove(entity);
            clustersCreated[newCluster].addEntityIdD1(entity);
            return newOF;
        } else {
            verticesToClusters[entity] = prevCluster;
            /*if (prevOF!=getOF())
        	{
        		System.out.println("prevOF="+prevOF);
        	}*/

            return prevOF;
        }
    }

    private int unifyClusters(int prevOF, int prevCluster, int newCluster) {
        final TIntList tobeRemoved = new TIntArrayList();

        for (int i = 0; i < clustersCreated[prevCluster].getEntityIdsD1().size(); i++) {
            int entity = clustersCreated[prevCluster].getEntityIdsD1().get(i);
            tobeRemoved.add(entity);

            verticesToClusters[entity] = newCluster;
        }
        int newOF = getOF();

        final TIntIterator iterator = tobeRemoved.iterator();
        if (newOF > prevOF) {
            //if solution is accepted update clusters
            while (iterator.hasNext()) {
                int entity1 = iterator.next();
                clustersCreated[prevCluster].getEntityIdsD1().remove(entity1);
                clustersCreated[newCluster].addEntityIdD1(entity1);
            }
            return newOF;
        } else {
            //else undo move
            while (iterator.hasNext()) {
                int entity1 = iterator.next();
                verticesToClusters[entity1] = prevCluster;
            }
            return prevOF;
        }
    }

    private int separateClusters(int prevOF, int prevCluster) {
        //create additional cluster
        int newCluster = numClusters;
        //try
        final TIntList tobeRemoved = new TIntArrayList();
        for (int i = 0; i < clustersCreated[prevCluster].getEntityIdsD1().size(); i += 2) {
            int entity = clustersCreated[prevCluster].getEntityIdsD1().get(i);
            tobeRemoved.add(entity);
            verticesToClusters[entity] = newCluster;
        }
        final TIntIterator iterator = tobeRemoved.iterator();
        int newOF = getOF();
        if (newOF > prevOF) {
            //update
            clustersCreated[newCluster] = new EquivalenceCluster();
            numClusters++;
            while (iterator.hasNext()) {
                int entity1 = iterator.next();
                clustersCreated[prevCluster].getEntityIdsD1().remove(entity1);
                clustersCreated[newCluster].addEntityIdD1(entity1);
            }
            return newOF;
        } else {
            //undo
            while (iterator.hasNext()) {
                int entity1 = iterator.next();
                verticesToClusters[entity1] = prevCluster;
            }
            /*int nowOF = getOF();
        	if (prevOF!=nowOF)
        	{
        		System.out.println("prevOFinSEPARATION="+prevOF);
        		System.out.println("nowOF             ="+nowOF);
        		System.out.println();
        	}*/
            return prevOF;
        }
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it gets equivalence clusters from correlation optimization step.";
    }

    @Override
    public String getMethodName() {
        return "Correlation Clustering";
    }
}
