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

class TestThreads implements Runnable{
    private final int id;
    public LFQueue queue;
    public ThreadData thread;

    TestThreads(LFQueue<Integer> q, int threadId){
        this.queue = q;
        this.id = threadId;
        this.thread = new ThreadData();
    }

    public void enqueue(int val){
        Future future;

        if(thread.opsQueue.size() == 0){
            queue.enqueueToShared(val);
        }else{
            future = futureEnqueue(new FutureOp(true, new Future(val)));
            evaluate(future);
        }

    }

    public Integer dequeue(){
        Future future;

        //System.out.println("In dequeue");
        //System.out.println("Thread queue " + thread.opsQueue.size());

        if(thread.opsQueue.size() == 0){
            return (Integer)queue.dequeueFromShared();
        }else{
            future = futureDequeue();
            evaluate(future);
        }

        return (Integer) future.returnVal;
    }

    // FutureEnqueue enqueues a FutureOp object representing an enqueue operation
    // and returns A pointer to the Future object encapsulated in the created FutureOp will be
    // returned by the method, so that the caller could later pass it to the Evaluate method.
    public Future futureEnqueue(FutureOp futureOp){
        // Get thread head/tail references for ease
        Node<Integer> localHead, localTail;
        localHead = (Node<Integer>) thread.enqHead.get();
        localTail = (Node<Integer>) thread.enqTail.get();

        // Add in the local linked list code.
        if (localHead == null)
        {

            thread.enqHead.compareAndSet(null, new Node<Integer>((Integer) futureOp.future.returnVal));
            thread.enqTail.compareAndSet(null, thread.enqHead.get());
        }
        else
        {
            // QUESTION: Why did we move the futureop to input again?
            localTail.next.compareAndSet(null, new Node<Integer>((Integer) futureOp.future.returnVal));
            //thread.enqTail.next = new LocalNode<T>((T) futureOp.future.returnVal);

            thread.enqTail.compareAndSet(localTail, localTail.next.get());
            //thread.enqTail = thread.enqTail.next;
        }

        thread.opsQueue.add(futureOp);
        thread.numEnqs++;

        return futureOp.future;
    }

    public Future futureDequeue(){
        FutureOp futureOp = new FutureOp(false, new Future(null));

        thread.opsQueue.add(futureOp);
        thread.numDeqs++;

        // Add in the excess dequeue algorithm.
        // If there are no enqueues for this deq to match with, this is an excess dequeue
        if (thread.unusedEnqs == 0)
        {
            thread.numExcessDeqs++;
        }

        // Otherwise if an unpaired enqueue exists, mark is as paired
        else
        {
            thread.unusedEnqs--;
        }

        return futureOp.future;
    }

    public void evaluate (Future future){
        Node<Integer> oldHead;
        NodeWithCount head;

        if (future.isDone){
            return;
        }

        if(thread.numEnqs > 0){

            oldHead = queue.executeBatch(new BatchRequest(thread));
                    // System.out.println("oldHead = " + oldHead);
            pairFutureWithResults(oldHead);

        }else{
            NodeWithCount deqBatchHead = executeDeqsBatch();
            pairDeqFuturesWithResults(deqBatchHead.node, deqBatchHead.count);
        }

        thread.numEnqs = 0;
        thread.numDeqs = 0;
        thread.numExcessDeqs = 0;
        thread.unusedEnqs = 0;
    }

    /*
        PairFuturesWithResults. PairFuturesWithResults receives the old head.
        It simulates the pending operations one by one according to their original order,
        which is recorded in the thread’s
        opsQueue. Namely, it simulates updates of the head and tail of the shared queue.
        This is done by advancing nextEnqNode (which represents the value of tail->next in
        the current moment of the simulation) on each enqueue, and by advancing currentHead on
        dequeues that occur when the queue in its current state is not empty.
        The simulation is run in order to set results for future objects related to the pending operations and mark them as done.
    */


    public void pairFutureWithResults(Node<Integer> oldHeadNode){
        Node<Integer> nextEnqNode;
        Node<Integer> currentHead;
        boolean noMoreSuccessfulDeqs = false;
        FutureOp op;

        nextEnqNode = (Node<Integer>) thread.enqHead.get();
        currentHead = oldHeadNode;

        while (!thread.opsQueue.isEmpty()){
        	//System.out.println("In Loop pairFutureWithResults");
            op = (FutureOp) thread.opsQueue.remove();

            // Check if operation is Enqueue
            if(op.isEnqueue){
                nextEnqNode = (Node<Integer>) nextEnqNode.next.get();
                // System.out.println("nextEnqNode = " + nextEnqNode);
            }
            // If dequeue operation
            else{

                if(noMoreSuccessfulDeqs || (Node<Integer>) currentHead.next.get() == nextEnqNode){
                    op.future.returnVal = null;
                }

                else{
                    currentHead = (Node<Integer>) currentHead.next.get();

                    if(currentHead == (Node<Integer>) thread.enqTail.get()){
                        noMoreSuccessfulDeqs = true;
                    }

                    op.future.returnVal = currentHead.val;
                }

            }
            op.future.isDone = true;
        }

        //System.out.println("Leaving pairFutureWithResults");

    }

