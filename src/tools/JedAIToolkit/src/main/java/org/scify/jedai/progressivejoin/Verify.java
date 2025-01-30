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
package org.scify.jedai.progressivejoin;

import java.util.ArrayList;
import java.util.Objects;

/**
 *
 * @author mthanos
 */
public class Verify {

    public static int[] verifiy_sim(ArrayList<Integer> record, ArrayList<Integer> indrecord, int minoverlap, int foundoverlap,
            int recpos, int indrecpos) {
        int reclen = record.size();
        int indreclen = indrecord.size();
        int maxrec = reclen - recpos + foundoverlap;
        int maxindrec = indreclen - indrecpos + foundoverlap;

        int nextposrec = -1;
        int nextposindrec = -1;

        while (maxrec >= minoverlap && maxindrec >= minoverlap && foundoverlap < minoverlap) {
            if (Objects.equals(record.get(recpos), indrecord.get(indrecpos))) {
                if (nextposrec == -1) {
                    nextposrec = recpos;
                    nextposindrec = indrecpos;
                }
                recpos++;
                indrecpos++;
                foundoverlap++;
            } else if (record.get(recpos) < indrecord.get(indrecpos)) {
                recpos++;
                maxrec--;
            } else {
                indrecpos++;
                maxindrec--;
            }
        }

        if (foundoverlap < minoverlap) {
            return new int[]{0, -1, -1};
        }

        while (recpos < reclen && indrecpos < indreclen) {
            if (Objects.equals(record.get(recpos), indrecord.get(indrecpos))) {
                if (nextposrec == -1) {
                    nextposrec = recpos;
                    nextposindrec = indrecpos;
                }
                recpos++;
                indrecpos++;
                foundoverlap++;
            } else if (record.get(recpos) < indrecord.get(indrecpos)) {
                recpos++;
            } else {
                indrecpos++;
            }
        }

        return new int[]{foundoverlap, nextposrec, nextposindrec};
    }
}
