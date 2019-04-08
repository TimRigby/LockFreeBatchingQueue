import java.util.LinkedList; 
import java.util.Queue;

// The local ThreadData object will hold the queue being built within a thread before being applied to the shared queue
// As well as head and tail references of the subqueue and operation arithmetic information
public class ThreadData<T> {
    Queue<FutureOp<T>> opsQueue;
    Node<T> enqHead;
    Node<T> enqTail;
    int numEnqs;
    int numDeqs;
    int numExcessDeqs;

    ThreadData(){
    	opsQueue = new LinkedList<>();
    }
}