    /*
        The ExecuteDeqsBatch method  rst as- sists a colliding ongoing batch operation if
        there is any (in Line 94). It then calculates the new head and the number of
        successful de- queues by traversing over the items to be dequeued in the loop in Line
        97. If there is at least one successful dequeue, the dequeues take e
        ect at once using a single CAS operation in Line 105. The CAS pushes the shared queue’s head success f
        ulDeqsNum nodes forward.
    */

    public NodeWithCount executeDeqsBatch(){
        NodeCountOrAnn oldHeadAndCnt;
        Node<Integer> newHeadNode;
        Node<Integer> headNextNode;
        int tempcount;
        NodeWithCount baseNewHead;
        int successfulDeqsNum = 0;

        while(true){
            oldHeadAndCnt = queue.helpAnnAndGetHead();
            baseNewHead = oldHeadAndCnt.nodeWCount;
            tempcount = baseNewHead.count;
            newHeadNode = baseNewHead.node;
            successfulDeqsNum = 0;

            // repeat threadData.deqsNum times:?

            for (int i = 0; i < thread.numDeqs; i++){
                headNextNode = (Node<Integer>) newHeadNode.next.get();

                if(headNextNode == null){
                    break;
                }

                successfulDeqsNum++;
                tempcount++;
                newHeadNode = headNextNode;
            }

            if(successfulDeqsNum == 0){
                break;
            }

            if(queue.head.compareAndSet(oldHeadAndCnt, new NodeCountOrAnn(null, new NodeWithCount(newHeadNode, tempcount), false)))
                break;

        }

        return new NodeWithCount(oldHeadAndCnt.nodeWCount.node, successfulDeqsNum);
    }

    // PairDeqFuturesWithResults to pair the successfully-dequeued-items to futures of the appropriate operations
    // n opsQueue. The remaining future dequeues are unsuccessful, thus their results are set to NULL.

    public void pairDeqFuturesWithResults(Node<Integer> oldHeadNode, int successfulDeqsNum){
        Node<Integer> currentHead;
        FutureOp op;
        currentHead = oldHeadNode;

        for(int i = 0; i < successfulDeqsNum; i++){
            currentHead = (Node<Integer>) currentHead.next.get();
            op = (FutureOp) thread.opsQueue.remove();

            op.future.returnVal = currentHead.val;
            op.future.isDone = true;
        }

        for(int j = 0; j < (thread.numDeqs - successfulDeqsNum); j++){
            op = (FutureOp) thread.opsQueue.remove();
            op.future.returnVal = null;
            op.future.isDone = true;
        }
    }

    public void run(){
    	Future d = null;
    	AtomicInteger numOps = new AtomicInteger(0);
    	Random rand = new Random();
    	int count = 0;
    	int op;
    	int enq;


    	while(numOps.get() < 500000){

    		if(count == 4){
    			evaluate(d);
    			count = 0;
    		}

    		op = rand.nextInt(2);

    		if(op == 0){
    			enq = rand.nextInt(100);
    			d = futureEnqueue(new FutureOp(true, new Future(enq)));
    		}

    		if(op == 1){
    			d = futureDequeue();    		
    		}

    		numOps.getAndIncrement();
    	}

    	evaluate(d);
    }


}

public class LFQueue<T> {
    // The queue will contain atomic references for both the head and the tail of the queue, enabling atomic setting
    public AtomicReference<NodeCountOrAnn> head;
    public  AtomicReference<NodeWithCount> tail;


