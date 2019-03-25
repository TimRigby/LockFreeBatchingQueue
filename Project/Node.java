// Define the class used for the Node structure
// A node will contain a point of data and a reference to its next node
public class Node<T>
{
    T val;
    Node<T> next;

    // Node constructor
    Node(T value)
    {
        val = value;
        next = null;
    }
}