package dk.itu.data.datastructure;

import dk.itu.common.models.Geographical2D;

import static dk.itu.data.datastructure.PhNode.*;

public class PhTree<T extends Geographical2D> {
    private PhNode<T> root;

    public void put(T element) {
        if (root == null) {
            root = new PhNode<>();
        }

        var eti = new ElementToInsert(element);
        var eti2 = new ElementToInsert2(element);

//        put(root, eti, 0);
        put2(root, eti2, 0);
        System.out.println();
    }

    private void put2(PhNode<T> node, ElementToInsert2 eti, int prefixOffset) {
        if (node == null) {
            // If node is null => create the node
            node = new PhNode<>();
            // Insert element in node
            put2(node, eti, prefixOffset);
        } else {
            if (node.getIsPrefixEmpty()) {
                node.setPrefix(eti.minLonBits, eti.minLatBits, eti.iodb, prefixOffset);
                node.addElement(eti.element);
                return;
            }
            long elementMinLonPrefixOffset = getFirstNBitsFromLong(eti.minLonBits, 64-prefixOffset, prefixOffset);
            long elementMaxLonPrefixOffset = getFirstNBitsFromLong(eti.maxLonBits, 64-prefixOffset, prefixOffset);
            long elementMinLatPrefixOffset = getFirstNBitsFromLong(eti.minLatBits, 64-prefixOffset, prefixOffset);
            long elementMaxLatPrefixOffset = getFirstNBitsFromLong(eti.maxLatBits, 64-prefixOffset, prefixOffset);

            // Where does element overlap
            int iodb = indexOfDifferentBitForElement(elementMinLonPrefixOffset, elementMinLatPrefixOffset, elementMaxLonPrefixOffset, elementMaxLatPrefixOffset);
            long elementLonBeforeIodb = getFirstNBitsFromLong(elementMinLonPrefixOffset, iodb);
            long elementLatBeforeIodb = getFirstNBitsFromLong(elementMinLatPrefixOffset, iodb);
            int iodbPrefix = indexOfDifferentBitForElement(elementLonBeforeIodb, elementLatBeforeIodb, node.getPrefixLon(), node.getPrefixLat());

            int prefixLength = node.getPrefixLength();

            /*
            iodb == 11
            iodbPrefix == 63
            prefixLength == 11

            => Add new element to node
             */

            /*
            iodb == 14
            iodbPrefix == 11
            prefixLength == 13

            => Split prefix of node at iodbPrefix
            => Add element to old node as child

            => Create new Node NN
            => Transfer elements of ON to NN
            => Set NN prefix to iodbPrefix+1 -> prefixLength
            => Add NN as child to ON at index iodbPrefix of prefix
            => Cut down prefix to iodbPrefix-1 bits of old node ON
            => Create new child node CN
            => Add element to CN
            => Add CN at index iodbPrefix of element
             */

            /*
            iodb == 14
            iodbPrefix == 11
            prefixLength == 11

            => Create new node for element

            => Create new Node NN
            => Set NN prefix to prefixLength+1 -> iodb
            => Add element to NN
            => Add NN as child to ON at index prefixLength of element
             */

            /*
            iodb == 13
            iodbPrefix == 12
            prefixLength == 12

            => Create new node NN
            => Add element to new node NN
            => Add NN as child to ON at index iodbPrefix of element
             */

            /*
            iodb == 12
            iodbPrefix == 12
            prefixLength == 13

            iodb == 11
            iodbPrefix == 12
            prefixLength == 13

            => Split ON at iodb
                => ON has element added
                => ON has NN as child at index iodb

            => Create new Node NN
            => Transfer elements of ON to NN
            => Set NN prefix to iodb+1 -> prefixLength (if equal => prefix is 0)
            => Add NN as child to ON at index iodb of prefix
            => Cut down prefix to iodb bits of old node ON
            => Add element to ON
             */

            System.out.println();

//            if (iodb < node.getPrefixLength()) {
//                // Overlaps before => new middle node must be created
//
//                // Prefix matches element up to iodb =>
//                if (iodbInPrefix < node.getPrefixLength()) {
//                    // Element doesn't match prefix => create middle node earlier
//                } else {
//                    // Element matches prefix => cut at idobInPrefix
//                }
//                // Prefix doesn't match element up to idobMinusPrefix => create middle node even earlier
//            } else if (iodb > node.getPrefixLength()) {
//                // Overlaps after => Add to child node
//            }

//            int iodb = indexOfDifferentBits(node.getPrefixLon(), node.getPrefixLat())
        }

        // Else, insert element in node
    }

