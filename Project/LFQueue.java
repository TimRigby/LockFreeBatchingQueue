// Lock Free linked list based Queue implementation serving as the framework for the
// Lock Free Queue with Batching data structure project
import java.util.concurrent.atomic.AtomicReference;

public class LFQueue<T> {
    // The queue will contain atomic references for both the head and the tail of the queue, enabling atomic setting
    private AtomicReference<Node<T>> head;
    private AtomicReference<Node<T>> tail;

    // Define the class used for the Node structure
    // A node will contain a point of data and a reference to its next node
    class Node<T>
    {
        T val;
        AtomicReference<Node<T>> next;

        // Node constructor
        Node(T value)
        {
            val = value;
            next = new AtomicReference<Node<T>>(null);
        }
    }

    // The future object will attach to deferred enqueue and dequeue operations to track status of execution
    // Contains a result that will hold a dequeued value if available and nothing for an enqueue operation
    // Contains a boolean flag to signal if the operation has been executed yet
    class Future<T> {
        T returnVal;
        boolean isDone;
    }

    // A BatchRequest object will hold the information necessary to apply a completed batch to the shared queue
    // Contains a reference to the first node in the sublist to be appended to the shared queue
    // Contains a reference to the last node in the sublist to be appeneded to the shared queue
    // Holds the number of enqueues and dequeues taking place within this batch as well as the
    // number of excess dequeues that will need to be applied outside of the batch on the shared queue
    class BatchRequest{
        Node<T> firstEnq;
        Node<T> lastEnq;
        int numEnqs;
        int numDeqs;
        int numExcessDeqs;
    }

    // The NodeWithCount object is used specifically for the head and the tail of the shared list
    // It contains the node reference as well as a counter that allows for quick calculation of the queue size
    class NodeWithCount {
        Node<T> node;
        int count;
    }

    // The Ann (announcement) object holds on to the state of the shared queue before applying a batch
    // It contains a reference to the head and the tail of the shared queue as well as a reference to the BatchRequest
    // object being applied
    class Ann {
        BatchRequest batchToApply;
        NodeWithCount oldHead;
        NodeWithCount oldTail;
    }

    // The head of our shared queue goes through multiple states. When a batch is not being applied it will be a NodeWithCount
    // object, but as a batch is applied it will change state to be an Announcment object in order to have other threads
    // help apply the batch to the shared queue and lock the state of the previous queue. It will have a boolean flag to
    // signify state
    class NodeCountOrAnn {
        boolean isAnnouncement;
        NodeWithCount nodeWCount;
        Ann announcement;
    }

    // A future Operation object will be used to track information in local thread batching and identify the type of
    // operation as well as a reference to its future operation object
    class FutureOp {
        boolean isEnqueue;
        Future<T> future;
    }

    // The local ThreadData object will hold the queue being built within a thread before being applied to the shared queue
    // As well as head and tail references of the subqueue and operation arithmetic information
    class ThreadData {
        LinkedList<Node<T>> opsQueue;
        Node<T> enqHead;
        Node<T> enqTail;
        int numEnqs;
        int numDeqs;
        int numExcessDeqs;
    }


     // Constructor for the Queue
    public LFQueue()
    {
        head = new AtomicReference<Node<T>>();
        tail = new AtomicReference<Node<T>>();
    }

    public void enq(T value)
    {
        // Create the node to be enqueued
        Node<T> newNode = new Node<T>(value);

        // Begin loop until successful enqueue
        while(true)
        {
            // Get the current tail and tail.next node
            Node<T> curTail = tail.get();
            Node<T> tailNext = curTail.next.get();

            // Preliminarily check if the enqueue has been interleaved resulting in a new tail
            if (curTail == tail.get())
            {
                // If the tail's next node is null we can try to enqueue
                if (tailNext == null)
                {
                    if (curTail.next.compareAndSet(tailNext, newNode))
                    {
                        // If the CAS succeeds we have enqueued the node
                        // Now try and update the tail pointer. If it fails it isn't an issue as another thread may
                        // help update the tail for this thread
                        tail.compareAndSet(curTail, newNode);
                        return;
                    }
                }

                // If the tail's next node is non-null, an enqueue has interleaved this, attempt to fix the tail and continue
                else
                {
                    tail.compareAndSet(curTail, tailNext);
                }
            }
        }
    }

    public T deq() throws Exception
    {
        // Loop until succeessful dequeue
        while(true)
        {
            // Get the head, the tail, and the head's next pointer
            Node<T> curHead = head.get();
            Node<T> headNext = curHead.next.get();
            Node<T> curTail = tail.get();

            // Preliminarily check if a dequeue has interleaved this and changed the head
            if (curHead == head.get()) {
                if (curHead == curTail) {
                    // If head equals tail and the head's next pointer is null, the queue is empty
                    if (headNext == null)
                        throw new Exception();

                    // Otherwise another queue has enqueued but hasn't finished reassigning, so help the other thread
                    tail.compareAndSet(curTail, headNext);
                } else {
                    // Retrieve the value in the head
                    T deqValue = curHead.val;

                    // Update the head pointer. If the CAS fails, this value has already been dequeued and we must retry
                    if (head.compareAndSet(curHead, headNext))
                        return deqValue;
                }
            }
        }
    }
}
