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
package org.scify.jedai.version3;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.blockbuilding.StandardBlocking;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.blockprocessing.blockcleaning.BlockFiltering;
import org.scify.jedai.blockprocessing.blockcleaning.ComparisonsBasedBlockPurging;
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
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class DBPediaBlockingBasedWorkflow {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        String mainDir = "/home/gpapadakis/data/ccerDatasets/allDatasets/";
        String datasetsD1 = "cleanDBPedia1";
        String datasetsD2 = "cleanDBPedia2";
        String groundtruthDirs = "newDBPediaMatches";

        final IEntityClustering ec = new UniqueMappingClustering();

        IEntityReader eReader1 = new EntitySerializationReader(mainDir + datasetsD1);
        List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles1.size());

        IEntityReader eReader2 = new EntitySerializationReader(mainDir + datasetsD2);
        List<EntityProfile> profiles2 = eReader2.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles2.size());

        IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthDirs);
        final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        long time1 = System.currentTimeMillis();
        
        IBlockBuilding blockBuildingMethod = new StandardBlocking();
        List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles1, profiles2);
        System.out.println("Original blocks\t:\t" + blocks.size());

        IBlockProcessing blockCleaningMethod1 = new ComparisonsBasedBlockPurging(1.000f);
        blocks = blockCleaningMethod1.refineBlocks(blocks);

        IBlockProcessing blockCleaningMethod2 = new BlockFiltering();
        blocks = blockCleaningMethod2.refineBlocks(blocks);

        IBlockProcessing comparisonCleaningMethod = new CardinalityNodePruning(WeightingScheme.JS);
        blocks = comparisonCleaningMethod.refineBlocks(blocks);

        long time2 = System.currentTimeMillis();
        
        BlocksPerformance blStats = new BlocksPerformance(blocks, duplicatePropagation);
        blStats.setStatistics();
        blStats.printStatistics(time2-time1, blockBuildingMethod.getMethodConfiguration(), blockBuildingMethod.getMethodName());

        List<RepresentationModel> vectorModels = new ArrayList<>();
        vectorModels.add(RepresentationModel.CHARACTER_BIGRAMS);
        vectorModels.add(RepresentationModel.CHARACTER_BIGRAMS_TF_IDF);
        vectorModels.add(RepresentationModel.CHARACTER_TRIGRAMS);
        vectorModels.add(RepresentationModel.CHARACTER_TRIGRAMS_TF_IDF);
        vectorModels.add(RepresentationModel.CHARACTER_FOURGRAMS);
        vectorModels.add(RepresentationModel.CHARACTER_FOURGRAMS_TF_IDF);
        vectorModels.add(RepresentationModel.TOKEN_UNIGRAMS);
        vectorModels.add(RepresentationModel.TOKEN_UNIGRAMS_TF_IDF);
        vectorModels.add(RepresentationModel.TOKEN_BIGRAMS);
        vectorModels.add(RepresentationModel.TOKEN_BIGRAMS_TF_IDF);
        vectorModels.add(RepresentationModel.TOKEN_TRIGRAMS);
        vectorModels.add(RepresentationModel.TOKEN_TRIGRAMS_TF_IDF);

        for (RepresentationModel rModel : vectorModels) {
            final List<SimilarityMetric> metrics = SimilarityMetric.getModelCompatibleSimMetrics(rModel);
            for (SimilarityMetric simMetric : metrics) {
                long time3 = System.currentTimeMillis();
                
                final IEntityMatching em = new ProfileMatcher(profiles1, profiles2, rModel, simMetric);
                final SimilarityPairs sims = em.executeComparisons(blocks);

                long time4 = System.currentTimeMillis();
                
                for (int k = 0; k < ec.getNumberOfGridConfigurations(); k++) {
                    long time5 = System.currentTimeMillis();
                    
                    ec.setNumberedGridConfiguration(k);
                    final EquivalenceCluster[] clusters = ec.getDuplicates(sims);

                    long time6 = System.currentTimeMillis();
                    
                    final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
                    clp.setStatistics();
                    long totalTime = time6 - time5 + time4 - time3;
                    System.out.println("EM\t:\t" + rModel + "\t" + simMetric + "\tEC\t:\t" + k + "F-M" + clp.getFMeasure() + "\tTime\t:\t" + totalTime);
                }
            }
        }
    }
}
