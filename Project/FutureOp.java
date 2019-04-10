// A future Operation object will be used to track information in local thread batching and identify the type of
// operation as well as a reference to its future operation object
public class FutureOp {
    boolean isEnqueue;
    Future future;

    FutureOp(boolean val, Future f){
    	this.isEnqueue = val;
    	this.future = f;
    }
}