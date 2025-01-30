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
package org.scify.jedai.utilities.graph;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

/**
 * The {@code CC} class represents a data type for determining the connected
 * components in an undirected graph. The <em>id</em> operation determines in
 * which connected component a given vertex lies; the <em>connected</em>
 * operation determines whether two vertices are in the same connected
 * component; the <em>count</em> operation determines the number of connected
 * components; and the <em>size</em> operation determines the number of vertices
 * in the connect component containing a given vertex.
 *
 * The <em>component identifier</em> of a connected component is one of the
 * vertices in the connected component: two vertices have the same component
 * identifier if and only if they are in the same connected component.
 *
 * <p>
 * This implementation uses depth-first search. The constructor takes time
 * proportional to <em>V</em> + <em>E</em>
 * (in the worst case), where <em>V</em> is the number of vertices and
 * <em>E</em> is the number of edges. Afterwards, the <em>id</em>,
 * <em>count</em>, <em>connected</em>, and <em>size</em> operations take
 * constant time.
 * <p>
 * For additional documentation, see
 * <a href="https://algs4.cs.princeton.edu/41graph">Section 4.1</a>
 * of <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 */
public class ConnectedComponents {

    private final boolean[] marked;   // marked[v] = has vertex v been marked?
    private final int[] id;           // id[v] = id of connected component containing v
    private final int[] size;         // size[id] = number of vertices in given component
    private int count;          // number of connected components

    /**
     * Computes the connected components of the undirected graph {@code G}.
     *
     * @param G the undirected graph
     */
    public ConnectedComponents(UndirectedGraph G) {
        marked = new boolean[G.V()];
        id = new int[G.V()];
        size = new int[G.V()];

        // to be able to iterate over each adjacency list, keeping track of which
        // vertex in each adjacency list needs to be explored next
        final TIntIterator[] adj = new TIntIterator[G.V()];
        for (int v = 0; v < G.V(); v++) {
            adj[v] = G.adj(v).iterator();
        }

        for (int v = 0; v < G.V(); v++) {
            if (!marked[v]) {
                nonRecursiveDFS(adj, v);
                count++;
            }
        }
    }

    // depth-first search for a Graph
    private void nonRecursiveDFS(TIntIterator[] adj, int s) {
        validateVertex(s);

        // depth-first search using an explicit stack
        final TIntStack stack = new TIntArrayStack();
        marked[s] = true;
        id[s] = count;
        size[count]++;
        stack.push(s);
        while (0 < stack.size()) {
            int v = stack.peek();
            if (adj[v].hasNext()) {
                int w = adj[v].next();
                if (!marked[w]) {
                    // discovered vertex w for the first time
                    marked[w] = true;
                    id[w] = count;
                    size[count]++;
                    stack.push(w);
                }
            } else {
                stack.pop();
            }
        }
    }

    /**
     * Returns the component id of the connected component containing vertex
     * {@code v}.
     *
     * @param v the vertex
     * @return the component id of the connected component containing vertex
     * {@code v}
     * @throws IllegalArgumentException unless {@code 0 <= v < V}
     */
    public int id(int v) {
        validateVertex(v);
        return id[v];
    }

    /**
     * Returns the number of vertices in the connected component containing
     * vertex {@code v}.
     *
     * @param v the vertex
     * @return the number of vertices in the connected component containing
     * vertex {@code v}
     * @throws IllegalArgumentException unless {@code 0 <= v < V}
     */
    public int size(int v) {
        validateVertex(v);
        return size[id[v]];
    }

    /**
     * Returns the number of connected components in the graph {@code G}.
     *
     * @return the number of connected components in the graph {@code G}
     */
    public int count() {
        return count;
    }

    /**
     * Returns true if vertices {@code v} and {@code w} are in the same
     * connected component.
     *
     * @param v one vertex
     * @param w the other vertex
     * @return {@code true} if vertices {@code v} and {@code w} are in the same
     * connected component; {@code false} otherwise
     * @throws IllegalArgumentException unless {@code 0 <= v < V}
     * @throws IllegalArgumentException unless {@code 0 <= w < V}
     */
    public boolean connected(int v, int w) {
        validateVertex(v);
        validateVertex(w);
        return id(v) == id(w);
    }

    // throw an IllegalArgumentException unless {@code 0 <= v < V}
    private void validateVertex(int v) {
        int V = marked.length;
        if (v < 0 || v >= V) {
            throw new IllegalArgumentException("vertex " + v + " is not between 0 and " + (V - 1));
        }
    }

//    public static void main (String[] args) {
//        UndirectedGraph ug = new UndirectedGraph(13);
//        ug.addEdge(0, 5);
//        ug.addEdge(4, 3);
//        ug.addEdge(0, 1);
//        ug.addEdge(9, 12);
//        ug.addEdge(6, 4);
//        ug.addEdge(5, 4);
//        ug.addEdge(0, 2);
//        ug.addEdge(11, 12);
//        ug.addEdge(9, 10);
//        ug.addEdge(0, 6);
//        ug.addEdge(7, 8);
//        ug.addEdge(9, 11);
//        ug.addEdge(5, 3);
//        
//        ConnectedComponents cc = new ConnectedComponents(ug);
//        System.out.println("Total ccs\t:\t" + cc.count());
//        for (int i = 0; i < 13; i++) {
//            System.out.println(cc.id(i));
//        }
//        
//    }
}