    private void put(PhNode<T> node, ElementToInsert eti, int prefixOffset) {
        eti.adjustIodb(prefixOffset);
        if (node.getIsPrefixEmpty()) {
            node.setPrefix(eti.minLonBits, eti.minLatBits, eti.iodb, prefixOffset);
            node.addElement(eti.element);
        } else {
            // Prefixes are used => keep looking down
            int pciodb = indexOfDifferentBits(node.getPrefixLon(), node.getPrefixLat(), getFirstNBitsFromLong(eti.minLonBits, eti.iodb, prefixOffset), getFirstNBitsFromLong(eti.minLatBits, eti.iodb, prefixOffset), 0, 64);

            if (eti.iodb < node.getPrefixLength()) {
                // 100% conflict => best case iodb - worst case pciodb
                fixNodeClashingWithPrefixBeforeHC(node, Math.min(pciodb, eti.iodb), eti);
            } else if (eti.iodb > node.getPrefixLength()) {
                // Check if prefix is respected
                if (pciodb < node.getPrefixLength()) {
                    // Split
                    fixNodeClashingWithPrefixBeforeHC(node, pciodb, eti);
                    // Split into 2 children at index

                    // Update prefix
                    long newPrefixLon = getFirstNBitsFromLong(eti.minLonBits, pciodb, prefixOffset);
                    long newPrefixLat = getFirstNBitsFromLong(eti.minLatBits, pciodb, prefixOffset);
                    node.setPrefixRaw(newPrefixLon, newPrefixLat, pciodb);

                    addElementAsChild(node, eti, prefixOffset);

                    for (T element : node.getElements()) {
                        addElementAsChild(node, new ElementToInsert(element), prefixOffset);
                    }

                    node.clearElements();


//                    int n = getNumberAtIndex(eti.minLonBits, eti.minLatBits, node.getPrefixLength()+prefixOffset);
//                    PhNode<T> childNode = node.getChild(n);
//                    if (childNode != null) {
//                        // Node currently exists, send it down with prefix skip
//                        put(childNode, eti, prefixOffset+node.getPrefixLength()+1);
//                    } else {
//                        // New node
//                        childNode = new PhNode<>();
//                        // New node, create it with prefix skip
//                        put(childNode, eti, prefixOffset+node.getPrefixLength()+1);
//                        node.setChild(childNode, n);
//                    }

                } else if (pciodb >= node.getPrefixLength()) {
                    // Prefix match => add it further down
                    addElementAsChild(node, eti, prefixOffset);
                }
            } else {
                // Element needs to be added after prefixes have been matched
                if (pciodb >= node.getPrefixLength()) {
                    // If pciodb is same or greater as prefix length => add as element
                    node.addElement(eti.element);
                } else {
                    // If pciodb is shorter than prefix => split
                    fixNodeClashingWithPrefixBeforeHC(node, pciodb, eti);
                }
            }
        }
    }

    private void addElementAsChild(PhNode<T> node, ElementToInsert eti, int prefixOffset) {
        int n = getNumberAtIndex(eti.minLonBits, eti.minLatBits, node.getPrefixLength()+prefixOffset);
        PhNode<T> childNode = node.getChild(n);
        if (childNode != null) {
            // Node currently exists, send it down with prefix skip
            put(childNode, eti, prefixOffset+node.getPrefixLength()+1);
        } else {
            // New node
            childNode = new PhNode<>();
            // New node, create it with prefix skip
            put(childNode, eti, prefixOffset+node.getPrefixLength()+1);
            node.setChild(childNode, n);
        }
    }

