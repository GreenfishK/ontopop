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

package org.scify.jedai.demoworkflows;

import java.io.File;
import org.scify.jedai.datamodel.Attribute;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import java.util.List;
import org.apache.log4j.BasicConfigurator;

/**
 *
 * @author gap2
 */

public class ViewEntityProfiles {
    public static void main(String[] args) {
        BasicConfigurator.configure();
        
        String mainDirectory = "data" + File.separator + "cleanCleanErDatasets" + File.separator;

        IEntityReader serializedEntityReader = new EntitySerializationReader(mainDirectory + "dblpProfiles");
        List<EntityProfile> serializedDBLP = serializedEntityReader.getEntityProfiles();
        System.out.println("Serialized DBLP Entity Profiles\t:\t" + serializedDBLP.size());

        for (EntityProfile currentProfile : serializedDBLP) {
            System.out.println("\n\nCurrent profile\t:\t" + currentProfile.getEntityUrl());
            for (Attribute currentAttribute : currentProfile.getAttributes()) {
                System.out.println(currentAttribute.getName() + "\t" + currentAttribute.getValue());
            }
        }
    }
}
