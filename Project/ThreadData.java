import java.util.LinkedList; 
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;


// The local ThreadData object will hold the queue being built within a thread before being applied to the shared queue
// As well as head and tail references of the subqueue and operation arithmetic information
public class ThreadData<T> {
    Queue<FutureOp> opsQueue;
    AtomicReference<Node<T>> enqHead;
    AtomicReference<Node<T>> enqTail;
    int numEnqs;
    int numDeqs;
    int numExcessDeqs;
    int unusedEnqs;

    ThreadData(){
    	opsQueue = new LinkedList<FutureOp>();
    	enqHead = new AtomicReference<Node<T>>();
    	enqTail = new AtomicReference<Node<T>>();
    }
}