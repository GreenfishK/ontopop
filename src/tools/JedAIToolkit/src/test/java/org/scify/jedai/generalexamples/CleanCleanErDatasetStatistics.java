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
package org.scify.jedai.generalexamples;

import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.Attribute;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author G.A.P. II
 */
public class CleanCleanErDatasetStatistics {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        String mainFolder = "/home/gap2/data/JedAIdata/datasets/largeCcerDatasets/";
        String[] entitiesFiles = {mainFolder + "newDBPedia1",
            mainFolder + "newDBPedia2"
        };
        String[] groundTruthFiles = {mainFolder + "newDBPediaMatches"
        };

        for (String entitiesFile : entitiesFiles) {
            System.out.println("\n\n\n\n\nCurrent dataset\t:\t" + entitiesFile);

            IEntityReader eReader = new EntitySerializationReader(entitiesFile);
            List<EntityProfile> profiles = eReader.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles.size());
            final Set<String> distinctAttributes = new HashSet<>();
            double nameValuePairs = 0;
            for (EntityProfile profile : profiles) {
                nameValuePairs += profile.getProfileSize();
                for (Attribute a : profile.getAttributes()) {
                    distinctAttributes.add(a.getName());
                }
            }
            System.out.println("Distinct attributes\t:\t" + distinctAttributes.size());
            System.out.println("Total Name-Value Pairs\t:\t" + nameValuePairs);
            System.out.println("Average Name-Value Pairs\t:\t" + nameValuePairs / profiles.size());
        }

        IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFiles[0]);
        final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());
    }

}
