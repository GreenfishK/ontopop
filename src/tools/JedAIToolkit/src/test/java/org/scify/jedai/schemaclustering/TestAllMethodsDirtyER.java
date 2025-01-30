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
package org.scify.jedai.schemaclustering;

import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.AttributeClusters;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

import java.util.List;

/**
 *
 * @author GAP2
 */
public class TestAllMethodsDirtyER {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        String mainDir = "D:\\Data\\JedAIdata\\datasets\\dirtyErDatasets\\";
        final String[] datasets = {"census", "cddb", "cora"};

        for (String dataset : datasets) {
            System.out.println("\n\n\n\n\nCurrent dataset\t:\t" + dataset);
            final EntitySerializationReader inReaderD1 = new EntitySerializationReader(mainDir + dataset + "Profiles");
            final List<EntityProfile> profiles = inReaderD1.getEntityProfiles();
            System.out.println("Profiles D1\t:\t" + profiles.size());

            final AttributeValueClustering avc = new AttributeValueClustering(RepresentationModel.CHARACTER_TRIGRAMS, SimilarityMetric.ENHANCED_JACCARD_SIMILARITY);
            final AttributeClusters[] clusters = avc.getClusters(profiles);
            for (int j = 0; j < clusters.length; j++) {
                System.out.println("\nDataset\t:\t" + (j + 1));
                for (int k = 0; k < clusters[j].getNoOfClusters(); k++) {
                    System.out.println(k + "\t" + clusters[j].getClusterEntropy(k));
                }
            }
        }
    }
}
