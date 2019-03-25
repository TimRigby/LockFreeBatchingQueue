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
    Node<T> next;

    // Empty Constructor
    public Node(){
        this.val = null;
        this.next = null;
    }

    // Node constructor
    Node(T value)
    {
        this.val = value;
        this.next = null;
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
    private AtomicReference<Node<T>> head;
    private AtomicReference<Node<T>> tail;


     // Constructor for the Queue
    public LFQueue()
    {
        head = new AtomicReference<Node<T>>();
        tail = new AtomicReference<Node<T>>();
    }

    public void enqueue(T value)
    {
        
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