    // Constructor for the Queue
    public LFQueue()
    {
        // Instantiate the head and tail with a dummy node
        NodeWithCount dummy = new NodeWithCount(new Node<Integer>(0), 0);

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
        	//System.out.println("In Loop enqueueToShared");
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
                executeAnnouncement(curHead);
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
        	//System.out.println("In dequeued to shared");
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
            if (head.compareAndSet(curHead, new NodeCountOrAnn(null, new NodeWithCount(headNextNode, curHead.nodeWCount.count + 1), false)))
                return headNextNode.val;
        }
    }



    // HelpAnnAndGetHead. This auxiliary method assists announcements in execution,
    // as long as there is an announcement installed in SQHead.

    public NodeCountOrAnn helpAnnAndGetHead(){
        NodeCountOrAnn head;

        while (true){
        	//System.out.println("In HelpAnnAndGetHead");

            // Getting the current head
            head = this.head.get();

            // Need to see how this translates to java since we don;t have the union struct.
            if(!head.isAnnouncement){
                return head;
            }

                   /*
                ~~~~~~~~~~~~~~~~ Implement executeAnn()
                 */
            executeAnnouncement(head);
        }
    }

    // Execute Batch
    // (1) Setting ann->oldHead to the head of the queue right before committing the batch.
    // (2) Installing ann in SQHead.
    // (3) Linking the batch’s items to SQTail.node->next.
    // (4) Setting oldTail  eld in the installed announcement ann.
    // (5) Promoting SQTail to point to the last node enqueued by the batch operation (and increasing its enqueue count by the number of enqueues).
    // (6) Setting SQHead to point to the last node dequeued by the batch operation in place of ann (and increasing its dequeue count by the number of successful dequeues).

    public Node<T> executeBatch(BatchRequest batch){
        NodeCountOrAnn ptr;
        // Creating a new announcement object and passing the batch request
        Announcement ann = new Announcement(batch);
        NodeCountOrAnn newHead;

        while (true){
        	//System.out.println("In Loop ExecuteBatch");
            // Checking if there is a coliding ongoing batch whose announcement
            // is installed in the SQHead.
            ptr = helpAnnAndGetHead();

            ann.oldHead = ptr.nodeWCount;

            newHead = new NodeCountOrAnn(ann, null, true);
            if(head.compareAndSet(ptr, newHead)){
                break;
            }

        }
        // Calling ExecuteAnn to carry out the batch
        executeAnnouncement(newHead);

        return ptr.nodeWCount.node;
    }

    public void executeAnnouncement(NodeCountOrAnn annHead)
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
            if (annOldTail != null && annOldTail.node != null)
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
        updateHead(annHead);
    }

    /*
        UpdateHead is to update SQHead to point to the last node dequeued by the batch. 
        This update uninstalls the announcement and completes its handling.

    */
    private void updateHead(NodeCountOrAnn ann){
        Node<T> newHeadNode = new Node<>();
        int oldQueueSize = ann.announcement.oldTail.count - ann.announcement.oldHead.count;
        int successfulDeqsNum = ann.announcement.batchToApply.numDeqs;

        //#successfulDequeues = #dequeues max{#excessDequeues n,0}

        if (ann.announcement.batchToApply.numExcessDeqs > oldQueueSize){
            successfulDeqsNum -= ann.announcement.batchToApply.numExcessDeqs - oldQueueSize;
        }

        if(successfulDeqsNum == 0){
            head.compareAndSet(ann, new NodeCountOrAnn(null, ann.announcement.oldHead, false));
            return;
        }

        if (oldQueueSize > successfulDeqsNum){
            newHeadNode = getNthNode(ann.announcement.oldHead.node, successfulDeqsNum);
        }else{
            newHeadNode = getNthNode(ann.announcement.oldTail.node, successfulDeqsNum - oldQueueSize);
        }

        

        head.compareAndSet(ann, new NodeCountOrAnn(null, new NodeWithCount(newHeadNode, ann.announcement.oldHead.count
                + successfulDeqsNum), false));

    }

    private Node<T> getNthNode(Node<T> node, int n)
    {	
        for (int i = 0; i < n; i++){
        		node = node.next.get();
        	}
            

        return node;
    }

    public static void main (String[] args) throws Exception{
    	int THREAD_NUMBER = 8;
	
		Thread threads[] = new Thread[THREAD_NUMBER];
		LFQueue<Integer> queue = new LFQueue<>();

		long start = System.currentTimeMillis();

		for(int i = 0; i < THREAD_NUMBER; i++){
			threads[i] = new Thread(new TestThreads(queue, i));
			threads[i].start();
		}

		for(int i = 0; i < THREAD_NUMBER; i++){
			threads[i].join();
		}

		long stop = System.currentTimeMillis();

		System.out.println("\nRuntime: " + (stop - start) + "ms.");
		// Node<Integer> temp = queue.head.get().nodeWCount.node;

		// while(temp.next.get() != null){
		// 	System.out.println(temp.val);
		// 	temp = (Node<Integer>)temp.next.get();
		// }

	}


}
