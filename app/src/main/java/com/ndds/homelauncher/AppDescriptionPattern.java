package com.ndds.homelauncher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class AppDescriptionPattern {
    public static class Result {
        final public String keyword;
        final public int detectedIndex;
        final public HashSet<String> packageNames;

        private Result(String keyword, int detectedIndex, WordInfo wordInfo) {
            this.keyword = keyword;
            this.detectedIndex = detectedIndex;
            this.packageNames = wordInfo.packageNames;
        }
    }

    private static class WordInfo {
        public boolean isCheckPoint = false;
        public HashSet<String> packageNames = null;
        int[] prefix;

        public WordInfo(int[] prefix) {
            this.prefix = prefix;
        }
    }

    private static class Node {
        public int letter;
        public int height = 1;
        public WordInfo wordInfo;
        public Node left, right, rootChildNode;

        public Node failureLink = null;

        public Node(int letter, WordInfo wordInfo) {
            this.letter = letter;
            this.wordInfo = wordInfo;
        }

        public Node(int letter) {
            this.letter = letter;
        }

        static class CapturedNode {
            Node availableNode;
        }
    }

    private final Node root;

    public AppDescriptionPattern() {
        root = new Node(0, new WordInfo(new int[0]));
    }

    public AppDescriptionPattern build() {
        createFailureLinks(root);
        return this;
    }

    private Node minValueNode(Node node) {
        Node current = node;
        while (current.left != null)
            current = current.left;
        return current;
    }

    private Node deleteBinary(Node node, int key) {
        if (node == null) return null;

        if (key < node.letter)
            node.left = deleteBinary(node.left, key);
        else if (key > node.letter)
            node.right = deleteBinary(node.right, key);
        else {
            // Node with one or zero child
            if (node.left == null || node.right == null) {
                node = (node.left != null) ? node.left : node.right;
            } else {
                // Node with two children
                Node successor = minValueNode(node.right);
                node.letter = successor.letter;
                node.right = deleteBinary(node.right, successor.letter);
            }
        }

        if (node == null) return null;

        node.height = Math.max(height(node.left), height(node.right)) + 1;

        int balance = getBalance(node);

        // LL
        if (balance > 1 && getBalance(node.left) >= 0)
            return rightRotate(node);

        // LR
        if (balance > 1 && getBalance(node.left) < 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }

        // RR
        if (balance < -1 && getBalance(node.right) <= 0)
            return leftRotate(node);

        // RL
        if (balance < -1 && getBalance(node.right) > 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }
    public void removeAbsoluteWord(String word) {
        Node selectedNode = root;
        Node parentNode = root;
        int removeCharacter = -1;
        characterIteration: for (int textIndex = 0; textIndex < word.length(); textIndex++) {
            int character = word.charAt(textIndex);
            Node n = selectedNode.rootChildNode;
            if (n.left != null || n.right != null) {
                removeCharacter = character;
                parentNode = selectedNode;
            }
            while (n != null) {
                if (n.letter == character) {
                    selectedNode = n;
                    continue characterIteration;
                } else if (character < n.letter)
                    n = n.left;
                else
                    n = n.right;
            }
            return;
        }

        parentNode.rootChildNode = deleteBinary(parentNode.rootChildNode, removeCharacter);
    }

    private int height(Node n) {
        return n == null ? 0 : n.height;
    }

    private int getBalance(Node n) {
        return n == null ? 0 : height(n.left) - height(n.right);
    }

    private Node rightRotate(Node r) {
        Node x = r.left;
        Node t2 = x.right;

        // Perform rotation
        x.right = r;
        r.left = t2;

        // Update heights
        r.height = Math.max(height(r.left), height(r.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;

        return x;
    }

    private Node leftRotate(Node x) {
        Node y = x.right;
        Node t2 = y.left;

        // Perform rotation
        y.left = x;
        x.right = t2;

        // Update heights
        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;

        return y;
    }

    private Node binaryInsert(Node node, int letter, Node parentNode, Node.CapturedNode capturedNode) {
        if (node == null) {
            Node newNode;
            if (parentNode != null) {
                int[] prefixCharacters = new int[parentNode.wordInfo.prefix.length + 1];
                System.arraycopy(parentNode.wordInfo.prefix, 0, prefixCharacters, 0, parentNode.wordInfo.prefix.length);
                prefixCharacters[prefixCharacters.length - 1] = letter;
                WordInfo wordInfo = new WordInfo(prefixCharacters);
                newNode = new Node(letter, wordInfo);
                newNode.failureLink = root;
            } else
                newNode = new Node(letter);
            capturedNode.availableNode = newNode;
            return newNode;
        }

        if (letter < node.letter)
            node.left = binaryInsert(node.left, letter, parentNode, capturedNode);
        else if (letter > node.letter)
            node.right = binaryInsert(node.right, letter, parentNode, capturedNode);
        else {
            capturedNode.availableNode = node;
            return node; // no duplicates
        }

        node.height = 1 + Math.max(height(node.left), height(node.right));

        int balance = getBalance(node);

        // LL
        if (balance > 1 && letter < node.left.letter)
            return rightRotate(node);

        // RR
        if (balance < -1 && letter > node.right.letter)
            return leftRotate(node);

        // LR
        if (balance > 1 && letter > node.left.letter) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }

        // RL
        if (balance < -1 && letter < node.right.letter) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }
    public AppDescriptionPattern addWord(String packageName, String word) {
        if (word.isEmpty()) return this;
        Node n = root;
        int textIndex = 0;
        while (textIndex < word.length()) {
            int character = word.charAt(textIndex);
            if (character >= 65 && character <= 90)
                character += 32;
            Node.CapturedNode capturedNode = new Node.CapturedNode();
            n.rootChildNode = binaryInsert(n.rootChildNode, character, n, capturedNode);
            n = capturedNode.availableNode;
            textIndex++;
        }

        n.wordInfo.isCheckPoint = true;
        if (n.wordInfo.packageNames == null)
            n.wordInfo.packageNames = new HashSet<>();
        n.wordInfo.packageNames.add(packageName);
        return this;
    }

    private void createFailureLinks(Node n) {
        if (n == null) return;
        for (int prefixStartIndex = 1; prefixStartIndex < n.wordInfo.prefix.length; prefixStartIndex++) {
            Node latestMatch = root;
            boolean hasMatchingPrefix = true;
            for (int prefixIndex = prefixStartIndex; prefixIndex < n.wordInfo.prefix.length; prefixIndex++) {
                boolean isCharacterMatch = false;
                int prefixCharacter = n.wordInfo.prefix[prefixIndex];
                Node trialNode = latestMatch.rootChildNode;
                while (trialNode != null) {
                    if (trialNode.letter == prefixCharacter) {
                        latestMatch = trialNode;
                        isCharacterMatch = true;
                        break;
                    }
                    if (prefixCharacter < trialNode.letter)
                        trialNode = trialNode.left;
                    else
                        trialNode = trialNode.right;
                }
                if (!isCharacterMatch) {
                    hasMatchingPrefix = false;
                    break;
                }
            }
            if (hasMatchingPrefix) {
                n.failureLink = latestMatch;
                break;
            }
        }

        if (!n.wordInfo.isCheckPoint)
            n.wordInfo = null;

        transverseAndBuildFailureLinks(n.rootChildNode);
    }

    private void transverseAndBuildFailureLinks(Node n) {
        if (n == null)
            return;
        createFailureLinks(n);
        transverseAndBuildFailureLinks(n.left);
        transverseAndBuildFailureLinks(n.right);
    }

    private String fromBytesString(int[] codePoints) {
        return new String(codePoints, 0, codePoints.length);
    }

    private boolean isWordComplete(int characterIndex, Node selectedNode, CharSequence wholeText) {
        int beforeFirstIndex = characterIndex - selectedNode.wordInfo.prefix.length;
        if (beforeFirstIndex >= 0) {
            char firstChar = wholeText.charAt(beforeFirstIndex);
            if (firstChar >= 'a' && firstChar <= 'z')
                return false;
        }
        int lastTextIndex = wholeText.length() - 1;
        if (characterIndex < lastTextIndex) {
            char lastCharacter = wholeText.charAt(characterIndex + 1);
            return lastCharacter < 'a' || lastCharacter > 'z';
        }
        return true;
    }
    public Result getMatch(CharSequence wholeText) {
        Node selectedNode = root;

        for (int characterIndex = 0; characterIndex < wholeText.length(); characterIndex++) {
            int character = wholeText.charAt(characterIndex);
            if (character >= 65 && character <= 90)
                character += 32;

            Node n = selectedNode.rootChildNode;
            while (n != null) {
                if (n.letter == character) {
                    selectedNode = n;
                    if (selectedNode.wordInfo.isCheckPoint  &&
                            isWordComplete(characterIndex, selectedNode, wholeText)) {
                        return new Result(fromBytesString(selectedNode.wordInfo.prefix), characterIndex, selectedNode.wordInfo);
                    }
                    break;
                }
                if (character < n.letter)
                    n = n.left;
                else
                    n = n.right;
            }
        }
        return null;
    }
}
