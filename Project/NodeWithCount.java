// The NodeWithCount object is used specifically for the head and the tail of the shared list
// It contains the node reference as well as a counter that allows for quick calculation of the queue size
public class NodeWithCount<T> {
    Node<T> node;
    int count;

    // Instantiate a NodeWithCount with a node reference but leave the counter blank
    // it will be updated with the previous head or tail's size
    NodeWithCount(T value)
    {
        node = new Node(value);
    }

    // Alternate constructor for instantiation of the first head/tail
    NodeWithCount(T value, int count)
    {
        node = new Node(value);
        this.count = count;
    }

    // Constructor to provide an already made node as the reference
    NodeWithCount(Node<T> node, int count)
    {
        this.node = node;
        this.count = count;
    }
}