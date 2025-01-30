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

import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.datamodel.EquivalenceCluster;

import com.esotericsoftware.minlog.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import org.scify.jedai.datamodel.EntityProfile;

/**
 *
 * @author gap2
 */
public class ClustersPerformance {

    private float fMeasure;
    private float precision;
    private float recall;
    private float totalMatches;

    private final AbstractDuplicatePropagation abstractDP;
    private final EquivalenceCluster[] entityClusters;

    public ClustersPerformance(EquivalenceCluster[] clusters, AbstractDuplicatePropagation adp) {
        abstractDP = adp;
        if (abstractDP != null) {
            abstractDP.resetDuplicates();
        }
        entityClusters = clusters;
    }

    public int getDetectedDuplicates() {
        return abstractDP == null ? 0 : abstractDP.getNoOfDuplicates();
    }

    public int getEntityClusters() {
        return entityClusters.length;
    }

    public int getExistingDuplicates() {
        return abstractDP == null ? 0 : abstractDP.getExistingDuplicates();
    }

    public float getFMeasure() {
        return fMeasure;
    }

    public float getPrecision() {
        return precision;
    }

    public float getRecall() {
        return recall;
    }

    public float getTotalMatches() {
        return totalMatches;
    }

    private boolean isCleanCleanER() {
        for (EquivalenceCluster cluster : entityClusters) {
            if (!cluster.getEntityIdsD2().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void printStatistics(long overheadTime, String methodName, String methodConfiguration) {
        System.out.println("\n\n\n**************************************************");
        System.out.println("Performance of : " + methodName);
        System.out.println("Configuration : " + methodConfiguration);
        System.out.println("**************************************************");
        System.out.println("No of clusters\t:\t" + entityClusters.length);
        if (abstractDP != null) {
            System.out.println("Detected duplicates\t:\t" + abstractDP.getNoOfDuplicates());
            System.out.println("Existing duplicates\t:\t" + abstractDP.getExistingDuplicates());
        }
        System.out.println("Total matches\t:\t" + totalMatches);
        System.out.println("Precision\t:\t" + precision);
        System.out.println("Recall\t:\t" + recall);
        System.out.println("F-Measure\t:\t" + fMeasure);
        System.out.println("Overhead time\t:\t" + overheadTime);
    }

    public void printDetailedResults(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, String outputFile) throws FileNotFoundException {
        if (entityClusters.length == 0) {
            Log.warn("Empty set of equivalence clusters given as input!");
            return;
        }

        if (abstractDP == null) {
            Log.error("No groundtruth was given as input!");
            return;
        }

        totalMatches = 0;
        final PrintWriter pw = new PrintWriter(new File(outputFile));
        final StringBuilder sb = new StringBuilder();

        abstractDP.resetDuplicates();
        if (abstractDP instanceof BilateralDuplicatePropagation) { // Clean-Clean ER
            for (EquivalenceCluster cluster : entityClusters) {
                if (cluster.getEntityIdsD1().size() != 1
                        || cluster.getEntityIdsD2().size() != 1) {
                    continue;
                }

                totalMatches++;

                final int entityId1 = cluster.getEntityIdsD1().get(0);
                final EntityProfile profile1 = profilesD1.get(entityId1);

                final int entityId2 = cluster.getEntityIdsD2().get(0);
                final EntityProfile profile2 = profilesD2.get(entityId2);

                sb.append(profile1.getEntityUrl()).append(",");
                sb.append(profile2.getEntityUrl()).append(",");

                if (abstractDP != null) {
                    final int originalDuplicates = abstractDP.getNoOfDuplicates();
                    abstractDP.isSuperfluous(entityId1, entityId2);
                    final int newDuplicates = abstractDP.getNoOfDuplicates();

                    if (originalDuplicates == newDuplicates) {
                        sb.append("FP,"); //false positive
                    } else { // originalDuplicates < newDuplicates
                        sb.append("TP,"); // true positive
                    }
                } else {
                    sb.append("-,");
                }

                sb.append("Profile 1:[").append(profile1).append("]");
                sb.append("Profile 2:[").append(profile2).append("]").append("\n");
            }

            if (abstractDP != null) {
                abstractDP.getFalseNegatives().forEach((duplicatesPair) -> {
                    final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
                    final EntityProfile profile2 = profilesD2.get(duplicatesPair.getEntityId2());

                    sb.append(profile1.getEntityUrl()).append(",");
                    sb.append(profile2.getEntityUrl()).append(",");
                    sb.append("FN,"); // false negative
                    sb.append("Profile 1:[").append(profile1).append("]");
                    sb.append("Profile 2:[").append(profile2).append("]").append("\n");
                });
            }
        } else { // Dirty ER
            for (EquivalenceCluster cluster : entityClusters) {
                final int[] duplicatesArray = cluster.getEntityIdsD1().toArray();

                for (int i = 0; i < duplicatesArray.length; i++) {
                    for (int j = i + 1; j < duplicatesArray.length; j++) {
                        totalMatches++;

                        final EntityProfile profile1 = profilesD1.get(duplicatesArray[i]);
                        final EntityProfile profile2 = profilesD1.get(duplicatesArray[j]);

                        sb.append(profile1.getEntityUrl()).append(",");
                        sb.append(profile2.getEntityUrl()).append(",");

                        if (abstractDP != null) {
                            final int originalDuplicates = abstractDP.getNoOfDuplicates();
                            abstractDP.isSuperfluous(duplicatesArray[i], duplicatesArray[j]);
                            final int newDuplicates = abstractDP.getNoOfDuplicates();

                            if (originalDuplicates == newDuplicates) {
                                sb.append("FP,"); //false positive
                            } else { // originalDuplicates < newDuplicates
                                sb.append("TP,"); // true positive
                            }
                        } else {
                            sb.append("-,");
                        }

                        sb.append("Profile 1:[").append(profile1).append("]");
                        sb.append("Profile 2:[").append(profile2).append("]").append("\n");
                    }
                }
            }

            if (abstractDP != null) {
                abstractDP.getFalseNegatives().forEach((duplicatesPair) -> {
                    final EntityProfile profile1 = profilesD1.get(duplicatesPair.getEntityId1());
                    final EntityProfile profile2 = profilesD1.get(duplicatesPair.getEntityId2());

                    sb.append(profile1.getEntityUrl()).append(",");
                    sb.append(profile2.getEntityUrl()).append(",");
                    sb.append("FN,"); // false negative
                    sb.append("Profile 1:[").append(profile1).append("]");
                    sb.append("Profile 2:[").append(profile2).append("]").append("\n");
                });
            }
        }

        if (0 < totalMatches) {
            precision = abstractDP.getNoOfDuplicates() / totalMatches;
        } else {
            precision = 0;
        }
        recall = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        if (0 < precision && 0 < recall) {
            fMeasure = 2 * precision * recall / (precision + recall);
        } else {
            fMeasure = 0;
        }

        pw.write("Precision\t:\t" + precision + "\n");
        pw.write("Recall\t:\t" + recall + "\n");
        pw.write("F-Measure\t:\t" + fMeasure + "\n");
        pw.write(sb.toString());
        pw.close();
    }

    public void setStatistics() {
        if (entityClusters.length == 0) {
            Log.warn("Empty set of equivalence clusters given as input!");
            return;
        }

        if (abstractDP == null) {
            setStatisticsWithoutGroundtruth();
            return;
        }

        totalMatches = 0;
        if (abstractDP instanceof BilateralDuplicatePropagation) { // Clean-Clean ER
            for (EquivalenceCluster cluster : entityClusters) {
                if (cluster.getEntityIdsD1().size() != 1
                        || cluster.getEntityIdsD2().size() != 1) {
                    continue;
                }

                final int entityId1 = cluster.getEntityIdsD1().get(0);
                final int entityId2 = cluster.getEntityIdsD2().get(0);

                totalMatches++;
                abstractDP.isSuperfluous(entityId1, entityId2);
            }
        } else { // Dirty ER
            for (EquivalenceCluster cluster : entityClusters) {
                final int[] duplicatesArray = cluster.getEntityIdsD1().toArray();

                for (int i = 0; i < duplicatesArray.length; i++) {
                    for (int j = i + 1; j < duplicatesArray.length; j++) {
                        totalMatches++;
                        abstractDP.isSuperfluous(duplicatesArray[i], duplicatesArray[j]);
                    }
                }
            }
        }

        if (0 < totalMatches) {
            precision = abstractDP.getNoOfDuplicates() / totalMatches;
        } else {
            precision = 0;
        }
        recall = ((float) abstractDP.getNoOfDuplicates()) / abstractDP.getExistingDuplicates();
        if (0 < precision && 0 < recall) {
            fMeasure = 2 * precision * recall / (precision + recall);
        } else {
            fMeasure = 0;
        }
    }

    private void setStatisticsWithoutGroundtruth() {
        totalMatches = 0;
        if (isCleanCleanER()) {
            for (EquivalenceCluster cluster : entityClusters) {
                if (cluster.getEntityIdsD1().size() != 1
                        || cluster.getEntityIdsD2().size() != 1) {
                    continue;
                }
                totalMatches++;
            }
        } else {
            for (EquivalenceCluster cluster : entityClusters) {
                totalMatches += cluster.getEntityIdsD1().size() * (cluster.getEntityIdsD1().size() - 1) / 2.0;
            }
        }
    }
}
