// The head of our shared queue goes through multiple states. When a batch is not being applied it will be a NodeWithCount
// object, but as a batch is applied it will change state to be an Announcment object in order to have other threads
// help apply the batch to the shared queue and lock the state of the previous queue. It will have a boolean flag to
// signify state
public class NodeCountOrAnn {
    boolean isAnnouncement;
    NodeWithCount nodeWCount;
    Announcement announcement;
}
