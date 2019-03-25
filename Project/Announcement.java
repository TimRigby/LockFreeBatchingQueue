// The Announcement object holds on to the state of the shared queue before applying a batch
// It contains a reference to the head and the tail of the shared queue as well as a reference to the BatchRequest
// object being applied
public class Announcement {
    BatchRequest batchToApply;
    NodeWithCount oldHead;
    NodeWithCount oldTail;
}