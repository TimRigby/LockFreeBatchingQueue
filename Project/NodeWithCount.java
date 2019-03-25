// The NodeWithCount object is used specifically for the head and the tail of the shared list
// It contains the node reference as well as a counter that allows for quick calculation of the queue size
public class NodeWithCount<T> {
    Node<T> node;
    int count;
}