// Lock Free linked list based Queue implementation serving as the framework for the
// Lock Free Queue with Batching data structure project
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.*;
import java.io.IOException;
import java.util.*;

// Define the class used for the Node structure
// A node will contain a point of data and a reference to its next node
class Node<T>
{
    T val;
    AtomicReference<Node<T>> next;

    // Empty Constructor
    public Node(){
        this.val = null;
        this.next = null;
    }

    // Node constructor
    Node(T value)
    {
        this.val = value;
        this.next = new AtomicReference<Node<T>>();
    }

    public T getValue(){
        return val;
    }

    public void setValue(T value){
        this.val = value;
    }
}

public class LFQueue<T> {
    // The queue will contain atomic references for both the head and the tail of the queue, enabling atomic setting
    private AtomicReference<NodeCountOrAnn> head;
    private AtomicReference<NodeWithCount> tail;


     // Constructor for the Queue
    public LFQueue()
    {
        // Instantiate the head and tail with a dummy node
        NodeWithCount dummy = new NodeWithCount(null, 0);

        head = new AtomicReference<NodeCountOrAnn>(new NodeCountOrAnn(dummy));
        tail = new AtomicReference<NodeWithCount>(dummy);
    }

    // Attempt to enqueue an item to the shared queue
    public void enqueueToShared(T value)
    {
        // Create the new node to enqueue
        Node<T> newNode = new Node(value);

        while (true)
        {
            // Get the current queue tail
            NodeWithCount tailCount = tail.get();

            // Atempt to link the new node to the tail of the shared queue
            if (tailCount.node.next.compareAndSet(null, newNode))
            {
                // Attempt to update the shared queue's tail with the new NodeWithCount reflecting
                // the new tail and new size of the queue. If it fails it won't matter as another thread will help
                tail.compareAndSet(tailCount, new NodeWithCount(newNode, tailCount.count + 1));

                // If we succeeded in appending the new node to the tail of the queue, enqueue has completed
                break;
            }

            // If the enqueue didn't help, check if the head contains an announcement the thread needs to help complete
            NodeCountOrAnn curHead = head.get();

            if (curHead.isAnnouncement)
            {
                //executeAnnouncement(curHead.announcement);
            }

            // If appending a node to the queue fails and the head isn't an announcement, try and help update the tail
            else
            {
                tail.compareAndSet(tailCount, new NodeWithCount(tailCount.node.next, tailCount.count + 1));
            }
        }
    }

    public T dequeue()
    {
        return null;
    }

    // FutureEnqueue enqueues a FutureOp object representing an enqueue operation
    // and returns A pointer to the Future object encapsulated in the created FutureOp will be 
    // returned by the method, so that the caller could later pass it to the Evaluate method.
    public Future futureEnqueue(FutureOp value){
        return null;
    }

    // FutureDequeue operates similarly, but updates the numbers of pending dequeue operations 
    // and excess dequeues
    public Future futureDequeue(){
        return null;
    }

    // Evaluate receives a future and ensures it is applied when the method returns.
    public Future evaluate(Future value){
        return null;
    }


    public static void main (String[] args){

    }


}
