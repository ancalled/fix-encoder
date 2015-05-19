package com.mcscm.fixtools.utils;

import org.sample.ExecutionReport;
import org.sample.MarketDataIncrementalRefresh;

import java.util.ArrayList;
import java.util.List;

public class RadixTree<T> {

    public final Node<T> root = new Node<>(0, null);

    public void add(byte[] key, T val) {
        root.add(key, 0, val);
    }

    public static class Node<T> {

        private final byte key;
        private List<Node<T>> children;
        private T value;

        public Node(int key, T value) {
            this((byte) key, value);
        }

        public Node(byte key, T value) {
            this.key = key;
            this.value = value;
        }

        public byte key() {
            return key;
        }

        public T get() {
            return value;
        }

        public void add(byte[] key, int level, T val) {
            if (key.length <= level) return;

            boolean last = level == key.length - 1;

            byte b = key[level];
            Node<T> n = find(b);
            if (n == null) {
                n = new Node<>(b, last ? val : null);
                addChild(n);
            } else if (last) {
                n.value = val;
            }

            n.add(key, level + 1, val);
        }

        public Node<T> find(int i) {
            return find((byte) i);
        }

        public Node<T> find(byte v) {
            if (children == null) return null;
            for (Node<T> n : children) {
                if (n.key == v) return n;
            }
            return null;
        }

        void addChild(Node<T> node) {
            if (children == null) {
                children = new ArrayList<>();
            }

            children.add(node);
        }

        @Override
        public String toString() {
            return "Node{" +
                    "value=" + key +
                    '}';
        }


    }

    public Node<T> find(int i) {
        return find((byte) i);
    }

    public Node<T> find(byte b) {
        return root.find(b);
    }

    public static void main(String[] args) {
        RadixTree<String> tree = new RadixTree<>();
        tree.add(MarketDataIncrementalRefresh.TAG_APPLQUEUEDEPTH, "813");
        tree.add(MarketDataIncrementalRefresh.TAG_APPLQUEUERESOLUTION, "814");
        tree.add(MarketDataIncrementalRefresh.TAG_MDREQID, "262");
        tree.add(MarketDataIncrementalRefresh.TAG_NOMDENTRIES, "268");
        tree.add(MarketDataIncrementalRefresh.TAG_MDBOOKTYPE, "1021");
        tree.add(ExecutionReport.TAG_ACCOUNT, "1");

        System.out.println(tree.find(50).find(54).find(56).value);
        System.out.println(tree.find(56).find(49).find(52).value);
        System.out.println(tree.find(49).value);


    }


}