    private void fixNodeClashingWithPrefixBeforeHC(PhNode<T> node, int pciodb, ElementToInsert eti)  {
        long prefixLon = node.getPrefixLon();
        long prefixLat = node.getPrefixLat();
        long newPrefixLon = getFirstNBitsFromLong(prefixLon, pciodb);
        long newPrefixLat = getFirstNBitsFromLong(prefixLat, pciodb);
        int newChildNodePrefixLength = node.getPrefixLength() - (pciodb+1); // Skip prefix + HC
        long newChildNodePrefixLon = getFirstNBitsFromLong(prefixLon, newChildNodePrefixLength, pciodb+1);
        long newChildNodePrefixLat = getFirstNBitsFromLong(prefixLat, newChildNodePrefixLength, pciodb+1);

        PhNode<T> newChildNode = new PhNode<>();
        newChildNode.setPrefixRaw(newChildNodePrefixLon, newChildNodePrefixLat, newChildNodePrefixLength);
        newChildNode.transferElements(node);

        node.setPrefixRaw(newPrefixLon, newPrefixLat, pciodb);
        node.clearElements();
        node.setChild(newChildNode, getNumberAtIndex(prefixLon, prefixLat, pciodb));
        node.addElement(eti.element);
    }

    private int indexOfDifferentBits(long lon, long lat, long prefixLon, long prefixLat, int prefixLength) {
        return indexOfDifferentBits(lon, lat, prefixLon, prefixLat, 0, prefixLength);
    }

    private int indexOfDifferentBitForElement(long minLonBits, long minLatBits, long maxLonBits, long maxLatBits) {
        return indexOfDifferentBits(minLonBits, minLatBits, maxLonBits, maxLatBits, 0, 64);
    }

    private int indexOfDifferentBits(long lon1, long lat1, long lon2, long lat2, int min, int max) {
        int lo = min, hi = max;

        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            long mask = ~0L << (64 - mid); // Create a mask with 1s from MSB to position mid

            boolean matches =
                    (lon1 & mask) == (lon2 & mask) &&
                            (lat1 & mask) == (lat2 & mask);

            if (matches) {
                // If matches, difference is in right side (larger indices)
                lo = mid + 1;
            } else {
                // If doesn't match, difference is in this position or left side
                hi = mid;
            }
        }

        lo--; // What if different bit is at index 0? => would give -1

        return lo;
    }

    private class ElementToInsert2 {
        private final T element;
        private final long minLonBits;
        private final long minLatBits;
        private final long maxLonBits;
        private final long maxLatBits;
        private final int originalIodb;
        private int iodb;

        public ElementToInsert2(T element) {
            this.element = element;
            var bounds = element.getBounds();
            this.minLatBits = Double.doubleToLongBits(bounds[0]);
            this.minLonBits = Double.doubleToLongBits(bounds[1]);
            this.maxLatBits = Double.doubleToLongBits(bounds[2]);
            this.maxLonBits = Double.doubleToLongBits(bounds[3]);
            this.originalIodb = indexOfDifferentBitForElement(minLonBits, minLatBits, maxLonBits, maxLatBits);
            this.iodb = originalIodb;
        }
        public void adjustIodb(int prefixOffset) {
            this.iodb = originalIodb - prefixOffset;
        }
        public int getIodb() {
            return iodb;
        }
    }

    private class ElementToInsert {
        private final T element;
        private final long minLonBits;
        private final long minLatBits;
        private final long maxLonBits;
        private final long maxLatBits;
        private final int originalIodb;
        private int iodb;

        public ElementToInsert(T element) {
            this.element = element;
            var bounds = element.getBounds();
            this.minLatBits = Double.doubleToLongBits(bounds[0]);
            this.minLonBits = Double.doubleToLongBits(bounds[1]);
            this.maxLatBits = Double.doubleToLongBits(bounds[2]);
            this.maxLonBits = Double.doubleToLongBits(bounds[3]);
            this.originalIodb = indexOfDifferentBitForElement(minLonBits, minLatBits, maxLonBits, maxLatBits);
            this.iodb = originalIodb;
        }
        public void adjustIodb(int prefixOffset) {
            this.iodb = originalIodb - prefixOffset;
        }
        public int getIodb() {
            return iodb;
        }
    }
}
