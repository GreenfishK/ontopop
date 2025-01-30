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

import java.util.List;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entityclustering.UniqueMappingClustering;
import org.scify.jedai.similarityjoins.tokenbased.*;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;

/**
 *
 * @author gap2
 */
public class BestConfigurationJoinBasedWorkflowCcer {

    public static void main(String[] args) {
        BasicConfigurator.configure();

//        int datasetId = Integer.parseInt(args[0]);
        float[] simThreshold = {0.90f, 0.4f, 0.45f, 0.8f, 0.9f, 0.8f, 0.45f};

        String mainDir = "/home/gap2/data/JedAIdata/datasets/cleanCleanErDatasets/";
        String[] datasetsD1 = {"restaurant1Profiles", "abtProfiles", "amazonProfiles", "dblpProfiles", "walmartProfiles", "dblpProfiles2", "imdbProfiles"};
        String[] datasetsD2 = {"restaurant2Profiles", "buyProfiles", "gpProfiles", "acmProfiles", "amazonProfiles2", "scholarProfiles", "dbpediaProfiles"};
        String[] groundtruthDirs = {"restaurantsIdDuplicates", "abtBuyIdDuplicates", "amazonGpIdDuplicates", "dblpAcmIdDuplicates", "amazonWalmartIdDuplicates",
            "dblpScholarIdDuplicates", "moviesIdDuplicates"};
        String[] attributeNamesD1 = {"http://www.okkam.org/ontology_restaurant1.owl#phone_number", "name", "title", "title", "modelno", "title", "title"};
        String[] attributeNamesD2 = {"http://www.okkam.org/ontology_restaurant2.owl#phone_number", "name", "title", "title", "modelno", "title", "title"};

        for (int datasetId = 0; datasetId < simThreshold.length; datasetId++) {
            System.out.println("\n\n\n\n\nCurrent dataset\t:\t" + groundtruthDirs[datasetId]);

            final IEntityReader eReader1 = new EntitySerializationReader(mainDir + datasetsD1[datasetId]);
            final List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
            System.out.println("\n\nInput Entity Profiles D1\t:\t" + profiles1.size());

            final IEntityReader eReader2 = new EntitySerializationReader(mainDir + datasetsD2[datasetId]);
            final List<EntityProfile> profiles2 = eReader2.getEntityProfiles();
            System.out.println("\n\nInput Entity Profiles D2\t:\t" + profiles2.size());

            IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthDirs[datasetId]);
            final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
            System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

            long time1 = System.currentTimeMillis();
//            AllPairs join = new AllPairs(simThreshold[datasetId]);
            final PPJoin join = new PPJoin(simThreshold[datasetId]);
            final SimilarityPairs simPairs = join.executeFiltering(attributeNamesD1[datasetId], attributeNamesD2[datasetId], profiles1, profiles2);
            System.out.println(simPairs.getNoOfComparisons());

            final IEntityClustering ec = new UniqueMappingClustering(simThreshold[datasetId]);
            final EquivalenceCluster[] clusters = ec.getDuplicates(simPairs);

            long time2 = System.currentTimeMillis();

            final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
            clp.setStatistics();
            clp.printStatistics(0, "", "");

            System.out.println("Running time\t:\t" + (time2 - time1));
        }
    }
}
