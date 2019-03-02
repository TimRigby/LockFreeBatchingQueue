// Lock Free linked list based Queue implementation serving as the framework for the
// Lock Free Queue with Batching data structure project
import java.util.concurrent.atomic.AtomicReference;

public class LFQueue<T> {
    // The queue will contain atomic references for both the head and the tail of the queue, enabling atomic setting
    private AtomicReference<Node<T>> head;
    private AtomicReference<Node<T>> tail;

    // Define the class used for the Node structure
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
