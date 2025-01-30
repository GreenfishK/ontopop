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

package org.scify.jedai.datareader;

import org.scify.jedai.datamodel.Attribute;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datareader.entityreader.EntityDBReader;

import java.util.List;

/**
 *
 * @author G.A.P. II
 */

public class TestDBReader {
    public static void main(String[] args) {
    	String url = "postgresql://localhost/bench_parallel";
    	EntityDBReader DBReader = new EntityDBReader(url);
    	DBReader.setTable("pgbench_tellers");
    	DBReader.setUser("postgres");
    	DBReader.setPassword("");
    	DBReader.setSSL(true);
		List<EntityProfile> profiles = DBReader.getEntityProfiles();
		for (EntityProfile profile : profiles) {
            System.out.println("\n\n" + profile.getEntityUrl());
            for (Attribute attribute : profile.getAttributes()) {
                System.out.print(attribute.toString());
                System.out.println();
            }
        }
//		String url = "mysql://localhost/domesDB";
//    	EntityDBReader DBReader = new EntityDBReader(url);
//    	DBReader.setTable("new_table");
//    	DBReader.setUser("root");
//    	DBReader.setPassword("");
//		List<EntityProfile> profiles = DBReader.getEntityProfiles();
//		for (EntityProfile profile : profiles) {
//            System.out.println("\n\n" + profile.getEntityUrl());
//            for (Attribute attribute : profile.getAttributes()) {
//                System.out.print(attribute.toString());
//                System.out.println();
//            }
//        }
    }
}