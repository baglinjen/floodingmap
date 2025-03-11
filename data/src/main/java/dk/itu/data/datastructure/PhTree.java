package dk.itu.data.datastructure;

import dk.itu.common.models.Geographical2D;
import dk.itu.common.models.osm.OsmElement;

import static dk.itu.data.datastructure.PhNode.getFirstNBitsFromLong;
import static dk.itu.data.datastructure.PhNode.printLong;

public class PhTree<T extends Geographical2D> {
    private PhNode<T> root;

    public void put(T element) {
        if (root == null) {
            root = new PhNode<>();
        }

        var eti = elementToBounds(element);

        if (root.getIsPrefixEmpty()) {
            // Prefixes are not used => first element in node

            // 1. Find first bit where a pair of min and max don't match => element overlaps quad
            // 2. Get bits up to different bit
            // 3. Set as prefix
            root.setPrefix(eti.maxLonBits, eti.minLatBits, eti.iodb+1);
            // 4. Add element to node
            root.addElement(element);
        } else {
            // Prefixes are used => keep looking down
            // 1. Get iodb for element
            System.out.println(Long.toBinaryString(eti.minLonBits));
            System.out.println(Long.toBinaryString(eti.maxLonBits));
            System.out.println(Long.toBinaryString(eti.minLatBits));
            System.out.println(Long.toBinaryString(eti.maxLatBits));
            System.out.println(eti.iodb);
            long lonBeforeIodb = getFirstNBitsFromLong(eti.minLonBits, eti.iodb+1);
            long latBeforeIodb = getFirstNBitsFromLong(eti.minLatBits, eti.iodb+1);
            long prefixLon = root.getPrefixLon();
            long prefixLat = root.getPrefixLat();
            System.out.println(Long.toBinaryString(lonBeforeIodb));
            System.out.println(Long.toBinaryString(latBeforeIodb));
            System.out.println(eti.iodb);
            System.out.println(Long.toBinaryString(prefixLon));
            System.out.println(Long.toBinaryString(prefixLat));
            System.out.println(root.getPrefixLength());
            System.out.println();


            int pciodb = indexOfDifferentBits(root.getPrefixLon(), root.getPrefixLat(), lonBeforeIodb, latBeforeIodb, 0, 64);
            System.out.println(pciodb);
            if (pciodb < root.getPrefixLength()) {
                long newPrefixLon = getFirstNBitsFromLong(root.getPrefixLon(), pciodb+1);
                long newPrefixLat = getFirstNBitsFromLong(root.getPrefixLat(), pciodb+1);
                int newChildNodePrefixLength = root.getPrefixLength() - (pciodb+1); // Skip prefix + HC
                long newChildNodePrefixLon = getFirstNBitsFromLong(root.getPrefixLon(), newChildNodePrefixLength, pciodb+1);
                long newChildNodePrefixLat = getFirstNBitsFromLong(root.getPrefixLat(), newChildNodePrefixLength, pciodb+1);

                PhNode<T> newChildNode = new PhNode<>();
                newChildNode.setPrefixRaw(newChildNodePrefixLon, newChildNodePrefixLat, 0);
                newChildNode.transferElements(root);

            } else {

            }


//            if (root.getPrefixLength() == eti.iodb) {
//                // Same length prefix and iodb
//                if (root.getPrefixLon() == prefixLon && root.getPrefixLat() == prefixLat) {
//                    // Exact same prefixes => Add element
//                    root.addElement(element);
//                } else {
//                    // Different prefixes => expect to split into 2 nodes => find where
//                    int iodbbp = indexOfDifferentBits(lonBeforeIodb, prefixLon, latBeforeIodb, prefixLat, eti.iodb);
//                    // Split at iodbbp
//                }
//            } else if (root.getPrefixLength() < eti.iodb) {
//                // Shorter => see if they fit
//                if (getFirstNBitsFromLong(root.getPrefixLon(), eti.iodb) == ) {}
//            } else {
//                // Longer => see if they fit
//            }

//            System.out.println(root.getPrefixLength());
//            System.out.println(Long.toBinaryString(lon));
//            System.out.println(Long.toBinaryString(prefixLon));
//            System.out.println(Long.toBinaryString(lat));
//            System.out.println(Long.toBinaryString(prefixLat));

//            if (eti.iodb > root.getPrefixLength()) {
//                // Add it further as child node
//            } else if (eti.iodb < root.getPrefixLength()) {
//                // Split up at iodb
//            } else {
//                // Add as element to node if they match
//
//            }
//
//            if (lon == prefixLon && lat == prefixLat) {
//                // Equal prefix and element => throw as sub node
//                return;
//            } else {
//                // Not equal prefix => Find where it splits before HC
//                int iodpb = indexOfDifferentBits(lon, lat, prefixLon, prefixLat, root.getPrefixLength());
//                System.out.println(iodpb);
//
//                long newPrefixLon = getFirstNBitsFromLong(prefixLon, iodpb);
//                long newPrefixLat = getFirstNBitsFromLong(prefixLat, iodpb);
//                System.out.println(Long.toBinaryString(newPrefixLon));
//                System.out.println(Long.toBinaryString(newPrefixLat));
//
//
//            }

            System.out.println();



            // 2. Test if prefix matches element up to iodb
            //    a. If match with iodb > prefix length => add it as further down as child node
            //    b. If match with iodb == prefix length => add it as element to node
            //    c. If match with iodb < prefix length => split it up 3a
            // 3. Else Doesn't


            // 3a. Split it up
            // 4a. Create new node with same specifications as current with shortened prefix by iodb
            // 5a. Cleanse specifications of current node
            // 7a. Set current node prefix to iodb
            // 8a. Set new node as child with index iodb value


        }

//        System.out.println();
    }

