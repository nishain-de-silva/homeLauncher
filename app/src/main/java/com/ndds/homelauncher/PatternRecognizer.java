package com.ndds.homelauncher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import com.ndds.homelauncher.models.AppInfo;
import com.ndds.homelauncher.utils.EdgeDetector;

import java.io.File;

public class PatternRecognizer {
    private static class EdgeNode {
        EdgeNode() {}
        EdgeNode(AppInfo appInfo, int viewCode, boolean isFreshInstall) {
            this.appInfo = appInfo;
            this.viewCode = viewCode;
            this.isFreshInstall = isFreshInstall;
        }

        AppInfo appInfo;
        EdgeNode next, previous;
        boolean isFreshInstall;
        int viewCode;
    }

    private static class Node {
        public int letter;
        public EdgeNode edgeNode;
        public int height = 1;
        public Node left, right, rootChildNode;

        public Node(int letter) {
            this.letter = letter;
        }

        static class CapturedNode {
            Node availableNode;
        }
    }

    private final Node root;
    private final Context context;
    private int globalViewId;
    private final EdgeNode listRoot = new EdgeNode();

    public PatternRecognizer(Context context) {
        this.context = context;
        root = new Node(0);
    }

    public void addPackage(AppInfo appInfo, boolean isFreshInstall) {
        int textIndex = 0;
        Node n = root;
        String packageName = appInfo.id;
        while (textIndex < packageName.length()) {
            int character = packageName.charAt(textIndex);
            Node.CapturedNode capturedNode = new Node.CapturedNode();
            n.rootChildNode = binaryInsert(n.rootChildNode, character, capturedNode);
            n = capturedNode.availableNode;
            textIndex += 3;
        }

        appInfo.isFresh = isFreshInstall;
        if (n.edgeNode != null) {
            appInfo.isPinned = n.edgeNode.appInfo.isPinned;
            n.edgeNode.appInfo = appInfo;
            n.edgeNode.isFreshInstall = isFreshInstall;
        } else {
            EdgeNode e = new EdgeNode(appInfo, globalViewId, isFreshInstall);
            n.edgeNode = e;
            e.next = listRoot.next;
            e.previous = listRoot;
            if (listRoot.next != null)
                listRoot.next.previous = e;
            listRoot.next = e;
        }
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

    public void removePackage(CharSequence packageName) {
        EdgeNode cursor = listRoot.next;
        while (cursor != null) {
            if (cursor.appInfo.packageName.contentEquals(packageName))
                removeEdge(cursor.appInfo.id);
            cursor = cursor.next;
        }
    }
    public void removeEdge(CharSequence word) {
        Node selectedNode = root;
        Node parentNode = root;
        int removeCharacter = -1;
        characterIteration:
        for (int textIndex = 0; textIndex < word.length(); textIndex += 3) {
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
        if (selectedNode.edgeNode == null)
            return;
        parentNode.rootChildNode = deleteBinary(parentNode.rootChildNode, removeCharacter);
        EdgeNode e = selectedNode.edgeNode;
        e.previous.next = e.next;
        if (e.next != null)
            e.next.previous = e.previous;
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

    private Node binaryInsert(Node node, int letter, Node.CapturedNode capturedNode) {
        if (node == null) {
            Node newNode = new Node(letter);
            capturedNode.availableNode = newNode;
            return newNode;
        }

        if (letter < node.letter)
            node.left = binaryInsert(node.left, letter, capturedNode);
        else if (letter > node.letter)
            node.right = binaryInsert(node.right, letter, capturedNode);
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

    public boolean markStart() {
        globalViewId++;
        return globalViewId == 1;
    }

    public void flushRemovedPackages() {
        EdgeNode cursor = listRoot.next;
        while (cursor != null) {
            if (cursor.viewCode != globalViewId)
                removeEdge(cursor.appInfo.id);
            cursor = cursor.next;
        }
    }

    private EdgeNode getEdgeNode(CharSequence packageName) {
        Node selectedNode = root;
        characterIteration:
        for (int characterIndex = 0; characterIndex < packageName.length(); characterIndex += 3) {
            int character = packageName.charAt(characterIndex);
            Node n = selectedNode.rootChildNode;
            while (n != null) {
                if (n.letter == character) {
                    selectedNode = n;
                    continue characterIteration;
                }
                if (character < n.letter)
                    n = n.left;
                else
                    n = n.right;
            }
            return null;
        }
        return selectedNode.edgeNode;
    }

    public AppInfo getAppFromDrawer(ResolveInfo resolveInfo, PackageManager pm, int appIconColor) {
        String label = resolveInfo.loadLabel(pm).toString();
        EdgeNode edgeNode = getEdgeNode(
                label + resolveInfo.activityInfo.applicationInfo.packageName
        );
        if (edgeNode == null) {
            Bitmap bitmap = null;
            File iconImage = new File(context.getFilesDir(), label+resolveInfo.activityInfo.applicationInfo.packageName+".png");
            if (iconImage.exists()) {
                bitmap = BitmapFactory.decodeFile(iconImage.getAbsolutePath());
            }
            AppInfo appInfo = new AppInfo(
                    label,
                    resolveInfo.activityInfo.applicationInfo.packageName,
                    resolveInfo.activityInfo.name,
                    bitmap
            );
            addPackage(appInfo, globalViewId > 1);
            return appInfo;
        }
        edgeNode.viewCode = globalViewId;
        return edgeNode.appInfo;
    }
    public AppInfo getAppInfo(CharSequence appId) {
        EdgeNode e = getEdgeNode(appId);
        if (e != null)
            return e.appInfo;
        return null;
    }
}