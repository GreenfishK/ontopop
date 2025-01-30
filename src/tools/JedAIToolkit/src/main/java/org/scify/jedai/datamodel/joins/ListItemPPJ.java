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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author G_A.Papadakis
 */

public class ListItemPPJ {

    private int pos;
    private List<IntPair> ids;

    public ListItemPPJ() {
        this.pos = 0;
        this.ids = new ArrayList<>();
    }

    public int getPos() {
        return this.pos;
    }

    public List<IntPair> getIds() {
        return this.ids;
    }

    public void setPos(int newPos) {
        this.pos = newPos;
    }

    public void setIds(List<IntPair> ids1) {
        this.ids = ids1;
    }
}