    private int indexOfDifferentBits(long lon, long lat, long prefixLon, long prefixLat, int prefixLength) {
        return indexOfDifferentBits(lon, lat, prefixLon, prefixLat, 0, prefixLength);
    }

    private int indexOfDifferentBitForElement(long minLonBits, long minLatBits, long maxLonBits, long maxLatBits) {
        return indexOfDifferentBits(minLonBits, minLatBits, maxLonBits, maxLatBits, 0, 64);
    }

    private int indexOfDifferentBits(long lon1, long lat1, long lon2, long lat2, int min, int max) {
        int indexOfDifferentBit = (max-min)/2, minLeftBits = min, maxRightBits = max;
        long maskLeftmost;

        while (!(minLeftBits == indexOfDifferentBit || maxRightBits == indexOfDifferentBit)) {
            maskLeftmost = 0xFFFFFFFFL << (64-indexOfDifferentBit);

            boolean noOverlaps =
                    (lon1 & maskLeftmost) == (lon2 & maskLeftmost) &&
                            (lat1 & maskLeftmost) == (lat2 & maskLeftmost);

            if (noOverlaps) {
                // No overlaps => look right
                minLeftBits = indexOfDifferentBit;
                indexOfDifferentBit += (maxRightBits - minLeftBits) / 2;
            } else {
                // Overlaps => look left
                maxRightBits = indexOfDifferentBit;
                indexOfDifferentBit -= (maxRightBits - minLeftBits) / 2;
            }
        }

        return indexOfDifferentBit-1;
    }

    private ElementToInsert<T> elementToBounds(T element) {
        var bounds = element.getBounds();
        long minLonBits = Double.doubleToLongBits(bounds[1]);
        long minLatBits = Double.doubleToLongBits(bounds[0]);
        long maxLonBits = Double.doubleToLongBits(bounds[3]);
        long maxLatBits = Double.doubleToLongBits(bounds[2]);
        int iodb = indexOfDifferentBitForElement(minLonBits, minLatBits, maxLonBits, maxLatBits);

        return new ElementToInsert<>(element, minLonBits, minLatBits, maxLonBits, maxLatBits, iodb, 0);
    }

    private record ElementToInsert<T>(
            T element,
            long minLonBits,
            long minLatBits,
            long maxLonBits,
            long maxLatBits,
            int iodb,
            int prefixCovered
    ) {}
}
