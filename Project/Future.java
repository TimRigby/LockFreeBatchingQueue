// The future object will attach to deferred enqueue and dequeue operations to track status of execution
// Contains a result that will hold a dequeued value if available and nothing for an enqueue operation
// Contains a boolean flag to signal if the operation has been executed yet
public class Future<T> {
    T returnVal;
    boolean isDone;

    Future(T val){
    	this.returnVal = val;
    }
}