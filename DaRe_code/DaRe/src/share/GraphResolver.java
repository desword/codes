/*
 * this class is used in decode process to handle the data block in complex
 * graph.
 *
 */
package share;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author smile
 */
public class GraphResolver implements Iterable<byte[]> {

    private final long number_of_block; //  it is the number of the source blocks.
    // Integer: index of src block;  set<xx>, the set of encoded blocks that have relationship with src block. 
    // GrahpaNode has the set of encoded blocks, and the encoded data.
    private final Map<Integer, Set<GraphNode>> graph;
    private final Map<Integer, byte[]> resolved_block;// it is resolved block currently.

    public GraphResolver(long k) {

        number_of_block = k;

        graph = new HashMap<Integer, Set<GraphNode>>() {

            @Override
            public Set<GraphNode> get(Object key) {

                if (null == super.get(key)) {
                    super.put((Integer) key, new HashSet<GraphNode>());
                }
                return super.get(key);
            }

        };

        resolved_block = new HashMap<>();
    }
    
    
    public int getResolvedBlocks(){
    	return resolved_block.size();
    }

    /**
     *
     * @param blocks related blocks
     * @param data block data. It is the resulted encoded block that xored the selected source blocks.
     * @return true if the graph resolve done
     */
    public boolean addBlock(Set<Integer> blocks, byte[] data) {

        // [Start spreading msg]When received a encoded block that xor only 1 src block ,then start to decode and pass message to other encoded blocks.
        if (1 == blocks.size()) {

        	// push the src block id, and the encoded block -data- into resolve.
            Set<Tuple<Integer, byte[]>> to_eliminate
                    = resolve(blocks.iterator().next(), data);

            Tuple<Integer, byte[]> current_eliminate;
            // recursively eliminate until empty
            while (!to_eliminate.isEmpty()) {
                // get
                current_eliminate = to_eliminate.iterator().next();
                // operate
                to_eliminate.addAll(resolve(
                        current_eliminate.getFirst(),
                        current_eliminate.getSecond()));
                // remove
                to_eliminate.remove(current_eliminate);
            }

        } else {
        	// find all of the source block index that has been successfully decoded.
            List<Integer> to_remove = new ArrayList<>();
            for (int block : blocks) {
                if (resolved_block.containsKey(block)) {
                    to_remove.add(block);
                }
            }

            // remove relation and fresh data
            int size = data.length;
            for (int key : to_remove) {
                blocks.remove(key);
                data = xorOperation(resolved_block.get(key), data, size);
            }

            to_remove = null;
            
            // [Start spreading msg] after removing the already decoded src block from the encoded block, 
            // if there is only one block, then we can obtain a new src block. 
            // The decoding procedure is again proceed. 
            if (1 == blocks.size()) {

                return addBlock(blocks, data);
            } else {

                GraphNode node = new GraphNode(blocks, data);

                // add to each seperate block
                for (int block : blocks) {
                    graph.get(block).add(node);
                }
            }
        }

        return resolved_block.size() >= number_of_block;
    }

    /**
     *
     * @param block source block index
     * @param data block data. Encoded blocks
     * @return new single block data
     */
    private Set<Tuple<Integer, byte[]>> resolve(int block, byte[] data) {

        // save information firstly
        resolved_block.put(block, data.clone());
        Set<GraphNode> nodes = graph.get(block);// get the list of the encoded blocks that have relationship with the source block with id -block-
        graph.remove(block);

        // pass message to all associated nodes
        Iterator<GraphNode> node_iterator = nodes.iterator();
        Set<Tuple<Integer, byte[]>> eliminate_set = new HashSet<>();

        while (node_iterator.hasNext()) {

            GraphNode current_node = node_iterator.next();
            current_node.data
                    = xorOperation(current_node.data, data, data.length);
            current_node.blocks
                    .remove(block);

            if (1 == current_node.blocks.size()) {
                // create new tuple
                Tuple<Integer, byte[]> tuple = new Tuple<>(
                        current_node.blocks.iterator().next(),
                        current_node.data);
                // add to the set
                eliminate_set.add(tuple);
            }
        }

        return eliminate_set;
    }

    /**
     *
     * @return the data of source file use iterator, the order is very important
     */
    @Override
    public Iterator<byte[]> iterator() {

        List<Map.Entry<Integer, byte[]>> list
                = new LinkedList<>(resolved_block.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<Integer, byte[]>>() {
            
            @Override
            public int compare(
                    Map.Entry<Integer, byte[]> o1, 
                    Map.Entry<Integer, byte[]> o2) {
                return Integer.compare(o1.getKey(), o2.getKey());
            }
        });

        ArrayList<byte[]> result_array = new ArrayList<>();
        
        for (Map.Entry<Integer, byte[]> entry : list) {
            result_array.add(entry.getValue());
        }
        
        return result_array.iterator();
    }

    /**
     *
     * @param a byte[] operand, this parameter can be changed.
     * @param b byte[] operand, this parameter can be changed.
     * @param size the length of the byte [], this parameter can be changed.
     *
     * @return the byte[] of length size
     */
    private byte[] xorOperation(
            final byte[] a,
            final byte[] b,
            final int size) {

        byte[] return_value = new byte[size];

        for (int index = 0; index < size; index++) {
            return_value[index] = (byte) (a[index] ^ b[index]);
        }

        return return_value;
    }

    // graph node here 
    private class GraphNode {

        public Set<Integer> blocks;
        public byte[] data;

        public GraphNode(Set<Integer> blocks, byte[] data) {

            this.data = data.clone();
            this.blocks = blocks;
        }
    }

    // tuple class
    private class Tuple<E1, E2> {

        private final E1 e1;
        private final E2 e2;

        public Tuple(E1 e1, E2 e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        public E1 getFirst() {
            return e1;
        }

        public E2 getSecond() {
            return e2;
        }
    }

}
