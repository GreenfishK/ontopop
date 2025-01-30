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
package org.scify.jedai.configuration;

import java.io.File;
import java.util.List;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.blockbuilding.StandardBlocking;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.blockprocessing.blockcleaning.BlockFiltering;
import org.scify.jedai.blockprocessing.blockcleaning.SizeBasedBlockPurging;
import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityNodePruning;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entityclustering.UniqueMappingClustering;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.entitymatching.ProfileMatcher;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;

/**
 *
 * @author GAP2
 */
public class HolisticGridConfigurationCCER {

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();

        String[] entitiesFilePath = {"data" + File.separator + "cleanCleanErDatasets" + File.separator + "abtProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "buyProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "amazonProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "gpProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "acmProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpProfiles2",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "scholarProfiles",};
        String[] groundTruthFilePath = {"data" + File.separator + "cleanCleanErDatasets" + File.separator + "abtBuyIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "amazonGpIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpAcmIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpScholarIdDuplicates"
        };

        for (int i = 0; i < groundTruthFilePath.length; i++) {
            System.out.println("\n\n\n\nCurrent dataset\t:\t" + groundTruthFilePath[i]);

            final IEntityReader eReader1 = new EntitySerializationReader(entitiesFilePath[i * 2]);
            final List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles1.size());

            final IEntityReader eReader2 = new EntitySerializationReader(entitiesFilePath[i * 2 + 1]);
            final List<EntityProfile> profiles2 = eReader2.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles2.size());

            final IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFilePath[i]);
            final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
            System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

            final IBlockBuilding bb = new StandardBlocking();
            final IBlockProcessing bp1 = new SizeBasedBlockPurging();
            final IBlockProcessing bp2 = new BlockFiltering();
            final IBlockProcessing cc = new CardinalityNodePruning();
            final IEntityMatching em = new ProfileMatcher(profiles1, profiles2);
            final IEntityClustering ec = new UniqueMappingClustering();

            final StringBuilder matchingWorkflowName = new StringBuilder();
            matchingWorkflowName.append(bb.getMethodName());
            matchingWorkflowName.append("->").append(bp1.getMethodName());
            matchingWorkflowName.append("->").append(bp2.getMethodName());
            matchingWorkflowName.append("->").append(cc.getMethodName());
            matchingWorkflowName.append("->").append(em.getMethodName());
            matchingWorkflowName.append("->").append(ec.getMethodName());

            int bestLoop0 = 0;
            int bestLoop1 = 0;
            int bestLoop2 = 0;
            int bestLoop3 = 0;
            int bestLoop4 = 0;
            int bestLoop5 = 0;
            float bestFMeasure = 0;
            for (int loop0 = 0; loop0 < bb.getNumberOfGridConfigurations(); loop0++) {
                bb.setNumberedGridConfiguration(loop0);
                final List<AbstractBlock> originalBlocks = bb.getBlocks(profiles1, profiles2);
                if (originalBlocks.isEmpty()) {
                    continue;
                }

                for (int loop1 = 0; loop1 < bp1.getNumberOfGridConfigurations(); loop1++) {
                    bp1.setNumberedGridConfiguration(loop1);
                    final List<AbstractBlock> purgedBlocks = bp1.refineBlocks(originalBlocks);
                    if (purgedBlocks.isEmpty()) {
                        continue;
                    }

                    for (int loop2 = 0; loop2 < bp2.getNumberOfGridConfigurations(); loop2++) {
                        bp2.setNumberedGridConfiguration(loop2);
                        final List<AbstractBlock> filteredBlocks = bp2.refineBlocks(purgedBlocks);
                        if (filteredBlocks.isEmpty()) {
                            continue;
                        }

                        for (int loop3 = 0; loop3 < cc.getNumberOfGridConfigurations(); loop3++) {
                            cc.setNumberedGridConfiguration(loop3);
                            final List<AbstractBlock> finalBlocks = cc.refineBlocks(filteredBlocks);
                            if (finalBlocks.isEmpty()) {
                                continue;
                            }

                            for (int loop4 = 0; loop4 < em.getNumberOfGridConfigurations(); loop4++) {
                                em.setNumberedGridConfiguration(loop4);
                                final SimilarityPairs sims = em.executeComparisons(finalBlocks);

                                for (int loop5 = 0; loop5 < ec.getNumberOfGridConfigurations(); loop5++) {
                                    ec.setNumberedGridConfiguration(loop5);
                                    final EquivalenceCluster[] clusters = ec.getDuplicates(sims);

                                    final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
                                    clp.setStatistics();
//                                    clp.printStatistics(0, "", "");
                                    float fMeasure = clp.getFMeasure();
                                    if (bestFMeasure < fMeasure) {
                                        bestLoop0 = loop0;
                                        bestLoop1 = loop1;
                                        bestLoop2 = loop2;
                                        bestLoop3 = loop3;
                                        bestLoop4 = loop4;
                                        bestLoop5 = loop5;
                                        bestFMeasure = fMeasure;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            System.out.println("\nBest Loop 0\t:\t" + bestLoop0);
            System.out.println("\nBest Loop 1\t:\t" + bestLoop1);
            System.out.println("\nBest Loop 2\t:\t" + bestLoop2);
            System.out.println("\nBest Loop 3\t:\t" + bestLoop3);
            System.out.println("\nBest Loop 4\t:\t" + bestLoop4);
            System.out.println("\nBest Loop 5\t:\t" + bestLoop5);
            System.out.println("Best FMeasure\t:\t" + bestFMeasure);

            long time1 = System.currentTimeMillis();

            bb.setNumberedRandomConfiguration(bestLoop0);
            final List<AbstractBlock> blocks = bb.getBlocks(profiles1, profiles2);

            bp1.setNumberedRandomConfiguration(bestLoop1);
            final List<AbstractBlock> purgedBlocks = bp1.refineBlocks(blocks);

            bp2.setNumberedRandomConfiguration(bestLoop2);
            final List<AbstractBlock> filteredBlocks = bp2.refineBlocks(purgedBlocks);

            cc.setNumberedRandomConfiguration(bestLoop3);
            final List<AbstractBlock> finalBlocks = cc.refineBlocks(filteredBlocks);

            em.setNumberedGridConfiguration(bestLoop4);
            final SimilarityPairs sims = em.executeComparisons(finalBlocks);

            ec.setNumberedGridConfiguration(bestLoop5);
            final EquivalenceCluster[] clusters = ec.getDuplicates(sims);

            long time2 = System.currentTimeMillis();

            final StringBuilder matchingWorkflowConf = new StringBuilder();
            matchingWorkflowConf.append(bb.getMethodConfiguration());
            matchingWorkflowConf.append("\n").append(bp1.getMethodConfiguration());
            matchingWorkflowConf.append("\n").append(bp2.getMethodConfiguration());
            matchingWorkflowConf.append("\n").append(cc.getMethodConfiguration());
            matchingWorkflowConf.append("\n").append(em.getMethodConfiguration());
            matchingWorkflowConf.append("\n").append(ec.getMethodConfiguration());

            final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
            clp.setStatistics();
            clp.printStatistics(time2 - time1, matchingWorkflowName.toString(), matchingWorkflowConf.toString());
        }
    }
}
