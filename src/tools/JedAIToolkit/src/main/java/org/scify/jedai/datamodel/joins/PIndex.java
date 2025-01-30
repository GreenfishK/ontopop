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

package org.scify.jedai.datamodel.joins;

/**
 *
 * @author G_A.Papadakis
 */

public class PIndex {

    private final int stPos;    // start position of substring
    private final int Lo;       // start position of segment
    private final int partLen;  // substring/segment length
    private final int len;      // length of indexed string

    public PIndex(int stPos, int lo, int partLen, int len) {
        this.stPos = stPos;
        Lo = lo;
        this.partLen = partLen;
        this.len = len;
    }

    public int getStPos() {
        return stPos;
    }

    public int getLo() {
        return Lo;
    }

    public int getPartLen() {
        return partLen;
    }

    public int getLen() {
        return len;
    }
}
