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
public class ConfigureRandomlyEndToEndWorkflowCCER {

    private final static int NO_OF_TRIALS = 60;

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        
        String[] entitiesFilePath = {"data" + File.separator + "cleanCleanErDatasets" + File.separator + "abtProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "buyProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "amazonProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "gpProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "acmProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpProfiles2",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "scholarProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "imdbProfiles",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dbpediaProfiles"
        };
        String[] groundTruthFilePath = {"data" + File.separator + "cleanCleanErDatasets" + File.separator + "abtBuyIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "amazonGpIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpAcmIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "dblpScholarIdDuplicates",
            "data" + File.separator + "cleanCleanErDatasets" + File.separator + "moviesIdDuplicates"
        };

        for (int i = 0; i < groundTruthFilePath.length; i++) {
            System.out.println("\n\n\n\nCurrent dataset\t:\t" + groundTruthFilePath[i]);
            
            IEntityReader eReader1 = new EntitySerializationReader(entitiesFilePath[i*2]);
            List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles1.size());
            
            IEntityReader eReader2 = new EntitySerializationReader(entitiesFilePath[i*2+1]);
            List<EntityProfile> profiles2 = eReader2.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles2.size());

            IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFilePath[i]);
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

            int bestIteration = 0;
            float bestFMeasure = 0;
            for (int j = 0; j < NO_OF_TRIALS; j++) {
                long time1 = System.currentTimeMillis();

                bb.setNextRandomConfiguration();
                final List<AbstractBlock> blocks = bb.getBlocks(profiles1, profiles2);

                bp1.setNextRandomConfiguration();
                final List<AbstractBlock> purgedBlocks = bp1.refineBlocks(blocks);
                if (purgedBlocks.isEmpty()) {
                    continue;
                }

                bp2.setNextRandomConfiguration();
                final List<AbstractBlock> filteredBlocks = bp2.refineBlocks(purgedBlocks);
                if (filteredBlocks.isEmpty()) {
                    continue;
                }

                cc.setNextRandomConfiguration();
                final List<AbstractBlock> finalBlocks = cc.refineBlocks(filteredBlocks);
                if (finalBlocks.isEmpty()) {
                    continue;
                }
                
                em.setNextRandomConfiguration();
                final SimilarityPairs sims = em.executeComparisons(finalBlocks);

                ec.setNextRandomConfiguration();
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

                float fMeasure = clp.getFMeasure();
                if (bestFMeasure < fMeasure) {
                    bestIteration = j;
                    bestFMeasure = fMeasure;
                }
            }

            System.out.println("\nBest Iteration\t:\t" + bestIteration);
            System.out.println("Best FMeasure\t:\t" + bestFMeasure);

            long time1 = System.currentTimeMillis();

            bb.setNumberedRandomConfiguration(bestIteration);
            final List<AbstractBlock> blocks = bb.getBlocks(profiles1, profiles2);

            bp1.setNumberedRandomConfiguration(bestIteration);
            final List<AbstractBlock> purgedBlocks = bp1.refineBlocks(blocks);

            bp2.setNumberedRandomConfiguration(bestIteration);
            final List<AbstractBlock> filteredBlocks = bp2.refineBlocks(purgedBlocks);

            cc.setNumberedRandomConfiguration(bestIteration);
            final List<AbstractBlock> finalBlocks = cc.refineBlocks(filteredBlocks);

            em.setNumberedRandomConfiguration(bestIteration);
            final SimilarityPairs sims = em.executeComparisons(finalBlocks);

            ec.setNumberedRandomConfiguration(bestIteration);
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
