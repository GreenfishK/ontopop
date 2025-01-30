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
package org.scify.jedai.utilities;

import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.datamodel.Comparison;
import com.esotericsoftware.minlog.Log;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.PairIterator;
import org.scify.jedai.datamodel.SimilarityPairs;

/**
 *
 * @author gap2
 */
public class SimPairsPerformance {

    private final boolean isCleanCleanER;

    private int noOfD1Entities;
    private int noOfD2Entities;
    private int detectedDuplicates;

    private final long aggregateCardinality;
    private float fMeasure;
    private float pc;
    private float pq;

    private final AbstractDuplicatePropagation abstractDP;
    private final SimilarityPairs similarityPairs;

    public SimPairsPerformance(SimilarityPairs simPairs, AbstractDuplicatePropagation adp) {
        abstractDP = adp;
        if (abstractDP != null) {
            abstractDP.resetDuplicates();
        }
        similarityPairs = simPairs;
        isCleanCleanER = simPairs.isCleanCleanER();
        aggregateCardinality = simPairs.getNoOfComparisons();
    }

    public long getAggregateCardinality() {
        return aggregateCardinality;
    }
    
    public int getDetectedDuplicates() {
        return detectedDuplicates;
    }

    private void getDuplicates() {
        if (isCleanCleanER) {
            final Iterator<Comparison> iterator = similarityPairs.getPairIterator();
            while (iterator.hasNext()) {
                final Comparison comp = iterator.next();
                abstractDP.isSuperfluous(comp.getEntityId1(), comp.getEntityId2());
            }
        } else {
            final Iterator<Comparison> iterator = similarityPairs.getPairIterator();
            while (iterator.hasNext()) {
                final Comparison comp = iterator.next();
                abstractDP.isSuperfluous(comp.getEntityId1(), comp.getEntityId2());
                abstractDP.isSuperfluous(comp.getEntityId2(), comp.getEntityId1());
            }
        }

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = ((float) abstractDP.getNoOfDuplicates()) / aggregateCardinality;

        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }
    }

    private void getEntities() {
        final TIntSet entitiesD1 = new TIntHashSet((int) aggregateCardinality);
        if (isCleanCleanER) {
            final TIntSet entitiesD2 = new TIntHashSet((int) aggregateCardinality);
            final Iterator<Comparison> iterator = similarityPairs.getPairIterator();
            while (iterator.hasNext()) {
                final Comparison comparison = iterator.next();
                entitiesD1.add(comparison.getEntityId1());
                entitiesD2.add(comparison.getEntityId2());
            }
            noOfD1Entities = entitiesD1.size();
            noOfD2Entities = entitiesD2.size();
        } else {
            final Iterator<Comparison> iterator = similarityPairs.getPairIterator();
            while (iterator.hasNext()) {
                final Comparison comparison = iterator.next();
                entitiesD1.add(comparison.getEntityId1());
                entitiesD1.add(comparison.getEntityId2());
            }
            noOfD1Entities = entitiesD1.size();
        }
    }

    public float getFMeasure() {
        return fMeasure;
    }

    public float getPc() {
        return pc;
    }

    public float getPq() {
        return pq;
    }

    public void printDetailedResults(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {
        if (similarityPairs.getNoOfComparisons() == 0) {
            Log.warn("Empty set of similarity pairs was given as input!");
            return;
        }

        if (abstractDP == null) {
            Log.error("No groundtruth was given as input!");
            return;
        }

        abstractDP.resetDuplicates();
        final PairIterator iterator = similarityPairs.getPairIterator();
        while (iterator.hasNext()) {
            final Comparison currentComparison = iterator.next();
            final EntityProfile profile1 = profilesD1.get(currentComparison.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(currentComparison.getEntityId2()) : profilesD1.get(currentComparison.getEntityId2());

            final int originalDuplicates = abstractDP.getNoOfDuplicates();
            abstractDP.isSuperfluous(currentComparison.getEntityId1(), currentComparison.getEntityId2());
            final int newDuplicates = abstractDP.getNoOfDuplicates();

            System.out.print(profile1.getEntityUrl() + ",");
            System.out.print(profile2.getEntityUrl() + ",");
            if (originalDuplicates == newDuplicates) {
                System.out.print("FP,"); //false positive
            } else { // originalDuplicates < newDuplicates
                System.out.print("TP,"); // true positive
            }
            System.out.print("Profile 1:[" + profilesD1 + "]");
            System.out.println("Profile 2:[" + profilesD2 + "]");
        }

        abstractDP.getFalseNegatives().forEach((duplicatesPair) -> {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            System.out.print(profile1.getEntityUrl() + ",");
            System.out.print(profile2.getEntityUrl() + ",");
            System.out.print("FN,"); // false negative
            System.out.print("Profile 1:[" + profile1 + "]");
            System.out.println("Profile 2:[" + profile2 + "]");
        });

        detectedDuplicates = abstractDP.getNoOfDuplicates();
        pc = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        pq = ((float) abstractDP.getNoOfDuplicates()) / aggregateCardinality;
        if (0 < pc && 0 < pq) {
            fMeasure = 2 * pc * pq / (pc + pq);
        } else {
            fMeasure = 0;
        }

        System.out.println("Pairs Quality (Precision)\t:\t" + pq);
        System.out.println("Pairs Completentess (Recall)\t:\t" + pc);
        System.out.println("F-Measure\t:\t" + fMeasure);
    }

    public void printFalseNegatives(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (similarityPairs.getNoOfComparisons() == 0) {
            Log.warn("Empty set of similarity pairs was given as input!");
            return;
        }

        if (abstractDP == null) {
            Log.error("No groundtruth was given as input!");
            return;
        }

        final PrintWriter pw = new PrintWriter(new File(outputFile));
        StringBuilder sb = new StringBuilder();

        abstractDP.resetDuplicates();
        final PairIterator iterator = similarityPairs.getPairIterator();
        while (iterator.hasNext()) {
            final Comparison comp = iterator.next();
            abstractDP.isSuperfluous(comp.getEntityId1(), comp.getEntityId2());
        }

        abstractDP.getFalseNegatives().forEach((duplicatesPair) -> {
            final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
            final EntityProfile profile2 = isCleanCleanER ? profilesD2.get(duplicatesPair.getEntityId2()) : profilesD1.get(duplicatesPair.getEntityId2());

            sb.append(profile1.getEntityUrl()).append(",");
            sb.append(profile2.getEntityUrl()).append(",");
            sb.append("FN,"); // false negative
            sb.append("Profile 1:[").append(profile1).append("]");
            sb.append("Profile 2:[").append(profile2).append("]");
        });

        pw.write(sb.toString());
        pw.close();
    }

    public void printStatistics(long overheadTime, String methodConfiguration, String methodName) {
        if (similarityPairs.getNoOfComparisons() == 0) {
            return;
        }

        System.out.println("\n\n\n**************************************************");
        System.out.println("Performance of : " + methodName);
        System.out.println("Configuration : " + methodConfiguration);
        System.out.println("**************************************************");
        System.out.println("Aggregate cardinality\t:\t" + aggregateCardinality);
        System.out.println("Entities in D1\t:\t" + noOfD1Entities);
        if (isCleanCleanER) {
            System.out.println("Entities in D2\t:\t" + noOfD2Entities);
            System.out.println("Total entities\t:\t" + (noOfD1Entities + noOfD2Entities));
        } 
        System.out.println("Detected duplicates\t:\t" + detectedDuplicates);
        System.out.println("PC\t:\t" + pc);
        System.out.println("PQ\t:\t" + pq);
        System.out.println("F-Measure\t:\t" + fMeasure);
        System.out.println("Overhead time\t:\t" + overheadTime);
    }

    public void setStatistics() {
        if (similarityPairs.getNoOfComparisons() == 0) {
            Log.warn("Empty set of similarity pairs was given as input!");
            return;
        }
        
        if (abstractDP == null) {
            Log.error("No groundtruth was given as input!");
            return;
        }

        getEntities();
        getDuplicates();
    }
}
