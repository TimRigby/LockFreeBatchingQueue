import java.util.LinkedList;

// The local ThreadData object will hold the queue being built within a thread before being applied to the shared queue
// As well as head and tail references of the subqueue and operation arithmetic information
public class ThreadData<T> {
    LinkedList<Node<T>> opsQueue;
    Node<T> enqHead;
    Node<T> enqTail;
    int numEnqs;
    int numDeqs;
    int numExcessDeqs;
}