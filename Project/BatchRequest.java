// A BatchRequest object will hold the information necessary to apply a completed batch to the shared queue
// Contains a reference to the first node in the sublist to be appended to the shared queue
// Contains a reference to the last node in the sublist to be appeneded to the shared queue
// Holds the number of enqueues and dequeues taking place within this batch as well as the
// number of excess dequeues that will need to be applied outside of the batch on the shared queue
public class BatchRequest<T>{
    Node<T> firstEnq;
    Node<T> lastEnq;
    int numEnqs;
    int numDeqs;
    int numExcessDeqs;

    BatchRequest(ThreadData data){
    	this.firstEnq = (Node<T>) data.enqHead.get();
    	this.lastEnq = (Node<T>) data.enqTail.get();
    	this.numEnqs = data.numEnqs;
    	this.numDeqs = data.numDeqs;
    	this.numExcessDeqs = data.numExcessDeqs;
    }
}