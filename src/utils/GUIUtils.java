package utils;

import model.Transaction;

public class GUIUtils {

    public static String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() > len ? s.substring(0, Math.max(0, len - 2)) + ".." : s;
    }

    public static String getSenderLabel(Transaction tx) {
        return tx.fromAddress != null ? tx.fromAddress : "SISTEMA";
    }

    public static String getSenderLabelTruncated(Transaction tx, int len) {
        String sender = getSenderLabel(tx);
        return truncate(sender, len);
    }

    public static String getReceiverLabelTruncated(Transaction tx, int len) {
        return truncate(tx.toAddress, len);
    }
}
