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

        head = new AtomicReference<NodeCountOrAnn>(new NodeCountOrAnn(null, dummy, false));
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
                /*
                ~~~~~~~~~~~~~~~~ Implement executeAnn()
                 */
                //executeAnn(curHead.announcement);
            }

            // If appending a node to the queue fails and the head isn't an announcement, try and help update the tail
            else
            {
                tail.compareAndSet(tailCount, new NodeWithCount(tailCount.node.next, tailCount.count + 1));
            }
        }
    }

    public T dequeueFromShared()
    {
        while (true)
        {
            // Get the head reference via helper function
            NodeCountOrAnn curHead = helpAnnAndGetHead();

            Node<T> headNextNode = (Node<T>) curHead.nodeWCount.node.next.get();

            // If the head's next node is null there is nothing to dequeue
            if (headNextNode == null)
                return null;

            // Create the new NodeCountOrAnn object to replace the head
            //?

            // Otherwise try and update the shared queue's head reference. Need to update the NodeWithCount component of
            // the head with a new NodeWithCount and the updated size
            if (head.compareAndSet(curHead, new NodeCountOrAnn(null, new NodeWithCount(headNextNode, curHead.nodeWCount.count - 1), false)))
            return headNextNode.val;
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


    // HelpAnnAndGetHead. This auxiliary method assists announcements in execution,
    // as long as there is an announcement installed in SQHead.

    private NodeCountOrAnn helpAnnAndGetHead(){
        NodeCountOrAnn head;

        while (true){

            // Getting the current head
            head = this.head.get();

            // Need to see how this translates to java since we don;t have the union struct.
            if(!head.isAnnouncement){
                return head;
            }

                   /*
                ~~~~~~~~~~~~~~~~ Implement executeAnn()
                 */
            //ExecuteAnn(head.announcement);
        }
    }

    // Execute Batch
    // (1) Setting ann->oldHead to the head of the queue right before committing the batch.
    // (2) Installing ann in SQHead.
    // (3) Linking the batch’s items to SQTail.node->next.
    // (4) Setting oldTail  eld in the installed announcement ann.
    // (5) Promoting SQTail to point to the last node enqueued by the batch operation (and increasing its enqueue count by the number of enqueues).
    // (6) Setting SQHead to point to the last node dequeued by the batch operation in place of ann (and increasing its dequeue count by the number of successful dequeues).

    private Node<T> executeBatch(BatchRequest batch){
        NodeCountOrAnn ptr;
        // Creating a new announcement object and passing the batch request
        Announcement ann = new Announcement(batch);
        NodeCountOrAnn newHead;

        while (true){
            // Checking if there is a coliding ongoing batch whose announcement
            // is installed in the SQHead.
            ptr = helpAnnAndGetHead();

            // Set the PtrCnt object returned to oldHead;
            /*
            ~~~~~~~~~~~~~ Implement Announcement.setOldHead function
             */
            //ann.setOldHead(ptr.nodeWCount);

            // Step 2: Installing ann in SQHead
            newHead = new NodeCountOrAnn(ann, null, true);
            if(head.compareAndSet(ptr, newHead)){
                break;
            }

        }
        // Calling ExecuteAnn to carry out the batch
        executeAnnouncement(newHead);

        return ptr.nodeWCount.node;
    }

    private void executeAnnouncement(NodeCountOrAnn annHead)
    {
        NodeWithCount tailAndCnt, annOldTail;

        // Link items to tail and update the announcement
        while(true)
        {
            tailAndCnt = tail.get();
            annOldTail = annHead.announcement.oldTail;

            // The old tail of the announcement is initially null and is only set to a value when a thread has
            // successfully linked the new batch list to the end of the shared queue before the batch was applied
            // So if a value is here, the batch has been applied
            if (annOldTail.node != null)
            {
                break;
            }

            // Attempt to attach the head of the batch's enqueues to the tail of the current shared list
            tailAndCnt.node.next.compareAndSet(null, annHead.announcement.batchToApply.firstEnq);

            // If the CAS was successful, store the previous tail in the announcement
            if (tailAndCnt.node.next.get() == annHead.announcement.batchToApply.firstEnq)
            {
                annOldTail = tailAndCnt;
                annHead.announcement.oldTail = tailAndCnt;
                break;
            }
            // Otherwise help to update the tail to reflect the actual contents of the queue
            else
            {
                tail.compareAndSet(tailAndCnt, new NodeWithCount(tailAndCnt.node.next, tailAndCnt.count + 1));
            }
        }

        // Create the new tail which will hold the last node enqueued by the batch and increase the size by the number of enqueues
        NodeWithCount newTailAndCnt = new NodeWithCount(annHead.announcement.batchToApply.lastEnq, annOldTail.count + annHead.announcement.batchToApply.numEnqs);
        tail.compareAndSet(annOldTail, newTailAndCnt);
        //UpdateHead(annHead);
    }

    private Node<T> getNthNode(Node<T> node, int n)
    {
        for (int i = 0; i < n; i++)
            node = node.next.get();

        return node;
    }

    public static void main (String[] args){

    }


}
